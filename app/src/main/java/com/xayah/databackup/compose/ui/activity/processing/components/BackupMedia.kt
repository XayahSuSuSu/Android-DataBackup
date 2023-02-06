package com.xayah.databackup.compose.ui.activity.processing.components

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.util.*

@ExperimentalMaterial3Api
@Composable
fun BackupMedia(allDone: MutableState<Boolean>, onFinish: () -> Unit) {
    /**
     * 全局单例对象
     */
    val globalObject = GlobalObject.getInstance()

    val context = LocalContext.current

    // 用于list带动画滑动
    val listState = rememberLazyListState()
    // Loading状态
    val (loadingState, setLoadingState) = remember {
        mutableStateOf(LoadingState.Loading)
    }
    // 标题栏标题
    var topBarTitle by remember {
        mutableStateOf(context.getString(R.string.loading))
    }
    // 进度
    var progress by remember {
        mutableStateOf(0)
    }
    // 备份对象列表
    val objectList = remember {
        mutableStateListOf<ProcessObjectItem>()
    }
    // 任务列表
    val taskList = remember {
        mutableStateListOf<ProcessingTask2>()
    }

    LaunchedEffect(null) {
        val tag = "BackupMedia"
        Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")

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
                    ProcessingTask2(
                        appName = it.name,
                        packageName = it.path,
                        appIcon = AppCompatResources.getDrawable(
                            context,
                            R.drawable.ic_round_android
                        ),
                        selectApp = false,
                        selectData = true,
                        taskState = TaskState.Waiting,
                    )
                })

        Logcat.getInstance().actionLogAddLine(tag, "Task added, size: ${taskList.size}.")

        val date =
            if (App.globalContext.readBackupStrategy() == BackupStrategy.Cover) GlobalString.cover else App.getTimeStamp()

        Logcat.getInstance().actionLogAddLine(tag, "Timestamp: ${date}.")
        Logcat.getInstance().actionLogAddLine(tag, "Date: ${Command.getDate(date)}.")

        // 前期准备完成
        setLoadingState(LoadingState.Success)
        for (i in 0 until taskList.size) {
            // 清除备份目标
            objectList.clear()

            // 进入Processing状态
            taskList[i] = taskList[i].copy(taskState = TaskState.Processing)
            val task = taskList[i]
            val mediaInfoBackup =
                globalObject.mediaInfoBackupMap.value[task.appName]!!

            // 滑动至目标媒体
            listState.animateScrollToItem(i)

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
                            objectList[j] = parseObjectItemBySrc(type, line ?: "", objectList[j])
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
                task.copy(taskState = if (isSuccess) TaskState.Success else TaskState.Failed)

            progress += 1
            topBarTitle = "${context.getString(R.string.backuping)}(${progress}/${taskList.size})"

        }

        // 保存列表数据
        GsonUtil.saveMediaInfoBackupMapToFile(globalObject.mediaInfoBackupMap.value)
        GsonUtil.saveMediaInfoRestoreMapToFile(globalObject.mediaInfoRestoreMap.value)
        Logcat.getInstance().actionLogAddLine(tag, "Save global map.")
        topBarTitle = "${context.getString(R.string.backup_finished)}!"
        allDone.value = true
        Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")
    }

    ProcessingScaffold(
        topBarTitle = topBarTitle,
        loadingState = loadingState,
        allDone = allDone.value,
        onFabClick = onFinish,
        objectList = objectList,
        taskList = taskList,
        listState = listState
    )
}
