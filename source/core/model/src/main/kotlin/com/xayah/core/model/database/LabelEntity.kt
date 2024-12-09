package com.xayah.core.model.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["label"], unique = true)])
data class LabelEntity(
    @PrimaryKey var label: String,
)

@Entity(
    indices = [Index(value = ["label", "packageName", "userId", "preserveId"], unique = true)],
    primaryKeys = ["label", "packageName", "userId", "preserveId"],
)
data class LabelAppCrossRefEntity(
    var label: String,
    var packageName: String,
    var userId: Int,
    var preserveId: Long,
)

@Entity(
    indices = [Index(value = ["label", "path", "preserveId"], unique = true)],
    primaryKeys = ["label", "path", "preserveId"],
)
data class LabelFileCrossRefEntity(
    var label: String,
    var path: String,
    var preserveId: Long,
)
