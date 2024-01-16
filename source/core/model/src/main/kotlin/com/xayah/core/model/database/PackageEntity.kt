package com.xayah.core.model.database

import android.content.pm.ApplicationInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.CompressionType
import com.xayah.core.model.OpType
import com.xayah.core.model.util.formatSize
import kotlinx.serialization.Serializable

const val DefaultPreserveId = 0L

@Serializable
data class PackageInfo(
    var packageName: String,
    var label: String,
    var versionName: String,
    var versionCode: Long,
    var flags: Int,
    var firstInstallTime: Long,
)

/**
 * @param preserveId [DefaultPreserveId] means not a preserved one, otherwise it's a timestamp id.
 * @param activated Marked to be backed up/restored.
 */
@Serializable
data class ExtraInfo(
    var uid: Int,
    var userId: Int,
    var preserveId: Long,
    var opType: OpType,
    var labels: List<String>,
    var compressionType: CompressionType,
    var hasKeystore: Boolean,
    var activated: Boolean,
)

enum class DataState {
    Selected,
    NotSelected,
    Disabled,
}

@Serializable
data class DataStates(
    var apkState: DataState = DataState.Selected,
    var userState: DataState = DataState.Selected,
    var userDeState: DataState = DataState.Selected,
    var dataState: DataState = DataState.Selected,
    var obbState: DataState = DataState.Selected,
    var mediaState: DataState = DataState.Selected,
)

@Serializable
data class StorageStats(
    var appBytes: Long = 0,
    var cacheBytes: Long = 0,
    var dataBytes: Long = 0,
    var externalCacheBytes: Long = 0,
)

@Serializable
data class DataStats(
    var apkBytes: Long = 0,
    var userBytes: Long = 0,
    var userDeBytes: Long = 0,
    var dataBytes: Long = 0,
    var obbBytes: Long = 0,
    var mediaBytes: Long = 0,
)

@Serializable
@Entity
data class PackageEntity(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @Embedded(prefix = "packageInfo_") var packageInfo: PackageInfo,
    @Embedded(prefix = "extraInfo_") var extraInfo: ExtraInfo,
    @Embedded(prefix = "dataStates_") var dataStates: DataStates,
    @Embedded(prefix = "storageStats_") var storageStats: StorageStats,
    @Embedded(prefix = "dataStats_") var dataStats: DataStats,
    @Embedded(prefix = "displayStats_") var displayStats: DataStats,
) {
    val packageName: String
        get() = packageInfo.packageName

    val userId: Int
        get() = extraInfo.userId

    val preserveId: Long
        get() = extraInfo.preserveId

    private val ctName: String
        get() = extraInfo.compressionType.type

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
