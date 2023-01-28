package com.xayah.databackup.util

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.topjohnwu.superuser.io.SuFile
import com.xayah.databackup.data.AppInfo
import com.xayah.databackup.data.AppInfoBackupMap
import com.xayah.databackup.data.AppInfoRestoreMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GsonUtil {
    object Instance {
        val instance = GsonUtil()
    }

    companion object {
        fun getInstance() = Instance.instance

        @SuppressLint("SetWorldWritable")
        suspend fun saveToFile(path: String, content: String) {
            withContext(Dispatchers.IO) {
                val parent = path.split("/").toMutableList().apply {
                    removeLast()
                }.joinToString(separator = "/")
                SuFile(parent).apply {
                    if (exists().not()) mkdirs()
                }
                SuFile(path).apply {
                    delete()
                    createNewFile()
                    setExecutable(true, false)
                    setWritable(true, false)
                    setExecutable(true, false)
                    writeText(content)
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
    }

    private val gson: Gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()

    fun fromAppInfoListJson(value: String): MutableList<AppInfo> {
        val mapType = object : TypeToken<MutableList<AppInfo>>() {}
        return gson.fromJson(value, mapType.type)
    }

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
}