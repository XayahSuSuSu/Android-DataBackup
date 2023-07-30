package com.xayah.databackup.util

object ExceptionUtil {
    /**
     * Run block and catch all exception without handling.
     */
    fun tryOn(block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
        }
    }

    /**
     * Run remote service and catch all exception.
     */
    suspend fun tryService(onFailed: (msg: String?) -> Unit = {}, block: suspend () -> Unit) {
        return try {
            block()
        } catch (e: Exception) {
            onFailed(e.message)
        }
    }
}
