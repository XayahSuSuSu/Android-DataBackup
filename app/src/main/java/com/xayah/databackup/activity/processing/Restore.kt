package com.xayah.databackup.activity.processing

import android.view.View
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.adapter.ProcessingTaskAdapter
import com.xayah.databackup.data.AppInfoBaseNum
import com.xayah.databackup.data.ProcessingTask
import com.xayah.databackup.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Restore(private val viewModel: ProcessingViewModel) {
    private lateinit var dataBinding: DataBinding

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
    private val appInfoRestoreListTotalNum
        get() = appInfoRestoreList.size

    // 媒体恢复列表
    private val mediaInfoRestoreList
        get() = App.mediaInfoRestoreList.value.filter { it.data }.toMutableList()
    private val mediaInfoRestoreListNum
        get() = mediaInfoRestoreList.size

    private var successNum = 0
    private var failedNum = 0

    init {
        viewModel.viewModelScope.launch {
            dataBinding = viewModel.dataBinding.apply {
                onRestoreClick = { v ->
                    if (viewModel.isMedia) onRestoreMediaClick(v)
                    else onRestoreAppClick(v)
                }
                btnText.set(GlobalString.restore)
                btnDesc.set(GlobalString.clickTheRightBtnToStart)
            }
            if (viewModel.isMedia) initializeMedia()
            else initializeApp()
            viewModel.dataBinding.isReady.set(true)
            viewModel.dataBinding.isFinished.postValue(false)
        }
    }

    private fun initializeApp() {
        viewModel.mAdapter.apply {
            val processingTaskList = mutableListOf<Any>()
            for (i in appInfoRestoreList) processingTaskList.add(
                ProcessingTask(
                    appName = i.appName,
                    packageName = i.packageName,
                    app = i.restoreList[i.restoreIndex].app,
                    data = i.restoreList[i.restoreIndex].data,
                    appIcon = i.appIcon
                )
            )
            register(ProcessingTaskAdapter())
            items = processingTaskList
            notifyDataSetChanged()
        }
        dataBinding.progressMax.set(appInfoRestoreListTotalNum)
        dataBinding.progressText.set("${GlobalString.progress}: ${dataBinding.progress.get()}/${dataBinding.progressMax.get()}")
        if (appInfoRestoreListTotalNum == 0) {
            dataBinding.btnText.set(GlobalString.finish)
            dataBinding.btnDesc.set(GlobalString.clickTheRightBtnToFinish)
        }
        dataBinding.totalTip.set(GlobalString.ready)
        appInfoRestoreListNum.apply {
            dataBinding.totalProgress.set("${GlobalString.selected} ${this.appNum} ${GlobalString.application}, ${this.dataNum} ${GlobalString.data}, ${App.globalContext.readBackupUser()} ${GlobalString.backupUser}, ${App.globalContext.readRestoreUser()} ${GlobalString.restoreUser}")
        }
    }

    private fun initializeMedia() {
        viewModel.mAdapter.apply {
            val processingTaskList = mutableListOf<Any>()
            for (i in mediaInfoRestoreList) processingTaskList.add(
                ProcessingTask(
                    appName = i.name,
                    packageName = i.path,
                    app = false,
                    data = true,
                    appIcon = null
                )
            )
            register(ProcessingTaskAdapter())
            items = processingTaskList
            notifyDataSetChanged()
        }
        dataBinding.progressMax.set(mediaInfoRestoreListNum)
        dataBinding.progressText.set("${GlobalString.progress}: ${dataBinding.progress.get()}/${dataBinding.progressMax.get()}")
        if (mediaInfoRestoreListNum == 0) {
            dataBinding.btnText.set(GlobalString.finish)
            dataBinding.btnDesc.set(GlobalString.clickTheRightBtnToFinish)
        }
        dataBinding.totalTip.set(GlobalString.ready)
        mediaInfoRestoreListNum.apply {
            dataBinding.totalProgress.set("${GlobalString.selected} $this ${GlobalString.data}")
        }

    }

    private fun setSizeAndSpeed(src: String?) {
        try {
            if (src == "install apk finished") {
                // 安装应用中
                dataBinding.size.set("0")
                dataBinding.sizeUnit.set("")
                dataBinding.speed.set(GlobalString.installing)
                dataBinding.speedUnit.set("")
            } else {
                val newSrc = src?.replace("[", "")?.replace("]", "")
                val sizeSrc = newSrc?.split(" ")?.filter { item -> item != "" }?.get(0)
                val speedSrc =
                    newSrc?.split(" ")?.filter { item -> item != "" }?.get(2)?.replace(" ", "")
                        ?.replace("]", "")
                dataBinding.size.set(sizeSrc?.filter { item -> item.isDigit() || item == '.' })
                dataBinding.sizeUnit.set(sizeSrc?.filter { item -> item.isLetter() })
                dataBinding.speed.set(speedSrc?.filter { item -> item.isDigit() || item == '.' })
                dataBinding.speedUnit.set(speedSrc?.filter { item -> item.isLetter() || item == '/' })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializeSizeAndSpeed() {
        dataBinding.size.set("0")
        dataBinding.sizeUnit.set("Mib")
        dataBinding.speed.set("0")
        dataBinding.speedUnit.set("Mib/s")
    }

    private fun onRestoreAppClick(v: View) {
        if (!dataBinding.isFinished.value!!) CoroutineScope(Dispatchers.IO).launch {
            dataBinding.isProcessing.set(true)
            dataBinding.totalTip.set(GlobalString.restoreProcessing)
            for ((index, i) in appInfoRestoreList.withIndex()) {
                // 准备备份卡片数据
                dataBinding.appName.set(i.appName)
                dataBinding.packageName.set(i.packageName)
                dataBinding.appVersion.set(i.restoreList[i.restoreIndex].versionName)
                dataBinding.appIcon.set(i.appIcon)
                dataBinding.isBackupApk.set(i.restoreList[i.restoreIndex].app)

                val packageName = dataBinding.packageName.get()!!
                val userId = App.globalContext.readRestoreUser()
                val date = i.restoreList[i.restoreIndex].date
                val inPath = "${Path.getBackupDataSavePath()}/${packageName}/${date}"
                val userPath = "${inPath}/user.tar*"
                val userDePath = "${inPath}/user_de.tar*"
                val dataPath = "${inPath}/data.tar*"
                val obbPath = "${inPath}/obb.tar*"
                if (i.restoreList[i.restoreIndex].data) {
                    Command.ls(userPath).apply { dataBinding.isBackupUser.set(this) }
                    Command.ls(userDePath).apply { dataBinding.isBackupUserDe.set(this) }
                    Command.ls(dataPath).apply { dataBinding.isBackupData.set(this) }
                    Command.ls(obbPath).apply { dataBinding.isBackupObb.set(this) }
                } else {
                    dataBinding.isBackupUser.set(false)
                    dataBinding.isBackupUserDe.set(false)
                    dataBinding.isBackupData.set(false)
                    dataBinding.isBackupObb.set(false)
                }

                val processingTask = ProcessingTask(
                    appName = i.appName,
                    packageName = i.packageName,
                    app = i.restoreList[i.restoreIndex].app,
                    data = i.restoreList[i.restoreIndex].data,
                    appIcon = i.appIcon
                )

                // 开始恢复
                var state = true // 该任务是否成功完成
                if (dataBinding.isBackupApk.get()) {
                    // 恢复应用
                    dataBinding.processingApk.set(true)
                    Command.installAPK(
                        inPath,
                        packageName,
                        userId,
                        i.restoreList[i.restoreIndex].versionCode.toString()
                    ) { setSizeAndSpeed(it) }.apply {
                        state = this
                    }
                    dataBinding.processingApk.set(false)
                    if (!state) {
                        failedNum += 1
                        viewModel.failedList.add(processingTask)
                        continue
                    }
                    initializeSizeAndSpeed()
                }

                // 判断是否安装该应用
                Bashrc.findPackage(userId, packageName).apply {
                    if (!this.first) {
                        // 未安装
                        dataBinding.isBackupUser.set(false)
                        dataBinding.isBackupData.set(false)
                        dataBinding.isBackupObb.set(false)
                        state = false
                        App.logcat.addLine("${packageName}: Not installed")
                    }
                }

                if (dataBinding.isBackupUser.get()) {
                    // 恢复User
                    dataBinding.processingUser.set(true)
                    Command.decompress(
                        Command.getCompressionTypeByPath(userPath),
                        "user",
                        userPath,
                        packageName,
                        Path.getUserPath(userId)
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                    }
                    Command.setOwnerAndSELinux(
                        "user", packageName, "${Path.getUserPath(userId)}/${packageName}", userId
                    )
                    dataBinding.processingUser.set(false)
                    initializeSizeAndSpeed()
                }
                if (dataBinding.isBackupUserDe.get()) {
                    // 恢复User_de
                    dataBinding.processingUserDe.set(true)
                    Command.decompress(
                        Command.getCompressionTypeByPath(userDePath),
                        "user_de",
                        userDePath,
                        packageName,
                        Path.getUserDePath(userId)
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                    }
                    Command.setOwnerAndSELinux(
                        "user_de",
                        packageName,
                        "${Path.getUserDePath(userId)}/${packageName}",
                        userId
                    )
                    dataBinding.processingUserDe.set(false)
                    initializeSizeAndSpeed()
                }
                if (dataBinding.isBackupData.get()) {
                    // 恢复Data
                    dataBinding.processingData.set(true)
                    Command.decompress(
                        Command.getCompressionTypeByPath(dataPath),
                        "data",
                        dataPath,
                        packageName,
                        Path.getDataPath(userId)
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                    }
                    Command.setOwnerAndSELinux(
                        "data", packageName, "${Path.getDataPath(userId)}/${packageName}", userId
                    )
                    dataBinding.processingData.set(false)
                    initializeSizeAndSpeed()
                }
                if (dataBinding.isBackupObb.get()) {
                    // 恢复Obb
                    dataBinding.processingObb.set(true)
                    Command.decompress(
                        Command.getCompressionTypeByPath(obbPath),
                        "obb",
                        obbPath,
                        packageName,
                        Path.getObbPath(userId)
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                    }
                    Command.setOwnerAndSELinux(
                        "obb", packageName, "${Path.getObbPath(userId)}/${packageName}", userId
                    )
                    dataBinding.processingObb.set(false)
                    initializeSizeAndSpeed()
                }
                if (state) {
                    successNum += 1
                    viewModel.successList.add(processingTask)
                } else {
                    failedNum += 1
                    viewModel.failedList.add(processingTask)
                }
                dataBinding.progress.set(index + 1)
                dataBinding.progressText.set("${GlobalString.progress}: ${dataBinding.progress.get()}/${dataBinding.progressMax.get()}")
            }
            dataBinding.totalTip.set(GlobalString.restoreFinished)
            dataBinding.totalProgress.set("${successNum + failedNum} ${GlobalString.total}")
            dataBinding.isProcessing.set(false)
            dataBinding.isFinished.postValue(true)
            dataBinding.btnText.set(GlobalString.finish)
            dataBinding.btnDesc.set(GlobalString.clickTheRightBtnToFinish)
            Bashrc.moveLogToOut()
        }
        else {
            v.context.getActivity()?.finish()
        }
    }

    private fun onRestoreMediaClick(v: View) {
        if (!dataBinding.isFinished.value!!) CoroutineScope(Dispatchers.IO).launch {
            dataBinding.isProcessing.set(true)
            dataBinding.totalTip.set(GlobalString.restoreProcessing)
            for ((index, i) in mediaInfoRestoreList.withIndex()) {
                // 准备备份卡片数据
                dataBinding.appName.set(i.name)
                dataBinding.packageName.set(i.path)
                dataBinding.isBackupData.set(i.data)

                val inPath = Path.getBackupMediaSavePath()

                // 开始恢复
                var state = true // 该任务是否成功完成
                if (dataBinding.isBackupData.get()) {
                    // 恢复Data
                    dataBinding.processingData.set(true)
                    // 恢复目录
                    val inputPath = "${inPath}/${i.name}.tar*"
                    Command.decompress(
                        Command.getCompressionTypeByPath(inputPath),
                        "media",
                        inputPath,
                        i.name,
                        i.path.replace("/${i.name}", "")
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                    }
                    dataBinding.processingData.set(false)
                    initializeSizeAndSpeed()
                }
                val processingTask = ProcessingTask(
                    appName = i.name,
                    packageName = i.path,
                    app = false,
                    data = true,
                    appIcon = null
                )
                if (state) {
                    successNum += 1
                    viewModel.successList.add(processingTask)
                } else {
                    failedNum += 1
                    viewModel.failedList.add(processingTask)
                }
                dataBinding.progress.set(index + 1)
                dataBinding.progressText.set("${GlobalString.progress}: ${dataBinding.progress.get()}/${dataBinding.progressMax.get()}")
            }
            dataBinding.totalTip.set(GlobalString.restoreFinished)
            dataBinding.totalProgress.set("${successNum + failedNum} ${GlobalString.total}")
            dataBinding.isProcessing.set(false)
            dataBinding.isFinished.postValue(true)
            dataBinding.btnText.set(GlobalString.finish)
            dataBinding.btnDesc.set(GlobalString.clickTheRightBtnToFinish)
            Bashrc.moveLogToOut()
        }
        else {
            v.context.getActivity()?.finish()
        }
    }
}
