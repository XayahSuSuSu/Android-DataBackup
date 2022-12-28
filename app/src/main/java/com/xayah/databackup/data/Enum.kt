package com.xayah.databackup.data

import com.xayah.databackup.util.GlobalString

/**
 * 应用列表类型
 */
enum class AppListType {
    InstalledApp, // 已安装的第三方应用
    SystemApp,    // 系统应用
}

/**
 * 应用列表排序类型
 */
enum class AppListSort {
    AlphabetAscending,          // 字母表顺序升序
    AlphabetDescending,         // 字母表顺序降序
    FirstInstallTimeAscending,  // 首次安装时间升序
    FirstInstallTimeDescending, // 首次安装时间降序
}

/**
 * 应用列表过滤类型
 */
enum class AppListFilter {
    None,        // 无
    Selected,    // 已选择
    NotSelected, // 未选择
}

/**
 * 应用列表选择类型
 */
enum class AppListSelection {
    None,        // 无
    App,         // 仅全选应用
    AppReverse,  // 仅全选应用(反选)
    All,         // 全选应用+数据
    AllReverse,  // 全选应用+数据(反选)
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