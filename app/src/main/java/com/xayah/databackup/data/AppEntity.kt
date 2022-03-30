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
    @ColumnInfo(name = "backupApp") var backupApp: Boolean = true,
    @ColumnInfo(name = "backupData") var backupData: Boolean = true,
) {
    @Ignore
    var icon: Drawable? = null

    @Ignore
    var onBackupApp: Boolean = false

    @Ignore
    var onBackupData: Boolean = false

    @Ignore
    var onBackupAppFinished: Boolean = false

    @Ignore
    var onBackupDataFinished: Boolean = false

    @Ignore
    var isProcessing: Boolean = false

    @Ignore
    var progress: String = ""
}