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
}
