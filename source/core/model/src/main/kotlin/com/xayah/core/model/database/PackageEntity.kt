package com.xayah.core.model.database

import android.app.AppOpsManager
import android.app.AppOpsManagerHidden
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.App
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataState
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.util.formatSize
import kotlinx.serialization.Serializable

@Serializable
data class PackagePermission @JvmOverloads constructor(
    var name: String = "",
    var isGranted: Boolean = false, // Only for runtime permissions
    var op: Int = AppOpsManagerHidden.OP_NONE,
    var mode: Int = AppOpsManager.MODE_IGNORED,
) : Parcelable {
    val isOpsAllowed: Boolean
        get() = run {
            var allowed = mode == AppOpsManager.MODE_ALLOWED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                allowed = allowed || mode == AppOpsManager.MODE_FOREGROUND
            }
            allowed
        }

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readInt(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeByte(if (isGranted) 1 else 0)
        parcel.writeInt(op)
        parcel.writeInt(mode)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PackagePermission> {
        override fun createFromParcel(parcel: Parcel): PackagePermission {
            return PackagePermission(parcel)
        }

        override fun newArray(size: Int): Array<PackagePermission?> {
            return arrayOfNulls(size)
        }
    }
}

@Serializable
data class PackageInfo(
    var label: String,
    var versionName: String,
    var versionCode: Long,
    var flags: Int,
    var firstInstallTime: Long,
    @ColumnInfo(defaultValue = "0") var lastUpdateTime: Long,
)

/**
 * @param activated Marked to be backed up/restored.
 */
@Serializable
data class PackageExtraInfo(
    var uid: Int,
    var hasKeystore: Boolean,
    var permissions: List<PackagePermission>,
    var ssaid: String,
    @ColumnInfo(defaultValue = "0") var lastBackupTime: Long,
    var blocked: Boolean,
    var activated: Boolean,
    @ColumnInfo(defaultValue = "1") var firstUpdated: Boolean,
    @ColumnInfo(defaultValue = "1") var enabled: Boolean,
)

@Serializable
data class PackageDataStates(
    var apkState: DataState = DataState.Selected,
    var userState: DataState = DataState.Selected,
    var userDeState: DataState = DataState.Selected,
    var dataState: DataState = DataState.Selected,
    var obbState: DataState = DataState.Selected,
    var mediaState: DataState = DataState.Selected,
    var permissionState: DataState = DataState.Selected,
    var ssaidState: DataState = DataState.Selected,
) {
    companion object {
        fun DataType.getSelected(states: PackageDataStates) = when (this) {
            DataType.PACKAGE_APK -> states.apkState == DataState.Selected
            DataType.PACKAGE_USER -> states.userState == DataState.Selected
            DataType.PACKAGE_USER_DE -> states.userDeState == DataState.Selected
            DataType.PACKAGE_DATA -> states.dataState == DataState.Selected
            DataType.PACKAGE_OBB -> states.obbState == DataState.Selected
            DataType.PACKAGE_MEDIA -> states.mediaState == DataState.Selected
            else -> false
        }

        fun DataType.setSelected(states: PackageDataStates, selected: Boolean): PackageDataStates = when (this) {
            DataType.PACKAGE_APK -> states.copy(apkState = if (selected) DataState.Selected else DataState.NotSelected)
            DataType.PACKAGE_USER -> states.copy(userState = if (selected) DataState.Selected else DataState.NotSelected)
            DataType.PACKAGE_USER_DE -> states.copy(userDeState = if (selected) DataState.Selected else DataState.NotSelected)
            DataType.PACKAGE_DATA -> states.copy(dataState = if (selected) DataState.Selected else DataState.NotSelected)
            DataType.PACKAGE_OBB -> states.copy(obbState = if (selected) DataState.Selected else DataState.NotSelected)
            DataType.PACKAGE_MEDIA -> states.copy(mediaState = if (selected) DataState.Selected else DataState.NotSelected)
            else -> states
        }

        fun DataType.getDisplayStats(displayStats: PackageDataStats?): Long? = if (displayStats == null) null else
            when (this) {
                DataType.PACKAGE_APK -> displayStats.apkBytes
                DataType.PACKAGE_USER -> displayStats.userBytes
                DataType.PACKAGE_USER_DE -> displayStats.userDeBytes
                DataType.PACKAGE_DATA -> displayStats.dataBytes
                DataType.PACKAGE_OBB -> displayStats.obbBytes
                DataType.PACKAGE_MEDIA -> displayStats.mediaBytes
                else -> null
            }
    }
}

@Serializable
data class PackageStorageStats(
    var appBytes: Long = 0,
    var cacheBytes: Long = 0,
    var dataBytes: Long = 0,
    var externalCacheBytes: Long = 0,
)

@Serializable
data class PackageDataStats(
    var apkBytes: Long = 0,
    var userBytes: Long = 0,
    var userDeBytes: Long = 0,
    var dataBytes: Long = 0,
    var obbBytes: Long = 0,
    var mediaBytes: Long = 0,
)

/**
 * @param preserveId [DefaultPreserveId] means not a preserved one, otherwise it's a timestamp id.
 */
@Serializable
data class PackageIndexInfo(
    var opType: OpType,
    var packageName: String,
    var userId: Int,
    var compressionType: CompressionType,
    var preserveId: Long,
    var cloud: String,
    var backupDir: String,
)

@Serializable
@Entity
data class PackageEntity(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @Embedded(prefix = "indexInfo_") var indexInfo: PackageIndexInfo,
    @Embedded(prefix = "packageInfo_") var packageInfo: PackageInfo,
    @Embedded(prefix = "extraInfo_") var extraInfo: PackageExtraInfo,
    @Embedded(prefix = "dataStates_") var dataStates: PackageDataStates,         // Selections
    @Embedded(prefix = "storageStats_") var storageStats: PackageStorageStats,   // Storage stats from system api
    @Embedded(prefix = "dataStats_") var dataStats: PackageDataStats,            // Storage stats for backing up
    @Embedded(prefix = "displayStats_") var displayStats: PackageDataStats,      // Storage stats for display
) {
    val packageName: String
        get() = indexInfo.packageName

    val userId: Int
        get() = indexInfo.userId

    val preserveId: Long
        get() = indexInfo.preserveId

    val apkSelected: Boolean
        get() = dataStates.apkState == DataState.Selected

    val userSelected: Boolean
        get() = dataStates.userState == DataState.Selected

    val userDeSelected: Boolean
        get() = dataStates.userDeState == DataState.Selected

    val dataSelected: Boolean
        get() = dataStates.dataState == DataState.Selected

    val obbSelected: Boolean
        get() = dataStates.obbState == DataState.Selected

    val mediaSelected: Boolean
        get() = dataStates.mediaState == DataState.Selected

    companion object {
        const val FLAG_NONE = 0
        const val FLAG_APK = 1     // 000001
        const val FLAG_DATA = 62   // 111110
        const val FLAG_ALL = 63    // 111111
    }

    val selectionFlag: Int
        get() {
            var flag = 0
            if (apkSelected) flag = flag or 1
            if (userSelected) flag = flag or 2
            if (userDeSelected) flag = flag or 4
            if (dataSelected) flag = flag or 8
            if (obbSelected) flag = flag or 16
            if (mediaSelected) flag = flag or 32
            return flag
        }

    val dataSelectedCount: Int
        get() = run {
            var count = 0
            if (userSelected) count++
            if (userDeSelected) count++
            if (dataSelected) count++
            if (obbSelected) count++
            if (mediaSelected) count++
            count
        }

    val permissionSelected: Boolean
        get() = dataStates.permissionState == DataState.Selected

    val ssaidSelected: Boolean
        get() = dataStates.ssaidState == DataState.Selected

    val storageStatsBytes: Double
        get() = (storageStats.appBytes + storageStats.dataBytes).toDouble()

    val displayStatsBytes: Double
        get() = (displayStats.apkBytes + displayStats.userBytes + displayStats.userDeBytes + displayStats.dataBytes + displayStats.obbBytes + displayStats.mediaBytes).toDouble()

    val storageStatsFormat: String
        get() = storageStatsBytes.formatSize()

    val displayStatsFormat: String
        get() = displayStatsBytes.formatSize()

    val isSystemApp: Boolean
        get() = (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

    val archivesRelativeDir: String
        get() = "${packageName}/user_${userId}${if (preserveId == 0L) "" else "@$preserveId"}"

    val pkgUserKey: String
        get() = "${packageName}-${userId}"
}


fun PackageEntity.asExternalModel() = App(
    id = id,
    packageName = packageName,
    label = packageInfo.label,
    preserveId = preserveId,
    isSystemApp = isSystemApp,
    selectionFlag = selectionFlag,
    selected = extraInfo.activated
)

// Part update entity
data class PackageDataStatesEntity(
    var id: Long,
    @Embedded(prefix = "dataStates_") var dataStates: PackageDataStates,
)

// Part update entity
data class PackageUpdateEntity(
    var id: Long,
    @Embedded(prefix = "packageInfo_") var packageInfo: PackageInfo,
    @Embedded(prefix = "extraInfo_") var extraInfo: PackageExtraInfo,
    @Embedded(prefix = "storageStats_") var storageStats: PackageStorageStats,
)