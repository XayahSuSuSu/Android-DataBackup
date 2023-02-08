package com.xayah.databackup.compose.ui.activity.processing.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.activity.guide.components.LoadingState
import com.xayah.databackup.compose.ui.activity.processing.ProcessingViewModel
import com.xayah.databackup.data.*
import com.xayah.databackup.util.GlobalString

/**
 * 解析src内容并返回ProcessObjectItem
 */
fun parseObjectItemBySrc(
    type: String,
    src: String,
    item: ProcessObjectItem
): ProcessObjectItem {
    try {
        when (type) {
            ProcessFinished -> {
                // 完成
                return item.copy(title = GlobalString.finished)
            }
            ProcessSkip -> {
                // 跳过
                return item.copy(subtitle = GlobalString.noChangeAndSkip)
            }
            ProcessCompressing -> {
                // 压缩中
                return item.copy(title = GlobalString.compressing)
            }
            ProcessDecompressing -> {
                // 解压中
                return item.copy(title = GlobalString.decompressing)
            }
            ProcessTesting -> {
                // 测试中
                return item.copy(title = GlobalString.testing)
            }
            ProcessSettingSELinux -> {
                // 设置SELinux中
                return item.copy(title = GlobalString.settingSELinux)
            }
            ProcessInstallingApk -> {
                // 安装APK中
                return item.copy(title = GlobalString.installing)
            }
            ProcessError -> {
                // 错误消息
                return item.copy(title = GlobalString.error, subtitle = src)
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
                        return item.copy(subtitle = "${GlobalString.size}: ${info[0]}, ${GlobalString.speed}: ${info[1]}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return item
}

@ExperimentalMaterial3Api
@Composable
fun ProcessingScaffold(viewModel: ProcessingViewModel, onFinish: () -> Unit) {
    val loadingState by viewModel.loadingState.collectAsState()
    val topBarTitle by viewModel.topBarTitle.collectAsState()
    val objectList by viewModel.objectList.collectAsState()
    val taskList by viewModel.taskList.collectAsState()
    val allDone by viewModel.allDone.collectAsState()
    viewModel.listState = rememberLazyListState()
    viewModel.scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val bigPadding = dimensionResource(R.dimen.padding_big)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (allDone)
                FloatingActionButton(
                    onClick = onFinish,
                ) {
                    Icon(Icons.Rounded.Done, null)
                }
        },
        floatingActionButtonPosition = FabPosition.Center,
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(bigPadding, nonePadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                innerPadding.calculateTopPadding()
                            )
                    )
                }
                if (loadingState != LoadingState.Success) {
                    item {
                        LoadingState(loadingState)
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
                                items(count = taskList.size) {
                                    Task(
                                        icon = rememberDrawablePainter(drawable = taskList[it].appIcon),
                                        appName = taskList[it].appName,
                                        taskState = taskList[it].taskState,
                                        clickable = allDone,
                                        onClick = {
                                            objectList.clear()
                                            objectList.addAll(taskList[it].objectList)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    items(count = objectList.size) {
                        ProcessObject(
                            cardState = objectList[it].state,
                            title = objectList[it].title,
                            subtitle = objectList[it].subtitle,
                            type = objectList[it].type,
                        )
                    }
                }
                item {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                innerPadding.calculateBottomPadding()
                            )
                    )
                }
            }
        }
    )
}
