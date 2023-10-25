package com.xayah.core.database.model

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.text.DecimalFormat

/**
 * Binary mask for operation code.
 */
object OperationMask {
    const val None = 0 // 00
    const val Data = 1 // 01
    const val Apk = 2 // 10
    const val Both = 3 // 11

    fun isApkSelected(opCode: Int): Boolean = opCode and Apk == Apk
    fun isDataSelected(opCode: Int): Boolean = opCode and Data == Data
}

data class StorageStats(
    var appBytes: Long = 0,
    var cacheBytes: Long = 0,
    var dataBytes: Long = 0,
    var externalCacheBytes: Long = 0,
)

fun formatSize(sizeBytes: Double): String = run {
    var unit = "Bytes"
    var size = sizeBytes
    val gb = (1000 * 1000 * 1000).toDouble()
    val mb = (1000 * 1000).toDouble()
    val kb = (1000).toDouble()
    if (sizeBytes > gb) {
        // GB
        size = sizeBytes / gb
        unit = "GB"
    } else if (sizeBytes > mb) {
        // GB
        size = sizeBytes / mb
        unit = "MB"
    } else if (sizeBytes > kb) {
        // GB
        size = sizeBytes / kb
        unit = "KB"
    }
    if (size == 0.0) "0.00 $unit" else "${DecimalFormat("#.00").format(size)} $unit"
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
    @Embedded var storageStats: StorageStats,
    var flags: Int,
    var firstInstallTime: Long,
    var active: Boolean,
) {
    @Ignore
    var selected: MutableState<Boolean> = mutableStateOf(false)

    val sizeBytes: Double
        get() = (storageStats.appBytes + storageStats.dataBytes).toDouble()

    val sizeDisplay: String
        get() = formatSize(sizeBytes)

    val isSystemApp: Boolean
        get() = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
}

/**
 * Insert or update item without some operation fields being updated.
 */
@Entity
data class PackageBackupUpdate(
    @PrimaryKey var packageName: String,
    var label: String,
    var versionName: String,
    var versionCode: Long,
    @Embedded var storageStats: StorageStats,
    var flags: Int,
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
    @Embedded var storageStats: StorageStats,
)

/**
 * For operation info in [com.xayah.databackup.service.OperationLocalServiceImpl.backupPackages].
 */
@Entity
data class PackageBackupOp(
    @PrimaryKey var packageName: String,
    var label: String,
    var operationCode: Int,
    var versionName: String,
    var versionCode: Long,
    var flags: Int,
)
