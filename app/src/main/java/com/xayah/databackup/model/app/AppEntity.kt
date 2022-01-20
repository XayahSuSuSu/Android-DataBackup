package com.xayah.databackup.model.app

import android.graphics.drawable.Drawable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class AppEntity(
    @PrimaryKey(autoGenerate = true) var uid: Int = 0,
    @ColumnInfo(name = "appName") var appName: String,
    @ColumnInfo(name = "appPackage") var appPackage: String,
    @ColumnInfo(name = "isOnly") val isOnly: Boolean = false,
    @ColumnInfo(name = "isSelected") val isSelected: Boolean = true,
) {
    @Ignore
    var appIcon: Drawable? = null
}
