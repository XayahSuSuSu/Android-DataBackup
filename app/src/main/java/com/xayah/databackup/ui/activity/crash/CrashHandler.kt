package com.xayah.databackup.ui.activity.crash

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.xayah.databackup.BuildConfig
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

class CrashHandler(private val mContext: Context) : Thread.UncaughtExceptionHandler {
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private var crashInfo = ""

    /**
     * 初始化
     */
    fun initialize() {
        try {
            val that = this
            mContext.applicationInfo.apply {
                if (flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) {
                    // 获取系统默认的UncaughtException处理
                    mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                    // 设置该CrashHandler为程序的默认处理
                    Thread.setDefaultUncaughtExceptionHandler(that)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 异常捕获
     */
    @ExperimentalMaterial3Api
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (!handleException(throwable) && mDefaultHandler != null) {
            // 使用系统默认的异常处理器处理
            mDefaultHandler!!.uncaughtException(thread, throwable)
        } else {
            FirebaseCrashlytics.getInstance().recordException(throwable)
            // 跳转到崩溃处理Activity
            val intent = Intent(
                mContext,
                com.xayah.databackup.ui.activity.crash.CrashActivity::class.java
            ).apply {
                putExtra("crashInfo", crashInfo)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            mContext.startActivity(intent)
            exitProcess(0) // 退出已崩溃的App进程
        }
    }

    /**
     * 自定义异常捕获
     */
    private fun handleException(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        getCrashInfo(throwable) // 收集错误信息
        return true
    }

    /**
     * 收集错误信息
     */
    private fun getCrashInfo(throwable: Throwable) {
        val writer: Writer = StringWriter()
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
        val stringBuilder = StringBuilder()
        try {
            val date =
                "Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())}\n"
            val version = "Version: ${BuildConfig.VERSION_NAME}\n"
            val model = "Model: ${Build.MODEL}\n"
            val abi = "ABIs: ${Build.SUPPORTED_ABIS.joinToString(separator = ", ")}\n"
            val sdk = "SDK: ${Build.VERSION.SDK_INT}\n"
            stringBuilder.apply {
                append("================================\n")
                append(date)
                append(version)
                append(model)
                append(abi)
                append(sdk)
                append("================================\n")
                append(errorMessage)
            }
            crashInfo = stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}