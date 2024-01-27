package com.xayah.core.util

import org.apache.commons.codec.digest.DigestUtils
import java.nio.file.Files
import java.nio.file.Paths

object HashUtil {
    fun calculateMD5(src: String) = DigestUtils.md5Hex(Files.newInputStream(Paths.get(src)))
}
