package com.xayah.databackup.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OperationState {
    IDLE,
    Processing,
    Uploading,
    SKIP,
    DONE,
    ERROR,
}

/**
 * Operation table of package backup.
 */
@Entity
data class PackageBackupOperation(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(defaultValue = "0") var timestamp: Long,
    @ColumnInfo(defaultValue = "0") var startTimestamp: Long,
    @ColumnInfo(defaultValue = "0") var endTimestamp: Long,
    var packageName: String,
    var label: String,
    var packageState: Boolean = false,
    var apkLog: String = "",
    @ColumnInfo(defaultValue = "IDLE") var apkState: OperationState = OperationState.IDLE,
    var userLog: String = "",
    @ColumnInfo(defaultValue = "IDLE") var userState: OperationState = OperationState.IDLE,
    var userDeLog: String = "",
    @ColumnInfo(defaultValue = "IDLE") var userDeState: OperationState = OperationState.IDLE,
    var dataLog: String = "",
    @ColumnInfo(defaultValue = "IDLE") var dataState: OperationState = OperationState.IDLE,
    var obbLog: String = "",
    @ColumnInfo(defaultValue = "IDLE") var obbState: OperationState = OperationState.IDLE,
    var mediaLog: String = "",
    @ColumnInfo(defaultValue = "IDLE") var mediaState: OperationState = OperationState.IDLE,
) {
    private fun isFinished(state: OperationState) = state != OperationState.IDLE && state != OperationState.Processing

    val isSucceed: Boolean
        get() {
            if (apkState == OperationState.ERROR) return false
            if (userState == OperationState.ERROR) return false
            if (userDeState == OperationState.ERROR) return false
            if (dataState == OperationState.ERROR) return false
            if (obbState == OperationState.ERROR) return false
            if (mediaState == OperationState.ERROR) return false
            return true
        }

    val progress: Float
        get() {
            var count = 0
            if (isFinished(apkState)) count++
            if (isFinished(userState)) count++
            if (isFinished(userDeState)) count++
            if (isFinished(dataState)) count++
            if (isFinished(obbState)) count++
            if (isFinished(mediaState)) count++
            return count.toFloat() / 6
        }
}
