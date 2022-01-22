package com.xayah.databackup.util

import android.content.Context
import android.content.Intent
import com.xayah.databackup.CrashActivity
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

class CrashHandler(val mContext: Context) : Thread.UncaughtExceptionHandler {
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null

    /**
     * 初始化
     */
    fun initialize() {
        // 获取系统默认的UncaughtException处理
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        // 设置该CrashHandler为程序的默认处理
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * 异常捕获
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (!handleException(throwable) && mDefaultHandler != null) {
            // 使用系统默认的异常处理器处理
            mDefaultHandler!!.uncaughtException(thread, throwable)
        } else {
            // 跳转到崩溃处理Activity
            val intent = Intent(mContext, CrashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
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
        printWriter.flush()
        printWriter.close()
        val errorMessage = writer.toString()
        val stringBuilder = StringBuilder()
        try {
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
            val date: String = simpleDateFormat.format(Date())
            stringBuilder.append(date).append("\n")
            stringBuilder.append(errorMessage)
            val fileName = "Crash-$date.txt"
            val path: String = mContext.filesDir.path.replace("/files", "") + "/Crash/"
            val crashDir = File(path)
            if (!crashDir.exists()) crashDir.mkdirs()
            val fileOutputStream = FileOutputStream(path + fileName, true)
            fileOutputStream.write(stringBuilder.toString().toByteArray())
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}