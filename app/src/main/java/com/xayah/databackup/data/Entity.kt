package com.xayah.databackup.data

import android.graphics.drawable.Drawable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.gson.annotations.Expose
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.internal.toLongOrDefault
import java.text.DecimalFormat
import kotlin.math.absoluteValue

/**
 * GitHub Api Release
 */
data class Release(
    val html_url: String, val name: String, val assets: List<Asset>, val body: String
)

/**
 * GitHub Api Asset
 */
data class Asset(
    val browser_download_url: String
)

fun formatSize(sizeBytes: Double): String {
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
    return "${DecimalFormat("#.00").format(size)} $unit"
}

/**
 * 应用存储信息
 */
data class AppInfoStorageStats(
    @Expose var appBytes: Long = 0,           // 应用大小
    @Expose var cacheBytes: Long = 0,         // 缓存大小
    @Expose var dataBytes: Long = 0,          // 数据大小
    @Expose var externalCacheBytes: Long = 0, // 外部共享存储缓存数据大小
)

/**
 * 应用详情基类
 */
data class AppInfoDetailBase(
    @Expose var isSystemApp: Boolean = false,    // 是否为系统应用
    @Expose var appName: String = "",            // 应用名称
    @Expose var packageName: String = "",        // 应用包名
    var appIcon: Drawable? = null,
)

/**
 * 应用备份详情
 */
data class AppInfoDetailBackup(
    @Expose var versionName: String = "",        // 版本名称
    @Expose var versionCode: Long = 0,           // 版本代码
    @Expose var appSize: String = "",            // 已备份APK大小
    @Expose var userSize: String = "",           // 已备份User数据大小
    @Expose var userDeSize: String = "",         // 已备份User_de数据大小
    @Expose var dataSize: String = "",           // 已备份Data数据大小
    @Expose var obbSize: String = "",            // 已备份Obb数据大小
    @Expose var date: String = "",               // 日期(10位时间戳)
    @Expose var selectApp: MutableState<Boolean> = mutableStateOf(false),      // 是否选中APK
    @Expose var selectData: MutableState<Boolean> = mutableStateOf(false),      // 是否选中数据
)

/**
 * 应用恢复详情
 */
data class AppInfoDetailRestore(
    @Expose var versionName: String = "",         // 版本名称
    @Expose var versionCode: Long = 0,            // 版本代码
    @Expose var appSize: String = "",             // 已备份APK大小
    @Expose var userSize: String = "",            // 已备份User数据大小
    @Expose var userDeSize: String = "",          // 已备份User_de数据大小
    @Expose var dataSize: String = "",            // 已备份Data数据大小
    @Expose var obbSize: String = "",             // 已备份Obb数据大小
    @Expose var date: String = "",                // 日期(10位时间戳)
    @Expose var selectApp: MutableState<Boolean> = mutableStateOf(false),       // 是否选中APK
    @Expose var selectData: MutableState<Boolean> = mutableStateOf(false),      // 是否选中数据
    @Expose var hasApp: MutableState<Boolean> = mutableStateOf(true),           // 是否含有APK文件
    @Expose var hasData: MutableState<Boolean> = mutableStateOf(true),          // 是否含有数据文件
)

/**
 * 备份应用信息
 */
data class AppInfoBackup(
    @Expose var detailBase: AppInfoDetailBase = AppInfoDetailBase(),        // 详情
    @Expose var firstInstallTime: Long = 0,                                 // 首次安装时间
    @Expose var detailBackup: AppInfoDetailBackup = AppInfoDetailBackup(),  // 备份详情
    @Expose var storageStats: AppInfoStorageStats = AppInfoStorageStats(),  // 存储相关
    var isOnThisDevice: Boolean = false,
) {
    val selectApp: MutableState<Boolean>
        get() = detailBackup.selectApp

    val selectData: MutableState<Boolean>
        get() = detailBackup.selectData

    val sizeBytes: Double
        get() = (storageStats.appBytes + storageStats.dataBytes).toDouble()

    val sizeDisplay: String
        get() = formatSize(sizeBytes)
}

/**
 * 恢复应用信息
 */
data class AppInfoRestore(
    @Expose var detailBase: AppInfoDetailBase = AppInfoDetailBase(),        // 详情
    @Expose var firstInstallTime: Long = 0,                                 // 首次安装时间
    @Expose var detailRestoreList: MutableList<AppInfoDetailRestore> = mutableListOf(),  // 备份详情
    var isOnThisDevice: MutableStateFlow<Boolean> = MutableStateFlow(false)
) {
    val selectApp: MutableState<Boolean>
        get() = detailRestoreList[restoreIndex].selectApp

    val selectData: MutableState<Boolean>
        get() = detailRestoreList[restoreIndex].selectData

    val hasApp: MutableState<Boolean>
        get() = detailRestoreList[restoreIndex].hasApp

    val hasData: MutableState<Boolean>
        get() = detailRestoreList[restoreIndex].hasData

    val sizeBytes: Double
        get() = (detailRestoreList[restoreIndex].appSize.toLongOrDefault(0) +
                detailRestoreList[restoreIndex].userSize.toLongOrDefault(0) +
                detailRestoreList[restoreIndex].userDeSize.toLongOrDefault(0) +
                detailRestoreList[restoreIndex].dataSize.toLongOrDefault(0) +
                detailRestoreList[restoreIndex].obbSize.toLongOrDefault(0)).toDouble()

    val sizeDisplay: String
        get() = formatSize(sizeBytes)

    @Expose
    var restoreIndex: Int = -1
        get() = run {
            var value = field
            if (value == -1 || value.absoluteValue >= detailRestoreList.size) {
                // 如果索引异常, 则恢复索引至列表尾部
                value = detailRestoreList.size - 1
                restoreIndex = value
            }
            value
        }
        set(value) {
            field = if (value.absoluteValue >= detailRestoreList.size) {
                // 如果索引异常, 则恢复索引至列表尾部
                detailRestoreList.size - 1
            } else {
                value
            }
        }
}

/**
 * 媒体详细信息基类
 */
data class MediaInfoDetailBase(
    @Expose var data: MutableState<Boolean> = mutableStateOf(false), // 是否选中
    @Expose var size: String = "",     // 数据大小
    @Expose var date: String = "",     // 备份日期(10位时间戳)
)

/**
 * 媒体备份信息
 */
data class MediaInfoBackup(
    @Expose var name: String = "",     // 媒体名称
    @Expose var path: String = "",     // 媒体路径
    @Expose var backupDetail: MediaInfoDetailBase = MediaInfoDetailBase(),
    @Expose var storageStats: AppInfoStorageStats = AppInfoStorageStats(),
) {
    val selectData: MutableState<Boolean>
        get() = backupDetail.data

    val sizeBytes: Double
        get() = storageStats.dataBytes.toDouble()

    val sizeDisplay: String
        get() = formatSize(sizeBytes)
}

/**
 * 媒体恢复信息
 */
data class MediaInfoRestore(
    @Expose var name: String = "",     // 媒体名称
    @Expose var path: String = "",     // 媒体路径
    @Expose var detailRestoreList: MutableList<MediaInfoDetailBase> = mutableListOf(),
) {
    val selectData: MutableState<Boolean>
        get() = detailRestoreList[restoreIndex].data

    val sizeBytes: Double
        get() = detailRestoreList[restoreIndex].size.toLongOrDefault(0).toDouble()

    val sizeDisplay: String
        get() = formatSize(sizeBytes)

    @Expose
    var restoreIndex: Int = -1
        get() = run {
            var value = field
            if (value == -1 || value.absoluteValue >= detailRestoreList.size) {
                // 如果索引异常, 则恢复索引至列表尾部
                value = detailRestoreList.size - 1
                restoreIndex = value
            }
            value
        }
        set(value) {
            field = if (value.absoluteValue >= detailRestoreList.size) {
                // 如果索引异常, 则恢复索引至列表尾部
                detailRestoreList.size - 1
            } else {
                value
            }
        }
}

/**
 * 备份记录
 */
data class BackupInfo(
    @Expose var version: String,
    @Expose var startTimeStamp: String,
    @Expose var endTimeStamp: String,
    @Expose var startSize: String,
    @Expose var endSize: String,
    @Expose var type: String,
    @Expose var backupUser: String
)

/**
 * Rclone配置信息
 */
data class RcloneConfig(
    var name: String = "",
    var type: String = "",
    var user: String = "",
    var pass: String = "",

    // WebDAV
    var url: String = "",
    var vendor: String = "",

    // FPT
    var host: String = "",
    var port: String = "21",
)

/**
 * Rclone挂载信息
 */
data class RcloneMount(
    @Expose var name: String = "",
    @Expose var src: String = "",
    @Expose var dest: String = "",
    @Expose var mounted: Boolean = false,
)

/**
 * 黑名单
 */
data class BlackListItem(
    @Expose var appName: String = "",
    @Expose var packageName: String,
)
