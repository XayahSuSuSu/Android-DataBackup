package com.xayah.databackup.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.data.BackupInfo
import com.xayah.databackup.data.MediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class JSON {
    companion object {
        fun stringToJsonArray(string: String): JsonArray {
            return JsonParser.parseString(string).asJsonArray
        }

        fun entityArrayToJsonArray(entityArray: MutableList<Any>): JsonArray {
            val jsonArray = JsonArray()
            for (i in entityArray) {
                jsonArray.add(entityToJsonElement(i))

            }
            return jsonArray
        }

        fun jsonArrayToEntityArray(
            jsonArray: JsonArray, classEntity: Class<MutableList<*>>
        ): MutableList<Any> {
            return GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
                .fromJson(jsonArray, classEntity) as MutableList<Any>
        }

        fun jsonElementToEntity(jsonElement: JsonElement, classEntity: Class<*>): Any {
            return GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
                .fromJson(jsonElement, classEntity)
        }

        fun entityToJsonElement(src: Any): JsonElement? {
            return JsonParser.parseString(
                GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(src)
            )
        }

        suspend fun writeJSONToFile(src: Any, outPut: String): Boolean {
            try {
                val json = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(src)
                Bashrc.writeToFile(json, outPut).apply {
                    if (!this.first) {
                        return false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            return true
        }

        suspend fun saveAppInfoBackupList(appInfoBackupList: MutableList<AppInfoBackup>) {
            withContext(Dispatchers.IO) {
                writeJSONToFile(
                    entityArrayToJsonArray(appInfoBackupList as MutableList<Any>),
                    Path.getAppInfoBackupListPath()
                )
            }
        }

        suspend fun saveAppInfoRestoreList(appInfoRestoreList: MutableList<AppInfoRestore>) {
            withContext(Dispatchers.IO) {
                writeJSONToFile(
                    entityArrayToJsonArray(appInfoRestoreList as MutableList<Any>),
                    Path.getAppInfoRestoreListPath()
                )
            }
        }

        suspend fun saveMediaInfoBackupList(mediaInfoBackupList: MutableList<MediaInfo>) {
            withContext(Dispatchers.IO) {
                writeJSONToFile(
                    entityArrayToJsonArray(mediaInfoBackupList as MutableList<Any>),
                    Path.getMediaInfoBackupListPath()
                )
            }
        }

        suspend fun saveMediaInfoRestoreList(mediaInfoRestoreList: MutableList<MediaInfo>) {
            withContext(Dispatchers.IO) {
                writeJSONToFile(
                    entityArrayToJsonArray(mediaInfoRestoreList as MutableList<Any>),
                    Path.getMediaInfoRestoreListPath()
                )
            }
        }

        suspend fun saveBackupInfoList(backupInfoList: MutableList<BackupInfo>) {
            withContext(Dispatchers.IO) {
                writeJSONToFile(
                    entityArrayToJsonArray(backupInfoList as MutableList<Any>),
                    Path.getBackupInfoListPath()
                )
            }
        }
    }
}