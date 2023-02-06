package com.xayah.databackup.data

import android.graphics.drawable.Drawable
import com.google.gson.annotations.Expose
import com.xayah.databackup.compose.ui.activity.processing.components.ProcessObjectItem
import okhttp3.internal.toLongOrDefault
import java.text.DecimalFormat
import kotlin.math.absoluteValue

data class Release(
    val html_url: String, val name: String, val assets: List<Asset>, val body: String
)

data class Asset(
    val browser_download_url: String
)

data class Issue(
    val html_url: String, val title: String, val body: String
)

/**
 * 应用存储信息
 */
data class AppInfoStorageStats(
    @Expose var appBytes: Long = 0,           // 应用大小
    @Expose var cacheBytes: Long = 0,         // 缓存大小
    @Expose var dataBytes: Long = 0,          // 数据大小
    @Expose var externalCacheBytes: Long = 0, // 外部共享存储缓存数据大小
) {
    val sizeBytes: Long
        get() = run {
            appBytes + dataBytes
        }

    val sizeDisplay: String
        get() = run {
            var unit = "Bytes"
            var size = sizeBytes.toDouble()
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
            "${DecimalFormat("#.00").format(size)} $unit"
        }
}

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
    @Expose var selectApp: Boolean = false,      // 是否选中APK
    @Expose var selectData: Boolean = false,      // 是否选中数据
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
    @Expose var selectApp: Boolean = false,       // 是否选中APK
    @Expose var selectData: Boolean = false,      // 是否选中数据
    @Expose var hasApp: Boolean = true,           // 是否含有APK文件
    @Expose var hasData: Boolean = true,          // 是否含有数据文件
) {
    val sizeBytes: Long
        get() = run {
            appSize.toLongOrDefault(0) +
                    userSize.toLongOrDefault(0) +
                    userDeSize.toLongOrDefault(0) +
                    dataSize.toLongOrDefault(0) +
                    obbSize.toLongOrDefault(0)
        }

    val sizeDisplay: String
        get() = run {
            var unit = "Bytes"
            val mSizeBytes = sizeBytes.toDouble() * 1000
            var size = mSizeBytes
            val gb = (1000 * 1000 * 1000).toDouble()
            val mb = (1000 * 1000).toDouble()
            val kb = (1000).toDouble()
            if (mSizeBytes > gb) {
                // GB
                size = mSizeBytes / gb
                unit = "GB"
            } else if (mSizeBytes > mb) {
                // GB
                size = mSizeBytes / mb
                unit = "MB"
            } else if (mSizeBytes > kb) {
                // GB
                size = mSizeBytes / kb
                unit = "KB"
            }
            "${DecimalFormat("#.00").format(size)} $unit"
        }
}

/**
 * 应用信息基类
 */
abstract class AppInfoBase(
    @Expose open var detailBase: AppInfoDetailBase = AppInfoDetailBase(),        // 详情
    @Expose open var firstInstallTime: Long = 0,                                 // 首次安装时间
)

/**
 * 备份应用信息
 */
data class AppInfoBackup(
    @Expose var detailBackup: AppInfoDetailBackup = AppInfoDetailBackup(),  // 备份详情
    @Expose var storageStats: AppInfoStorageStats = AppInfoStorageStats(),  // 存储相关
    var isOnThisDevice: Boolean = false,
) : AppInfoBase()

/**
 * 恢复应用信息
 */
data class AppInfoRestore(
    @Expose var detailRestoreList: MutableList<AppInfoDetailRestore> = mutableListOf(),  // 备份详情
) : AppInfoBase() {
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
 * 应用信息列表计数
 */
data class AppInfoBaseNum(
    var appNum: Int, var dataNum: Int
)

/**
 * 应用信息列表计数
 */
data class AppInfoListSelectedNum(
    var installed: Int, var system: Int
)

/**
 * 媒体详细信息基类
 */
data class MediaInfoDetailBase(
    @Expose var data: Boolean = false, // 是否选中
    @Expose var size: String = "",     // 数据大小
    @Expose var date: String = "",     // 备份日期(10位时间戳)
)

/**
 * 媒体信息基类
 */
abstract class MediaInfoBase(
    @Expose open var name: String = "",     // 媒体名称
    @Expose open var path: String = "",     // 媒体路径
)

/**
 * 媒体备份信息
 */
data class MediaInfoBackup(
    @Expose var backupDetail: MediaInfoDetailBase = MediaInfoDetailBase(),
) : MediaInfoBase()

/**
 * 媒体恢复信息
 */
data class MediaInfoRestore(
    @Expose var detailRestoreList: MutableList<MediaInfoDetailBase> = mutableListOf(),
) : MediaInfoBase() {
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
 * 备份应用信息
 */
data class ProcessingTask(
    var appName: String,
    var packageName: String,
    var appIcon: Drawable? = null,
    var selectApp: Boolean,
    var selectData: Boolean,
    var taskState: TaskState,
    var objectList: List<ProcessObjectItem>
)
