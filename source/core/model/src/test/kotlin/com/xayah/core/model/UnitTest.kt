package com.xayah.core.model

import com.xayah.core.model.util.formatSize
import org.junit.Test

class UnitTest {
    @Test
    fun testFormatSize() {
        val sizeBytes = 102400.0
        println(sizeBytes.formatSize())
    }
}
