package com.xayah.core.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.OpType
import com.xayah.core.model.TaskType

@Entity
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) var id: Long,
    var opType: OpType,
    var taskType: TaskType,
    var startTimestamp: Long,
    var endTimestamp: Long,
    var backupDir: String,
    var rawBytes: Double,
    var availableBytes: Double,
    var totalBytes: Double,
    var totalCount: Int,
    var successCount: Int,
    var failureCount: Int,
    var isProcessing: Boolean,
)
