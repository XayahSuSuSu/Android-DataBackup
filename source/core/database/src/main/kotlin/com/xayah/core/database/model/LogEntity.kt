package com.xayah.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.xayah.core.util.DateUtil

enum class LogCmdType {
    SHELL_IN,
    SHELL_OUT,
    SHELL_CODE,
}

@Entity
data class LogEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var startTimestamp: Long,
    var timestamp: Long = DateUtil.getTimestamp(),
    var tag: String,
    var msg: String,
)

/**
 * Shell command records.
 */
@Entity(foreignKeys = [ForeignKey(entity = LogEntity::class, parentColumns = arrayOf("id"), childColumns = arrayOf("logId"), onDelete = ForeignKey.CASCADE)])
data class CmdEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(index = true) var logId: Long,
    var timestamp: Long = DateUtil.getTimestamp(),
    var type: LogCmdType,
    var msg: String,
)

@Entity
data class LogCmdEntity(
    @Embedded val log: LogEntity,
    @Relation(parentColumn = "id", entityColumn = "logId") val cmdList: List<CmdEntity>,
)
