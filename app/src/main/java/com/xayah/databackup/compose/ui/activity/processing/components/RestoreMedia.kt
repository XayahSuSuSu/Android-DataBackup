package com.xayah.databackup.compose.ui.activity.processing.components

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.xayah.databackup.R
import com.xayah.databackup.data.LoadingState
import com.xayah.databackup.data.ProcessingObjectType
import com.xayah.databackup.data.ProcessingTask
import com.xayah.databackup.data.TaskState
import com.xayah.databackup.util.*

@ExperimentalMaterial3Api
@Composable
fun RestoreMedia(allDone: MutableState<Boolean>, onFinish: () -> Unit) {
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
        mutableStateListOf<ProcessingTask>()
    }

    LaunchedEffect(null) {
        val tag = "RestoreMedia"
        Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")

        // 检查列表
        if (globalObject.mediaInfoRestoreMap.value.isEmpty()) {
            globalObject.mediaInfoRestoreMap.emit(Command.getMediaInfoRestoreMap())
        }
        Logcat.getInstance().actionLogAddLine(tag, "Global map check finished.")

        // 备份信息列表
        taskList.addAll(
            globalObject.mediaInfoRestoreMap.value.values.toList()
                .filter { if (it.detailRestoreList.isNotEmpty()) it.detailRestoreList[it.restoreIndex].data else false }
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

        // 前期准备完成
        setLoadingState(LoadingState.Success)
        for (i in 0 until taskList.size) {
            // 清除恢复目标
            objectList.clear()

            // 进入Processing状态
            taskList[i] = taskList[i].copy(taskState = TaskState.Processing)
            val task = taskList[i]
            val mediaInfoRestore =
                globalObject.mediaInfoRestoreMap.value[task.appName]!!

            // 滑动至目标应用
            listState.animateScrollToItem(i)

            var isSuccess = true
            val inPath =
                "${Path.getBackupMediaSavePath()}/${task.appName}/${mediaInfoRestore.detailRestoreList[mediaInfoRestore.restoreIndex].date}"

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
                        val inputPath = "${inPath}/${task.appName}.tar"
                        Command.decompress(
                            Command.getCompressionTypeByPath(inputPath),
                            "media",
                            inputPath,
                            task.appName,
                            task.packageName.replace("/${task.appName}", "")
                        ) { type, line ->
                            objectList[j] = parseObjectItemBySrc(type, line ?: "", objectList[j])
                        }.apply {
                            if (!this) isSuccess = false
                        }
                        if (!isSuccess) {
                            objectList[j] = objectList[j].copy(state = TaskState.Failed)
                        } else {
                            objectList[j] = objectList[j].copy(state = TaskState.Success)
                        }
                    }
                    else -> {
                        isSuccess = false
                    }
                }
            }

            taskList[i] =
                task.copy(
                    taskState = if (isSuccess) TaskState.Success else TaskState.Failed,
                    objectList = objectList.toList()
                )

            progress += 1
            topBarTitle = "${context.getString(R.string.restoring)}(${progress}/${taskList.size})"

        }

        topBarTitle = "${context.getString(R.string.restore_finished)}!"
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
        taskClickable = allDone.value,
        taskOnClick = {
            objectList.clear()
            objectList.addAll(it)
        },
        listState = listState
    )
}