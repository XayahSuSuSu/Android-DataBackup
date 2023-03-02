package com.xayah.databackup.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.xayah.databackup.data.*
import com.xayah.librootservice.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Paths
import kotlin.io.path.pathString

private class MutableStateBooleanAdapter : TypeAdapter<MutableState<Boolean>>() {
    override fun write(writer: JsonWriter, value: MutableState<Boolean>) {
        writer.value(value.value)
    }

    override fun read(reader: JsonReader): MutableState<Boolean> {
        return mutableStateOf(reader.nextBoolean())
    }
}

class GsonUtil {
    object Instance {
        val instance = GsonUtil()
    }

    companion object {
        fun getInstance() = Instance.instance

        suspend fun saveToFile(path: String, content: String) {
            withContext(Dispatchers.IO) {
                RootService.getInstance().mkdirs(Paths.get(path).parent.pathString)
                RootService.getInstance().writeText(path, content)
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

    private val gson: Gson =
        GsonBuilder()
            .registerTypeAdapter(
                object : TypeToken<MutableState<Boolean>>() {}.type,
                MutableStateBooleanAdapter()
            )
            .excludeFieldsWithoutExposeAnnotation().create()

    /**
     * Json转AppInfoBackupMap
     */
    fun fromAppInfoBackupMapJson(value: String): AppInfoBackupMap {
        return try {
            val mapType = object : TypeToken<AppInfoBackupMap>() {}
            gson.fromJson(value, mapType.type)
        } catch (e: Exception) {
            AppInfoBackupMap()
        }
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
        return try {
            val mapType = object : TypeToken<AppInfoRestoreMap>() {}
            gson.fromJson(value, mapType.type)
        } catch (e: Exception) {
            AppInfoRestoreMap()
        }
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
        return try {
            val mapType = object : TypeToken<MediaInfoBackupMap>() {}
            gson.fromJson(value, mapType.type)
        } catch (e: Exception) {
            MediaInfoBackupMap()
        }
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
        return try {
            val mapType = object : TypeToken<MediaInfoRestoreMap>() {}
            gson.fromJson(value, mapType.type)
        } catch (e: Exception) {
            MediaInfoRestoreMap()
        }
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
        return try {
            val mapType = object : TypeToken<RcloneMountMap>() {}
            gson.fromJson(value, mapType.type)
        } catch (e: Exception) {
            RcloneMountMap()
        }
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
        return try {
            val mapType = object : TypeToken<BackupInfoList>() {}
            gson.fromJson(value, mapType.type)
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    /**
     * BackupInfoList转Json
     */
    fun toBackupInfoListJson(value: BackupInfoList): String {
        return gson.toJson(value)
    }

    /**
     * Json转BlackListMap
     */
    fun fromBlackListMapJson(value: String): BlackListMap {
        return try {
            val mapType = object : TypeToken<BlackListMap>() {}
            gson.fromJson(value, mapType.type)
        } catch (e: Exception) {
            BlackListMap()
        }
    }

    /**
     * BlackListMap转Json
     */
    fun toBlackListMapJson(value: BlackListMap): String {
        return gson.toJson(value)
    }

    /**
     * 保存黑名单哈希表
     */
    suspend fun saveBlackListMapToFile(path: String, map: BlackListMap) {
        withContext(Dispatchers.IO) {
            try {
                saveToFile(
                    path,
                    getInstance().toBlackListMapJson(map)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}