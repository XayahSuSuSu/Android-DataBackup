package com.xayah.core.hfx

import java.nio.ByteBuffer

interface BufferAllocator {
    fun allocate(size: Int): ByteBuffer?
    fun free(buffer: ByteBuffer)
}

object JvmDirectBufferAllocator : BufferAllocator {
    override fun allocate(size: Int): ByteBuffer? {
        return try {
            ByteBuffer.allocateDirect(size)
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    override fun free(buffer: ByteBuffer) {
        // Managed by JVM GC
    }
}

