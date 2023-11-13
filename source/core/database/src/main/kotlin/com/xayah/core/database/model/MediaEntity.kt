package com.xayah.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.xayah.core.model.OperationState
import kotlinx.serialization.Serializable

@Entity
data class MediaBackupEntity(
    @PrimaryKey var path: String,
    var name: String,
    @ColumnInfo(defaultValue = "0") var sizeBytes: Long,
    @ColumnInfo(defaultValue = "0") var selected: Boolean,
) {
    val sizeDisplay: String
        get() = formatSize(sizeBytes.toDouble())
}

@Entity
data class MediaBackupEntityUpsert(
    @PrimaryKey var path: String,
    var name: String,
)

@Serializable
@Entity
data class MediaRestoreEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var timestamp: Long,
    var path: String,
    var name: String,
    var sizeBytes: Long,
    @ColumnInfo(defaultValue = "0") var selected: Boolean,
    @ColumnInfo(defaultValue = "") var savePath: String,
) {
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
    @ColumnInfo(defaultValue = "IDLE") var mediaState: OperationState = OperationState.IDLE,
    @Embedded(prefix = "data_") val dataOp: Operation = Operation(),
) {
    val isSucceed: Boolean
        get() {
            if (dataOp.state == OperationState.ERROR) return false
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
    @ColumnInfo(defaultValue = "IDLE") var mediaState: OperationState = OperationState.IDLE,
    @Embedded(prefix = "data_") val dataOp: Operation = Operation(),
) {
    val isSucceed: Boolean
        get() {
            if (dataOp.state == OperationState.ERROR) return false
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
