package com.xayah.databackup.util

import com.xayah.databackup.App
import com.xayah.databackup.data.AppInfoBackupMap
import com.xayah.databackup.data.AppInfoRestoreMap
import com.xayah.databackup.data.MediaInfoBackupMap
import com.xayah.databackup.data.MediaInfoRestoreMap
import com.xayah.databackup.ui.activity.settings.components.initializeBackupDirectory
import com.xayah.librootservice.RootService
import kotlinx.coroutines.flow.MutableStateFlow

class GlobalObject {
    object Instance {
        val instance = GlobalObject()
    }

    companion object {
        fun getInstance() = Instance.instance

        const val defaultUserId = "0"

        fun initializeRootService(onInitialize: () -> Unit) {
            // Check initialization of ipc
            if (RootService.getInstance().isInitialize()) {
                onInitialize()
            } else {
                RootService.getInstance().initialize(App.globalContext) {
                    initializeBackupDirectory()
                    Logcat.getInstance().init()
                    onInitialize()
                }
            }
        }
    }

    // 应用备份哈希表
    val appInfoBackupMap by lazy {
        MutableStateFlow<AppInfoBackupMap>(hashMapOf())
    }

    // 应用恢复哈希表
    val appInfoRestoreMap by lazy {
        MutableStateFlow<AppInfoRestoreMap>(hashMapOf())
    }

    // 媒体备份哈希表
    val mediaInfoBackupMap by lazy {
        MutableStateFlow<MediaInfoBackupMap>(hashMapOf())
    }

    // 媒体恢复哈希表
    val mediaInfoRestoreMap by lazy {
        MutableStateFlow<MediaInfoRestoreMap>(hashMapOf())
    }

    /**
     * 单次启动全局时间戳
     */
    val timeStampOnStart by lazy {
        System.currentTimeMillis().toString()
    }
}