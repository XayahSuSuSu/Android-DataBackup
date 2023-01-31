package com.xayah.databackup.util

import java.io.File

/**
 * 自动捕获异常SuFile
 */
class SafeFile {
    companion object {
        fun create(path: String, onSafeCallback: (suFile: File) -> Unit = {}): File? {
            return try {
                val file = File(path)
                onSafeCallback(file)
                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
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