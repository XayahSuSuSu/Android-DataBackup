package com.xayah.core.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.StorageType
import com.xayah.core.model.util.formatSize

@Entity
data class DirectoryEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var title: String,
    var parent: String,
    var child: String,
    @ColumnInfo(defaultValue = "[]") var tags: List<String>,
    @ColumnInfo(defaultValue = "") var error: String,
    @ColumnInfo(defaultValue = "0") var availableBytes: Long,
    @ColumnInfo(defaultValue = "0") var totalBytes: Long,
    @ColumnInfo(defaultValue = "INTERNAL") var storageType: StorageType,
    @ColumnInfo(defaultValue = "0") var selected: Boolean,
    @ColumnInfo(defaultValue = "1") var enabled: Boolean,
    @ColumnInfo(defaultValue = "0") var active: Boolean,
) {
    val path: String
        get() = "${parent}/${child}"

    val usedBytesDisplay: String
        get() = (totalBytes - availableBytes).toDouble().formatSize()

    val totalBytesDisplay: String
        get() = totalBytes.toDouble().formatSize()
}

@Entity
data class DirectoryUpsertEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var title: String,
    var parent: String,
    var child: String,
    var storageType: StorageType,
    var active: Boolean = false,
) {
    val path: String
        get() = "${parent}/${child}"
}
