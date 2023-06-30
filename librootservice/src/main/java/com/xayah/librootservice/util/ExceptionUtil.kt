package com.xayah.librootservice.util

object ExceptionUtil {
    /**
     * Run block and catch all exception without handling.
     */
    fun tryOn(block: () -> Unit): Boolean {
        return try {
            block()
            true
        } catch (_: Exception) {
            false
        }
    }
}
