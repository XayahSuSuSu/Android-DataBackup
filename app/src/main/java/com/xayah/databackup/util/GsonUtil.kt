package com.xayah.databackup.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.xayah.databackup.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GsonUtil {
    object Instance {
        val instance = GsonUtil()
    }

    companion object {
        fun getInstance() = Instance.instance

        suspend fun saveToFile(path: String, content: String) {
            withContext(Dispatchers.IO) {
                val parent = path.split("/").toMutableList().apply {
                    removeLast()
                }.joinToString(separator = "/")
                SafeFile.create(parent) {
                    it.apply {
                        if (exists().not()) mkdirs()
                    }
                }
                SafeFile.create(path) {
                    it.apply {
                        delete()
                        SafeFile.createNewFile(this)
                        writeText(content)
                    }
                }
            }
        }

        /**
         * 保存应用备份哈希表
         */
        suspend fun saveAppInfoBackupMapToFile(map: AppInfoBackupMap) {
            withContext(Dispatchers.IO) {
                try {
                    saveToFile(
                        Path.getAppInfoBackupMapPath(),
                        getInstance().toAppInfoBackupMapJson(map),
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * 保存应用备份哈希表
         */
        suspend fun saveAppInfoRestoreMapToFile(map: AppInfoRestoreMap) {
            withContext(Dispatchers.IO) {
                try {
                    saveToFile(
                        Path.getAppInfoRestoreMapPath(),
                        getInstance().toAppInfoRestoreMapJson(map)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * 保存媒体备份哈希表
         */
        suspend fun saveMediaInfoBackupMapToFile(map: MediaInfoBackupMap) {
            withContext(Dispatchers.IO) {
                try {
                    saveToFile(
                        Path.getMediaInfoBackupMapPath(),
                        getInstance().toMediaInfoBackupMapJson(map)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * 保存媒体恢复哈希表
         */
        suspend fun saveMediaInfoRestoreMapToFile(map: MediaInfoRestoreMap) {
            withContext(Dispatchers.IO) {
                try {
                    saveToFile(
                        Path.getMediaInfoRestoreMapPath(),
                        getInstance().toMediaInfoRestoreMapJson(map)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * 保存Rclone挂载哈希表
         */
        suspend fun saveRcloneMountMapToFile(map: RcloneMountMap) {
            withContext(Dispatchers.IO) {
                try {
                    saveToFile(
                        Path.getRcloneMountListPath(),
                        getInstance().toRcloneMountMapJson(map)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * 保存备份记录
         */
        suspend fun saveBackupInfoListToFile(list: BackupInfoList) {
            withContext(Dispatchers.IO) {
                try {
                    saveToFile(
                        Path.getBackupInfoListPath(),
                        getInstance().toBackupInfoListJson(list)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val gson: Gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()

    /**
     * Json转AppInfoBackupMap
     */
    fun fromAppInfoBackupMapJson(value: String): AppInfoBackupMap {
        val mapType = object : TypeToken<AppInfoBackupMap>() {}
        return gson.fromJson(value, mapType.type)
    }

    /**
     * AppInfoBackupMap转Json
     */
    fun toAppInfoBackupMapJson(value: AppInfoBackupMap): String {
        return gson.toJson(value)
    }

    /**
     * Json转AppInfoRestoreMap
     */
    fun fromAppInfoRestoreMapJson(value: String): AppInfoRestoreMap {
        val mapType = object : TypeToken<AppInfoRestoreMap>() {}
        return gson.fromJson(value, mapType.type)
    }

    /**
     * AppInfoRestoreMap转Json
     */
    fun toAppInfoRestoreMapJson(value: AppInfoRestoreMap): String {
        return gson.toJson(value)
    }

    /**
     * Json转MediaInfoBackupMap
     */
    fun fromMediaInfoBackupMapJson(value: String): MediaInfoBackupMap {
        val mapType = object : TypeToken<MediaInfoBackupMap>() {}
        return gson.fromJson(value, mapType.type)
    }

    /**
     * MediaInfoBackupMap转Json
     */
    fun toMediaInfoBackupMapJson(value: MediaInfoBackupMap): String {
        return gson.toJson(value)
    }

    /**
     * Json转MediaInfoRestoreMap
     */
    fun fromMediaInfoRestoreMapJson(value: String): MediaInfoRestoreMap {
        val mapType = object : TypeToken<MediaInfoRestoreMap>() {}
        return gson.fromJson(value, mapType.type)
    }

    /**
     * MediaInfoRestoreMap转Json
     */
    fun toMediaInfoRestoreMapJson(value: MediaInfoRestoreMap): String {
        return gson.toJson(value)
    }


    /**
     * Json转RcloneMountMap
     */
    fun fromRcloneMountMapJson(value: String): RcloneMountMap {
        val mapType = object : TypeToken<RcloneMountMap>() {}
        return gson.fromJson(value, mapType.type)
    }

    /**
     * RcloneMountMap转Json
     */
    fun toRcloneMountMapJson(value: RcloneMountMap): String {
        return gson.toJson(value)
    }

    /**
     * Json转BackupInfoList
     */
    fun fromBackupInfoListJson(value: String): BackupInfoList {
        val mapType = object : TypeToken<BackupInfoList>() {}
        return gson.fromJson(value, mapType.type)
    }

    /**
     * BackupInfoList转Json
     */
    fun toBackupInfoListJson(value: BackupInfoList): String {
        return gson.toJson(value)
    }
}