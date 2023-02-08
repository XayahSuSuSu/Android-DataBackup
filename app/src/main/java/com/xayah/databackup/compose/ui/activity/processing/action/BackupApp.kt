package com.xayah.databackup.compose.ui.activity.processing.action

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.activity.processing.ProcessingViewModel
import com.xayah.databackup.compose.ui.activity.processing.components.ProcessObjectItem
import com.xayah.databackup.compose.ui.activity.processing.components.parseObjectItemBySrc
import com.xayah.databackup.data.*
import com.xayah.databackup.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun onBackupAppProcessing(
    viewModel: ProcessingViewModel,
    context: Context,
    globalObject: GlobalObject
) {
    if (viewModel.isFirst.value) {
        viewModel.isFirst.value = false
        CoroutineScope(Dispatchers.IO).launch {
            val tag = "BackupApp"
            Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")

            val loadingState = viewModel.loadingState
            val progress = viewModel.progress
            val topBarTitle = viewModel.topBarTitle
            val taskList = viewModel.taskList.value
            val objectList = viewModel.objectList.value
            val allDone = viewModel.allDone

            // 检查列表
            if (globalObject.appInfoBackupMap.value.isEmpty()) {
                globalObject.appInfoBackupMap.emit(Command.getAppInfoBackupMap())
            }
            if (globalObject.appInfoRestoreMap.value.isEmpty()) {
                globalObject.appInfoRestoreMap.emit(Command.getAppInfoRestoreMap())
            }
            Logcat.getInstance().actionLogAddLine(tag, "Global map check finished.")

            // 备份信息列表
            taskList.addAll(globalObject.appInfoBackupMap.value.values.toList()
                .filter { (it.detailBackup.selectApp || it.detailBackup.selectData) && it.isOnThisDevice }
                .map {
                    ProcessingTask(
                        appName = it.detailBase.appName,
                        packageName = it.detailBase.packageName,
                        appIcon = it.detailBase.appIcon ?: AppCompatResources.getDrawable(
                            context,
                            R.drawable.ic_round_android
                        ),
                        selectApp = it.detailBackup.selectApp,
                        selectData = it.detailBackup.selectData,
                        taskState = TaskState.Waiting,
                        objectList = listOf()
                    )
                })

            Logcat.getInstance().actionLogAddLine(tag, "Task added, size: ${taskList.size}.")

            // 获取默认输入法和无障碍
            val keyboard = Bashrc.getKeyboard()
            val services = Bashrc.getAccessibilityServices()

            Logcat.getInstance()
                .actionLogAddLine(tag, "keyboard: ${keyboard}, services: ${services}.")

            // 备份自身
            if (App.globalContext.readIsBackupItself()) {
                val isSuccess = Command.backupItself(
                    "com.xayah.databackup",
                    App.globalContext.readBackupSavePath(),
                    App.globalContext.readBackupUser()
                )
                Logcat.getInstance()
                    .actionLogAddLine(tag, "Copy com.xayah.databackup to out: ${isSuccess}.")
            }


            val date =
                if (App.globalContext.readBackupStrategy() == BackupStrategy.Cover) GlobalString.cover else App.getTimeStamp()
            val userId = App.globalContext.readBackupUser()
            val compressionType = App.globalContext.readCompressionType()

            Logcat.getInstance().actionLogAddLine(tag, "Timestamp: ${date}.")
            Logcat.getInstance().actionLogAddLine(tag, "Date: ${Command.getDate(date)}.")
            Logcat.getInstance().actionLogAddLine(tag, "userId: ${userId}.")
            Logcat.getInstance().actionLogAddLine(tag, "CompressionType: ${compressionType}.")

            // 前期准备完成
            loadingState.value = LoadingState.Success
            for (i in 0 until taskList.size) {
                // 清除备份目标
                objectList.clear()

                // 进入Processing状态
                taskList[i] = taskList[i].copy(taskState = TaskState.Processing)
                val task = taskList[i]
                val appInfoBackup =
                    globalObject.appInfoBackupMap.value[task.packageName]!!

                // 滑动至目标应用
                if (viewModel.listStateIsInitialized) {
                    if (viewModel.scopeIsInitialized) {
                        viewModel.scope.launch {
                            viewModel.listState.animateScrollToItem(i)
                        }
                    }
                }

                var isSuccess = true
                val packageName = task.packageName
                val outPutPath = "${Path.getBackupDataSavePath()}/${packageName}/$date"
                val outPutIconPath =
                    "${Path.getBackupDataSavePath()}/${packageName}/icon.png"
                val userPath = "${Path.getUserPath()}/${packageName}"
                val userDePath = "${Path.getUserDePath()}/${packageName}"
                val dataPath = "${Path.getDataPath()}/${packageName}"
                val obbPath = "${Path.getObbPath()}/${packageName}"

                Logcat.getInstance().actionLogAddLine(tag, "AppName: ${task.appName}.")
                Logcat.getInstance().actionLogAddLine(tag, "PackageName: ${task.packageName}.")

                if (task.selectApp) {
                    // 检查是否备份APK
                    objectList.add(
                        ProcessObjectItem(
                            state = TaskState.Waiting,
                            title = GlobalString.ready,
                            subtitle = GlobalString.pleaseWait,
                            type = ProcessingObjectType.APP
                        )
                    )
                }
                if (task.selectData) {
                    // 检查是否备份数据
                    // USER为必备份项
                    objectList.add(
                        ProcessObjectItem(
                            state = TaskState.Waiting,
                            title = GlobalString.ready,
                            subtitle = GlobalString.pleaseWait,
                            type = ProcessingObjectType.USER
                        )
                    )
                    // 检测是否存在USER_DE
                    Command.ls(userDePath).apply {
                        if (this) {
                            objectList.add(
                                ProcessObjectItem(
                                    state = TaskState.Waiting,
                                    title = GlobalString.ready,
                                    subtitle = GlobalString.pleaseWait,
                                    type = ProcessingObjectType.USER_DE
                                )
                            )

                        }
                    }
                    // 检测是否存在DATA
                    Command.ls(dataPath).apply {
                        if (this) {
                            objectList.add(
                                ProcessObjectItem(
                                    state = TaskState.Waiting,
                                    title = GlobalString.ready,
                                    subtitle = GlobalString.pleaseWait,
                                    type = ProcessingObjectType.DATA
                                )
                            )

                        }
                    }
                    // 检测是否存在OBB
                    Command.ls(obbPath).apply {
                        if (this) {
                            objectList.add(
                                ProcessObjectItem(
                                    state = TaskState.Waiting,
                                    title = GlobalString.ready,
                                    subtitle = GlobalString.pleaseWait,
                                    type = ProcessingObjectType.OBB
                                )
                            )

                        }
                    }
                }

                for (j in 0 until objectList.size) {
                    if (viewModel.isCancel.value) break
                    objectList[j] = objectList[j].copy(state = TaskState.Processing)
                    when (objectList[j].type) {
                        ProcessingObjectType.APP -> {
                            Command.compressAPK(
                                compressionType,
                                packageName,
                                outPutPath,
                                userId,
                                appInfoBackup.detailBackup.appSize
                            ) { type, line ->
                                objectList[j] =
                                    parseObjectItemBySrc(type, line ?: "", objectList[j])
                            }.apply {
                                if (!this) {
                                    objectList[j] = objectList[j].copy(state = TaskState.Failed)
                                    isSuccess = false
                                }
                                // 保存apk大小
                                else {
                                    objectList[j] =
                                        objectList[j].copy(state = TaskState.Success)
                                    appInfoBackup.detailBackup.appSize = Command.countSize(
                                        Bashrc.getAPKPath(task.packageName, userId).second,
                                        1
                                    )
                                }
                            }
                        }
                        ProcessingObjectType.USER -> {
                            Command.compress(
                                compressionType,
                                "user",
                                packageName,
                                outPutPath,
                                Path.getUserPath(),
                                appInfoBackup.detailBackup.userSize
                            ) { type, line ->
                                objectList[j] =
                                    parseObjectItemBySrc(type, line ?: "", objectList[j])
                            }.apply {
                                if (!this) {
                                    objectList[j] = objectList[j].copy(state = TaskState.Failed)
                                    isSuccess = false
                                }
                                // 保存user大小
                                else {
                                    objectList[j] =
                                        objectList[j].copy(state = TaskState.Success)
                                    appInfoBackup.detailBackup.userSize =
                                        Command.countSize(userPath, 1)
                                }
                            }
                        }
                        ProcessingObjectType.USER_DE -> {
                            Command.compress(
                                compressionType,
                                "user_de",
                                packageName,
                                outPutPath,
                                Path.getUserDePath(),
                                appInfoBackup.detailBackup.userDeSize
                            ) { type, line ->
                                objectList[j] =
                                    parseObjectItemBySrc(type, line ?: "", objectList[j])
                            }.apply {
                                if (!this) {
                                    objectList[j] = objectList[j].copy(state = TaskState.Failed)
                                    isSuccess = false
                                }
                                // 保存user大小
                                else {
                                    objectList[j] =
                                        objectList[j].copy(state = TaskState.Success)
                                    appInfoBackup.detailBackup.userDeSize =
                                        Command.countSize(userDePath, 1)
                                }
                            }
                        }
                        ProcessingObjectType.DATA -> {
                            Command.compress(
                                compressionType,
                                "data",
                                packageName,
                                outPutPath,
                                Path.getDataPath(),
                                appInfoBackup.detailBackup.dataSize
                            ) { type, line ->
                                objectList[j] =
                                    parseObjectItemBySrc(type, line ?: "", objectList[j])
                            }.apply {
                                if (!this) {
                                    objectList[j] = objectList[j].copy(state = TaskState.Failed)
                                    isSuccess = false
                                }
                                // 保存user大小
                                else {
                                    objectList[j] =
                                        objectList[j].copy(state = TaskState.Success)
                                    appInfoBackup.detailBackup.dataSize =
                                        Command.countSize(dataPath, 1)
                                }
                            }
                        }
                        ProcessingObjectType.OBB -> {
                            Command.compress(
                                compressionType,
                                "obb",
                                packageName,
                                outPutPath,
                                Path.getObbPath(),
                                appInfoBackup.detailBackup.obbSize
                            ) { type, line ->
                                objectList[j] =
                                    parseObjectItemBySrc(type, line ?: "", objectList[j])
                            }.apply {
                                if (!this) {
                                    objectList[j] = objectList[j].copy(state = TaskState.Failed)
                                    isSuccess = false
                                }
                                // 保存user大小
                                else {
                                    objectList[j] =
                                        objectList[j].copy(state = TaskState.Success)
                                    appInfoBackup.detailBackup.obbSize =
                                        Command.countSize(obbPath, 1)
                                }
                            }
                        }
                    }
                }
                if (viewModel.isCancel.value) break

                appInfoBackup.detailBackup.date = date
                // 保存应用图标
                if (App.globalContext.readIsBackupIcon()) {
                    SafeFile.create(outPutIconPath) {
                        it.apply {
                            val outputStream = outputStream()
                            appInfoBackup.detailBase.appIcon?.toBitmap()
                                ?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            outputStream.flush()
                            outputStream.close()
                        }
                    }
                    Logcat.getInstance().actionLogAddLine(tag, "Trying to save icon.")
                }

                if (isSuccess) {
                    val detail = AppInfoDetailRestore().apply {
                        this.selectApp = false
                        this.selectData = false
                        this.hasApp = appInfoBackup.detailBackup.selectApp
                        this.hasData = appInfoBackup.detailBackup.selectData
                        this.versionName = appInfoBackup.detailBackup.versionName
                        this.versionCode = appInfoBackup.detailBackup.versionCode
                        this.appSize = appInfoBackup.detailBackup.appSize
                        this.userSize = appInfoBackup.detailBackup.userSize
                        this.userDeSize = appInfoBackup.detailBackup.userDeSize
                        this.dataSize = appInfoBackup.detailBackup.dataSize
                        this.obbSize = appInfoBackup.detailBackup.obbSize
                        this.date = appInfoBackup.detailBackup.date
                    }
                    if (globalObject.appInfoRestoreMap.value.containsKey(
                            packageName
                        ).not()
                    ) {
                        globalObject.appInfoRestoreMap.value[packageName] =
                            AppInfoRestore()
                    }
                    val appInfoRestore =
                        globalObject.appInfoRestoreMap.value[packageName]!!.apply {
                            this.detailBase = appInfoBackup.detailBase
                            this.firstInstallTime = appInfoBackup.firstInstallTime
                        }

                    val itemIndex =
                        appInfoRestore.detailRestoreList.indexOfFirst { date == it.date }
                    if (itemIndex == -1) {
                        // RestoreList中不存在该Item
                        appInfoRestore.detailRestoreList.add(detail)
                        appInfoRestore.restoreIndex++
                    } else {
                        // RestoreList中已存在该Item
                        appInfoRestore.detailRestoreList[itemIndex] = detail
                    }
                }

                taskList[i] =
                    task.copy(
                        taskState = if (isSuccess) TaskState.Success else TaskState.Failed,
                        objectList = objectList.toList()
                    )

                progress.value += 1
                topBarTitle.value =
                    "${context.getString(R.string.backuping)}(${progress.value}/${taskList.size})"
            }

            // 恢复默认输入法和无障碍
            keyboard.apply {
                if (this.first) Bashrc.setKeyboard(this.second)
            }
            services.apply {
                if (this.first) Bashrc.setAccessibilityServices(this.second)
            }
            Logcat.getInstance().actionLogAddLine(tag, "Restore keyboard and services.")

            // 保存列表数据
            GsonUtil.saveAppInfoBackupMapToFile(globalObject.appInfoBackupMap.value)
            GsonUtil.saveAppInfoRestoreMapToFile(globalObject.appInfoRestoreMap.value)
            globalObject.appInfoRestoreMap.value.clear()
            Logcat.getInstance().actionLogAddLine(tag, "Save global map.")
            topBarTitle.value = "${context.getString(R.string.backup_finished)}!"
            allDone.value = true
            Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")
        }
    }
}
