package com.xayah.core.model.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.OperationState

data class Info(
    var bytes: Long = 0,
    var log: String = "",
    var title: String = "",
    var progress: Float = -1f,
    var state: OperationState = OperationState.IDLE,
)

@Entity
data class TaskDetailPackageEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val taskId: Long,
    var state: OperationState = OperationState.IDLE,
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

    private fun isOpFinished(state: OperationState) =
        state != OperationState.IDLE && state != OperationState.PROCESSING && state != OperationState.UPLOADING

    val isFinished: Boolean
        get() {
            if (isOpFinished(apkInfo.state).not()) return false
            if (isOpFinished(userInfo.state).not()) return false
            if (isOpFinished(userDeInfo.state).not()) return false
            if (isOpFinished(dataInfo.state).not()) return false
            if (isOpFinished(obbInfo.state).not()) return false
            return isOpFinished(mediaInfo.state)
        }
}

/**
 * Preprocessing info
 */
@Entity
data class TaskDetailPackagePreEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val taskId: Long,
    @Embedded(prefix = "pre_") val preInfo: Info = Info(),
) {
    val isSuccess: Boolean
        get() {
            return preInfo.state != OperationState.ERROR
        }

    private fun isOpFinished(state: OperationState) =
        state != OperationState.IDLE && state != OperationState.PROCESSING && state != OperationState.UPLOADING

    val isFinished: Boolean
        get() {
            return isOpFinished(preInfo.state)
        }
}

/**
 * Post-processing info
 */
@Entity
data class TaskDetailPackagePostEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val taskId: Long,
    @Embedded(prefix = "post_") val postInfo: Info = Info(),
    @Embedded(prefix = "backup_itself_") val backupItselfInfo: Info = Info(),
    @Embedded(prefix = "save_icons_") val saveIconsInfo: Info = Info(),
) {
    val isSuccess: Boolean
        get() {
            if (postInfo.state == OperationState.ERROR) return false
            if (backupItselfInfo.state == OperationState.ERROR) return false
            return saveIconsInfo.state != OperationState.ERROR
        }

    private fun isOpFinished(state: OperationState) =
        state != OperationState.IDLE && state != OperationState.PROCESSING && state != OperationState.UPLOADING

    val isFinished: Boolean
        get() {
            if (isOpFinished(postInfo.state).not()) return false
            if (isOpFinished(backupItselfInfo.state).not()) return false
            return isOpFinished(saveIconsInfo.state)
        }
}

@Entity
data class TaskDetailMediaEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val taskId: Long,
    @Embedded(prefix = "mediaEntity_") var mediaEntity: MediaEntity,
    @Embedded(prefix = "data_") val dataInfo: Info = Info(),
) {
    val isSuccess: Boolean
        get() {
            return dataInfo.state != OperationState.ERROR
        }

    private fun isOpFinished(state: OperationState) =
        state != OperationState.IDLE && state != OperationState.PROCESSING && state != OperationState.UPLOADING

    val isFinished: Boolean
        get() {
            return isOpFinished(dataInfo.state)
        }
}
