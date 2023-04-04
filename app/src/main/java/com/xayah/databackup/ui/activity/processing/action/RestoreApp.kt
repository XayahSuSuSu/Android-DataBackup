package com.xayah.databackup.ui.activity.processing.action

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.mutableStateOf
import androidx.core.graphics.drawable.toDrawable
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
import com.xayah.databackup.util.command.SELinux
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun onRestoreAppProcessing(viewModel: ProcessingViewModel, context: Context, globalObject: GlobalObject, retry: Boolean = false) {
    if (viewModel.isFirst.value) {
        viewModel.isFirst.value = false
        CoroutineScope(Dispatchers.IO).launch {
            val tag = "# RestoreApp #"
            Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")

            val loadingState = viewModel.loadingState
            val progress = viewModel.progress
            val topBarTitle = viewModel.topBarTitle
            val taskList = viewModel.taskList.value
            val objectList = viewModel.objectList.value.apply {
                clear()
                addAll(listOf(DataType.APK, DataType.USER, DataType.USER_DE, DataType.DATA, DataType.OBB).map {
                    ProcessObjectItem(type = it)
                })
            }
            val allDone = viewModel.allDone

            // Check global map
            if (globalObject.appInfoRestoreMap.value.isEmpty()) {
                globalObject.appInfoRestoreMap.emit(Command.getAppInfoRestoreMap())
            }
            Logcat.getInstance().actionLogAddLine(tag, "Global map check finished.")

            if (retry.not()) {
                // Add processing tasks
                taskList.addAll(
                    globalObject.appInfoRestoreMap.value.values.toList()
                        .filter { if (it.detailRestoreList.isNotEmpty()) it.selectApp.value || it.selectData.value else false }
                        .map {
                            ProcessingTask(
                                appName = it.detailBase.appName,
                                packageName = it.detailBase.packageName,
                                appIcon = it.detailBase.appIcon,
                                selectApp = it.selectApp.value,
                                selectData = it.selectData.value,
                                objectList = listOf()
                            ).apply {
                                if (App.globalContext.readIsReadIcon()) {
                                    try {
                                        val task = this
                                        val bytes = RootService.getInstance().readBytesByDescriptor("${Path.getBackupDataSavePath()}/${it.detailBase.packageName}/icon.png")
                                        task.appIcon = (BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toDrawable(context.resources))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        })
            } else {
                Logcat.getInstance().actionLogAddLine(tag, "Retrying.")
            }

            Logcat.getInstance().actionLogAddLine(tag, "Task added, size: ${taskList.size}.")

            val userId = App.globalContext.readRestoreUser()
            val compressionType = CompressionType.of(App.globalContext.readCompressionType())

            Logcat.getInstance().actionLogAddLine(tag, "userId: ${userId}.")
            Logcat.getInstance().actionLogAddLine(tag, "CompressionType: ${compressionType.type}.")

            // Early stage finished
            loadingState.value = LoadingState.Success
            topBarTitle.value =
                "${context.getString(R.string.restoring)}(${progress.value}/${taskList.size})"
            for ((index, i) in taskList.withIndex()) {
                // Skip while retrying not-failed task
                if (retry && i.taskState.value != TaskState.Failed) continue

                // Reset object list
                for (j in objectList) {
                    j.apply {
                        state.value = TaskState.Waiting
                        title.value = GlobalString.ready
                        visible.value = false
                        subtitle.value = GlobalString.pleaseWait
                    }
                }

                // Enter processing state
                i.taskState.value = TaskState.Processing
                val appInfoRestore = globalObject.appInfoRestoreMap.value[i.packageName]!!

                // Scroll to processing task
                if (viewModel.listStateIsInitialized) {
                    if (viewModel.scopeIsInitialized) {
                        viewModel.scope.launch {
                            viewModel.listState.animateScrollToItem(index)
                        }
                    }
                }

                var isSuccess = true
                val packageName = i.packageName
                val date = appInfoRestore.date
                val inPath = "${Path.getBackupDataSavePath()}/${packageName}/${date}"
                val suffix = compressionType.suffix
                val apkPath = "${inPath}/apk.$suffix"
                val userPath = "${inPath}/user.$suffix"
                val userDePath = "${inPath}/user_de.$suffix"
                val dataPath = "${inPath}/data.$suffix"
                val obbPath = "${inPath}/obb.$suffix"

                Logcat.getInstance().actionLogAddLine(tag, "AppName: ${i.appName}.")
                Logcat.getInstance().actionLogAddLine(tag, "PackageName: ${i.packageName}.")

                if (i.selectApp) {
                    objectList[0].visible.value = true
                }
                if (i.selectData) {
                    // USER is required in any case
                    objectList[1].visible.value = true

                    // Detect the existence of USER_DE
                    if (RootService.getInstance().exists(userDePath)) {
                        objectList[2].visible.value = true
                    }

                    // Detect the existence of DATA
                    if (RootService.getInstance().exists(dataPath)) {
                        objectList[3].visible.value = true
                    }

                    // Detect the existence of OBB
                    if (RootService.getInstance().exists(obbPath)) {
                        objectList[4].visible.value = true
                    }
                }
                for ((jIndex, j) in objectList.withIndex()) {
                    if (viewModel.isCancel.value) break
                    if (j.visible.value) {
                        j.state.value = TaskState.Processing
                        when (j.type) {
                            DataType.APK -> {
                                isSuccess = Command.installAPK(compressionType, apkPath, packageName, userId, appInfoRestore.versionCode.toString())
                                { type, line ->
                                    onInfoUpdate(type, line ?: "", j)
                                }

                                // If the app isn't installed, the restoring can't move on
                                if (!isSuccess) {
                                    for (k in jIndex + 1 until objectList.size) {
                                        onInfoUpdate(ProcessError, "Apk not installed.", objectList[k])
                                    }
                                    break
                                }
                            }
                            DataType.USER -> {
                                // Read the original SELinux context
                                val (_, contextSELinux) = SELinux.getContext("${Path.getUserPath(userId)}/${packageName}")
                                Command.decompress(compressionType, DataType.USER, userPath, packageName, Path.getUserPath(userId))
                                { type, line ->
                                    onInfoUpdate(type, line ?: "", j)
                                }.apply {
                                    if (!this) isSuccess = false
                                }
                                Command.setOwnerAndSELinux(DataType.USER, packageName, "${Path.getUserPath(userId)}/${packageName}", userId, contextSELinux)
                                { type, line ->
                                    onInfoUpdate(type, line ?: "", j)
                                }.apply {
                                    if (!this) isSuccess = false
                                }
                            }
                            DataType.USER_DE -> {
                                // Read the original SELinux context
                                val (_, contextSELinux) = SELinux.getContext("${Path.getUserDePath(userId)}/${packageName}")
                                Command.decompress(compressionType, DataType.USER_DE, userDePath, packageName, Path.getUserDePath(userId))
                                { type, line ->
                                    onInfoUpdate(type, line ?: "", j)
                                }.apply {
                                    if (!this) isSuccess = false
                                }
                                Command.setOwnerAndSELinux(DataType.USER_DE, packageName, "${Path.getUserDePath(userId)}/${packageName}", userId, contextSELinux)
                                { type, line ->
                                    onInfoUpdate(type, line ?: "", j)
                                }.apply {
                                    if (!this) isSuccess = false
                                }
                            }
                            DataType.DATA -> {
                                // Read the original SELinux context
                                val (_, contextSELinux) = SELinux.getContext("${Path.getDataPath(userId)}/${packageName}")
                                Command.decompress(compressionType, DataType.DATA, dataPath, packageName, Path.getDataPath(userId))
                                { type, line ->
                                    onInfoUpdate(type, line ?: "", j)
                                }.apply {
                                    if (!this) isSuccess = false
                                }
                                Command.setOwnerAndSELinux(DataType.DATA, packageName, "${Path.getDataPath(userId)}/${packageName}", userId, contextSELinux)
                                { type, line ->
                                    onInfoUpdate(type, line ?: "", j)
                                }.apply {
                                    if (!this) isSuccess = false
                                }
                            }
                            DataType.OBB -> {
                                // Read the original SELinux context
                                val (_, contextSELinux) = SELinux.getContext("${Path.getObbPath(userId)}/${packageName}")
                                Command.decompress(compressionType, DataType.OBB, obbPath, packageName, Path.getObbPath(userId))
                                { type, line ->
                                    onInfoUpdate(type, line ?: "", j)
                                }.apply {
                                    if (!this) isSuccess = false
                                }
                                Command.setOwnerAndSELinux(DataType.OBB, packageName, "${Path.getObbPath(userId)}/${packageName}", userId, contextSELinux)
                                { type, line ->
                                    onInfoUpdate(type, line ?: "", j)
                                }.apply {
                                    if (!this) isSuccess = false
                                }
                            }
                            else -> {}
                        }
                    }
                }
                if (viewModel.isCancel.value) break

                i.apply {
                    if (isSuccess) {
                        if (appInfoRestore.selectApp.value) appInfoRestore.selectApp.value = false
                        if (appInfoRestore.selectData.value) appInfoRestore.selectData.value = false
                        this.taskState.value = TaskState.Success
                    } else {
                        this.taskState.value = TaskState.Failed
                    }
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
                topBarTitle.value = "${context.getString(R.string.restoring)}(${progress.value}/${taskList.size})"
            }

            GsonUtil.saveAppInfoRestoreMapToFile(globalObject.appInfoRestoreMap.value)
            Logcat.getInstance().actionLogAddLine(tag, "Save global map.")
            topBarTitle.value = "${context.getString(R.string.restore_finished)}!"
            allDone.targetState = true
            Logcat.getInstance().actionLogAddLine(tag, "===========${tag}===========")
        }
    }
}
