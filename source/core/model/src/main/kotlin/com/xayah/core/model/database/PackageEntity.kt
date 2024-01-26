package com.xayah.core.model.database

import android.content.pm.ApplicationInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataState
import com.xayah.core.model.OpType
import com.xayah.core.model.util.formatSize
import kotlinx.serialization.Serializable

@Serializable
data class PackagePermission(
    var name: String,
    var isGranted: Boolean,
)

@Serializable
data class PackageInfo(
    var label: String,
    var versionName: String,
    var versionCode: Long,
    var flags: Int,
    var firstInstallTime: Long,
)

/**
 * @param activated Marked to be backed up/restored.
 */
@Serializable
data class PackageExtraInfo(
    var uid: Int,
    var labels: List<String>,
    var hasKeystore: Boolean,
    var permissions: List<PackagePermission>,
    var ssaid: String,
    var activated: Boolean,
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
)

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
)

@Serializable
@Entity
data class PackageEntity(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @Embedded(prefix = "indexInfo_") var indexInfo: PackageIndexInfo,
    @Embedded(prefix = "packageInfo_") var packageInfo: PackageInfo,
    @Embedded(prefix = "extraInfo_") var extraInfo: PackageExtraInfo,
    @Embedded(prefix = "dataStates_") var dataStates: PackageDataStates,
    @Embedded(prefix = "storageStats_") var storageStats: PackageStorageStats,
    @Embedded(prefix = "dataStats_") var dataStats: PackageDataStats,
    @Embedded(prefix = "displayStats_") var displayStats: PackageDataStats,
) {
    val packageName: String
        get() = indexInfo.packageName

    val userId: Int
        get() = indexInfo.userId

    val preserveId: Long
        get() = indexInfo.preserveId

    private val ctName: String
        get() = indexInfo.compressionType.type

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

    val permissionSelected: Boolean
        get() = dataStates.permissionState == DataState.Selected

    val ssaidSelected: Boolean
        get() = dataStates.ssaidState == DataState.Selected

    val storageStatsBytes: Double
        get() = (storageStats.appBytes + storageStats.dataBytes).toDouble()

    val storageStatsFormat: String
        get() = storageStatsBytes.formatSize()

    val isSystemApp: Boolean
        get() = (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

    val archivesRelativeDir: String
        get() = "${packageName}/${userId}/${ctName}"

    val archivesPreserveRelativeDir: String
        get() = "${archivesRelativeDir}/${preserveId}"
}
