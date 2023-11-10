package com.xayah.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.OpType
import com.xayah.core.model.TaskType

@Entity
data class TaskEntity(
    @PrimaryKey var timestamp: Long,
    var opType: OpType,
    var taskType: TaskType,
    var startTimestamp: Long,
    var endTimestamp: Long,
    var path: String,
    var rawBytes: Double,
    var availableBytes: Double,
    var totalBytes: Double,
)
