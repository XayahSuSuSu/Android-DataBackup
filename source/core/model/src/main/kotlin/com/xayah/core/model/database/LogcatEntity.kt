package com.xayah.core.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LogcatEntity(
    @PrimaryKey(autoGenerate = true) var id: Long,
    var timestamp: String,
    var date: String,
    var time: String,
    var pid: String,
    var tid: String,
    var level: String,
    var tag: String,
    var msg: String,
)
