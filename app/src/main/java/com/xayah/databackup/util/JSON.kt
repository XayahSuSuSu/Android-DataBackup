package com.xayah.databackup.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser


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

        fun writeJSONToFile(src: Any, outPut: String): Boolean {
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
    }
}