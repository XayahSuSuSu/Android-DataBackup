package com.xayah.core.database.model

import androidx.room.ColumnInfo
import com.xayah.core.model.OperationState

data class Operation(
    @ColumnInfo(defaultValue = "0") var bytes: Long = 0,
    @ColumnInfo(defaultValue = "") var log: String = "",
    @ColumnInfo(defaultValue = "IDLE") var state: OperationState = OperationState.IDLE,
)
