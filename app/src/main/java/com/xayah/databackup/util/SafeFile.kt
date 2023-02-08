package com.xayah.databackup.util

import android.annotation.SuppressLint
import com.topjohnwu.superuser.io.SuFile

/**
 * 自动捕获异常SuFile
 */
class SafeFile {
    companion object {
        fun create(path: String, onSafeCallback: (suFile: SuFile) -> Unit = {}): SuFile? {
            return try {
                val file = SuFile(path)
                onSafeCallback(file)
                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun mkdirs(path: String) {
            try {
                SuFile(path).mkdirs()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun createNewFile(suFile: SuFile) {
            try {
                suFile.apply {
                    createNewFile()
                    setPermissions()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @SuppressLint("SetWorldWritable", "SetWorldReadable")
        fun SuFile.setPermissions() {
            this.setExecutable(true, false)
            this.setWritable(true, false)
            this.setReadable(true, false)
        }
    }
}