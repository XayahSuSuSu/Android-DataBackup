package com.xayah.databackup.ui.activity.crash

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.util.command.toLineString
import com.xayah.librootservice.util.ExceptionUtil
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CrashHandler(private val mContext: Context) : Thread.UncaughtExceptionHandler {
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private var crashInfo = ""

    fun initialize() {
        ExceptionUtil.tryOn {
            if (mContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) {
                mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler(this)
            }
        }
    }

    @ExperimentalMaterial3Api
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (!handleException(throwable) && mDefaultHandler != null) {
            mDefaultHandler!!.uncaughtException(thread, throwable)
        } else {
            val intent = Intent(mContext, CrashActivity::class.java).apply {
                putExtra("crashInfo", crashInfo)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            mContext.startActivity(intent)
            exitProcess(0)
        }
    }

    private fun handleException(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        getCrashInfo(throwable)
        return true
    }

    private fun getCrashInfo(throwable: Throwable) {
        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        throwable.printStackTrace(printWriter)
        var cause = throwable.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.apply {
            flush()
            close()
        }
        val errorMessage = writer.toString()
        ExceptionUtil.tryOn {
            val infoList = mutableListOf(
                "================================",
                "Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())}",
                "Version: ${BuildConfig.VERSION_NAME}",
                "Model: ${Build.MODEL}",
                "ABIs: ${Build.SUPPORTED_ABIS.joinToString(separator = ", ")}",
                "SDK: ${Build.VERSION.SDK_INT}",
                "================================",
                errorMessage,
            )
            crashInfo = infoList.toLineString().trim()
        }
    }
}
