package com.xayah.core.hiddenapi

import org.lsposed.hiddenapibypass.HiddenApiBypass

object HiddenApiBypassUtil {
    fun addHiddenApiExemptions(vararg prefix: String) = HiddenApiBypass.addHiddenApiExemptions(*prefix)
}
