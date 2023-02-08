package com.xayah.databackup.compose.ui.activity.processing

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.xayah.databackup.compose.ui.activity.processing.components.ProcessObjectItem
import com.xayah.databackup.data.LoadingState
import com.xayah.databackup.data.ProcessingTask
import com.xayah.databackup.util.GlobalString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class ProcessingViewModel : ViewModel() {
    // Loading状态
    val loadingState = MutableStateFlow(LoadingState.Loading)

    // 标题栏标题
    val topBarTitle = MutableStateFlow(GlobalString.loading)

    // 进度
    val progress = MutableStateFlow(0)

    // 备份对象列表
    val objectList = MutableStateFlow(SnapshotStateList<ProcessObjectItem>())

    // 任务列表
    val taskList = MutableStateFlow(SnapshotStateList<ProcessingTask>())
    val allDone = MutableStateFlow(false)
    val isFirst = MutableStateFlow(true)
    val isCancel = MutableStateFlow(false)

    lateinit var listState: LazyListState
    val listStateIsInitialized
        get() = this::listState.isInitialized

    lateinit var scope: CoroutineScope
    val scopeIsInitialized
        get() = this::scope.isInitialized
}