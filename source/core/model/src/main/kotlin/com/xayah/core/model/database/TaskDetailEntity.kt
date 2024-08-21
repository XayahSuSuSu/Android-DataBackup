package com.xayah.core.model.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.OperationState
import com.xayah.core.model.ProcessingInfoType
import com.xayah.core.model.ProcessingType

data class Info(
    var bytes: Long = 0,
    var log: String = "",
    var title: String = "",
    var content: String = "",
    var progress: Float = 0f,
    var state: OperationState = OperationState.IDLE,
)

@Entity
data class TaskDetailPackageEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val taskId: Long,
    var state: OperationState = OperationState.IDLE,
    @ColumnInfo(defaultValue = "0") var processingIndex: Int = 0,
    @Embedded(prefix = "packageEntity_") var packageEntity: PackageEntity,
    @Embedded(prefix = "apk_") val apkInfo: Info = Info(),
    @Embedded(prefix = "user_") val userInfo: Info = Info(),
    @Embedded(prefix = "userDe_") val userDeInfo: Info = Info(),
    @Embedded(prefix = "data_") val dataInfo: Info = Info(),
    @Embedded(prefix = "obb_") val obbInfo: Info = Info(),
    @Embedded(prefix = "media_") val mediaInfo: Info = Info(),
) {
    val isSuccess: Boolean
        get() {
            if (apkInfo.state == OperationState.ERROR) return false
            if (userInfo.state == OperationState.ERROR) return false
            if (userDeInfo.state == OperationState.ERROR) return false
            if (dataInfo.state == OperationState.ERROR) return false
            if (obbInfo.state == OperationState.ERROR) return false
            return mediaInfo.state != OperationState.ERROR
        }
}

@Entity
data class TaskDetailMediaEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val taskId: Long,
    var state: OperationState = OperationState.IDLE,
    @ColumnInfo(defaultValue = "0") var processingIndex: Int = 0,
    @Embedded(prefix = "mediaEntity_") var mediaEntity: MediaEntity,
    @Embedded(prefix = "media_") val mediaInfo: Info = Info(),
) {
    val isSuccess: Boolean
        get() {
            return mediaInfo.state != OperationState.ERROR
        }
}

@Entity
data class ProcessingInfoEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var taskId: Long,
    var type: ProcessingType,
    @ColumnInfo(defaultValue = "NONE") var infoType: ProcessingInfoType = ProcessingInfoType.NONE,
    var bytes: Long = 0,
    var log: String = "",
    var title: String = "",
    var content: String = "",
    var progress: Float = -1f,
    var state: OperationState = OperationState.IDLE,
)
