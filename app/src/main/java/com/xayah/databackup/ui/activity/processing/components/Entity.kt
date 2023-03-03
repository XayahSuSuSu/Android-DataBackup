package com.xayah.databackup.ui.activity.processing.components

import android.graphics.drawable.Drawable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.xayah.databackup.data.ProcessingObjectType
import com.xayah.databackup.data.TaskState
import com.xayah.databackup.util.GlobalString

data class ProcessObjectItem(
    val state: MutableState<TaskState> = mutableStateOf(TaskState.Waiting),
    val visible: MutableState<Boolean> = mutableStateOf(false),
    val title: MutableState<String> = mutableStateOf(GlobalString.ready),
    val subtitle: MutableState<String> = mutableStateOf(GlobalString.pleaseWait),
    val type: ProcessingObjectType,
)

/**
 * 备份应用信息
 */
data class ProcessingTask(
    var appName: String,
    var packageName: String,
    var appIcon: Drawable? = null,
    var selectApp: Boolean,
    var selectData: Boolean,
    val visible: MutableState<Boolean> = mutableStateOf(true),
    var taskState: MutableState<TaskState> = mutableStateOf(TaskState.Waiting),
    var objectList: List<ProcessObjectItem>
)