package com.xayah.core.service.util

import android.os.ParcelFileDescriptor
import com.xayah.core.network.client.StreamingCloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.CRC32

data class StreamBackupResult(
    val isSuccess: Boolean,
    val compressedBytes: Long,
    val logs: List<String>,
)

object StreamingBackupPipeline {
    private data class ChunkTask(
        val chunkIndex: Int,
        val chunkFile: File,
        val offset: Long,
        val length: Long,
        val crc32: Long,
    )

    suspend fun streamArchiveToRemote(
        client: StreamingCloudClient,
        rootService: RemoteRootService,
        bufferPool: BoundedBufferPool,
        binDir: String,
        streamCommand: String,
        remoteTargetPath: String,
        onProgress: (uploadedBytes: Long, speedBytesPerSec: Long, bufferUsedBytes: Long, bufferMaxBytes: Long) -> Unit = { _, _, _, _ -> },
    ): StreamBackupResult {
        if (client.continuousStream) {
            return streamContinuousZeroDisk(
                client = client,
                rootService = rootService,
                binDir = binDir,
                streamCommand = streamCommand,
                remoteTargetPath = remoteTargetPath,
                onProgress = onProgress,
            )
        }
        return streamChunkedWithDiskBuffer(
            client = client,
            rootService = rootService,
            bufferPool = bufferPool,
            binDir = binDir,
            streamCommand = streamCommand,
            remoteTargetPath = remoteTargetPath,
            onProgress = onProgress,
        )
    }

    private suspend fun streamContinuousZeroDisk(
        client: StreamingCloudClient,
        rootService: RemoteRootService,
        binDir: String,
        streamCommand: String,
        remoteTargetPath: String,
        onProgress: (uploadedBytes: Long, speedBytesPerSec: Long, bufferUsedBytes: Long, bufferMaxBytes: Long) -> Unit,
    ): StreamBackupResult = withContext(Dispatchers.IO) {
        val sessionId = "cstream_${UUID.randomUUID()}"
        val lastModified = System.currentTimeMillis()
        var rootProcessStarted = false
        var stderrCollected = ""
        try {
            val pfd = rootService.startStreamingCommand(sessionId, binDir, streamCommand)
            rootProcessStarted = true
            val totalCompressed = ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                client.streamContinuous(
                    inputStream = input,
                    targetVirtualPath = remoteTargetPath,
                    lastModified = lastModified,
                    validatePackResult = {
                        val exitCode = runBlocking { rootService.waitStreamingCommand(sessionId) }
                        stderrCollected = runBlocking { rootService.getStreamingCommandStderr(sessionId) }
                        rootProcessStarted = false
                        if (exitCode == 0) {
                            true to ""
                        } else {
                            false to "Pack command exited with code $exitCode: $stderrCollected"
                        }
                    },
                    onProgress = { uploaded, speed ->
                        // 连续流模式下手机磁盘缓冲占用为 0
                        onProgress(uploaded, speed, 0L, 0L)
                    },
                )
            }
            StreamBackupResult(
                isSuccess = true,
                compressedBytes = totalCompressed,
                logs = if (stderrCollected.isNotBlank()) listOf(stderrCollected) else emptyList(),
            )
        } catch (e: Throwable) {
            if (rootProcessStarted) {
                runCatching { rootService.killStreamingCommand(sessionId) }
                runCatching { stderrCollected = rootService.getStreamingCommandStderr(sessionId) }
            }
            val logs = mutableListOf("Continuous stream error: ${e.message ?: e}")
            if (stderrCollected.isNotBlank()) logs.add(stderrCollected)
            StreamBackupResult(
                isSuccess = false,
                compressedBytes = 0L,
                logs = logs,
            )
        }
    }

    private suspend fun streamChunkedWithDiskBuffer(
        client: StreamingCloudClient,
        rootService: RemoteRootService,
        bufferPool: BoundedBufferPool,
        binDir: String,
        streamCommand: String,
        remoteTargetPath: String,
        onProgress: (uploadedBytes: Long, speedBytesPerSec: Long, bufferUsedBytes: Long, bufferMaxBytes: Long) -> Unit,
    ): StreamBackupResult {
        val streamId = UUID.randomUUID().toString()
        val sessionId = "stream_$streamId"
        val lastModified = System.currentTimeMillis()
        val jobDir = File(bufferPool.streamRootDir, streamId).apply { mkdirs() }
        val failure = AtomicReference<Throwable?>(null)
        val totalProducedBytes = AtomicLong(0L)
        val uploadedTotalBytes = AtomicLong(0L)
        val lastSpeedBytesPerSec = AtomicLong(0L)
        var streamStartedOnRemote = false
        var rootProcessStarted = false

        try {
            client.streamBegin(streamId, remoteTargetPath, lastModified)
            streamStartedOnRemote = true

            val pfd = rootService.startStreamingCommand(sessionId, binDir, streamCommand)
            rootProcessStarted = true

            val chunkQueue = Channel<ChunkTask>(Channel.UNLIMITED)
            val chunkSize = bufferPool.effectiveChunkBytes

            coroutineScope {
                val producer = launch(Dispatchers.IO) {
                    val readBuf = ByteArray(256 * 1024)
                    var chunkIndex = 0
                    var totalOffset = 0L
                    try {
                        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                            var firstRead = input.read(readBuf)
                            if (firstRead == -1) {
                                // 0 字节空流：直接结束，后续 STREAM_COMMIT(0)
                                return@use
                            }

                            var pendingLen = firstRead
                            while (pendingLen > 0 && failure.get() == null) {
                                bufferPool.acquire(chunkSize) { failure.get() != null }
                                val chunkFile = File(jobDir, "chunk_%06d.part".format(chunkIndex))
                                val crc = CRC32()
                                var writtenInChunk = 0L
                                var transferredToQueue = false
                                try {
                                    FileOutputStream(chunkFile).use { fos ->
                                        // 先写入上次预读的缓冲
                                        val firstToWrite = minOf(pendingLen.toLong(), chunkSize - writtenInChunk).toInt()
                                        fos.write(readBuf, 0, firstToWrite)
                                        crc.update(readBuf, 0, firstToWrite)
                                        writtenInChunk += firstToWrite
                                        pendingLen = 0

                                        while (writtenInChunk < chunkSize && failure.get() == null) {
                                            val maxRead = minOf(readBuf.size.toLong(), chunkSize - writtenInChunk).toInt()
                                            val n = input.read(readBuf, 0, maxRead)
                                            if (n == -1) {
                                                break
                                            }
                                            if (n > 0) {
                                                fos.write(readBuf, 0, n)
                                                crc.update(readBuf, 0, n)
                                                writtenInChunk += n
                                            }
                                        }
                                        fos.flush()
                                    }
                                    bufferPool.adjustAndReleaseDelta(chunkSize, writtenInChunk)
                                    chunkQueue.send(
                                        ChunkTask(
                                            chunkIndex = chunkIndex,
                                            chunkFile = chunkFile,
                                            offset = totalOffset,
                                            length = writtenInChunk,
                                            crc32 = crc.value,
                                        )
                                    )
                                    transferredToQueue = true
                                    totalOffset += writtenInChunk
                                    totalProducedBytes.set(totalOffset)
                                    chunkIndex++

                                    // 预读下一片首块以判断是否已到流尾 EOF
                                    if (failure.get() == null) {
                                        val nextRead = input.read(readBuf)
                                        pendingLen = if (nextRead == -1) 0 else nextRead
                                    }
                                } finally {
                                    if (!transferredToQueue) {
                                        chunkFile.delete()
                                        bufferPool.release(chunkSize)
                                    }
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        if (e !is CancellationException) {
                            failure.compareAndSet(null, e)
                        }
                        rootService.killStreamingCommand(sessionId)
                    } finally {
                        chunkQueue.close()
                    }
                }

                val consumer = launch(Dispatchers.IO) {
                    try {
                        for (task in chunkQueue) {
                            if (failure.get() != null) {
                                task.chunkFile.delete()
                                bufferPool.release(task.length)
                                continue
                            }
                            var attempts = 0
                            var chunkSuccess = false
                            var lastErr: Exception? = null
                            while (attempts < 5 && !chunkSuccess && failure.get() == null) {
                                attempts++
                                try {
                                    client.streamAppendChunk(
                                        streamId = streamId,
                                        chunkFilePath = task.chunkFile.absolutePath,
                                        chunkOffset = task.offset,
                                        chunkCrc32 = task.crc32,
                                        onProgress = { sent, _ ->
                                            onProgress(
                                                uploadedTotalBytes.get() + sent,
                                                lastSpeedBytesPerSec.get(),
                                                bufferPool.usedBytes.get(),
                                                bufferPool.effectiveMaxBytes,
                                            )
                                        },
                                        onSpeedBytesPerSec = { spd ->
                                            lastSpeedBytesPerSec.set(spd)
                                        },
                                    )
                                    chunkSuccess = true
                                } catch (ioe: IOException) {
                                    lastErr = ioe
                                    if (attempts >= 5) break
                                    // 断线保护：不删除 task.chunkFile，等待 PC 自动重连后 STREAM_RESUME 续传当前分片
                                    runCatching {
                                        client.reconnectTransport()
                                        client.streamResume(streamId, remoteTargetPath, lastModified)
                                    }.onFailure { reconnectErr ->
                                        lastErr = IOException("Reconnect/Resume failed: ${reconnectErr.message}", reconnectErr)
                                    }
                                }
                            }
                            if (!chunkSuccess) {
                                val err = lastErr ?: IOException("Chunk ${task.chunkIndex} upload failed")
                                failure.compareAndSet(null, err)
                                rootService.killStreamingCommand(sessionId)
                                task.chunkFile.delete()
                                bufferPool.release(task.length)
                                break
                            }
                            task.chunkFile.delete()
                            bufferPool.release(task.length)
                            val doneBytes = uploadedTotalBytes.addAndGet(task.length)
                            onProgress(
                                doneBytes,
                                lastSpeedBytesPerSec.get(),
                                bufferPool.usedBytes.get(),
                                bufferPool.effectiveMaxBytes,
                            )
                        }
                    } catch (e: Throwable) {
                        if (e !is CancellationException) {
                            failure.compareAndSet(null, e)
                        }
                        rootService.killStreamingCommand(sessionId)
                    }
                }

                producer.join()
                consumer.join()
            }

            val exitCode = if (rootProcessStarted) rootService.waitStreamingCommand(sessionId) else -1
            val stderrLog = if (rootProcessStarted) rootService.getStreamingCommandStderr(sessionId) else ""
            val err = failure.get()

            return if (err == null && exitCode == 0) {
                val finalSize = totalProducedBytes.get()
                client.streamCommit(streamId, finalSize)
                StreamBackupResult(
                    isSuccess = true,
                    compressedBytes = finalSize,
                    logs = if (stderrLog.isNotBlank()) listOf(stderrLog) else emptyList(),
                )
            } else {
                if (streamStartedOnRemote) {
                    runCatching { client.streamAbort(streamId) }
                }
                val logs = mutableListOf<String>()
                if (err != null) logs.add("Streaming error: ${err.message ?: err}")
                if (exitCode != 0) logs.add("Pack command exited with code $exitCode")
                if (stderrLog.isNotBlank()) logs.add(stderrLog)
                StreamBackupResult(
                    isSuccess = false,
                    compressedBytes = 0L,
                    logs = logs,
                )
            }
        } catch (e: Throwable) {
            if (rootProcessStarted) {
                rootService.killStreamingCommand(sessionId)
            }
            if (streamStartedOnRemote) {
                runCatching { client.streamAbort(streamId) }
            }
            return StreamBackupResult(
                isSuccess = false,
                compressedBytes = 0L,
                logs = listOf("Streaming pipeline exception: ${e.message ?: e}"),
            )
        } finally {
            // 兜底清理该任务目录，确保绝不残留本地垃圾分片
            runCatching { jobDir.deleteRecursively() }
        }
    }
}

