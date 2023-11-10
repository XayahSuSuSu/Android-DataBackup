package com.xayah.core.database.model

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.xayah.core.model.CompressionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * All fields are defined here.
 *
 */
@Serializable
@Entity
data class PackageRestoreEntire(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var packageName: String,
    var label: String = "",
    @ColumnInfo(defaultValue = "0") var backupOpCode: Int, // Defined during the backup processing, limits the range of choices.
    @ColumnInfo(defaultValue = "0") var operationCode: Int = OperationMask.None,
    @ColumnInfo(defaultValue = "0") var timestamp: Long,
    var versionName: String = "",
    var versionCode: Long = 0,
    @ColumnInfo(defaultValue = "0") var sizeBytes: Long = 0,
    @ColumnInfo(defaultValue = "0") var installed: Boolean = false,
    var flags: Int = 0,
    var compressionType: CompressionType,
    var active: Boolean = false,
    @ColumnInfo(defaultValue = "") var savePath: String,
) {
    @Ignore
    @Transient
    var selected: MutableState<Boolean> = mutableStateOf(false)

    val isSystemApp: Boolean
        get() = (flags and ApplicationInfo.FLAG_SYSTEM) != 0

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
