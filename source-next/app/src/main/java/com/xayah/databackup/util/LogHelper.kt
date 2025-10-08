package com.xayah.databackup.util

import android.util.Log

object LogHelper {
    private fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    fun d(tag: String, functionName: String, msg: String) {
        d("$tag#$functionName", msg)
    }

    private fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    fun i(tag: String, functionName: String, msg: String) {
        i("$tag#$functionName", msg)
    }

    private fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    fun w(tag: String, functionName: String, msg: String) {
        w("$tag#$functionName", msg)
    }

    private fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    fun e(tag: String, functionName: String, msg: String) {
        e("$tag#$functionName", msg)
    }

    private fun e(tag: String, msg: String, tr: Throwable?) {
        Log.e(tag, msg, tr)
    }

    fun e(tag: String, functionName: String, msg: String, tr: Throwable?) {
        e("$tag#$functionName", msg, tr)
    }
}
