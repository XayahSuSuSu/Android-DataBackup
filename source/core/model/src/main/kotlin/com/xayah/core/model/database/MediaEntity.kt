package com.xayah.core.model.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.CompressionType
import com.xayah.core.model.File
import com.xayah.core.model.OpType
import kotlinx.serialization.Serializable

@Serializable
data class MediaIndexInfo(
    var opType: OpType,
    var name: String,
    var compressionType: CompressionType,
    var preserveId: Long,
    var cloud: String,
    var backupDir: String,
)

@Serializable
data class MediaInfo(
    var path: String,
    var dataBytes: Long,
    var displayBytes: Long,
)

@Serializable
data class MediaExtraInfo(
    @ColumnInfo(defaultValue = "0") var lastBackupTime: Long,
    var blocked: Boolean,
    var activated: Boolean,
    var existed: Boolean,
)

@Serializable
@Entity
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @Embedded(prefix = "indexInfo_") var indexInfo: MediaIndexInfo,
    @Embedded(prefix = "mediaInfo_") var mediaInfo: MediaInfo,
    @Embedded(prefix = "extraInfo_") var extraInfo: MediaExtraInfo,
) {
    private val ctName: String
        get() = indexInfo.compressionType.type

    val name: String
        get() = indexInfo.name

    val path: String
        get() = mediaInfo.path

    val preserveId: Long
        get() = indexInfo.preserveId

    val displayStatsBytes: Double
        get() = mediaInfo.displayBytes.toDouble()

    val archivesRelativeDir: String
        get() = "${indexInfo.name}${if (preserveId == 0L) "" else "@$preserveId"}"

    val existed: Boolean
        get() = extraInfo.existed

    val enabled: Boolean
        get() = extraInfo.existed && path.isNotEmpty()
}

fun MediaEntity.asExternalModel() = File(
    id = id,
    name = name,
    path = path,
    preserveId = preserveId,
    selected = extraInfo.activated
)
