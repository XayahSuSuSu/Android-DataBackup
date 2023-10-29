package com.xayah.core.model

import com.xayah.core.util.toLineString
import com.xayah.core.util.toSpaceString

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
