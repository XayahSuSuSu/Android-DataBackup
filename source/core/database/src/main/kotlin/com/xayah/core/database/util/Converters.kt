package com.xayah.core.database.util

import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import com.xayah.core.model.database.PackagePermission
import com.xayah.core.util.GsonUtil

class StringListConverters {
    @TypeConverter
    fun fromStringListJson(json: String): List<String> =
        GsonUtil().fromJson(json, object : TypeToken<List<String>>() {}.type)

    @TypeConverter
    fun toStringListJson(list: List<String>): String =
        GsonUtil().toJson(list)

    @TypeConverter
    fun fromPermissionListJson(json: String): List<PackagePermission> =
        GsonUtil().fromJson(json, object : TypeToken<List<PackagePermission>>() {}.type)

    @TypeConverter
    fun toPermissionListJson(list: List<PackagePermission>): String =
        GsonUtil().toJson(list)
}
