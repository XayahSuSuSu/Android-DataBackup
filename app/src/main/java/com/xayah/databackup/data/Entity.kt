package com.xayah.databackup.data

import android.graphics.drawable.Drawable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.xayah.databackup.util.GlobalString
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
 * 应用信息单项
 */
data class AppInfoItem(
    @Expose var app: Boolean,        // 是否选中APK
    @Expose var data: Boolean,       // 是否选中数据
    @Expose var hasApp: Boolean,     // 是否含有APK(仅作为恢复项有效)
    @Expose var hasData: Boolean,    // 是否含有数据(仅作为恢复项有效)
    @Expose var versionName: String, // 版本名称
    @Expose var versionCode: Long,   // 版本代码
    @Expose var appSize: String,     // APK大小
    @Expose var userSize: String,    // User数据大小
    @Expose var userDeSize: String,  // User_de数据大小
    @Expose var dataSize: String,    // Data数据大小
    @Expose var obbSize: String,     // Obb数据大小
    @Expose var date: String,        // 备份日期(10位时间戳)
)

data class StorageStats(
    @Expose var appBytes: Long = 0,           // 应用大小
    @Expose var cacheBytes: Long = 0,         // 缓存大小
    @Expose var dataBytes: Long = 0,          // 数据大小
    @Expose var externalCacheBytes: Long = 0, // 外部共享存储缓存数据大小
)

/**
 * 应用信息
 */
data class AppInfo(
    @Expose var appName: String,                         // 应用名称
    @Expose var packageName: String,                     // 应用包名
    @Expose var isSystemApp: Boolean = false,            // 是否为系统应用
    @Expose var firstInstallTime: Long = 0,              // 首次安装时间
    @Expose var backup: AppInfoItem,                     // 备份信息
    @Expose var _restoreIndex: Int,                      // 恢复选中索引
    @Expose var restoreList: MutableList<AppInfoItem>,   // 恢复信息列表
    @Expose var appIconString: String?,                  // 应用图标(以String方式存储)
    @Expose var storageStats: StorageStats?,              // 存储相关
    var isOnThisDevice: Boolean = false,
    var appIcon: Drawable? = null,
) {
    @SerializedName("restoreIndex")
    var restoreIndex: Int = -1
        get() = run {
            var value = field
            value = _restoreIndex
            if (value == -1 || value.absoluteValue >= restoreList.size) {
                // 如果索引异常, 则恢复索引至列表尾部
                value = restoreList.size - 1
                restoreIndex = value
            } else {
                value = _restoreIndex
            }
            value
        }
        set(value) {
            _restoreIndex = if (value.absoluteValue >= restoreList.size) {
                // 如果索引异常, 则恢复索引至列表尾部
                restoreList.size - 1
            } else {
                value
            }
        }

    val sizeBytes: Long
        get() = run {
            if (storageStats != null) {
                storageStats!!.appBytes + storageStats!!.dataBytes
            } else {
                0L
            }
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

data class ProcessingTask(
    @Expose var appName: String,       // 应用名称
    @Expose var packageName: String,   // 应用包名
    @Expose var app: Boolean,          // 是否选中APK
    @Expose var data: Boolean,         // 是否选中数据
    var appIcon: Drawable? = null,
)

/**
 * Processing项目
 */
data class ProcessingItem(
    @Expose var type: String,          // 项目类型: APK, USER, USER_DE, DATA, OBB
    @Expose var title: String,         // 项目标题
    @Expose var subtitle: String,      // 项目副标题
    @Expose var isProcessing: Boolean, // 是否正在进行中, 决定了CircularProgressIndicator是否显示
    var weight: Int,               // 排序权重
) {
    companion object {
        fun APK(): ProcessingItem {
            return ProcessingItem(
                type = ProcessingItemTypeAPK,
                title = GlobalString.ready,
                subtitle = GlobalString.pleaseWait,
                isProcessing = false,
                weight = 1
            )
        }

        fun USER(): ProcessingItem {
            return ProcessingItem(
                type = ProcessingItemTypeUSER,
                title = GlobalString.ready,
                subtitle = GlobalString.pleaseWait,
                isProcessing = false,
                weight = 2
            )
        }

        fun USERDE(): ProcessingItem {
            return ProcessingItem(
                type = ProcessingItemTypeUSERDE,
                title = GlobalString.ready,
                subtitle = GlobalString.pleaseWait,
                isProcessing = false,
                weight = 3
            )
        }

        fun DATA(): ProcessingItem {
            return ProcessingItem(
                type = ProcessingItemTypeDATA,
                title = GlobalString.ready,
                subtitle = GlobalString.pleaseWait,
                isProcessing = false,
                weight = 4
            )
        }

        fun OBB(): ProcessingItem {
            return ProcessingItem(
                type = ProcessingItemTypeOBB,
                title = GlobalString.ready,
                subtitle = GlobalString.pleaseWait,
                isProcessing = false,
                weight = 5
            )
        }
    }
}

data class MediaInfoItem(
    @Expose var data: Boolean, // 是否选中
    @Expose var size: String,  // 数据大小
    @Expose var date: String,  // 备份日期(10位时间戳)
)

data class MediaInfo(
    @Expose var name: String,                              // 媒体名称
    @Expose var path: String,                              // 媒体路径
    @Expose var backup: MediaInfoItem,                     // 备份信息
    @Expose var _restoreIndex: Int,                        // 恢复选中索引
    @Expose var restoreList: MutableList<MediaInfoItem>,   // 恢复信息列表
) {
    @SerializedName("restoreIndex")
    var restoreIndex: Int = -1
        get() = run {
            var value = field
            value = _restoreIndex
            if (value == -1 || value.absoluteValue >= restoreList.size) {
                // 如果索引异常, 则恢复索引至列表尾部
                value = restoreList.size - 1
                restoreIndex = value
            } else {
                value = _restoreIndex
            }
            value
        }
        set(value) {
            _restoreIndex = if (value.absoluteValue >= restoreList.size) {
                // 如果索引异常, 则恢复索引至列表尾部
                restoreList.size - 1
            } else {
                value
            }
        }
}

data class BackupInfo(
    @Expose var version: String,
    @Expose var startTimeStamp: String,
    @Expose var endTimeStamp: String,
    @Expose var startSize: String,
    @Expose var endSize: String,
    @Expose var type: String,
    @Expose var backupUser: String
)

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

data class RcloneMount(
    @Expose var name: String = "",
    @Expose var src: String = "",
    @Expose var dest: String = "",
    @Expose var mounted: Boolean = false,
)
