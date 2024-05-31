package com.xayah.core.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider.getUriForFile
import com.xayah.core.common.util.BuildConfigUtil
import com.xayah.core.datastore.readCustomSUFile
import com.xayah.core.util.SymbolUtil.LF
import com.xayah.core.util.SymbolUtil.USD
import com.xayah.core.util.command.BaseUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.PrintWriter
import java.io.RandomAccessFile
import java.io.StringWriter
import java.nio.channels.FileChannel

object LogUtil {
    private lateinit var cacheDir: String
    private lateinit var logFile: RandomAccessFile
    private val timestamp: Long = DateUtil.getTimestamp()
    private const val SEPARATOR = "    "
    private const val LOG_FILE_PREFIX = "log_"
    private const val TAG_COMMON = "Common    "
    const val TAG_SHELL_IN = "SHELL_IN  "
    const val TAG_SHELL_OUT = "SHELL_OUT "
    const val TAG_SHELL_CODE = "SHELL_CODE"

    private fun getLogFileName() = "$LOG_FILE_PREFIX$timestamp.txt"

    fun initialize(context: Context, cacheDir: String) = runCatching {
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
        log("Global Namespace:     ${runBlocking { BaseUtil.readLink("1") }}")
        log("Namespace:            ${runBlocking { BaseUtil.readLink("self") }}")
        log("SU:                   ${runBlocking { BaseUtil.readSuVersion(context.readCustomSUFile().first()) }}")
        log("${USD}PATH:                ${runBlocking { BaseUtil.readVariable("PATH").trim() }}")
        log("${USD}HOME:                ${runBlocking { BaseUtil.readVariable("HOME").trim() }}")
    }

    private fun appendLine(msg: String) = runCatching {
        val bytes = (msg + LF).toByteArray()
        val pos = logFile.channel.size()
        val buffer = logFile.channel.map(FileChannel.MapMode.READ_WRITE, pos, bytes.size.toLong())
        buffer.put(bytes)
    }

    private fun appendWithTimestamp(tag: String, msg: String) = run {
        Log.d(tag, msg)
        appendLine("${DateUtil.formatTimestamp(DateUtil.getTimestamp())}$SEPARATOR$tag$SEPARATOR$msg")
    }

    fun log(content: () -> Pair<String, String>) {
        appendWithTimestamp(tag = content().first, msg = content().second)
    }

    fun log(msg: String) {
        appendWithTimestamp(tag = TAG_COMMON, msg = msg)
    }

    fun shareLog(context: Context, name: String) {
        val sharingLog = File(cacheDir, name)
        val sharingUri =
            getUriForFile(context, "com.xayah.core.provider.FileSharingProvider.${BuildConfigUtil.FLAVOR_feature.lowercase()}", sharingLog)
        val sharingIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, sharingUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(sharingIntent)
    }
}

fun <T> Result<T>.withLog(): Result<T> {
    exceptionOrNull()?.let {
        val stringWriter = StringWriter()
        it.printStackTrace(PrintWriter(stringWriter))
        LogUtil.log { "Exception" to stringWriter.toString() }
    }
    return this
}
