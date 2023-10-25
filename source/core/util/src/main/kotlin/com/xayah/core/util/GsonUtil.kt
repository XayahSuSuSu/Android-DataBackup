package com.xayah.core.util

import com.google.gson.Gson
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GsonUtil @Inject constructor() {
    private val gson = Gson()

    fun toJson(src: Any): String = gson.toJson(src)

    fun <T> fromJson(json: String, type: Type): T = run {
        gson.fromJson(json, type)
    }
}
