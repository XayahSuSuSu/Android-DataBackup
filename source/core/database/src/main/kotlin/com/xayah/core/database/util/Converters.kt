package com.xayah.core.database.util

import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import com.xayah.core.util.GsonUtil

class StringListConverters {
    @TypeConverter
    fun fromJson(json: String): List<String> = GsonUtil().fromJson(json, object : TypeToken<List<String>>() {}.type)

    @TypeConverter
    fun toJson(list: List<String>): String = GsonUtil().toJson(list)
}
