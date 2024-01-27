package com.xayah.core.model.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataState
import com.xayah.core.model.OpType
import kotlinx.serialization.Serializable

@Serializable
data class MediaIndexInfo(
    var opType: OpType,
    var name: String,
    var compressionType: CompressionType,
    var preserveId: Long,
)

@Serializable
data class MediaInfo(
    var path: String,
    var dataState: DataState,
    var dataBytes: Long,
    var displayBytes: Long,
)

@Serializable
data class MediaExtraInfo(
    var labels: List<String>,
    var activated: Boolean,
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

    val dataSelected: Boolean
        get() = mediaInfo.dataState == DataState.Selected

    val archivesRelativeDir: String
        get() = "${indexInfo.name}/${ctName}"

    val archivesPreserveRelativeDir: String
        get() = "${archivesRelativeDir}/${indexInfo.preserveId}"
}

data class MediaEntityWithCount(
    @Embedded val entity: MediaEntity,
    val count: Int
)
