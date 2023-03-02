package com.xayah.databackup.ui.activity.processing.action

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.xayah.databackup.R
import com.xayah.databackup.data.LoadingState
import com.xayah.databackup.data.ProcessingObjectType
import com.xayah.databackup.data.TaskState
import com.xayah.databackup.ui.activity.processing.ProcessingViewModel
import com.xayah.databackup.ui.activity.processing.components.ProcessObjectItem
import com.xayah.databackup.ui.activity.processing.components.ProcessingTask
import com.xayah.databackup.ui.activity.processing.components.parseObjectItemBySrc
import com.xayah.databackup.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun onRestoreMediaProcessing(
    viewModel: ProcessingViewModel,
    context: Context,
    globalObject: GlobalObject
) {
    if (viewModel.isFirst.value) {
        viewModel.isFirst.value = false
        CoroutineScope(Dispatchers.IO).launch {
            val tag = "RestoreMedia"
            Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")

            val loadingState = viewModel.loadingState
            val progress = viewModel.progress
            val topBarTitle = viewModel.topBarTitle
            val taskList = viewModel.taskList.value
            val objectList = viewModel.objectList.value.apply {
                add(ProcessObjectItem(type = ProcessingObjectType.DATA))
            }
            val allDone = viewModel.allDone

            // 检查列表
            if (globalObject.mediaInfoRestoreMap.value.isEmpty()) {
                globalObject.mediaInfoRestoreMap.emit(Command.getMediaInfoRestoreMap())
            }
            Logcat.getInstance().actionLogAddLine(tag, "Global map check finished.")

            // 备份信息列表
            taskList.addAll(
                globalObject.mediaInfoRestoreMap.value.values.toList()
                    .filter { if (it.detailRestoreList.isNotEmpty()) it.selectData.value else false }
                    .map {
                        ProcessingTask(
                            appName = it.name,
                            packageName = it.path.ifEmpty { it.name },
                            appIcon = AppCompatResources.getDrawable(
                                context,
                                R.drawable.ic_round_android
                            ),
                            selectApp = false,
                            selectData = true,
                            objectList = listOf()
                        )
                    })

            Logcat.getInstance().actionLogAddLine(tag, "Task added, size: ${taskList.size}.")

            // 前期准备完成
            loadingState.value = LoadingState.Success
            topBarTitle.value =
                "${context.getString(R.string.restoring)}(${progress.value}/${taskList.size})"
            for (i in 0 until taskList.size) {
                // 重置恢复目标
                objectList[0].apply {
                    state.value = TaskState.Waiting
                    title.value = GlobalString.ready
                    visible.value = false
                    subtitle.value = GlobalString.pleaseWait
                }

                // 进入Processing状态
                taskList[i].taskState.value = TaskState.Processing
                val task = taskList[i]
                val mediaInfoRestore =
                    globalObject.mediaInfoRestoreMap.value[task.appName]!!

                // 滑动至目标应用
                if (viewModel.listStateIsInitialized) {
                    if (viewModel.scopeIsInitialized) {
                        viewModel.scope.launch {
                            viewModel.listState.animateScrollToItem(i)
                        }
                    }
                }

                var isSuccess = true
                val inPath =
                    "${Path.getBackupMediaSavePath()}/${task.appName}/${mediaInfoRestore.detailRestoreList[mediaInfoRestore.restoreIndex].date}"

                Logcat.getInstance().actionLogAddLine(tag, "Name: ${task.appName}.")
                Logcat.getInstance().actionLogAddLine(tag, "Path: ${task.packageName}.")

                if (task.selectData) {
                    // 添加Data备份项
                    objectList[0].visible.value = true
                }
                for (j in 0 until objectList.size) {
                    if (viewModel.isCancel.value) break
                    if (objectList[j].visible.value) {
                        objectList[j].state.value = TaskState.Processing
                        when (objectList[j].type) {
                            ProcessingObjectType.DATA -> {
                                val inputPath = "${inPath}/${task.appName}.tar"
                                Command.decompress(
                                    Command.getCompressionTypeByPath(inputPath),
                                    "media",
                                    inputPath,
                                    task.appName,
                                    task.packageName.replace("/${task.appName}", "")
                                ) { type, line ->
                                    parseObjectItemBySrc(type, line ?: "", objectList[j])
                                }.apply {
                                    if (!this) isSuccess = false
                                }
                            }
                            else -> {
                                isSuccess = false
                            }
                        }
                    }
                }
                if (viewModel.isCancel.value) break

                taskList[i].apply {
                    this.taskState.value = if (isSuccess) TaskState.Success else TaskState.Failed
                    this.objectList = objectList.toList()
                }

                progress.value += 1
                topBarTitle.value =
                    "${context.getString(R.string.restoring)}(${progress.value}/${taskList.size})"

            }

            topBarTitle.value = "${context.getString(R.string.restore_finished)}!"
            allDone.value = true
            Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")
        }
    }
}
