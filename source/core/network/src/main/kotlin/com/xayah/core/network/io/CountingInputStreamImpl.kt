package com.xayah.core.network.io

import org.apache.commons.io.input.CountingInputStream
import java.io.InputStream

class CountingInputStreamImpl(
    `in`: InputStream,
    private val fileSize: Long,
    inline val onProgress: (read: Long, total: Long) -> Unit
) : CountingInputStream(`in`) {
    private var totalRead: Long = 0

    override fun beforeRead(n: Int) {
        super.beforeRead(n)
        totalRead += n
        onProgress(totalRead, fileSize)
    }
}
