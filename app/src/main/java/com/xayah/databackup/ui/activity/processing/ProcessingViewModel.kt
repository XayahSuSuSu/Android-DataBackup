package com.xayah.databackup.ui.activity.processing

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.LoadingState
import com.xayah.databackup.data.ProcessingTaskFilter
import com.xayah.databackup.data.TaskState
import com.xayah.databackup.data.TypeBackupApp
import com.xayah.databackup.ui.activity.processing.components.ProcessObjectItem
import com.xayah.databackup.ui.activity.processing.components.ProcessingTask
import com.xayah.databackup.util.GlobalString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class ProcessingViewModel : ViewModel() {
    var listType = TypeBackupApp

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
    val allDone by lazy { MutableTransitionState(false) }
    val isFirst = MutableStateFlow(true)
    val isCancel = MutableStateFlow(false)

    fun refreshTaskList() {
        objectList.value.clear()
        when (filter.value) {
            ProcessingTaskFilter.None -> {
                for (i in taskList.value) {
                    i.visible.value = true
                }
            }
            ProcessingTaskFilter.Succeed -> {
                for (i in taskList.value) {
                    i.visible.value = i.taskState.value == TaskState.Success
                }
            }
            ProcessingTaskFilter.Failed -> {
                for (i in taskList.value) {
                    i.visible.value = i.taskState.value != TaskState.Success
                }
            }
        }
    }

    // 过滤
    val filter by lazy { MutableStateFlow(ProcessingTaskFilter.None) }

    lateinit var listState: LazyListState
    val listStateIsInitialized
        get() = this::listState.isInitialized

    lateinit var scope: CoroutineScope
    val scopeIsInitialized
        get() = this::scope.isInitialized
}