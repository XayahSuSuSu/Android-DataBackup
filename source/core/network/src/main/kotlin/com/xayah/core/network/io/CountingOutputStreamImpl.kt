package com.xayah.core.network.io

import org.apache.commons.io.output.CountingOutputStream
import java.io.OutputStream

class CountingOutputStreamImpl(
    `out`: OutputStream,
    private val fileSize: Long,
    inline val onProgress: (written: Long, total: Long) -> Unit
) : CountingOutputStream(`out`) {
    private var totalWrite: Long = 0

    override fun beforeWrite(n: Int) {
        super.beforeWrite(n)
        totalWrite += n
        onProgress(totalWrite, fileSize)
    }
}
