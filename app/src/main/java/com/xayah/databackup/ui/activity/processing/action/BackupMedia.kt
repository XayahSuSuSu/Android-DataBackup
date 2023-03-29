package com.xayah.databackup.ui.activity.processing.action

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.mutableStateOf
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.librootservice.RootService
import com.xayah.databackup.ui.activity.processing.ProcessingViewModel
import com.xayah.databackup.ui.activity.processing.components.ProcessObjectItem
import com.xayah.databackup.ui.activity.processing.components.ProcessingTask
import com.xayah.databackup.ui.activity.processing.components.onInfoUpdate
import com.xayah.databackup.util.*
import com.xayah.databackup.util.command.Command
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun onBackupMediaProcessing(viewModel: ProcessingViewModel, context: Context, globalObject: GlobalObject, retry: Boolean = false) {
    if (viewModel.isFirst.value) {
        viewModel.isFirst.value = false
        CoroutineScope(Dispatchers.IO).launch {
            val tag = "# BackupMedia #"
            Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")

            val loadingState = viewModel.loadingState
            val progress = viewModel.progress
            val topBarTitle = viewModel.topBarTitle
            val taskList = viewModel.taskList.value
            val objectList = viewModel.objectList.value.apply {
                clear()
                add(ProcessObjectItem(type = DataType.DATA))
            }
            val allDone = viewModel.allDone

            // Check global map
            if (globalObject.mediaInfoBackupMap.value.isEmpty()) {
                globalObject.mediaInfoBackupMap.emit(Command.getMediaInfoBackupMap())
            }
            if (globalObject.mediaInfoRestoreMap.value.isEmpty()) {
                globalObject.mediaInfoRestoreMap.emit(Command.getMediaInfoRestoreMap())
            }
            Logcat.getInstance().actionLogAddLine(tag, "Global map check finished.")

            if (retry.not()) {
                // Add processing tasks
                taskList.addAll(
                    globalObject.mediaInfoBackupMap.value.values.toList()
                        .filter { it.selectData.value }
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
                                objectList = listOf()
                            )
                        })
            } else {
                Logcat.getInstance().actionLogAddLine(tag, "Retrying.")
            }

            Logcat.getInstance().actionLogAddLine(tag, "Task added, size: ${taskList.size}.")

            val date = if (App.globalContext.readBackupStrategy() == BackupStrategy.Cover) GlobalString.cover else App.getTimeStamp()
            val compatibleMode = App.globalContext.readCompatibleMode()

            Logcat.getInstance().actionLogAddLine(tag, "Timestamp: ${date}.")
            Logcat.getInstance().actionLogAddLine(tag, "Date: ${Command.getDate(date)}.")

            // Early stage finished
            loadingState.value = LoadingState.Success
            topBarTitle.value = "${context.getString(R.string.backuping)}(${progress.value}/${taskList.size})"
            for ((index, i) in taskList.withIndex()) {
                // Skip while retrying not-failed task
                if (retry && i.taskState.value != TaskState.Failed) continue

                // Reset object list
                objectList[0].apply {
                    state.value = TaskState.Waiting
                    title.value = GlobalString.ready
                    visible.value = false
                    subtitle.value = GlobalString.pleaseWait
                }

                // Enter processing state
                i.taskState.value = TaskState.Processing
                val mediaInfoBackup = globalObject.mediaInfoBackupMap.value[i.appName]!!

                // Scroll to processing task
                if (viewModel.listStateIsInitialized) {
                    if (viewModel.scopeIsInitialized) {
                        viewModel.scope.launch {
                            viewModel.listState.animateScrollToItem(index)
                        }
                    }
                }

                var isSuccess = true
                val outPutPath = "${Path.getBackupMediaSavePath()}/${i.appName}/${date}"

                Logcat.getInstance().actionLogAddLine(tag, "Name: ${i.appName}.")
                Logcat.getInstance().actionLogAddLine(tag, "Path: ${i.packageName}.")

                if (i.selectData) {
                    objectList[0].visible.value = true
                }
                for (j in objectList) {
                    if (viewModel.isCancel.value) break
                    if (j.visible.value) {
                        j.state.value = TaskState.Processing
                        when (j.type) {
                            DataType.DATA -> {
                                Command.compress(CompressionType.TAR, DataType.MEDIA, i.appName, outPutPath, i.packageName, mediaInfoBackup.backupDetail.size, compatibleMode)
                                { type, line ->
                                    onInfoUpdate(type, line ?: "", j)
                                }.apply {
                                    if (!this) {
                                        isSuccess = false
                                    } else {
                                        // Save the size of archive
                                        mediaInfoBackup.backupDetail.size = RootService.getInstance().countSize(i.packageName).toString()
                                    }
                                }
                            }
                            else -> {
                                isSuccess = false
                            }
                        }
                    }
                }
                if (viewModel.isCancel.value) break

                mediaInfoBackup.backupDetail.date = date
                if (isSuccess) {
                    val detail = MediaInfoDetailBase(
                        data = mutableStateOf(false),
                        size = mediaInfoBackup.backupDetail.size,
                        date = mediaInfoBackup.backupDetail.date
                    )

                    if (globalObject.mediaInfoRestoreMap.value.containsKey(i.appName).not()) {
                        globalObject.mediaInfoRestoreMap.value[i.appName] = MediaInfoRestore()
                    }
                    val mediaInfoRestore =
                        globalObject.mediaInfoRestoreMap.value[i.appName]!!.apply {
                            this.name = mediaInfoBackup.name
                            this.path = mediaInfoBackup.path
                        }

                    val detailIndex =
                        mediaInfoRestore.detailRestoreList.indexOfFirst { date == it.date }
                    if (detailIndex == -1) {
                        mediaInfoRestore.detailRestoreList.add(detail)
                        mediaInfoRestore.restoreIndex++
                    } else {
                        mediaInfoRestore.detailRestoreList[detailIndex] = detail
                    }
                }

                i.apply {
                    this.taskState.value = if (isSuccess) TaskState.Success else TaskState.Failed
                    val list = mutableListOf<ProcessObjectItem>()
                    for (j in objectList) {
                        list.add(
                            ProcessObjectItem(
                                state = mutableStateOf(j.state.value),
                                visible = mutableStateOf(j.visible.value),
                                title = mutableStateOf(j.title.value),
                                subtitle = mutableStateOf(j.subtitle.value),
                                type = j.type,
                            )
                        )
                    }
                    this.objectList = list.toList()
                }

                progress.value += 1
                topBarTitle.value = "${context.getString(R.string.backuping)}(${progress.value}/${taskList.size})"
            }

            // Save lists
            GsonUtil.saveMediaInfoBackupMapToFile(globalObject.mediaInfoBackupMap.value)
            GsonUtil.saveMediaInfoRestoreMapToFile(globalObject.mediaInfoRestoreMap.value)
            Logcat.getInstance().actionLogAddLine(tag, "Save global map.")
            topBarTitle.value = "${context.getString(R.string.backup_finished)}!"
            allDone.targetState = true
            Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")
        }
    }
}
