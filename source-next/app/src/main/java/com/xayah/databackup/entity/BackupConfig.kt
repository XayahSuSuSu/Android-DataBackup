package com.xayah.databackup.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.util.TimeHelper
import kotlin.uuid.Uuid

@JsonClass(generateAdapter = true)
data class BackupConfig(
    @Json(ignore = true) var source: Source = Source.LOCAL,
    @Json(ignore = true) var path: String = "",
    @Json(name = "uuid") var uuid: Uuid = Uuid.random(),
    @Json(name = "created_at") var createdAt: Long = 0,
    @Json(name = "updated_at") var updatedAt: Long = 0,
    @Json(name = "name") var name: String = "",
) {
    val displayTitle: String by lazy {
        if (name.isNotEmpty()) {
            name
        } else if (createdAt != 0L) {
            TimeHelper.formatTimestampInShort(createdAt)
        } else {
            App.application.getString(R.string.unknown)
        }
    }
}
