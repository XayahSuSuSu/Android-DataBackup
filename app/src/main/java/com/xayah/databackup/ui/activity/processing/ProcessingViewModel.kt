package com.xayah.databackup.ui.activity.processing

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.lazy.LazyListState
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
import kotlinx.coroutines.flow.asStateFlow

class ProcessingViewModel : ViewModel() {
    var listType = TypeBackupApp

    // Loading状态
    val loadingState = MutableStateFlow(LoadingState.Loading)

    // 标题栏标题
    val topBarTitle = MutableStateFlow(GlobalString.loading)

    // 进度
    val progress = MutableStateFlow(0)

    // 备份对象列表
    private val _objectList = MutableStateFlow(listOf<ProcessObjectItem>())
    val objectList = _objectList.asStateFlow()

    fun emitObjectList(list: List<ProcessObjectItem>) {
        _objectList.value = list
    }

    // 任务列表
    private val _taskList = MutableStateFlow(listOf<ProcessingTask>())
    val taskList = _taskList.asStateFlow()

    fun emitTaskList(list: List<ProcessingTask>) {
        _taskList.value = list
    }

    val allDone by lazy { MutableTransitionState(false) }
    val isFirst = MutableStateFlow(true)
    val isCancel = MutableStateFlow(false)

    fun refreshTaskList() {
        emitObjectList(listOf())
        val taskList = taskList.value.toMutableList()
        when (filter.value) {
            ProcessingTaskFilter.None -> {
                for (i in taskList) {
                    i.visible.value = true
                }
            }

            ProcessingTaskFilter.Succeed -> {
                for (i in taskList) {
                    i.visible.value = i.taskState.value == TaskState.Success
                }
            }
            ProcessingTaskFilter.Failed -> {
                for (i in taskList) {
                    i.visible.value = i.taskState.value != TaskState.Success
                }
            }
        }
        emitTaskList(taskList)
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