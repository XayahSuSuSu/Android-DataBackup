package com.xayah.databackup.util

import com.topjohnwu.superuser.io.SuFile

/**
 * 自动捕获异常SuFile
 */
class SafeFile {
    companion object {
        fun create(path: String, callback: (suFile: SuFile) -> Unit) {
            try {
                callback(SuFile(path))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}