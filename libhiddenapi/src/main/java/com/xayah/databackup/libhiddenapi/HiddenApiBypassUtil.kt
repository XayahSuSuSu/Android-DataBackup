package com.xayah.databackup.libhiddenapi

import android.annotation.SuppressLint
import org.lsposed.hiddenapibypass.HiddenApiBypass

@SuppressLint("NewApi")
class HiddenApiBypassUtil {
    companion object {
        fun addHiddenApiExemptions(vararg prefix: String) {
            HiddenApiBypass.addHiddenApiExemptions(*prefix)
        }
    }
}