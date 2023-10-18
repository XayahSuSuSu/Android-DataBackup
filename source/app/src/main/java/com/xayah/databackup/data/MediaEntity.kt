package com.xayah.databackup.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.xayah.databackup.util.CompressionType
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.PathUtil

@Entity
data class MediaBackupEntity(
    @PrimaryKey var path: String,
    var name: String,
    @ColumnInfo(defaultValue = "0") var sizeBytes: Long,
    @ColumnInfo(defaultValue = "1") var selected: Boolean,
) {
    val sizeDisplay: String
        get() = formatSize(sizeBytes.toDouble())
}

@Entity
data class MediaBackupEntityUpsert(
    @PrimaryKey var path: String,
    var name: String,
)

@Entity
data class MediaRestoreEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var timestamp: Long,
    var path: String,
    var name: String,
    var sizeBytes: Long,
    var selected: Boolean,
    @ColumnInfo(defaultValue = "") var savePath: String,
) {
    val archivePath: String
        get() = "${PathUtil.getRestoreMediumSavePath()}/${name}/$timestamp/${DataType.MEDIA_MEDIA.type}.${CompressionType.TAR.suffix}"

    val sizeDisplay: String
        get() = formatSize(sizeBytes.toDouble())
}

@Entity(
    foreignKeys = [ForeignKey(
        entity = MediaBackupEntity::class,
        parentColumns = arrayOf("path"),
        childColumns = arrayOf("path"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class MediaBackupOperationEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var timestamp: Long,
    var startTimestamp: Long,
    var endTimestamp: Long,
    @ColumnInfo(index = true) var path: String,
    var name: String,
    var opLog: String,
    var opState: OperationState = OperationState.IDLE,
    var state: Boolean = false,
) {
    val isSucceed: Boolean
        get() {
            if (opState == OperationState.ERROR) return false
            return true
        }
}

@Entity(
    foreignKeys = [ForeignKey(
        entity = MediaRestoreEntity::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("entityId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class MediaRestoreOperationEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(index = true) var entityId: Long,
    var timestamp: Long,
    var startTimestamp: Long,
    var endTimestamp: Long,
    var path: String,
    var name: String,
    var opLog: String,
    var opState: OperationState = OperationState.IDLE,
    var state: Boolean = false,
) {
    val isSucceed: Boolean
        get() {
            if (opState == OperationState.ERROR) return false
            return true
        }
}

@Entity
data class MediaBackupWithOpEntity(
    @Embedded val media: MediaBackupEntity,
    @Relation(parentColumn = "path", entityColumn = "path") val opList: List<MediaBackupOperationEntity>,
)

@Entity
data class MediaRestoreWithOpEntity(
    @Embedded val media: MediaRestoreEntity,
    @Relation(parentColumn = "id", entityColumn = "entityId") val opList: List<MediaRestoreOperationEntity>,
)
