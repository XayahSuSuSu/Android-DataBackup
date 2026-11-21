package com.xayah.core.service.util

import android.os.StatFs
import java.io.File
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BoundedBufferPool(
    val streamRootDir: File,
    configuredMaxBytes: Long,
    configuredChunkBytes: Long,
) {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    val usedBytes = AtomicLong(0L)
    val effectiveMaxBytes: Long
    val effectiveChunkBytes: Long

    init {
        // 启动前强制清理上次崩溃或中断可能残留的分片文件
        if (streamRootDir.exists()) {
            streamRootDir.deleteRecursively()
        }
        streamRootDir.mkdirs()

        val physicalAvail = runCatching { StatFs(streamRootDir.absolutePath).availableBytes }
            .getOrDefault(configuredMaxBytes)
        // 为系统保留至少 256MB（若物理剩余已低于 512MB 则保留 20%），严防满盘 ENOSPC
        val safetyReserve = minOf(256L * 1024L * 1024L, physicalAvail / 5L)
        val safeCeiling = maxOf(64L * 1024L * 1024L, physicalAvail - safetyReserve)
        // 严格取 min(用户设定硬上限, 物理安全上限)，绝不超过用户设定值
        effectiveMaxBytes = minOf(configuredMaxBytes.coerceAtLeast(64L * 1024L * 1024L), safeCeiling)
        effectiveChunkBytes = minOf(configuredChunkBytes, effectiveMaxBytes / 2L).coerceAtLeast(16L * 1024L * 1024L)
    }

    fun acquire(requestBytes: Long, isCancelled: () -> Boolean) {
        lock.withLock {
            while (true) {
                if (isCancelled()) {
                    throw CancellationException("Buffer acquire cancelled")
                }
                val wouldExceedQuota = usedBytes.get() + requestBytes > effectiveMaxBytes && usedBytes.get() > 0L
                val physicalAvail = runCatching { StatFs(streamRootDir.absolutePath).availableBytes }.getOrDefault(Long.MAX_VALUE)
                val physicalLow = physicalAvail < requestBytes + 64L * 1024L * 1024L && usedBytes.get() > 0L
                if (!wouldExceedQuota && !physicalLow) {
                    usedBytes.addAndGet(requestBytes)
                    return
                }
                condition.await(200, TimeUnit.MILLISECONDS)
            }
        }
    }

    fun adjustAndReleaseDelta(reservedBytes: Long, actualBytes: Long) {
        val delta = reservedBytes - actualBytes
        if (delta > 0L) {
            release(delta)
        }
    }

    fun release(bytes: Long) {
        if (bytes <= 0L) return
        lock.withLock {
            val next = (usedBytes.get() - bytes).coerceAtLeast(0L)
            usedBytes.set(next)
            condition.signalAll()
        }
    }

    fun reset() {
        lock.withLock {
            usedBytes.set(0L)
            condition.signalAll()
        }
        runCatching { streamRootDir.deleteRecursively() }
        streamRootDir.mkdirs()
    }
}
