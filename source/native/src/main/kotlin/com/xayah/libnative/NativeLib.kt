package com.xayah.libnative

import java.nio.ByteBuffer

object NativeLib {
    @Volatile
    private var loaded: Boolean = false

    fun ensureLoaded(): Boolean {
        if (loaded) return true
        synchronized(this) {
            if (loaded) return true
            return try {
                System.loadLibrary("nativelib")
                loaded = true
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    external fun calculateSize(path: String): Long
    external fun getUidGid(path: String): IntArray
    external fun allocateLargeBuffer(size: Int): ByteBuffer?
    external fun freeBuffer(buffer: ByteBuffer)
}
