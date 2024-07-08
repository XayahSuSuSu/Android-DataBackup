package com.xayah.feature.crash

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.util.BuildConfigUtil
import com.xayah.core.common.util.toLineString
import com.xayah.core.util.DateUtil
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class CrashHandler(private val mContext: Context) : Thread.UncaughtExceptionHandler {
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private var crashInfo = ""

    fun initialize() {
        runCatching {
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
            val intent = Intent(mContext, MainActivity::class.java).apply {
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
        val stringWriter = StringWriter()
        PrintWriter(stringWriter).apply {
            throwable.printStackTrace(this)
            var cause = throwable.cause
            while (cause != null) {
                cause.printStackTrace(this)
                cause = cause.cause
            }
            flush()
            close()
        }

        runCatching {
            val infoList = mutableListOf(
                "================================",
                "Date:     ${DateUtil.formatTimestamp(DateUtil.getTimestamp())}",
                "Version:  ${BuildConfigUtil.VERSION_NAME}",
                "Model:    ${Build.MODEL}",
                "ABIs:     ${Build.SUPPORTED_ABIS.joinToString(separator = ", ")}",
                "SDK:      ${Build.VERSION.SDK_INT}",
                "================================",
                stringWriter.toString(),
            )
            crashInfo = infoList.toLineString().trim()
        }
    }
}
