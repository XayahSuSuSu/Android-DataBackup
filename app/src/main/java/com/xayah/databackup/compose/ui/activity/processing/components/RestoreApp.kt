package com.xayah.databackup.compose.ui.activity.processing.components

import android.graphics.BitmapFactory
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toDrawable
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.LoadingState
import com.xayah.databackup.data.ProcessingObjectType
import com.xayah.databackup.data.ProcessingTask2
import com.xayah.databackup.data.TaskState
import com.xayah.databackup.util.*

@ExperimentalMaterial3Api
@Composable
fun RestoreApp(onFinish: () -> Unit) {
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

    // 是否完成
    var allDone by remember {
        mutableStateOf(false)
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
        // 检查列表
        if (globalObject.appInfoRestoreMap.value.isEmpty()) {
            globalObject.appInfoRestoreMap.emit(Command.getAppInfoRestoreMap())
        }

        // 备份信息列表
        taskList.addAll(
            globalObject.appInfoRestoreMap.value.values.toList()
                .filter { if (it.detailRestoreList.isNotEmpty()) it.detailRestoreList[it.restoreIndex].selectApp || it.detailRestoreList[it.restoreIndex].selectData else false }
                .map {
                    ProcessingTask2(
                        appName = it.detailBase.appName,
                        packageName = it.detailBase.packageName,
                        appIcon = AppCompatResources.getDrawable(
                            context,
                            R.drawable.ic_round_android
                        ),
                        selectApp = it.detailRestoreList[it.restoreIndex].selectApp,
                        selectData = it.detailRestoreList[it.restoreIndex].selectData,
                        taskState = TaskState.Waiting,
                    ).apply {
                        if (App.globalContext.readIsReadIcon()) {
                            val task = this
                            SafeFile.create("${Path.getBackupDataSavePath()}/${it.detailBase.packageName}/icon.png") { file ->
                                file.apply {
                                    val bytes = readBytes()
                                    task.appIcon =
                                        (BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            .toDrawable(context.resources))
                                }
                            }
                        }
                    }
                })

        val userId = App.globalContext.readRestoreUser()
        val compressionType = App.globalContext.readCompressionType()
        // 前期准备完成
        setLoadingState(LoadingState.Success)
        for (i in 0 until taskList.size) {
            // 清除恢复目标
            objectList.clear()

            // 进入Processing状态
            taskList[i] = taskList[i].copy(taskState = TaskState.Processing)
            val task = taskList[i]
            val appInfoRestore =
                globalObject.appInfoRestoreMap.value[task.packageName]!!

            // 滑动至目标应用
            listState.animateScrollToItem(i)

            var isSuccess = true
            val packageName = task.packageName
            val date = appInfoRestore.detailRestoreList[appInfoRestore.restoreIndex].date
            val inPath = "${Path.getBackupDataSavePath()}/${packageName}/${date}"
            val suffix = Command.getSuffixByCompressionType(compressionType)
            val userPath = "${inPath}/user.$suffix"
            val userDePath = "${inPath}/user_de.$suffix"
            val dataPath = "${inPath}/data.$suffix"
            val obbPath = "${inPath}/obb.$suffix"

            if (task.selectApp) {
                // 检查是否备份APK
                objectList.add(
                    ProcessObjectItem(
                        state = TaskState.Waiting,
                        title = GlobalString.ready,
                        subtitle = GlobalString.pleaseWait,
                        type = ProcessingObjectType.APP
                    )
                )
            }
            if (task.selectData) {
                // 检查是否备份数据
                // USER为必备份项
                objectList.add(
                    ProcessObjectItem(
                        state = TaskState.Waiting,
                        title = GlobalString.ready,
                        subtitle = GlobalString.pleaseWait,
                        type = ProcessingObjectType.USER
                    )
                )
                // 检测是否存在USER_DE
                Command.ls(userDePath).apply {
                    if (this) {
                        objectList.add(
                            ProcessObjectItem(
                                state = TaskState.Waiting,
                                title = GlobalString.ready,
                                subtitle = GlobalString.pleaseWait,
                                type = ProcessingObjectType.USER_DE
                            )
                        )

                    }
                }
                // 检测是否存在DATA
                Command.ls(dataPath).apply {
                    if (this) {
                        objectList.add(
                            ProcessObjectItem(
                                state = TaskState.Waiting,
                                title = GlobalString.ready,
                                subtitle = GlobalString.pleaseWait,
                                type = ProcessingObjectType.DATA
                            )
                        )

                    }
                }
                // 检测是否存在OBB
                Command.ls(obbPath).apply {
                    if (this) {
                        objectList.add(
                            ProcessObjectItem(
                                state = TaskState.Waiting,
                                title = GlobalString.ready,
                                subtitle = GlobalString.pleaseWait,
                                type = ProcessingObjectType.OBB
                            )
                        )

                    }
                }
            }
            for (j in 0 until objectList.size) {
                objectList[j] = objectList[j].copy(state = TaskState.Processing)
                when (objectList[j].type) {
                    ProcessingObjectType.APP -> {
                        Command.installAPK(
                            inPath,
                            packageName,
                            userId,
                            appInfoRestore.detailRestoreList[appInfoRestore.restoreIndex].versionCode.toString()
                        ) {
                            it?.apply {
                                objectList[j] = parseObjectItemBySrc(this, objectList[j])
                            }
                        }

                        // 判断是否安装该应用
                        Bashrc.findPackage(userId, packageName).apply {
                            isSuccess = this.first
                            if (!isSuccess) {
                                objectList[j] = objectList[j].copy(state = TaskState.Failed)
                                Logcat.getInstance()
                                    .shellLogAddLine("${packageName}: Not installed")
                            } else {
                                objectList[j] = objectList[j].copy(state = TaskState.Success)
                            }
                        }

                        // 如果未安装该应用, 则无法完成后续恢复
                        if (!isSuccess) continue
                    }
                    ProcessingObjectType.USER -> {
                        // 读取原有SELinux context
                        val contextSELinux =
                            Bashrc.getSELinuxContext("${Path.getUserPath(userId)}/${packageName}")
                        // 恢复User
                        Command.decompress(
                            Command.getCompressionTypeByPath(userPath),
                            "user",
                            userPath,
                            packageName,
                            Path.getUserPath(userId)
                        ) {
                            it?.apply {
                                objectList[j] = parseObjectItemBySrc(this, objectList[j])
                            }
                        }.apply {
                            if (!this) isSuccess = false
                        }
                        Command.setOwnerAndSELinux(
                            "user",
                            packageName,
                            "${Path.getUserPath(userId)}/${packageName}",
                            userId,
                            contextSELinux
                        ) {
                            it?.apply {
                                objectList[j] = parseObjectItemBySrc(this, objectList[j])
                            }
                        }.apply {
                            if (!this) isSuccess = false
                        }
                        if (!isSuccess) {
                            objectList[j] = objectList[j].copy(state = TaskState.Failed)
                        } else {
                            objectList[j] = objectList[j].copy(state = TaskState.Success)
                        }
                    }
                    ProcessingObjectType.USER_DE -> {
                        // 读取原有SELinux context
                        val contextSELinux =
                            Bashrc.getSELinuxContext("${Path.getUserDePath(userId)}/${packageName}")
                        // 恢复User_de
                        Command.decompress(
                            Command.getCompressionTypeByPath(userDePath),
                            "user_de",
                            userDePath,
                            packageName,
                            Path.getUserDePath(userId)
                        ) {
                            it?.apply {
                                objectList[j] = parseObjectItemBySrc(this, objectList[j])
                            }
                        }.apply {
                            if (!this) isSuccess = false
                        }
                        Command.setOwnerAndSELinux(
                            "user_de",
                            packageName,
                            "${Path.getUserDePath(userId)}/${packageName}",
                            userId,
                            contextSELinux
                        ) {
                            it?.apply {
                                objectList[j] = parseObjectItemBySrc(this, objectList[j])
                            }
                        }.apply {
                            if (!this) isSuccess = false
                        }
                        if (!isSuccess) {
                            objectList[j] = objectList[j].copy(state = TaskState.Failed)
                        } else {
                            objectList[j] = objectList[j].copy(state = TaskState.Success)
                        }
                    }
                    ProcessingObjectType.DATA -> {
                        // 读取原有SELinux context
                        val contextSELinux =
                            Bashrc.getSELinuxContext("${Path.getDataPath(userId)}/${packageName}")
                        // 恢复Data
                        Command.decompress(
                            Command.getCompressionTypeByPath(dataPath),
                            "data",
                            dataPath,
                            packageName,
                            Path.getDataPath(userId)
                        ) {
                            it?.apply {
                                objectList[j] = parseObjectItemBySrc(this, objectList[j])
                            }
                        }.apply {
                            if (!this) isSuccess = false
                        }
                        Command.setOwnerAndSELinux(
                            "data",
                            packageName,
                            "${Path.getDataPath(userId)}/${packageName}",
                            userId,
                            contextSELinux
                        ) {
                            it?.apply {
                                objectList[j] = parseObjectItemBySrc(this, objectList[j])
                            }
                        }.apply {
                            if (!this) isSuccess = false
                        }
                        if (!isSuccess) {
                            objectList[j] = objectList[j].copy(state = TaskState.Failed)
                        } else {
                            objectList[j] = objectList[j].copy(state = TaskState.Success)
                        }
                    }
                    ProcessingObjectType.OBB -> {
                        // 读取原有SELinux context
                        val contextSELinux =
                            Bashrc.getSELinuxContext("${Path.getObbPath(userId)}/${packageName}")
                        // 恢复Obb
                        Command.decompress(
                            Command.getCompressionTypeByPath(obbPath),
                            "obb",
                            obbPath,
                            packageName,
                            Path.getObbPath(userId)
                        ) {
                            it?.apply {
                                objectList[j] = parseObjectItemBySrc(this, objectList[j])
                            }
                        }.apply {
                            if (!this) isSuccess = false
                        }
                        Command.setOwnerAndSELinux(
                            "obb",
                            packageName,
                            "${Path.getObbPath(userId)}/${packageName}",
                            userId,
                            contextSELinux
                        ) {
                            it?.apply {
                                objectList[j] = parseObjectItemBySrc(this, objectList[j])
                            }
                        }.apply {
                            if (!this) isSuccess = false
                        }
                        if (!isSuccess) {
                            objectList[j] = objectList[j].copy(state = TaskState.Failed)
                        } else {
                            objectList[j] = objectList[j].copy(state = TaskState.Success)
                        }
                    }
                }
            }

            taskList[i] =
                task.copy(taskState = if (isSuccess) TaskState.Success else TaskState.Failed)

            progress += 1
            topBarTitle = "${context.getString(R.string.restoring)}(${progress}/${taskList.size})"

        }

        topBarTitle = "${context.getString(R.string.restore_finished)}!"
        allDone = true
    }

    ProcessingScaffold(
        topBarTitle = topBarTitle,
        loadingState = loadingState,
        allDone = allDone,
        onFabClick = onFinish,
        objectList = objectList,
        taskList = taskList,
        listState = listState
    )
}