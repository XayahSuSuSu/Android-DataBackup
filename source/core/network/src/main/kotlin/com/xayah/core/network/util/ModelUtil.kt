package com.xayah.core.network.util

import com.google.gson.reflect.TypeToken
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.util.GsonUtil

inline fun <reified T> CloudEntity.getExtraEntity() = runCatching { GsonUtil().fromJson<T>(extra, object : TypeToken<T>() {}.type) }.getOrNull()
