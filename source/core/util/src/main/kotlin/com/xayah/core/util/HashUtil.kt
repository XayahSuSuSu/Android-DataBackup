package com.xayah.core.util

import android.annotation.TargetApi
import android.os.Build
import org.apache.commons.codec.digest.DigestUtils
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths

object HashUtil {
    fun calculateMD5(src: String): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        calculateMD5Api26(src)
    } else {
        calculateMD5Api24(src)
    }

    private fun calculateMD5Api24(src: String) = DigestUtils.md5Hex(FileInputStream(src))

    @TargetApi(Build.VERSION_CODES.O)
    private fun calculateMD5Api26(src: String) = DigestUtils.md5Hex(Files.newInputStream(Paths.get(src)))
}
