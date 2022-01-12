package com.xayah.databackup.model.app

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AppEntity(
    @PrimaryKey(autoGenerate = true) var uid: Int = 0,
    @ColumnInfo(name = "appName") val appName: String,
    @ColumnInfo(name = "appPackage") val appPackage: String,
    @ColumnInfo(name = "isOnly") val isOnly: Boolean = false,
    @ColumnInfo(name = "isSelected") val isSelected: Boolean = true,
)
