package com.xayah.libnative

object NativeLib {
    external fun calculateSize(path: String): Long
    external fun getUidGid(path: String): IntArray
}