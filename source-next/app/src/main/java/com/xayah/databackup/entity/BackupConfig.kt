package com.xayah.databackup.entity

import arrow.optics.optics
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.util.TimeHelper
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

enum class AppsBackupStrategy {
    Incremental,
    Standalone,
}

@optics
@Serializable
@JsonClass(generateAdapter = true)
data class BackupConfig(
    @Json(ignore = true) var source: Source = Source.LOCAL,
    @Json(ignore = true) var path: String = "",
    @Json(name = "uuid") var uuid: Uuid = Uuid.random(),
    @Json(name = "created_at") var createdAt: Long = 0,
    @Json(name = "updated_at") var updatedAt: Long = 0,
    @Json(name = "name") var name: String = "",
    @Json(name = "apps_backup_strategy") var appsBackupStrategy: AppsBackupStrategy = AppsBackupStrategy.Incremental,
) {
    companion object

    val uuidString: String
        get() = uuid.toString()

    val displayCreatedAt: String
        get() = if (createdAt != 0L) {
            TimeHelper.formatTimestampInShort(createdAt)
        } else {
            App.application.getString(R.string.unknown)
        }

    val displayUpdatedAt: String
        get() = if (updatedAt != 0L) {
            TimeHelper.formatTimestampInShort(updatedAt)
        } else {
            App.application.getString(R.string.unknown)
        }

    val displayTitle: String
        get() = name.ifEmpty {
            displayCreatedAt
        }

    val displayName: String
        get() = name.ifEmpty {
            App.application.getString(R.string.unnamed)
        }
}
