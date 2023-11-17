package com.xayah.core.util

import android.os.Build
import com.xayah.core.util.SymbolUtil.LF
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

object LogUtil {
    private lateinit var cacheDir: String
    private lateinit var logFile: RandomAccessFile
    private val timestamp: Long = DateUtil.getTimestamp()
    private const val SEPARATOR = "    "
    const val LOG_FILE_Prefix = "log_"
    private const val TAG_COMMON = "Common    "
    const val TAG_SHELL_IN = "SHELL_IN  "
    const val TAG_SHELL_OUT = "SHELL_OUT "
    const val TAG_SHELL_CODE = "SHELL_CODE"

    fun getLogFileName() = "$LOG_FILE_Prefix$timestamp"

    fun initialize(cacheDir: String) = runCatching {
        // Clear empty log files.
        FileUtil.listFilePaths(cacheDir).forEach { path ->
            File(path).apply {
                if (readLines().size <= 4) deleteRecursively()
            }
        }

        File(cacheDir).apply {
            if (exists().not()) mkdirs()
        }
        this.cacheDir = cacheDir
        this.logFile = RandomAccessFile("$cacheDir/${getLogFileName()}", "rw")
        log("Version:    ${BuildConfigUtil.VERSION_NAME}")
        log("Model:      ${Build.MODEL}")
        log("ABIs:       ${Build.SUPPORTED_ABIS.firstOrNull() ?: ""}")
        log("SDK:        ${Build.VERSION.SDK_INT}")
    }

    private fun appendLine(msg: String) = runCatching {
        val bytes = (msg + LF).toByteArray()
        val pos = logFile.channel.size()
        val buffer = logFile.channel.map(FileChannel.MapMode.READ_WRITE, pos, bytes.size.toLong())
        buffer.put(bytes)
    }

    private fun appendWithTimestamp(tag: String, msg: String) = appendLine("${DateUtil.formatTimestamp(DateUtil.getTimestamp())}$SEPARATOR$tag$SEPARATOR$msg")

    fun log(content: () -> Pair<String, String>) {
        appendWithTimestamp(tag = content().first, msg = content().second)
    }

    fun log(msg: String) {
        appendWithTimestamp(tag = TAG_COMMON, msg = msg)
    }
}
