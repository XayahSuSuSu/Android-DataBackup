package com.xayah.databackup.compose.ui.activity.processing.action

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
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

fun onBackupMediaProcessing(
    viewModel: ProcessingViewModel,
    context: Context,
    globalObject: GlobalObject
) {
    if (viewModel.isFirst.value) {
        viewModel.isFirst.value = false
        CoroutineScope(Dispatchers.IO).launch {
            val tag = "BackupMedia"
            Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")

            val loadingState = viewModel.loadingState
            val progress = viewModel.progress
            val topBarTitle = viewModel.topBarTitle
            val taskList = viewModel.taskList.value
            val objectList = viewModel.objectList.value
            val allDone = viewModel.allDone

            // 检查列表
            if (globalObject.mediaInfoBackupMap.value.isEmpty()) {
                globalObject.mediaInfoBackupMap.emit(Command.getMediaInfoBackupMap())
            }
            if (globalObject.mediaInfoRestoreMap.value.isEmpty()) {
                globalObject.mediaInfoRestoreMap.emit(Command.getMediaInfoRestoreMap())
            }
            Logcat.getInstance().actionLogAddLine(tag, "Global map check finished.")

            // 备份信息列表
            taskList.addAll(
                globalObject.mediaInfoBackupMap.value.values.toList()
                    .filter { it.backupDetail.data }
                    .map {
                        ProcessingTask(
                            appName = it.name,
                            packageName = it.path,
                            appIcon = AppCompatResources.getDrawable(
                                context,
                                R.drawable.ic_round_android
                            ),
                            selectApp = false,
                            selectData = true,
                            taskState = TaskState.Waiting,
                            objectList = listOf()
                        )
                    })

            Logcat.getInstance().actionLogAddLine(tag, "Task added, size: ${taskList.size}.")

            val date =
                if (App.globalContext.readBackupStrategy() == BackupStrategy.Cover) GlobalString.cover else App.getTimeStamp()

            Logcat.getInstance().actionLogAddLine(tag, "Timestamp: ${date}.")
            Logcat.getInstance().actionLogAddLine(tag, "Date: ${Command.getDate(date)}.")

            // 前期准备完成
            loadingState.value = LoadingState.Success
            for (i in 0 until taskList.size) {
                // 清除备份目标
                objectList.clear()

                // 进入Processing状态
                taskList[i] = taskList[i].copy(taskState = TaskState.Processing)
                val task = taskList[i]
                val mediaInfoBackup =
                    globalObject.mediaInfoBackupMap.value[task.appName]!!

                // 滑动至目标媒体
                if (viewModel.listStateIsInitialized) {
                    if (viewModel.scopeIsInitialized) {
                        viewModel.scope.launch {
                            viewModel.listState.animateScrollToItem(i)
                        }
                    }
                }

                var isSuccess = true
                val outPutPath = "${Path.getBackupMediaSavePath()}/${task.appName}/${date}"

                Logcat.getInstance().actionLogAddLine(tag, "Name: ${task.appName}.")
                Logcat.getInstance().actionLogAddLine(tag, "Path: ${task.packageName}.")

                if (task.selectData) {
                    // 添加Data备份项
                    objectList.add(
                        ProcessObjectItem(
                            state = TaskState.Waiting,
                            title = GlobalString.ready,
                            subtitle = GlobalString.pleaseWait,
                            type = ProcessingObjectType.DATA
                        )
                    )
                }
                for (j in 0 until objectList.size) {
                    if (viewModel.isCancel.value) break
                    objectList[j] = objectList[j].copy(state = TaskState.Processing)
                    when (objectList[j].type) {
                        ProcessingObjectType.DATA -> {
                            Command.compress(
                                "tar",
                                "media",
                                task.appName,
                                outPutPath,
                                task.packageName,
                                mediaInfoBackup.backupDetail.size
                            ) { type, line ->
                                objectList[j] =
                                    parseObjectItemBySrc(type, line ?: "", objectList[j])
                            }.apply {
                                if (!this) {
                                    objectList[j] = objectList[j].copy(state = TaskState.Failed)
                                    isSuccess = false
                                }
                                // 保存大小
                                else {
                                    objectList[j] = objectList[j].copy(state = TaskState.Success)
                                    mediaInfoBackup.backupDetail.size = Command.countSize(
                                        task.packageName, 1
                                    )
                                }
                            }
                        }
                        else -> {
                            isSuccess = false
                        }
                    }
                }
                if (viewModel.isCancel.value) break

                mediaInfoBackup.backupDetail.date = date
                if (isSuccess) {
                    val detail = MediaInfoDetailBase(
                        data = false,
                        size = mediaInfoBackup.backupDetail.size,
                        date = mediaInfoBackup.backupDetail.date
                    )

                    if (globalObject.mediaInfoRestoreMap.value.containsKey(
                            task.appName
                        ).not()
                    ) {
                        globalObject.mediaInfoRestoreMap.value[task.appName] =
                            MediaInfoRestore()
                    }
                    val mediaInfoRestore =
                        globalObject.mediaInfoRestoreMap.value[task.appName]!!.apply {
                            this.name = mediaInfoBackup.name
                            this.path = mediaInfoBackup.path
                        }

                    val detailIndex =
                        mediaInfoRestore.detailRestoreList.indexOfFirst { date == it.date }
                    if (detailIndex == -1) {
                        // RestoreList中不存在该Item
                        mediaInfoRestore.detailRestoreList.add(detail)
                        mediaInfoRestore.restoreIndex++
                    } else {
                        // RestoreList中已存在该Item
                        mediaInfoRestore.detailRestoreList[detailIndex] = detail
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

            // 保存列表数据
            GsonUtil.saveMediaInfoBackupMapToFile(globalObject.mediaInfoBackupMap.value)
            GsonUtil.saveMediaInfoRestoreMapToFile(globalObject.mediaInfoRestoreMap.value)
            Logcat.getInstance().actionLogAddLine(tag, "Save global map.")
            topBarTitle.value = "${context.getString(R.string.backup_finished)}!"
            allDone.value = true
            Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")
        }
    }
}
