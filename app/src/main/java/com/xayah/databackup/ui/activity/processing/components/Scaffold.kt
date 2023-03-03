package com.xayah.databackup.ui.activity.processing.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.ui.activity.processing.ProcessingViewModel
import com.xayah.databackup.ui.components.LoadingView
import com.xayah.databackup.ui.components.Scaffold
import com.xayah.databackup.ui.components.TopBarTitle
import com.xayah.databackup.ui.components.animation.ContentFade
import com.xayah.databackup.util.GlobalString

/**
 * 解析src内容并返回ProcessObjectItem
 */
fun parseObjectItemBySrc(
    type: String,
    src: String,
    item: ProcessObjectItem
) {
    if (item.state.value == TaskState.Failed) return
    when (type) {
        ProcessFinished -> {
            // 完成
            item.apply {
                state.value = TaskState.Success
                title.value = GlobalString.finished
            }
        }
        ProcessSkip -> {
            // 跳过
            item.apply {
                subtitle.value = GlobalString.noChangeAndSkip
            }
        }
        ProcessCompressing -> {
            // 压缩中
            item.apply {
                state.value = TaskState.Processing
                title.value = GlobalString.compressing
            }
        }
        ProcessDecompressing -> {
            // 解压中
            item.apply {
                state.value = TaskState.Processing
                title.value = GlobalString.decompressing
            }
        }
        ProcessTesting -> {
            // 测试中
            item.apply {
                state.value = TaskState.Processing
                title.value = GlobalString.testing
            }
        }
        ProcessSettingSELinux -> {
            // 设置SELinux中
            item.apply {
                state.value = TaskState.Processing
                title.value = GlobalString.settingSELinux
            }
        }
            ProcessInstallingApk -> {
                // 安装APK中
                item.apply {
                    state.value = TaskState.Processing
                    title.value = GlobalString.installing
                }
            }
            ProcessError -> {
                // 错误消息
                item.apply {
                    state.value = TaskState.Failed
                    title.value = GlobalString.error
                    subtitle.value = src
                }
            }
            ProcessShowTotal -> {
                // Total bytes written: 74311680 (71MiB, 238MiB/s)
                try {
                    "\\((.*?)\\)".toRegex().find(src)?.apply {
                        // (71MiB, 238MiB/s)
                        val newSrc = this.value
                            .replace("(", "")
                            .replace(")", "")
                            .replace(",", "")
                            .trim()
                        val info = newSrc.split(" ")
                        item.apply {
                            subtitle.value =
                                "${GlobalString.size}: ${info[0]}, ${GlobalString.speed}: ${info[1]}"
                        }
                    }
                } catch (_: Exception) {
                }
            }
    }

}

@ExperimentalMaterial3Api
@Composable
fun ProcessingScaffold(
    viewModel: ProcessingViewModel,
    actions: @Composable (RowScope.() -> Unit) = {},
    onFinish: () -> Unit
) {
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val loadingState by viewModel.loadingState.collectAsState()
    val topBarTitle by viewModel.topBarTitle.collectAsState()
    val objectList by viewModel.objectList.collectAsState()
    val taskList by viewModel.taskList.collectAsState()
    val allDone = viewModel.allDone
    viewModel.listState = rememberLazyListState()
    viewModel.scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            ContentFade(allDone) {
                FloatingActionButton(
                    modifier = Modifier.padding(mediumPadding),
                    onClick = onFinish,
                ) {
                    Icon(Icons.Rounded.Done, null)
                }
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    TopBarTitle(text = topBarTitle)
                },
                scrollBehavior = this,
                actions = {
                    ContentFade(allDone) {
                        actions()
                    }
                }
            )
        },
        topPaddingRate = 1,
        content = {
            if (loadingState != LoadingState.Success) {
                item {
                    LoadingView(loadingState)
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .clickable {}
                            .fillMaxWidth(),
                    ) {
                        LazyRow(
                            state = viewModel.listState
                        ) {
                            items(
                                count = taskList.size,
                                key = {
                                    taskList[it].packageName
                                }) {
                                Task(
                                    icon = rememberDrawablePainter(drawable = taskList[it].appIcon),
                                    appName = taskList[it].appName,
                                    visible = taskList[it].visible.value,
                                    taskState = taskList[it].taskState.value,
                                    clickable = allDone.currentState,
                                    onClick = {
                                        objectList.clear()
                                        objectList.addAll(taskList[it].objectList)
                                    }
                                )
                            }
                        }
                    }
                }
                items(
                    count = objectList.size,
                    key = {
                        objectList[it].type
                    }) {
                    ProcessObject(
                        cardState = objectList[it].state.value,
                        visible = objectList[it].visible.value,
                        title = objectList[it].title.value,
                        subtitle = objectList[it].subtitle.value,
                        type = objectList[it].type,
                    )
                }
            }
        }
    )
}
