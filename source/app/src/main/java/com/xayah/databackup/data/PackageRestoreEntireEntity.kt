package com.xayah.databackup.data

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.xayah.databackup.util.CompressionType
import com.xayah.databackup.util.command.PackagesRestoreUtil

/**
 * All fields are defined here.
 *
 */
@Entity
data class PackageRestoreEntire(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var packageName: String,
    var label: String,
    @ColumnInfo(defaultValue = "0") var backupOpCode: Int, // Defined during the backup processing, limits the range of choices.
    @ColumnInfo(defaultValue = "0") var operationCode: Int,
    @ColumnInfo(defaultValue = "0") var timestamp: Long,
    var versionName: String,
    var versionCode: Long,
    @ColumnInfo(defaultValue = "0") var sizeBytes: Long,
    var flags: Int,
    var compressionType: CompressionType,
    var active: Boolean,
) {
    @Ignore
    @Transient
    var selected: MutableState<Boolean> = mutableStateOf(false)

    val isSystemApp: Boolean
        get() = (flags and ApplicationInfo.FLAG_SYSTEM) != 0

    val savePath: String
        get() = PackagesRestoreUtil.getPackageItemSavePath(packageName = packageName, timestamp = timestamp)

    val sizeDisplay: String
        get() = formatSize(sizeBytes.toDouble())
}

/**
 * For manifest only.
 */
@Entity
data class PackageRestoreManifest(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var packageName: String,
    var label: String,
)
