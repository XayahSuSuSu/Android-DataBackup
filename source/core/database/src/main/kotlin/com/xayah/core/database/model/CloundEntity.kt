package com.xayah.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Only used for premium build, but reserved in foss.
 */

enum class AccountConfigSizeBytes(val value: Long) {
    Loading(-255L),
    FetchFailed(-1L)
}

data class AccountConfig(
    @ColumnInfo(defaultValue = "") val type: String = "",
    @ColumnInfo(defaultValue = "") val host: String = "",
    @ColumnInfo(defaultValue = "-1") val port: Int = -1,
    @ColumnInfo(defaultValue = "") val url: String = "",
    @ColumnInfo(defaultValue = "") val user: String = "",
    @ColumnInfo(defaultValue = "") val pass: String = "",
    @ColumnInfo(defaultValue = "") val vendor: String = "",
    @ColumnInfo(defaultValue = "-255") val sizeBytes: Long = AccountConfigSizeBytes.Loading.value,
)

typealias AccountMap = HashMap<String, AccountConfig>

data class MountConfig(
    @ColumnInfo(defaultValue = "0") val mounted: Boolean,
    @ColumnInfo(defaultValue = "") val remote: String,
    @ColumnInfo(defaultValue = "") val local: String,
)

data class CloudAccountEntity(
    @PrimaryKey var name: String,
    @Embedded val account: AccountConfig,
    @ColumnInfo(defaultValue = "0") var active: Boolean,
)

data class CloudMountEntity(
    @PrimaryKey var name: String,
    @Embedded val mount: MountConfig,
)

@Entity
data class CloudEntity(
    @PrimaryKey var name: String,
    @ColumnInfo(defaultValue = "") val backupSavePath: String,
    @Embedded val account: AccountConfig,
    @Embedded val mount: MountConfig,
    @ColumnInfo(defaultValue = "0") var active: Boolean,
)

data class CloudBaseEntity(
    @PrimaryKey var name: String,
    val backupSavePath: String,
)
