package com.xayah.databackup.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.xayah.databackup.data.BackupInfo
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.data.RcloneMount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class JSON {
    companion object {
        fun stringToJsonArray(string: String): JsonArray {
            return JsonParser.parseString(string).asJsonArray
        }

        fun fromMountHashMapJson(string: String): HashMap<String, RcloneMount> {
            val mapType = object : TypeToken<HashMap<String, RcloneMount>>() {}
            return GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
                .fromJson(string, mapType.type)
        }

        suspend fun saveMountHashMapJson(hashMap: HashMap<String, RcloneMount>) {
            withContext(Dispatchers.IO) {
                writeJSONToFile(hashMap, Path.getRcloneMountListPath())
            }
        }

        private fun entityArrayToJsonArray(entityArray: MutableList<Any>): JsonArray {
            val jsonArray = JsonArray()
            for (i in entityArray) {
                jsonArray.add(entityToJsonElement(i))

            }
            return jsonArray
        }


        fun jsonElementToEntity(jsonElement: JsonElement, classEntity: Class<*>): Any {
            return GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
                .fromJson(jsonElement, classEntity)
        }

        private fun entityToJsonElement(src: Any): JsonElement? {
            return JsonParser.parseString(
                GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(src)
            )
        }

        private suspend fun writeJSONToFile(src: Any, outPut: String): Boolean {
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

        suspend fun saveMediaInfoList(mediaInfoBackupList: MutableList<MediaInfo>) {
            withContext(Dispatchers.IO) {
                writeJSONToFile(
                    entityArrayToJsonArray(mediaInfoBackupList as MutableList<Any>),
                    Path.getMediaInfoListPath()
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