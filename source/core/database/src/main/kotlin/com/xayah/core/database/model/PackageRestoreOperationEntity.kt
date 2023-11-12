package com.xayah.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.OperationState

/**
 * Operation table of package backup.
 */
@Entity
data class PackageRestoreOperation(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(defaultValue = "0") var timestamp: Long,
    @ColumnInfo(defaultValue = "0") var startTimestamp: Long,
    @ColumnInfo(defaultValue = "0") var endTimestamp: Long,
    var packageName: String,
    var label: String,
    @ColumnInfo(defaultValue = "IDLE") var packageState: OperationState = OperationState.IDLE,
    @Embedded(prefix = "apk_") val apkOp: Operation = Operation(),
    @Embedded(prefix = "user_") val userOp: Operation = Operation(),
    @Embedded(prefix = "userDe_") val userDeOp: Operation = Operation(),
    @Embedded(prefix = "data_") val dataOp: Operation = Operation(),
    @Embedded(prefix = "obb_") val obbOp: Operation = Operation(),
    @Embedded(prefix = "media_") val mediaOp: Operation = Operation(),
) {
    private fun isFinished(state: OperationState) = state != OperationState.IDLE && state != OperationState.PROCESSING

    val isSucceed: Boolean
        get() {
            if (apkOp.state == OperationState.ERROR) return false
            if (userOp.state == OperationState.ERROR) return false
            if (userDeOp.state == OperationState.ERROR) return false
            if (dataOp.state == OperationState.ERROR) return false
            if (obbOp.state == OperationState.ERROR) return false
            if (mediaOp.state == OperationState.ERROR) return false
            return true
        }

    val progress: Float
        get() {
            var count = 0
            if (isFinished(apkOp.state)) count++
            if (isFinished(userOp.state)) count++
            if (isFinished(userDeOp.state)) count++
            if (isFinished(dataOp.state)) count++
            if (isFinished(obbOp.state)) count++
            if (isFinished(mediaOp.state)) count++
            return count.toFloat() / 6
        }
}
