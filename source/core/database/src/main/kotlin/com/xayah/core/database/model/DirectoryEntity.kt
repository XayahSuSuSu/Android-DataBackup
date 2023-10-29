package com.xayah.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.OpType
import com.xayah.core.model.StorageType

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
    @ColumnInfo(defaultValue = "BACKUP") var opType: OpType,
    @ColumnInfo(defaultValue = "INTERNAL") var storageType: StorageType,
    @ColumnInfo(defaultValue = "0") var selected: Boolean,
    @ColumnInfo(defaultValue = "1") var enabled: Boolean,
    @ColumnInfo(defaultValue = "0") var active: Boolean,
) {
    val path: String
        get() = "${parent}/${child}"

    val usedBytesDisplay: String
        get() = formatSize((totalBytes - availableBytes).toDouble())

    val totalBytesDisplay: String
        get() = formatSize(totalBytes.toDouble())
}

@Entity
data class DirectoryUpsertEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var title: String,
    var parent: String,
    var child: String,
    var opType: OpType,
    var storageType: StorageType,
    var active: Boolean = false,
) {
    val path: String
        get() = "${parent}/${child}"
}
