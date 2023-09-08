package com.xayah.libhiddenapi

import org.lsposed.hiddenapibypass.HiddenApiBypass

object HiddenApiBypassUtil {
    fun addHiddenApiExemptions(vararg prefix: String) {
        HiddenApiBypass.addHiddenApiExemptions(*prefix)
    }
}
