package com.xayah.databackup.data

import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.gson.annotations.Expose
import com.xayah.databackup.App
import com.xayah.databackup.R
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
    var appIcon: Drawable? = AppCompatResources.getDrawable(App.globalContext, R.drawable.ic_iconfont_unknown),
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
        get() = detailRestoreList[_restoreIndex.value].selectApp

    val selectData: MutableState<Boolean>
        get() = detailRestoreList[_restoreIndex.value].selectData

    val hasApp: MutableState<Boolean>
        get() = detailRestoreList[_restoreIndex.value].hasApp

    val hasData: MutableState<Boolean>
        get() = detailRestoreList[_restoreIndex.value].hasData

    val sizeBytes: Double
        get() = (detailRestoreList[_restoreIndex.value].appSize.toLongOrDefault(0) +
                detailRestoreList[_restoreIndex.value].userSize.toLongOrDefault(0) +
                detailRestoreList[_restoreIndex.value].userDeSize.toLongOrDefault(0) +
                detailRestoreList[_restoreIndex.value].dataSize.toLongOrDefault(0) +
                detailRestoreList[_restoreIndex.value].obbSize.toLongOrDefault(0)).toDouble()

    val sizeDisplay: String
        get() = formatSize(sizeBytes)

    val date: String
        get() = detailRestoreList[_restoreIndex.value].date

    val versionCode: Long
        get() = detailRestoreList[_restoreIndex.value].versionCode

    private var _restoreIndex: MutableState<Int> = mutableStateOf(0)

    @Expose
    var restoreIndex: Int = 0
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
            _restoreIndex.value = field
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

fun getMediaIcon(name: String): Drawable? {
    return if (name.lowercase().contains("picture"))
        AppCompatResources.getDrawable(App.globalContext, R.drawable.ic_iconfont_pictures)
    else if (name.lowercase().contains("music"))
        AppCompatResources.getDrawable(App.globalContext, R.drawable.ic_iconfont_music)
    else if (name.lowercase().contains("download"))
        AppCompatResources.getDrawable(App.globalContext, R.drawable.ic_iconfont_download)
    else if (name.lowercase().contains("dcim"))
        AppCompatResources.getDrawable(App.globalContext, R.drawable.ic_iconfont_camera)
    else
        AppCompatResources.getDrawable(App.globalContext, R.drawable.ic_iconfont_folder)
}

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

    val icon: Drawable?
        get() = getMediaIcon(name)
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

    val icon: Drawable?
        get() = getMediaIcon(name)

    val date: String
        get() = detailRestoreList[_restoreIndex.value].date

    private var _restoreIndex: MutableState<Int> = mutableStateOf(0)

    @Expose
    var restoreIndex: Int = 0
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
            _restoreIndex.value = field
        }
}

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

/**
 * SMS database item
 */
data class SmsItem(
    @Expose var address: String,
    @Expose var body: String,
    @Expose var creator: String,
    @Expose var date: Long,
    @Expose var dateSent: Long,
    @Expose var errorCode: Long,
    @Expose var locked: Long,
    @Expose var person: Long,
    @Expose var protocol: Long,
    @Expose var read: Long,
    @Expose var replyPathPresent: Long,
    @Expose var seen: Long,
    @Expose var serviceCenter: String,
    @Expose var status: Long,
    @Expose var subject: String,
    @Expose var subscriptionId: Long,
    /**
     * 1: Received
     *
     * 2: Sent
     */
    @Expose var type: Long,
    var isSelected: MutableState<Boolean>,
    var isInLocal: MutableState<Boolean>,
    var isOnThisDevice: MutableState<Boolean>,
)

/**
 * MMS pdu table item
 */
data class MmsPduItem(
    @Expose var contentClass: Long,
    @Expose var contentLocation: String,
    @Expose var contentType: String,
    @Expose var date: Long,
    @Expose var dateSent: Long,
    @Expose var deliveryReport: Long,
    @Expose var deliveryTime: Long,
    @Expose var expiry: Long,
    @Expose var locked: Long,
    @Expose var messageBox: Long,
    @Expose var messageClass: String,
    @Expose var messageId: String,
    @Expose var messageSize: Long,
    @Expose var messageType: Long,
    @Expose var mmsVersion: Long,
    @Expose var priority: Long,
    @Expose var read: Long,
    @Expose var readReport: Long,
    @Expose var readStatus: Long,
    @Expose var reportAllowed: Long,
    @Expose var responseStatus: Long,
    @Expose var responseText: String,
    @Expose var retrieveStatus: Long,
    @Expose var retrieveText: String,
    @Expose var retrieveTextCharset: Long,
    @Expose var seen: Long,
    @Expose var status: Long,
    @Expose var subject: String,
    @Expose var subjectCharset: Long,
    @Expose var subscriptionId: Long,
    @Expose var textOnly: Long,
    @Expose var transactionId: String,
)

/**
 * MMS addr table item
 */
data class MmsAddrItem(
    @Expose var address: String,
    @Expose var charset: Long,
    @Expose var contactId: Long,
    @Expose var type: Long,
)

/**
 * MMS part table item
 */
data class MmsPartItem(
    @Expose var charset: String,
    @Expose var contentDisposition: String,
    @Expose var contentId: String,
    @Expose var contentLocation: String,
    @Expose var contentType: String,
    @Expose var ctStart: Long,
    @Expose var ctType: String,
    @Expose var filename: String,
    @Expose var name: String,
    @Expose var seq: Long,
    @Expose var text: String,
    @Expose var _data: String,
)

/**
 * MMS database item
 */
data class MmsItem(
    @Expose var pdu: MmsPduItem,
    @Expose var addr: MutableList<MmsAddrItem>,
    @Expose var part: MutableList<MmsPartItem>,
    var isSelected: MutableState<Boolean>,
    var isInLocal: MutableState<Boolean>,
    var isOnThisDevice: MutableState<Boolean>,
) {
    val address: String
        get() = run {
            for (i in addr) {
                if (i.address != "insert-address-token") {
                    return@run i.address
                }
            }
            ""
        }

    val type: Long
        get() = run {
            for (i in addr) {
                if (i.address != "insert-address-token") {
                    return@run i.type
                }
            }
            0L
        }

    val content: String
        get() = run {
            var tmp = ""
            for (i in part) {
                if (i.contentType == "text/plain") {
                    tmp += i.text + "\n"
                } else if (i.contentType != "application/smil") {
                    tmp += "[${i.contentType}]" + "\n"
                }
            }
            tmp.trim()
        }

    val smilText: String
        get() = run {
            for (i in part) {
                if (i.contentType == "application/smil") {
                    return@run i.text
                }
            }
            ""
        }

    val plainText: String
        get() = run {
            for (i in part) {
                if (i.contentType == "text/plain") {
                    return@run i.text
                }
            }
            ""
        }
}

/**
 * Contact data table item
 */
data class ContactDataItem(
    @Expose var data1: String,
    @Expose var data2: String,
    @Expose var data3: String,
    @Expose var data4: String,
    @Expose var data5: String,
    @Expose var data6: String,
    @Expose var data7: String,
    @Expose var data8: String,
    @Expose var data9: String,
    @Expose var data10: String,
    @Expose var data11: String,
    @Expose var data12: String,
    @Expose var data13: String,
    @Expose var data14: String,
    @Expose var data15: String,
    @Expose var dataVersion: Long,
    @Expose var isPrimary: Long,
    @Expose var isSuperPrimary: Long,
    @Expose var mimetype: String,
    @Expose var preferredPhoneAccountComponentName: String,
    @Expose var preferredPhoneAccountId: String,
    @Expose var sync1: String,
    @Expose var sync2: String,
    @Expose var sync3: String,
    @Expose var sync4: String,
)

/**
 * Contact raw_contacts table item
 */
data class ContactRawContactItem(
    @Expose var aggregationMode: Long,
    @Expose var deleted: Long,
    @Expose var customRingtone: String,
    @Expose var displayNameAlternative: String,
    @Expose var displayNamePrimary: String,
    @Expose var displayNameSource: Long,
    @Expose var phoneticName: String,
    @Expose var phoneticNameStyle: String,
    @Expose var sortKeyAlternative: String,
    @Expose var sortKeyPrimary: String,
    @Expose var dirty: Long,
    @Expose var version: Long,
)


/**
 * Contact database item
 */
data class ContactItem(
    @Expose var rawContact: ContactRawContactItem,
    @Expose var data: MutableList<ContactDataItem>,
    var isSelected: MutableState<Boolean>,
    var isInLocal: MutableState<Boolean>,
    var isOnThisDevice: MutableState<Boolean>,
) {
    val bodyText: String
        get() = run {
            var body = ""
            for (i in data) {
                body += i.data1 + "\n"
            }
            body.trim()
        }
}

/**
 * Call log database item
 */
data class CallLogItem(
    @Expose var cachedFormattedNumber: String,
    @Expose var cachedLookupUri: String,
    @Expose var cachedMatchedNumber: String,
    @Expose var cachedName: String,
    @Expose var cachedNormalizedNumber: String,
    @Expose var cachedNumberLabel: String,
    @Expose var cachedNumberType: Long,
    @Expose var cachedPhotoId: Long,
    @Expose var cachedPhotoUri: String,
    @Expose var countryIso: String,
    @Expose var dataUsage: Long,
    @Expose var date: Long,
    @Expose var duration: Long,
    @Expose var features: Long,
    @Expose var geocodedLocation: String,
    @Expose var isRead: Long,
    @Expose var lastModified: Long,
    @Expose var new: Long,
    @Expose var number: String,
    @Expose var numberPresentation: Long,
    @Expose var phoneAccountComponentName: String,
    @Expose var phoneAccountId: String,
    @Expose var postDialDigits: String,
    @Expose var transcription: String,
    @Expose var type: Long,
    @Expose var viaNumber: String,
    var isSelected: MutableState<Boolean>,
    var isInLocal: MutableState<Boolean>,
    var isOnThisDevice: MutableState<Boolean>,
)
