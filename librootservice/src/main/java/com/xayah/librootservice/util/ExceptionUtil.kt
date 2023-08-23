package com.xayah.librootservice.util

object ExceptionUtil {
    /**
     * Run block and catch all exception without handling.
     */
    fun tryWithBoolean(block: () -> Unit): Boolean {
        return try {
            block()
            true
        } catch (_: Exception) {
            false
        }
    }

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
     * Run block and catch all exception with handling.
     */
    fun <T> tryOn(block: () -> T, onException: () -> T): T {
        return try {
            block()
        } catch (_: Exception) {
            onException()
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
