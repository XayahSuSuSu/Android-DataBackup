package com.xayah.libnative

object NativeLib {
    external fun calculateTreeSize(path: String): Long
    external fun getUidGid(path: String): IntArray
}
