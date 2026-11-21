package com.xayah.core.hfx

import top.weixiansen574.hybridfilexfer.core.ContinuousStreamBlock
import top.weixiansen574.hybridfilexfer.core.ControllerIdentifiers
import top.weixiansen574.hybridfilexfer.core.FileBlock
import top.weixiansen574.hybridfilexfer.core.HFXService
import top.weixiansen574.hybridfilexfer.core.ReadFileCall
import top.weixiansen574.hybridfilexfer.core.SpeedMonitorThread
import top.weixiansen574.hybridfilexfer.core.TransferConnection
import top.weixiansen574.hybridfilexfer.core.TransferIdentifiers
import top.weixiansen574.hybridfilexfer.core.WriteFileCall
import top.weixiansen574.hybridfilexfer.core.bean.Directory
import top.weixiansen574.hybridfilexfer.core.bean.RemoteFile
import top.weixiansen574.hybridfilexfer.core.bean.ServerNetInterface
import top.weixiansen574.hybridfilexfer.core.bean.TrafficInfo
import top.weixiansen574.hybridfilexfer.core.callback.TransferFileCallback
import top.weixiansen574.nio.DataByteChannel
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.CRC32
import kotlin.concurrent.withLock

data class RemoteStat(
    val exists: Boolean,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
)

class HFXServerEngine(
    private val bufferAllocator: BufferAllocator = JvmDirectBufferAllocator,
) : HFXService() {

    private val controlLock = ReentrantLock()
    private val allAllocatedBuffers = mutableListOf<ByteBuffer>()
    private var serverSocketChannel: ServerSocketChannel? = null

    @Volatile
    private var connected: Boolean = false

    fun isConnected(): Boolean = connected && ctChannel?.isOpen == true

    fun startAndWaitClient(
        port: Int = 5740,
        interfaces: List<ServerNetInterface>,
        localBufferCount: Int = 64,
        remoteBufferCount: Int = 128,
        timeoutMs: Long = 30_000L,
        socketBufferMb: Int = 4,
    ) = controlLock.withLock {
        forceCloseLocked()
        if (interfaces.isEmpty()) {
            throw IOException("至少需要启用一条传输通道（如 USB_ADB 或 Wi-Fi）")
        }

        val ssc = ServerSocketChannel.open()
        serverSocketChannel = ssc
        ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true)
        ssc.bind(InetSocketAddress(port))

        val deadline = System.currentTimeMillis() + timeoutMs
        try {
            while (true) {
                val remaining = deadline - System.currentTimeMillis()
                if (remaining <= 0) {
                    throw IOException("等待电脑接收端连接超时（${timeoutMs / 1000}秒），请确认电脑已启动 pc-receiver 并连接数据线/同一 Wi-Fi")
                }
                val ctrlSocket = acceptWithTimeout(ssc, remaining)
                    ?: throw IOException("等待电脑接收端连接超时（${timeoutMs / 1000}秒），请确认电脑已启动 pc-receiver 并连接数据线/同一 Wi-Fi")

                val ct = DataByteChannel(ctrlSocket)
                ctChannel = ct

                val headerBytes = ByteArray(4)
                try {
                    ct.readFully(headerBytes)
                } catch (_: IOException) {
                    runCatching { ct.close() }
                    continue
                }
                if (String(headerBytes, StandardCharsets.UTF_8) != CLIENT_HEADER) {
                    runCatching { ct.write("protocol error\n".toByteArray(StandardCharsets.UTF_8)) }
                    runCatching { ct.close() }
                    continue
                }

                val clientVer = ct.readInt()
                if (clientVer != VERSION_CODE) {
                    ct.writeBoolean(false)
                    ct.writeInt(VERSION_CODE)
                    runCatching { ct.close() }
                    throw IOException("协议版本不一致：手机端 V$VERSION_CODE，电脑端 V$clientVer，请更新电脑端 pc-receiver")
                }
                ct.writeBoolean(true)

                // 下发网卡列表
                ct.writeInt(interfaces.size)
                for (ni in interfaces) {
                    ct.writeUTF(ni.name)
                    val addr = ni.address.address
                    ct.writeByte(addr.size)
                    ct.write(addr)
                    val bind = ni.clientBindAddress?.address
                    if (bind == null) {
                        ct.writeByte(0)
                    } else {
                        ct.writeByte(bind.size)
                        ct.write(bind)
                    }
                }

                // 逐条建数据通道
                val conns = ArrayList<TransferConnection>(interfaces.size)
                connections = conns
                var allChannelsOk = true
                for (i in interfaces.indices) {
                    val ok = ct.readBoolean()
                    val name = ct.readUTF()
                    if (!ok) {
                        runCatching { ct.writeBoolean(false) }
                        allChannelsOk = false
                        break
                    }
                    val dc = acceptWithTimeout(ssc, 10_000L)
                        ?: throw IOException("数据通道 [$name] 建链超时")
                    tuneDataSocket(dc, socketBufferMb)
                    conns.add(TransferConnection(name, DataByteChannel(dc)))
                    ct.writeBoolean(true)
                }
                if (!allChannelsOk) {
                    closeSocketsOnlyLocked()
                    continue
                }

                // 缓冲池协商
                ct.writeInt(remoteBufferCount)
                if (!ct.readBoolean()) {
                    throw IOException("电脑端内存不足，无法分配 ${remoteBufferCount}MB 接收缓冲")
                }
                for (i in 0 until localBufferCount) {
                    val buf = bufferAllocator.allocate(FileBlock.BLOCK_SIZE)
                    if (buf == null) {
                        ct.writeBoolean(false)
                        throw IOException("手机端内存不足，仅分配了 $i/$localBufferCount MB 传输缓冲")
                    }
                    allAllocatedBuffers.add(buf)
                    buffers.add(buf)
                }
                ct.writeBoolean(true)

                // 读取对端文件系统与虚拟根路径
                ct.readInt()
                ct.readUTF()

                // 握手完成后关闭监听 Socket，释放端口占用
                runCatching { ssc.close() }
                serverSocketChannel = null
                connected = true
                return@withLock
            }
        } catch (e: Exception) {
            forceCloseLocked()
            throw e
        }
    }

    private fun acceptWithTimeout(ssc: ServerSocketChannel, timeoutMs: Long): SocketChannel? {
        ssc.configureBlocking(false)
        Selector.open().use { selector ->
            ssc.register(selector, SelectionKey.OP_ACCEPT)
            val ready = selector.select(timeoutMs.coerceAtLeast(1L))
            if (ready <= 0) return null
            val client = ssc.accept() ?: return null
            client.configureBlocking(true)
            return client
        }
    }

    fun stat(virtualPath: String): RemoteStat = controlLock.withLock {
        ensureConnected()
        val ct = ctChannel!!
        ct.writeShort(ControllerIdentifiers.STAT_FILE.toInt())
        ct.writeUTF(virtualPath)
        val exists = ct.readBoolean()
        val isDir = ct.readBoolean()
        val size = ct.readLong()
        val lastMod = ct.readLong()
        RemoteStat(exists, isDir, size, lastMod)
    }

    fun listFiles(virtualPath: String): List<RemoteFile> = controlLock.withLock {
        ensureConnected()
        val ct = ctChannel!!
        ct.writeShort(ControllerIdentifiers.LIST_FILES.toInt())
        ct.writeUTF(virtualPath)
        val count = ct.readInt()
        if (count < 0) return@withLock emptyList()
        val list = ArrayList<RemoteFile>(count)
        for (i in 0 until count) {
            val name = ct.readUTF()
            val path = ct.readUTF()
            val lastMod = ct.readLong()
            val size = ct.readLong()
            val isDir = ct.readBoolean()
            list.add(RemoteFile(name, path, lastMod, size, isDir))
        }
        list
    }

    fun mkdir(parentVirtualPath: String, childName: String = ""): Boolean = controlLock.withLock {
        ensureConnected()
        val ct = ctChannel!!
        ct.writeShort(ControllerIdentifiers.MKDIR.toInt())
        ct.writeUTF(parentVirtualPath)
        ct.writeUTF(childName)
        ct.readBoolean()
    }

    fun delete(virtualPath: String): Boolean = controlLock.withLock {
        ensureConnected()
        val ct = ctChannel!!
        ct.writeShort(ControllerIdentifiers.DELETE_FILE.toInt())
        ct.writeUTF(virtualPath)
        ct.readBoolean()
    }

    fun rename(srcVirtualPath: String, dstVirtualPath: String): Boolean = controlLock.withLock {
        ensureConnected()
        val ct = ctChannel!!
        ct.writeShort(ControllerIdentifiers.RENAME_FILE.toInt())
        ct.writeUTF(srcVirtualPath)
        ct.writeUTF(dstVirtualPath)
        ct.readBoolean()
    }

    fun uploadFile(
        localFile: File,
        remoteVirtualDir: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
        onSpeed: (List<TrafficInfo>) -> Unit = {},
    ) = controlLock.withLock {
        ensureConnected()
        val ct = ctChannel!!
        ct.writeShort(ControllerIdentifiers.REQUEST_RECEIVE.toInt())
        val localDir = Directory(localFile.parentFile?.absolutePath ?: "/", Directory.FILE_SYSTEM_UNIX)
        val remoteDir = Directory(remoteVirtualDir, Directory.FILE_SYSTEM_UNIX)
        var errorMsg: String? = null
        val cb = object : TransferFileCallback {
            override fun onFileUploading(iName: String, path: String, targetSize: Long, totalSize: Long) {
                onProgress(targetSize, totalSize)
            }

            override fun onFileDownloading(iName: String, path: String, targetSize: Long, totalSize: Long) {}
            override fun onSpeedInfo(trafficInfoList: List<TrafficInfo>) = onSpeed(trafficInfoList)
            override fun onChannelComplete(iName: String, traffic: Long, time: Long) {}
            override fun onChannelError(iName: String, errorType: Int, message: String?) {
                if (errorMsg == null) errorMsg = "Channel $iName error ($errorType): $message"
            }

            override fun onReadFileError(message: String?) {
                errorMsg = "Read error: $message"
            }

            override fun onWriteFileError(message: String?) {
                errorMsg = "PC write error: $message"
            }

            override fun onComplete(isUpload: Boolean, traffic: Long, time: Long) {
                onProgress(localFile.length(), localFile.length())
            }

            override fun onIncomplete() {
                connected = false
                errorMsg = "Transfer interrupted"
            }
        }
        val ok = sendFiles(listOf(RemoteFile(localFile)), localDir, remoteDir, cb)
        if (!ok || errorMsg != null) {
            if (!ok) connected = false
            throw IOException(errorMsg ?: "Upload failed: ${localFile.name}")
        }
    }

    fun downloadFile(
        remoteVirtualFile: String,
        localDstDir: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
        onSpeed: (List<TrafficInfo>) -> Unit = {},
    ) = controlLock.withLock {
        ensureConnected()
        if (!localDstDir.exists()) localDstDir.mkdirs()
        val parentVirtual = remoteVirtualFile.substringBeforeLast('/', "/")
            .ifEmpty { "/" }
        val ct = ctChannel!!
        ct.writeShort(ControllerIdentifiers.REQUEST_SEND.toInt())
        ct.writeInt(1)
        ct.writeUTF(remoteVirtualFile)
        ct.writeUTF(localDstDir.absolutePath)
        ct.writeInt(Directory.FILE_SYSTEM_UNIX)
        ct.writeUTF(parentVirtual)

        var errorMsg: String? = null
        val cb = object : TransferFileCallback {
            override fun onFileUploading(iName: String, path: String, targetSize: Long, totalSize: Long) {}
            override fun onFileDownloading(iName: String, path: String, targetSize: Long, totalSize: Long) {
                onProgress(targetSize, totalSize)
            }

            override fun onSpeedInfo(trafficInfoList: List<TrafficInfo>) = onSpeed(trafficInfoList)
            override fun onChannelComplete(iName: String, traffic: Long, time: Long) {}
            override fun onChannelError(iName: String, errorType: Int, message: String?) {
                if (errorMsg == null) errorMsg = "Channel $iName error ($errorType): $message"
            }

            override fun onReadFileError(message: String?) {
                errorMsg = "PC read error: $message"
            }

            override fun onWriteFileError(message: String?) {
                errorMsg = "Phone write error: $message"
            }

            override fun onComplete(isUpload: Boolean, traffic: Long, time: Long) {
                onProgress(traffic, traffic)
            }

            override fun onIncomplete() {
                connected = false
                errorMsg = "Download interrupted"
            }
        }
        val ok = receiveFiles(cb)
        if (!ok || errorMsg != null) {
            if (!ok) connected = false
            throw IOException(errorMsg ?: "Download failed: $remoteVirtualFile")
        }
    }

    fun streamBegin(streamId: String, targetVirtualPath: String, lastModified: Long) = controlLock.withLock {
        ensureConnected()
        val ct = ctChannel!!
        ct.writeShort(ControllerIdentifiers.STREAM_BEGIN.toInt())
        ct.writeUTF(streamId)
        ct.writeUTF(targetVirtualPath)
        ct.writeLong(lastModified)
        if (!ct.readBoolean()) {
            throw IOException("STREAM_BEGIN failed: ${ct.readUTF()}")
        }
    }

    fun streamResume(streamId: String, targetVirtualPath: String, lastModified: Long): Long = controlLock.withLock {
        ensureConnected()
        val ct = ctChannel!!
        ct.writeShort(ControllerIdentifiers.STREAM_RESUME.toInt())
        ct.writeUTF(streamId)
        ct.writeUTF(targetVirtualPath)
        ct.writeLong(lastModified)
        val ok = ct.readBoolean()
        val size = ct.readLong()
        if (!ok) {
            throw IOException("STREAM_RESUME failed: ${ct.readUTF()}")
        }
        size
    }

    fun streamAppendChunk(
        streamId: String,
        chunkFile: File,
        chunkOffset: Long,
        chunkCrc32: Long,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
        onSpeed: (List<TrafficInfo>) -> Unit = {},
    ) = controlLock.withLock {
        ensureConnected()
        val chunkLen = chunkFile.length()
        val ct = ctChannel!!
        ct.writeShort(ControllerIdentifiers.STREAM_APPEND_CHUNK.toInt())
        ct.writeUTF(streamId)
        ct.writeLong(chunkOffset)
        ct.writeLong(chunkLen)
        ct.writeLong(chunkCrc32)

        var errorMsg: String? = null
        val readCall = ChunkReadFileCall(buffers, chunkFile, streamId, chunkLen, connections.size)
        val cb = object : TransferFileCallback {
            override fun onFileUploading(iName: String, path: String, targetSize: Long, totalSize: Long) {
                onProgress(targetSize, totalSize)
            }

            override fun onFileDownloading(iName: String, path: String, targetSize: Long, totalSize: Long) {}
            override fun onSpeedInfo(trafficInfoList: List<TrafficInfo>) = onSpeed(trafficInfoList)
            override fun onChannelComplete(iName: String, traffic: Long, time: Long) {}
            override fun onChannelError(iName: String, errorType: Int, message: String?) {
                if (errorMsg == null) errorMsg = "Channel $iName error ($errorType): $message"
            }

            override fun onReadFileError(message: String?) {
                errorMsg = "Phone read chunk error: $message"
            }

            override fun onWriteFileError(message: String?) {
                errorMsg = "PC write/CRC32 error: $message"
            }

            override fun onComplete(isUpload: Boolean, traffic: Long, time: Long) {
                onProgress(chunkLen, chunkLen)
            }

            override fun onIncomplete() {
                connected = false
                errorMsg = "Chunk transfer interrupted"
            }
        }
        val ok = sendWithReadCall(readCall, cb)
        if (!ok || errorMsg != null) {
            if (!ok) connected = false
            throw IOException(errorMsg ?: "STREAM_APPEND_CHUNK failed at offset $chunkOffset")
        }
    }

    fun streamCommit(streamId: String, expectedTotalSize: Long) = controlLock.withLock {
        ensureConnected()
        val ct = ctChannel!!
        ct.writeShort(ControllerIdentifiers.STREAM_COMMIT.toInt())
        ct.writeUTF(streamId)
        ct.writeLong(expectedTotalSize)
        if (!ct.readBoolean()) {
            throw IOException("STREAM_COMMIT failed: ${ct.readUTF()}")
        }
    }

    private fun tuneDataSocket(sc: SocketChannel, socketBufferMb: Int) {
        val bufBytes = socketBufferMb.coerceIn(1, 16) * 1024 * 1024
        runCatching { sc.setOption(StandardSocketOptions.TCP_NODELAY, true) }
        runCatching { sc.setOption(StandardSocketOptions.SO_SNDBUF, bufBytes) }
        runCatching { sc.setOption(StandardSocketOptions.SO_RCVBUF, bufBytes) }
    }

    fun streamContinuousFromInputStream(
        inputStream: InputStream,
        targetVirtualPath: String,
        lastModified: Long,
        verifyInlineCrc32: Boolean,
        validatePackResult: () -> Pair<Boolean, String>,
        onProgress: (uploadedBytes: Long, speedBytesPerSec: Long) -> Unit = { _, _ -> },
    ): Long = controlLock.withLock {
        ensureConnected()
        val ct = ctChannel!!
        val conns = connections!!
        ct.writeShort(ControllerIdentifiers.STREAM_CONTINUOUS.toInt())
        ct.writeUTF(targetVirtualPath)
        ct.writeLong(lastModified)
        ct.writeBoolean(verifyInlineCrc32)

        if (!ct.readBoolean()) {
            throw IOException("PC rejected continuous stream: ${ct.readUTF()}")
        }

        val queue = LinkedBlockingDeque<ContinuousStreamBlock>()
        val cancelled = AtomicBoolean(false)
        val totalSentBytes = AtomicLong(0L)
        val totalProducedBytes = AtomicLong(0L)
        val lastSpeed = AtomicLong(0L)

        fun drainAndRecycleQueue() {
            while (true) {
                val b = queue.poll() ?: break
                if (b.data != null) buffers.add(b.data)
            }
        }

        val speedCb = object : TransferFileCallback {
            override fun onFileUploading(iName: String, path: String, targetSize: Long, totalSize: Long) {}
            override fun onFileDownloading(iName: String, path: String, targetSize: Long, totalSize: Long) {}
            override fun onSpeedInfo(trafficInfoList: List<TrafficInfo>) {
                val sum = trafficInfoList.sumOf { it.uploadTraffic }
                lastSpeed.set(sum)
                onProgress(totalSentBytes.get(), sum)
            }
            override fun onChannelComplete(iName: String, traffic: Long, time: Long) {}
            override fun onChannelError(iName: String, errorType: Int, message: String?) {}
            override fun onReadFileError(message: String?) {}
            override fun onWriteFileError(message: String?) {}
            override fun onComplete(isUpload: Boolean, traffic: Long, time: Long) {}
            override fun onIncomplete() {}
        }
        val speedMonitor = SpeedMonitorThread(conns, speedCb).apply {
            name = "SpeedMonitor_CS"
            start()
        }

        // 生产者线程：直接从 InputStream 读入 1MB DirectByteBuffer（零磁盘 IO + 内存池自然反压）
        var readerThread: Thread? = null
        val readerTask = FutureTask(Callable<Void> {
            readerThread = Thread.currentThread()
            val tmpArray = ByteArray(256 * 1024)
            val crc32 = if (verifyInlineCrc32) CRC32() else null
            var offset = 0L
            try {
                while (!cancelled.get()) {
                    val firstRead = inputStream.read(tmpArray)
                    if (firstRead == -1) break

                    val buf = buffers.take()
                    if (cancelled.get()) {
                        buffers.add(buf)
                        break
                    }
                    var enqueued = false
                    try {
                        buf.clear()
                        buf.put(tmpArray, 0, firstRead)
                        while (buf.hasRemaining() && !cancelled.get()) {
                            val toRead = minOf(tmpArray.size, buf.remaining())
                            val n = inputStream.read(tmpArray, 0, toRead)
                            if (n == -1) break
                            if (n > 0) buf.put(tmpArray, 0, n)
                        }
                        val blkLen = buf.position()
                        val crcVal = if (crc32 != null) {
                            val dup = buf.duplicate()
                            dup.flip()
                            crc32.reset()
                            crc32.update(dup)
                            crc32.value.toInt()
                        } else {
                            0
                        }
                        queue.add(ContinuousStreamBlock(offset, blkLen, crcVal, buf))
                        enqueued = true
                        offset += blkLen
                        totalProducedBytes.set(offset)
                    } finally {
                        if (!enqueued) buffers.add(buf)
                    }
                }
                if (!cancelled.get()) {
                    repeat(conns.size) { queue.add(ContinuousStreamBlock.END_POINT) }
                }
            } catch (e: Exception) {
                if (!cancelled.get()) {
                    cancelled.set(true)
                    drainAndRecycleQueue()
                    repeat(conns.size) { queue.addFirst(ContinuousStreamBlock.READ_ERROR) }
                    throw e
                }
            }
            null
        })
        Thread(readerTask, "StreamRead_CS").start()

        // 消费者线程组：每条通道 1 线程，18 字节合并帧头 + 1MB 载荷（仅 2 次 writeFully）
        val sendTasks = ArrayList<FutureTask<Void>>(conns.size)
        for (conn in conns) {
            conn.resetTotalTrafficInfo()
            val task = FutureTask(Callable<Void> {
                val ch = conn.channel
                val headerBuf = ByteBuffer.allocateDirect(18).order(ByteOrder.BIG_ENDIAN)
                var blk: ContinuousStreamBlock? = null
                var recycled = false
                try {
                    while (true) {
                        blk = queue.take()
                        recycled = false
                        if (blk.offset < 0L) {
                            when {
                                blk === ContinuousStreamBlock.END_POINT -> ch.writeShort(TransferIdentifiers.EOF.toInt())
                                blk === ContinuousStreamBlock.READ_ERROR -> ch.writeShort(TransferIdentifiers.END_OF_READ_ERROR.toInt())
                                blk === ContinuousStreamBlock.WRITE_ERROR -> ch.writeShort(TransferIdentifiers.END_OF_WRITE_ERROR.toInt())
                                else -> ch.writeShort(TransferIdentifiers.END_OF_INTERRUPTED.toInt())
                            }
                            break
                        }
                        headerBuf.clear()
                        headerBuf.putShort(TransferIdentifiers.STREAM_BLOCK)
                        headerBuf.putLong(blk.offset)
                        headerBuf.putInt(blk.length)
                        headerBuf.putInt(blk.crc32)
                        headerBuf.flip()
                        ch.writeFully(headerBuf)

                        blk.data.flip()
                        ch.writeFully(blk.data)
                        val len = blk.length
                        buffers.add(blk.data)
                        recycled = true
                        conn.addUploadedBytes(len.toLong())
                        val sent = totalSentBytes.addAndGet(len.toLong())
                        if (sent % (16L * 1024L * 1024L) < len) {
                            onProgress(sent, lastSpeed.get())
                        }
                    }
                } catch (e: Exception) {
                    if (!recycled && blk != null && blk.data != null) {
                        buffers.add(blk.data)
                    }
                    cancelled.set(true)
                    readerThread?.interrupt()
                    drainAndRecycleQueue()
                    repeat(conns.size) { queue.addFirst(ContinuousStreamBlock.INTERRUPT) }
                    throw e
                }
                null
            })
            sendTasks.add(task)
            Thread(task, "UL_CS_${conn.iName}").start()
        }

        // 阶段 1：等待 PC 端接收落盘状态
        val pcWriteOk = try {
            ct.readBoolean()
        } catch (e: IOException) {
            speedMonitor.cancel()
            cancelled.set(true)
            readerThread?.interrupt()
            drainAndRecycleQueue()
            repeat(conns.size) { queue.addFirst(ContinuousStreamBlock.INTERRUPT) }
            awaitContinuousTasks(sendTasks, readerTask)
            connected = false
            throw IOException("连续流传输连接中断: ${e.message}", e)
        }
        speedMonitor.cancel()

        if (!pcWriteOk) {
            val errMsg = ct.readUTF()
            cancelled.set(true)
            readerThread?.interrupt()
            drainAndRecycleQueue()
            repeat(conns.size) { queue.addFirst(ContinuousStreamBlock.WRITE_ERROR) }
            awaitContinuousTasks(sendTasks, readerTask)
            throw IOException("电脑端写入或 CRC32 校验失败: $errMsg")
        }

        for (task in sendTasks) {
            try {
                task.get()
            } catch (e: Exception) {
                cancelled.set(true)
                readerThread?.interrupt()
                awaitContinuousTasks(sendTasks, readerTask)
                connected = false
                throw IOException("传输通道异常: ${e.cause?.message ?: e.message}", e)
            }
        }

        var readErr: String? = null
        try {
            readerTask.get()
        } catch (e: Exception) {
            readErr = e.cause?.message ?: e.toString()
        }

        // 阶段 2：核对 Root 打包命令退出码并通知 PC 提交或中止
        val (packOk, packMsg) = if (readErr == null) {
            validatePackResult()
        } else {
            false to readErr
        }
        val finalTotalSize = totalProducedBytes.get()
        ct.writeBoolean(packOk)
        ct.writeLong(finalTotalSize)
        if (!packOk) {
            ct.writeUTF(packMsg)
            runCatching { ct.readBoolean() }
            throw IOException("手机端打包失败: $packMsg")
        }

        // 阶段 3：等待 PC 原子重命名完成确认
        val commitOk = ct.readBoolean()
        if (!commitOk) {
            throw IOException("电脑端提交归档失败: ${ct.readUTF()}")
        }
        onProgress(finalTotalSize, lastSpeed.get())
        finalTotalSize
    }

    private fun awaitContinuousTasks(sendTasks: List<FutureTask<Void>>, readerTask: FutureTask<Void>) {
        runCatching { readerTask.get(3, TimeUnit.SECONDS) }.onFailure { readerTask.cancel(true) }
        for (t in sendTasks) {
            runCatching { t.get(3, TimeUnit.SECONDS) }.onFailure { t.cancel(true) }
        }
    }

    fun streamAbort(streamId: String) = controlLock.withLock {
        if (!isConnected()) return@withLock
        val ct = ctChannel!!
        runCatching {
            ct.writeShort(ControllerIdentifiers.STREAM_ABORT.toInt())
            ct.writeUTF(streamId)
            ct.readBoolean()
        }
    }

    fun detach() = controlLock.withLock {
        if (connected && ctChannel?.isOpen == true) {
            runCatching {
                ctChannel!!.writeShort(ControllerIdentifiers.DETACH.toInt())
                ctChannel!!.readBoolean()
            }
        }
        forceCloseLocked()
    }

    fun forceClose() = controlLock.withLock {
        forceCloseLocked()
    }

    private fun ensureConnected() {
        if (!isConnected()) {
            throw IOException("双轨快传未连接或已断开")
        }
    }

    private fun closeSocketsOnlyLocked() {
        runCatching { ctChannel?.close() }
        ctChannel = null
        connections?.forEach { runCatching { it.close() } }
        connections = null
    }

    private fun forceCloseLocked() {
        connected = false
        closeSocketsOnlyLocked()
        runCatching { serverSocketChannel?.close() }
        serverSocketChannel = null
        buffers.clear()
        for (buf in allAllocatedBuffers) {
            runCatching { bufferAllocator.free(buf) }
        }
        allAllocatedBuffers.clear()
    }

    override fun createWriteFileCall(buffers: LinkedBlockingDeque<ByteBuffer>, dequeCount: Int): WriteFileCall {
        return LocalWriteFileCall(buffers, dequeCount)
    }

    override fun createReadFileCall(
        buffers: LinkedBlockingDeque<ByteBuffer>,
        files: List<RemoteFile>,
        localDir: Directory,
        remoteDir: Directory,
        operateThreadCount: Int,
    ): ReadFileCall {
        return LocalReadFileCall(buffers, files, localDir, remoteDir, operateThreadCount)
    }
}

