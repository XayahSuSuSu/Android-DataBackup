package com.xayah.core.network.io

import org.apache.commons.io.output.CountingOutputStream
import java.io.OutputStream
import java.util.Timer
import java.util.TimerTask

class CountingOutputStreamImpl(
    `out`: OutputStream,
    private val fileSize: Long,
    inline val onProgress: (written: Long, total: Long) -> Unit
) : CountingOutputStream(`out`) {
    private var totalWrite: Long = 0
    private val timer = Timer()
    private val task = object : TimerTask() {
        override fun run() {
            onProgress(totalWrite, fileSize)
        }
    }

    init {
        timer.schedule(task, 0, 1000)
    }

    override fun beforeWrite(n: Int) {
        super.beforeWrite(n)
        totalWrite += n
    }

    override fun close() {
        super.close()
        timer.cancel()
    }
}
