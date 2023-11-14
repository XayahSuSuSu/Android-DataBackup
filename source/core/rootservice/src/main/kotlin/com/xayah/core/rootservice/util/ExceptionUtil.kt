package com.xayah.core.rootservice.util

object ExceptionUtil {
    /**
     * Run block and catch all exception without handling.
     */
    fun tryWithBoolean(block: () -> Unit): Boolean = try {
        block()
        true
    } catch (_: Exception) {
        false
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
     * Run suspend block and catch all exception without handling.
     */
    suspend fun tryOnScope(block: suspend () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
        }
    }

    /**
     * Run block and catch all exception with handling.
     */
    fun <T> tryOn(block: () -> T, onException: () -> T): T = try {
        block()
    } catch (_: Exception) {
        onException()
    }

    /**
     * Run block and catch all exception with handling.
     */
    suspend fun <T> tryOnScope(block: suspend () -> T, onException: suspend (e: Exception) -> T): T = try {
        block()
    } catch (e: Exception) {
        onException(e)
    }

    /**
     * Run remote service and catch all exception.
     */
    suspend fun tryService(onFailed: (msg: String?) -> Unit = {}, block: suspend () -> Unit) = try {
        block()
    } catch (e: Exception) {
        onFailed(e.message)
    }
}
