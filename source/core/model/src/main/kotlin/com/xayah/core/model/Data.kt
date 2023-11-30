package com.xayah.core.model

import com.xayah.core.common.util.toLineString
import com.xayah.core.common.util.toSpaceString

data class ShellResult(
    var code: Int,
    var input: List<String>,
    var out: List<String>,
) {
    val isSuccess: Boolean
        get() = code == 0

    val inputString: String
        get() = input.toSpaceString()

    val outString: String
        get() = out.toLineString()
}

data class RcloneSizeInfo(
    val count: Int = 0,
    val bytes: Long = 0,
    val sizeless: Int = 0,
)
