package com.xayah.databackup.service

import com.xayah.databackup.IRemoteFileService
import java.io.File
import java.io.FileOutputStream

class RemoteFileIPC : IRemoteFileService.Stub() {
    private lateinit var actionLogFile: File

    override fun exists(path: String?): Boolean {
        return if (path != null) {
            try {
                return File(path).exists()
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    override fun createNewFile(path: String?): Boolean {
        return if (path != null) {
            try {
                File(path).createNewFile()
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    override fun mkdirs(path: String?): Boolean {
        return if (path != null) {
            try {
                val file = File(path)
                if (file.exists().not()) file.mkdirs() else true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    override fun readText(path: String?): String {
        return if (path != null) {
            try {
                val file = File(path)
                file.readText()
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
    }

    override fun readBytes(path: String?): ByteArray {
        return if (path != null) {
            try {
                val file = File(path)
                file.readBytes()
            } catch (e: Exception) {
                ByteArray(0)
            }
        } else {
            ByteArray(0)
        }
    }

    override fun writeText(path: String?, text: String?): Boolean {
        return if (path != null && text != null) {
            try {
                val file = File(path)
                file.writeText(text)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    override fun writeBytes(path: String?, bytes: ByteArray?): Boolean {
        return if (path != null && bytes != null) {
            try {
                val fileOutputStream = FileOutputStream(path)
                fileOutputStream.write(bytes)
                fileOutputStream.close()
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    override fun initActionLogFile(path: String?): Boolean {
        return if (path != null) {
            try {
                actionLogFile = File(path)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    override fun appendActionLog(text: String?): Boolean {
        return if (text != null) {
            try {
                actionLogFile.appendText(text)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
}
