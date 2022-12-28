package com.xayah.databackup.activity.processing

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.adapter.ProcessingTaskAdapter
import com.xayah.databackup.data.AppInfoBaseNum
import com.xayah.databackup.data.ProcessingTask
import com.xayah.databackup.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProcessingRestoreAppActivity : ProcessingBaseActivity() {
    lateinit var viewModel: ProcessingBaseViewModel

    // 应用恢复列表
    private val appInfoRestoreList
        get() = App.appInfoList.value.filter { if (it.restoreList.isNotEmpty()) it.restoreList[it.restoreIndex].app || it.restoreList[it.restoreIndex].data else false }
            .toMutableList()
    private val appInfoRestoreListNum
        get() = run {
            val appInfoBaseNum = AppInfoBaseNum(0, 0)
            for (i in appInfoRestoreList) {
                if (i.restoreList[i.restoreIndex].app) appInfoBaseNum.appNum++
                if (i.restoreList[i.restoreIndex].data) appInfoBaseNum.dataNum++
            }
            appInfoBaseNum
        }

    // 任务列表
    private val processingTaskList by lazy {
        MutableStateFlow(mutableListOf<ProcessingTask>())
    }

    override fun initialize(viewModel: ProcessingBaseViewModel) {
        this.viewModel = viewModel
        viewModel.viewModelScope.launch {
            // 设置适配器
            viewModel.mAdapter.apply {
                for (i in appInfoRestoreList) processingTaskList.value.add(
                    ProcessingTask(
                        appName = i.appName,
                        packageName = i.packageName,
                        app = i.restoreList[i.restoreIndex].app,
                        data = i.restoreList[i.restoreIndex].data,
                        appIcon = i.appIcon
                    )
                )
                register(ProcessingTaskAdapter())
                items = processingTaskList.value
                notifyDataSetChanged()
            }

            // 设置备份状态
            viewModel.btnText.set(GlobalString.restore)
            viewModel.btnDesc.set(GlobalString.clickTheRightBtnToStart)
            viewModel.progressMax.set(appInfoRestoreList.size)
            viewModel.progressText.set("${GlobalString.progress}: ${viewModel.progress.get()}/${viewModel.progressMax.get()}")
            viewModel.totalTip.set(GlobalString.ready)
            appInfoRestoreListNum.apply {
                viewModel.totalProgress.set("${GlobalString.selected} ${this.appNum} ${GlobalString.application}, ${this.dataNum} ${GlobalString.data}, ${App.globalContext.readBackupUser()} ${GlobalString.backupUser}, ${App.globalContext.readRestoreUser()} ${GlobalString.restoreUser}")
            }
            viewModel.isReady.set(true)
            viewModel.isFinished.postValue(false)
        }
    }

    override fun onFabClick() {
        viewModel.viewModelScope.launch {
            if (!viewModel.isFinished.value!!) withContext(Dispatchers.IO) {
                viewModel.isProcessing.set(true)
                viewModel.totalTip.set(GlobalString.restoreProcessing)
                for ((index, i) in appInfoRestoreList.withIndex()) {
                    // 准备备份卡片数据
                    viewModel.appName.set(i.appName)
                    viewModel.packageName.set(i.packageName)
                    viewModel.appVersion.set(i.restoreList[i.restoreIndex].versionName)
                    viewModel.appIcon.set(i.appIcon)
                    viewModel.isBackupApk.set(i.restoreList[i.restoreIndex].app)

                    val packageName = viewModel.packageName.get()!!
                    val userId = App.globalContext.readRestoreUser()
                    val date = i.restoreList[i.restoreIndex].date
                    val inPath = "${Path.getBackupDataSavePath()}/${packageName}/${date}"
                    val userPath = "${inPath}/user.tar*"
                    val userDePath = "${inPath}/user_de.tar*"
                    val dataPath = "${inPath}/data.tar*"
                    val obbPath = "${inPath}/obb.tar*"
                    if (i.restoreList[i.restoreIndex].data) {
                        Command.ls(userPath).apply { viewModel.isBackupUser.set(this) }
                        Command.ls(userDePath).apply { viewModel.isBackupUserDe.set(this) }
                        Command.ls(dataPath).apply { viewModel.isBackupData.set(this) }
                        Command.ls(obbPath).apply { viewModel.isBackupObb.set(this) }
                    } else {
                        viewModel.isBackupUser.set(false)
                        viewModel.isBackupUserDe.set(false)
                        viewModel.isBackupData.set(false)
                        viewModel.isBackupObb.set(false)
                    }

                    // 开始恢复
                    var state = true // 该任务是否成功完成
                    if (viewModel.isBackupApk.get()) {
                        // 恢复应用
                        viewModel.processingApk.set(true)
                        Command.installAPK(
                            inPath,
                            packageName,
                            userId,
                            i.restoreList[i.restoreIndex].versionCode.toString()
                        ) { setSizeAndSpeed(viewModel, it) }.apply {
                            state = this
                        }
                        viewModel.processingApk.set(false)
                        if (!state) {
                            viewModel.failedList.value.add(processingTaskList.value[index])
                            continue
                        }
                        initializeSizeAndSpeed(viewModel)
                    }

                    // 判断是否安装该应用
                    Bashrc.findPackage(userId, packageName).apply {
                        if (!this.first) {
                            // 未安装
                            viewModel.isBackupUser.set(false)
                            viewModel.isBackupData.set(false)
                            viewModel.isBackupObb.set(false)
                            state = false
                            App.logcat.addLine("${packageName}: Not installed")
                        }
                    }

                    if (viewModel.isBackupUser.get()) {
                        // 恢复User
                        viewModel.processingUser.set(true)
                        Command.decompress(
                            Command.getCompressionTypeByPath(userPath),
                            "user",
                            userPath,
                            packageName,
                            Path.getUserPath(userId)
                        ) { setSizeAndSpeed(viewModel, it) }.apply {
                            if (!this) state = false
                        }
                        Command.setOwnerAndSELinux(
                            "user",
                            packageName,
                            "${Path.getUserPath(userId)}/${packageName}",
                            userId
                        )
                        viewModel.processingUser.set(false)
                        initializeSizeAndSpeed(viewModel)
                    }
                    if (viewModel.isBackupUserDe.get()) {
                        // 恢复User_de
                        viewModel.processingUserDe.set(true)
                        Command.decompress(
                            Command.getCompressionTypeByPath(userDePath),
                            "user_de",
                            userDePath,
                            packageName,
                            Path.getUserDePath(userId)
                        ) { setSizeAndSpeed(viewModel, it) }.apply {
                            if (!this) state = false
                        }
                        Command.setOwnerAndSELinux(
                            "user_de",
                            packageName,
                            "${Path.getUserDePath(userId)}/${packageName}",
                            userId
                        )
                        viewModel.processingUserDe.set(false)
                        initializeSizeAndSpeed(viewModel)
                    }
                    if (viewModel.isBackupData.get()) {
                        // 恢复Data
                        viewModel.processingData.set(true)
                        Command.decompress(
                            Command.getCompressionTypeByPath(dataPath),
                            "data",
                            dataPath,
                            packageName,
                            Path.getDataPath(userId)
                        ) { setSizeAndSpeed(viewModel, it) }.apply {
                            if (!this) state = false
                        }
                        Command.setOwnerAndSELinux(
                            "data",
                            packageName,
                            "${Path.getDataPath(userId)}/${packageName}",
                            userId
                        )
                        viewModel.processingData.set(false)
                        initializeSizeAndSpeed(viewModel)
                    }
                    if (viewModel.isBackupObb.get()) {
                        // 恢复Obb
                        viewModel.processingObb.set(true)
                        Command.decompress(
                            Command.getCompressionTypeByPath(obbPath),
                            "obb",
                            obbPath,
                            packageName,
                            Path.getObbPath(userId)
                        ) { setSizeAndSpeed(viewModel, it) }.apply {
                            if (!this) state = false
                        }
                        Command.setOwnerAndSELinux(
                            "obb", packageName, "${Path.getObbPath(userId)}/${packageName}", userId
                        )
                        viewModel.processingObb.set(false)
                        initializeSizeAndSpeed(viewModel)
                    }
                    if (state) {
                        viewModel.successList.value.add(processingTaskList.value[index])
                    } else {
                        viewModel.failedList.value.add(processingTaskList.value[index])
                    }
                    viewModel.progress.set(index + 1)
                    viewModel.progressText.set("${GlobalString.progress}: ${viewModel.progress.get()}/${viewModel.progressMax.get()}")
                }
                viewModel.totalTip.set(GlobalString.restoreFinished)
                viewModel.totalProgress.set("${viewModel.successNum + viewModel.failedNum} ${GlobalString.total}")
                viewModel.isProcessing.set(false)
                viewModel.isFinished.postValue(true)
                viewModel.btnText.set(GlobalString.finish)
                viewModel.btnDesc.set(GlobalString.clickTheRightBtnToFinish)
                Bashrc.moveLogToOut()
            } else {
                finish()
            }
        }
    }
}