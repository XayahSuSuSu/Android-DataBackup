package com.xayah.databackup.service.archive

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import com.github.luben.zstd.ZstdInputStream
import com.github.luben.zstd.ZstdOutputStream
import com.xayah.databackup.rootservice.CountingOutputStream
import com.xayah.databackup.rootservice.ICallback
import com.xayah.databackup.util.PathHelper.TMP_FIFO_PREFIX
import com.xayah.databackup.util.PathHelper.TMP_STDERR_PREFIX
import com.xayah.databackup.util.PathHelper.TMP_SUFFIX
import com.xayah.libnative.TarWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Streams Archive backups between GNU tar and Zstandard without storing intermediate TAR files.
 * Packs and compresses selected files for backup, and feeds decompressed archives to tar for listing or extraction.
 * Runs blocking I/O on coroutine workers in the root process; callers must serialize operations.
 * Both directions own their FIFO and wait for tar to finish before cleanup, including on stream failure.
 * Callers own archive validation and destination preparation.
 */
internal object ArchiveStreamHelper {
    suspend fun packageAndCompress(
        outputPath: String,
        workDir: File,
        callback: ICallback? = null,
        vararg inputArgs: String,
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val operationContext = currentCoroutineContext()
        val result = runCatching {
            withFifo(workDir) { fifo ->
                // Open the read end before tar opens the FIFO for writing.
                // Nonblocking reads let us check whether tar has finished when no data is available.
                val reader = Os.open(fifo.path, OsConstants.O_RDONLY or OsConstants.O_NONBLOCK, 0)
                try {
                    processTar(fifo.path, workDir, arrayOf("tar", "-cpf", "-", *inputArgs)) { isTarFinished ->
                        val input = FifoInputStream(reader, isTarFinished, operationContext)
                        val compressionResult = runCatching { compress(input, outputPath, 1, callback) }
                        // Drain the FIFO even if compression fails, so tar can finish writing.
                        // Closing our reader is insufficient: tar inherits a read descriptor and may block on a full FIFO.
                        val drainResult = runCatching {
                            val remainingInput = FifoInputStream(reader, isTarFinished)
                            val buffer = ByteArray(64 * 1024)
                            while (remainingInput.read(buffer) >= 0) {
                                // Discard the remaining TAR bytes after a failed output write.
                            }
                        }
                        drainResult.onFailure { error ->
                            compressionResult.exceptionOrNull()?.addSuppressed(error) ?: throw error
                        }
                        compressionResult.getOrThrow()
                    }
                } finally {
                    Os.close(reader)
                }
            }
        }
        result.onFailure { error ->
            runCatching {
                val output = File(outputPath)
                check(!output.exists() || output.delete()) { "Failed to remove incomplete archive" }
            }.onFailure { error.addSuppressed(it) }
        }
        result.getOrThrow()
    }

    private fun compress(input: InputStream, outputPath: String, level: Int, callback: ICallback?) {
        FileOutputStream(outputPath).use { output ->
            CountingOutputStream(
                source = output,
                onProgress = if (callback != null) {
                    { bytesWritten, speed -> callback.onProgress(bytesWritten, speed, 0f) }
                } else {
                    null
                },
            ).use { countingOutput ->
                ZstdOutputStream(countingOutput, level).use { compressedOutput ->
                    compressedOutput.setWorkers(Runtime.getRuntime().availableProcessors())
                    input.copyTo(compressedOutput)
                }
            }
        }
    }

    suspend fun decompressAndProcessTar(archive: File, workDir: File, stdoutPath: String, vararg args: String) = withContext(Dispatchers.IO) {
        val operationContext = currentCoroutineContext()
        withFifo(workDir) { fifo ->
            // Open the source before starting tar so an input-open failure cannot strand a FIFO reader.
            ZstdInputStream(archive.inputStream().buffered()).use { input ->
                val (exitCode, diagnostics) = processTar(
                    stdoutPath = stdoutPath,
                    workDir = workDir,
                    args = arrayOf("tar", "--file=${fifo.path}", "--read-full-records", *args),
                ) { isTarFinished ->
                    var writer: FileDescriptor? = null
                    val streamResult = runCatching {
                        writer = openWriter(fifo, isTarFinished, operationContext)
                        val buffer = ByteArray(64 * 1024)
                        var hasReader = writer != null
                        while (true) {
                            operationContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) {
                                break
                            }
                            if (hasReader) {
                                hasReader = writeBuffer(checkNotNull(writer), buffer, count, isTarFinished, operationContext)
                            }
                            // Tar can stop at its end markers before Zstandard EOF. Drain the remaining
                            // input even then, so trailing compressed data/checksum errors cannot be hidden.
                        }
                    }
                    val closeResult = runCatching {
                        if (writer != null) {
                            Os.close(writer)
                        } else if (!isTarFinished()) {
                            // Release a reader waiting in open if connecting the producer failed.
                            openWriter(fifo, isTarFinished, currentCoroutineContext())?.let { Os.close(it) }
                        }
                    }
                    streamResult.getOrThrow()
                    closeResult.getOrThrow()
                }
                // Treat path rewriting and other warnings as failures, as in the validated archive path.
                check(exitCode == 0 && diagnostics.isEmpty()) { "Archive tar operation failed (exit code $exitCode)" }
            }
        }
    }

    private suspend fun processTar(
        stdoutPath: String,
        workDir: File,
        args: Array<String>,
        processStream: suspend (isTarFinished: () -> Boolean) -> Unit,
    ): Pair<Int, String> {
        val operationContext = currentCoroutineContext()
        // JNI cannot be canceled. Keep tar alive until the stream has closed or drained the FIFO,
        // and await its actual completion before propagating cancellation or releasing resources.
        return withTempFile(workDir, TMP_STDERR_PREFIX) { stderrFile ->
            withContext(NonCancellable) {
                coroutineScope {
                    val tarTask = async(Dispatchers.IO) { runCatching { TarWrapper.callCli(stdoutPath, stderrFile.path, args) } }
                    val streamResult = runCatching { processStream { tarTask.isCompleted } }
                    val tarResult = tarTask.await()
                    tarResult.onFailure { error -> streamResult.exceptionOrNull()?.addSuppressed(error) ?: throw error }
                    streamResult.getOrThrow()
                    operationContext.ensureActive()
                    tarResult.getOrThrow() to stderrFile.readText()
                }
            }
        }
    }

    private class FifoInputStream(
        private val mReader: FileDescriptor,
        private val mIsTarFinished: () -> Boolean,
        private val mOperationContext: CoroutineContext? = null,
    ) : InputStream() {
        override fun read(): Int {
            val byte = ByteArray(1)
            return if (read(byte) < 0) -1 else byte[0].toInt() and 255
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) {
                return 0
            }
            val pollFd = StructPollfd().apply {
                fd = mReader
                events = OsConstants.POLLIN.toShort()
            }
            while (true) {
                mOperationContext?.ensureActive()
                try {
                    val count = Os.read(mReader, buffer, offset, length)
                    if (count > 0) {
                        return count
                    }
                    if (mIsTarFinished()) {
                        return -1
                    }
                    // Before tar connects, read returns EOF. Do not treat that as the end of input.
                    Os.poll(arrayOf(pollFd), 100)
                } catch (error: ErrnoException) {
                    when (error.errno) {
                        OsConstants.EAGAIN -> Os.poll(arrayOf(pollFd), 100)
                        OsConstants.EINTR -> continue
                        else -> throw error
                    }
                }
            }
        }
    }

    private suspend fun openWriter(
        fifo: File,
        isTarFinished: () -> Boolean,
        operationContext: CoroutineContext,
    ): FileDescriptor? {
        while (!isTarFinished()) {
            operationContext.ensureActive()
            try {
                // Never block in open: tar may fail before it opens the read end.
                return Os.open(fifo.path, OsConstants.O_WRONLY or OsConstants.O_NONBLOCK, 0)
            } catch (error: ErrnoException) {
                if (error.errno != OsConstants.ENXIO) {
                    throw error
                }
                delay(50.milliseconds)
            }
        }
        return null
    }

    private fun writeBuffer(
        writer: FileDescriptor,
        buffer: ByteArray,
        count: Int,
        isTarFinished: () -> Boolean,
        operationContext: CoroutineContext,
    ): Boolean {
        val pollFd = StructPollfd().apply {
            fd = writer
            events = OsConstants.POLLOUT.toShort()
        }
        var offset = 0
        while (offset < count) {
            operationContext.ensureActive()
            if (isTarFinished()) {
                return false
            }
            try {
                offset += Os.write(writer, buffer, offset, count - offset)
            } catch (error: ErrnoException) {
                when (error.errno) {
                    OsConstants.EPIPE -> return false
                    OsConstants.EAGAIN -> Os.poll(arrayOf(pollFd), 100)
                    OsConstants.EINTR -> continue
                    else -> throw error
                }
            }
        }
        return true
    }

    private inline fun <T> withFifo(directory: File, block: (File) -> T): T = withTempFile(directory, TMP_FIFO_PREFIX) { fifo ->
        check(fifo.delete()) { "Cannot prepare archive FIFO" }
        Os.mkfifo(fifo.path, 384) // 0600
        block(fifo)
    }

    private inline fun <T> withTempFile(directory: File, prefix: String, block: (File) -> T): T {
        val file = File.createTempFile(prefix, TMP_SUFFIX, directory)
        val result = runCatching { block(file) }
        runCatching { check(file.delete()) { "Failed to remove temporary archive file" } }.onFailure { cleanupError ->
            result.exceptionOrNull()?.addSuppressed(cleanupError) ?: throw cleanupError
        }
        return result.getOrThrow()
    }
}
