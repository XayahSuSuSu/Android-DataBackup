package com.xayah.databackup.data

import android.graphics.drawable.Drawable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class AppEntity(
    @PrimaryKey(autoGenerate = true) var uid: Int = 0,
    @ColumnInfo(name = "appName") var appName: String,
    @ColumnInfo(name = "packageName") val packageName: String,
    @ColumnInfo(name = "appVersion") val appVersion: String = "",
    @ColumnInfo(name = "backupApp") var backupApp: Boolean = true,
    @ColumnInfo(name = "backupData") var backupData: Boolean = true,
) {
    @Ignore
    var icon: Drawable? = null

    @Ignore
    var isProcessing: Boolean = false

    @Ignore
    var backupPath: String = ""

    @Ignore
    var appInfo: AppInfo? = null
}