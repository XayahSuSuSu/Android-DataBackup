package com.xayah.core.rootservice.util

object NativeLib {
    external fun calculateSize(path: String): Long
    external fun getUidGid(path: String): IntArray
}