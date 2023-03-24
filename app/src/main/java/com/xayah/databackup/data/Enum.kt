package com.xayah.databackup.data

import com.xayah.databackup.util.GlobalString

enum class CompressionType(val type: String) {
    TAR("tar"),
    ZSTD("zstd"),
    LZ4("lz4");

    companion object {
        fun to(name: String): CompressionType {
            return try {
                CompressionType.valueOf(name)
            } catch (e: Exception) {
                ZSTD
            }
        }
    }
}

enum class DataType(val type: String) {
    USER("user"),
    USER_DE("user_de"),
    DATA("data"),
    OBB("obb"),
    MEDIA("media");

    companion object {
        fun to(name: String): DataType {
            return try {
                DataType.valueOf(name)
            } catch (e: Exception) {
                USER
            }
        }
    }
}

/**
 * 应用列表类型
 */
enum class AppListType {
    None,         // 不区分
    InstalledApp, // 已安装的第三方应用
    SystemApp,    // 系统应用
}

/**
 * 应用列表排序类型
 */
enum class AppListSort {
    Alphabet,          // 字母表顺序
    FirstInstallTime,  // 首次安装时间
    DataSize,          // 数据大小
}

/**
 * String转BackupStrategy枚举
 */
fun toAppListSort(s: String?): AppListSort {
    return try {
        AppListSort.valueOf(s!!)
    } catch (e: Exception) {
        AppListSort.Alphabet
    }
}

/**
 * 应用列表过滤类型
 */
enum class AppListFilter {
    None,          // 无
    Selected,      // 已选择
    NotSelected,   // 未选择
    Installed,     // 已安装
    NotInstalled,  // 未安装
}

/**
 * 备份策略
 */
enum class BackupStrategy {
    Cover,  // 覆盖
    ByTime, // 分时
}

/**
 * String转BackupStrategy枚举
 */
fun toBackupStrategy(s: String?): BackupStrategy {
    return try {
        BackupStrategy.valueOf(s!!)
    } catch (e: Exception) {
        BackupStrategy.Cover
    }
}

fun ofBackupStrategy(backupStrategy: BackupStrategy): String {
    return when (backupStrategy) {
        BackupStrategy.Cover -> {
            GlobalString.cover
        }
        BackupStrategy.ByTime -> {
            GlobalString.byTime
        }
    }
}

/**
 * 备份/恢复状态
 */
const val ProcessFinished = "Finished"
const val ProcessSkip = "Skip"
const val ProcessError = "Error"
const val ProcessShowTotal = "ShowTotal"
const val ProcessCompressing = "Compressing"
const val ProcessDecompressing = "Decompressing"
const val ProcessTesting = "Testing"
const val ProcessSettingSELinux = "SettingSELinux"
const val ProcessInstallingApk = "InstallingApk"

/**
 * Processing项目类型
 */
const val ProcessingItemTypeAPK = "APK"
const val ProcessingItemTypeUSER = "USER"
const val ProcessingItemTypeUSERDE = "USER_DE"
const val ProcessingItemTypeDATA = "DATA"
const val ProcessingItemTypeOBB = "OBB"

/**
 * ProcessingTaskFilter
 */
enum class ProcessingTaskFilter {
    None,          // 无
    Succeed,      // 已选择
    Failed,   // 未选择
}

/**
 * 应用列表选择类型
 */
enum class GuideType {
    Introduction,   // 介绍页面
    Update,         // 更新页面
    Env,            // 环境检测页面
}

/**
 * 应用列表选择类型
 */
enum class LoadingState {
    Loading,   // 加载中
    Success,   // 成功
    Failed,    // 失败
}

/**
 * ProcessingTask类型
 */
enum class TaskState {
    Processing, // 加载中
    Waiting,    // 等待中
    Success,    // 成功
    Failed,     // 失败
}

/**
 * ProcessingObject类型
 */
enum class ProcessingObjectType {
    APP,
    USER,
    USER_DE,
    DATA,
    OBB,
}

/**
 * 项目类型
 */
const val TypeActivityTag = "TypeActivityTag"
const val TypeBackupApp = "TypeBackupApp"
const val TypeBackupMedia = "TypeBackupMedia"
const val TypeRestoreApp = "TypeRestoreApp"
const val TypeRestoreMedia = "TypeRestoreMedia"
const val TypeBackupTelephony = "TypeBackupTelephony"
const val TypeRestoreTelephony = "TypeRestoreTelephony"

/**
 * 更新通道
 */
enum class UpdateChannel {
    Stable,
    Test,
}

/**
 * String转UpdateChannel枚举
 */
fun toUpdateChannel(s: String?): UpdateChannel {
    return try {
        UpdateChannel.valueOf(s!!)
    } catch (e: Exception) {
        UpdateChannel.Test
    }
}

fun ofUpdateChannel(updateChannel: UpdateChannel): String {
    return when (updateChannel) {
        UpdateChannel.Stable -> {
            GlobalString.stable
        }
        UpdateChannel.Test -> {
            GlobalString.test
        }
    }
}
