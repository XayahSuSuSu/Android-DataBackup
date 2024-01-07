package com.xayah.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.reflect.TypeToken
import com.xayah.core.model.CloudType
import com.xayah.core.model.SmbVersion
import com.xayah.core.util.GsonUtil

data class FTPExtra(
    val port: Int,
)

data class SMBExtra(
    val share: String,
    val port: Int,
    val domain: String,
    val version: List<SmbVersion>,
)

/**
 * Only used for premium build, but reserved in foss.
 */
@Entity
data class CloudEntity(
    @PrimaryKey var name: String,
    val type: CloudType,
    val host: String,
    val user: String,
    val pass: String,
    val remote: String,
    val extra: String,
) {
    inline fun <reified T> getExtraEntity() = runCatching { GsonUtil().fromJson<T>(extra, object : TypeToken<T>() {}.type) }.getOrNull()
}
