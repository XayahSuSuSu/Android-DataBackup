package com.xayah.databackup.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Binary mask for operation code.
 */
object OperationMask {
    const val None = 0 // 00
    const val Data = 1 // 01
    const val Apk = 2 // 10
    const val Both = 3 // 11

    fun isApkSelected(packageInfo: PackageBackupEntire): Boolean = packageInfo.operationCode and Apk == Apk
    fun isDataSelected(packageInfo: PackageBackupEntire): Boolean = packageInfo.operationCode and Data == Data
}

/**
 * All fields are defined here.
 *
 */
@Entity
data class PackageBackupEntire(
    @PrimaryKey var packageName: String,
    var label: String,
    /**
     * Operation type: Coded by 2 bits.
     * None: 00, Data: 01, APK: 10, Both:11
     */
    @ColumnInfo(defaultValue = "0") var operationCode: Int,
    @ColumnInfo(defaultValue = "0") var timestamp: Long,
    var versionName: String,
    var versionCode: Long,
    var apkSize: Long,
    var userSize: Long,
    var userDeSize: Long,
    var dataSize: Long,
    var obbSize: Long,
    var mediaSize: Long,
    var firstInstallTime: Long,
    var active: Boolean,
)

/**
 * Insert or update item without some operation fields being updated.
 */
@Entity
data class PackageBackupUpdate(
    @PrimaryKey var packageName: String,
    var label: String,
    var versionName: String,
    var versionCode: Long,
    var apkSize: Long,
    var userSize: Long,
    var userDeSize: Long,
    var dataSize: Long,
    var obbSize: Long,
    var mediaSize: Long,
    var firstInstallTime: Long,
    var active: Boolean,
)

/**
 * For activating only.
 */
@Entity
data class PackageBackupActivate(
    @PrimaryKey var packageName: String,
    var active: Boolean,
)

/**
 * For manifest only.
 */
@Entity
data class PackageBackupManifest(
    @PrimaryKey var packageName: String,
    var label: String,
)
