package com.xayah.core.network.io

import org.apache.commons.io.input.CountingInputStream
import java.io.InputStream
import java.util.Timer
import java.util.TimerTask

class CountingInputStreamImpl(
    `in`: InputStream,
    private val fileSize: Long,
    inline val onProgress: (read: Long, total: Long) -> Unit
) : CountingInputStream(`in`) {
    private var totalRead: Long = 0
    private val timer = Timer()
    private val task = object : TimerTask() {
        override fun run() {
            onProgress(totalRead, fileSize)
        }
    }

    init {
        timer.schedule(task, 0, 1000)
    }

    override fun beforeRead(n: Int) {
        super.beforeRead(n)
        totalRead += n
    }

    override fun close() {
        super.close()
        timer.cancel()
    }
}
