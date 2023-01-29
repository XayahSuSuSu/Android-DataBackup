package com.xayah.databackup.util

import java.io.File

/**
 * 自动捕获异常SuFile
 */
class SafeFile {
    companion object {
        fun create(path: String, callback: (suFile: File) -> Unit) {
            try {
                callback(File(path))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun mkdirs(path: String) {
            try {
                File(path).mkdirs()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}