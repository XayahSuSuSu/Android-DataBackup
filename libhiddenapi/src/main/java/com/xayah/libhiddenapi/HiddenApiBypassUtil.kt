package com.xayah.libhiddenapi

import org.lsposed.hiddenapibypass.HiddenApiBypass

class HiddenApiBypassUtil {
    companion object {
        fun addHiddenApiExemptions(vararg prefix: String) {
            HiddenApiBypass.addHiddenApiExemptions(*prefix)
        }
    }
}