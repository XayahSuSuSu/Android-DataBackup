package com.xayah.databackup.activity.processing

import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toDrawable
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
        get() = GlobalObject.getInstance().appInfoRestoreMap.value.values.toList()
            .filter { if (it.detailRestoreList.isNotEmpty()) it.detailRestoreList[it.restoreIndex].selectApp || it.detailRestoreList[it.restoreIndex].selectData else false }
            .toMutableList()
    private val appInfoRestoreListNum
        get() = run {
            val appInfoBaseNum = AppInfoBaseNum(0, 0)
            for (i in appInfoRestoreList) {
                if (i.detailRestoreList[i.restoreIndex].selectApp) appInfoBaseNum.appNum++
                if (i.detailRestoreList[i.restoreIndex].selectData) appInfoBaseNum.dataNum++
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
                for (i in appInfoRestoreList) {
                    val task = ProcessingTask(
                        appName = i.detailBase.appName,
                        packageName = i.detailBase.packageName,
                        app = i.detailRestoreList[i.restoreIndex].selectApp,
                        data = i.detailRestoreList[i.restoreIndex].selectData,
                        appIcon = i.detailBase.appIcon
                    )
                    if (task.appIcon == null) {
                        SafeFile.create("${Path.getBackupDataSavePath()}/${i.detailBase.packageName}/icon.png") {
                            it.apply {
                                val bytes = readBytes()
                                task.appIcon = (BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    .toDrawable(this@ProcessingRestoreAppActivity.resources))
                            }
                        }
                    }
                    processingTaskList.value.add(task)
                }
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
        if (!viewModel.isFinished.value!!) {
            if (viewModel.isProcessing.get().not()) {
                viewModel.isProcessing.set(true)
                viewModel.totalTip.set(GlobalString.restoreProcessing)
                viewModel.viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        for ((index, i) in appInfoRestoreList.withIndex()) {
                            // 准备备份卡片数据
                            viewModel.appName.set(i.detailBase.appName)
                            viewModel.packageName.set(i.detailBase.packageName)
                            viewModel.appVersion.set(i.detailRestoreList[i.restoreIndex].versionName)
                            viewModel.appIcon.set(i.detailBase.appIcon)

                            val packageName = viewModel.packageName.get()!!
                            val userId = App.globalContext.readRestoreUser()
                            val compressionType = App.globalContext.readCompressionType()
                            val date = i.detailRestoreList[i.restoreIndex].date
                            val inPath = "${Path.getBackupDataSavePath()}/${packageName}/${date}"
                            val suffix = Command.getSuffixByCompressionType(compressionType)
                            val userPath = "${inPath}/user.$suffix"
                            val userDePath = "${inPath}/user_de.$suffix"
                            val dataPath = "${inPath}/data.$suffix"
                            val obbPath = "${inPath}/obb.$suffix"

                            // 设置匹配项
                            if (i.detailRestoreList[i.restoreIndex].selectApp) {
                                // 检查是否备份APK
                                viewModel.apkTitle.set(GlobalString.ready)
                                viewModel.apkSubtitle.set(GlobalString.pleaseWait)
                                viewModel.apkIsProcessing.set(false)
                            }
                            viewModel.apkNeedProcessing.set(i.detailRestoreList[i.restoreIndex].selectApp)
                            if (i.detailRestoreList[i.restoreIndex].selectData) {
                                // 检查是否备份数据
                                Command.ls(userPath).apply {
                                    if (this) {
                                        viewModel.userTitle.set(GlobalString.ready)
                                        viewModel.userSubtitle.set(GlobalString.pleaseWait)
                                        viewModel.userIsProcessing.set(false)
                                    }
                                    viewModel.userNeedProcessing.set(this)
                                }
                                Command.ls(userDePath).apply {
                                    if (this) {
                                        viewModel.userDeTitle.set(GlobalString.ready)
                                        viewModel.userDeSubtitle.set(GlobalString.pleaseWait)
                                        viewModel.userDeIsProcessing.set(false)
                                    }
                                    viewModel.userDeNeedProcessing.set(this)
                                }
                                Command.ls(dataPath).apply {
                                    if (this) {
                                        viewModel.dataTitle.set(GlobalString.ready)
                                        viewModel.dataSubtitle.set(GlobalString.pleaseWait)
                                        viewModel.dataIsProcessing.set(false)
                                    }
                                    viewModel.dataNeedProcessing.set(this)
                                }
                                Command.ls(obbPath).apply {
                                    if (this) {
                                        viewModel.obbTitle.set(GlobalString.ready)
                                        viewModel.obbSubtitle.set(GlobalString.pleaseWait)
                                        viewModel.obbIsProcessing.set(false)
                                    }
                                    viewModel.obbNeedProcessing.set(this)
                                }
                            }

                            // 开始恢复
                            var state = true // 该任务是否成功完成
                            if (viewModel.apkNeedProcessing.get()) {
                                viewModel.apkIsProcessing.set(true)

                                // 恢复应用
                                Command.installAPK(
                                    inPath,
                                    packageName,
                                    userId,
                                    i.detailRestoreList[i.restoreIndex].versionCode.toString()
                                ) {
                                    setProcessingItem(
                                        it,
                                        viewModel.apkTitle,
                                        viewModel.apkSubtitle
                                    )
                                }.apply {
                                    state = this
                                }
                                if (!state) {
                                    viewModel.failedList.value.add(processingTaskList.value[index])
                                    continue
                                }

                                viewModel.apkIsProcessing.set(false)
                            }

                            // 判断是否安装该应用
                            Bashrc.findPackage(userId, packageName).apply {
                                if (!this.first) {
                                    // 未安装
                                    viewModel.userNeedProcessing.set(false)
                                    viewModel.userDeNeedProcessing.set(false)
                                    viewModel.dataNeedProcessing.set(false)
                                    viewModel.obbNeedProcessing.set(false)
                                    state = false
                                    Logcat.getInstance().shellLogAddLine("${packageName}: Not installed")
                                }
                            }

                            if (viewModel.userNeedProcessing.get()) {
                                viewModel.userIsProcessing.set(true)

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
                                    setProcessingItem(
                                        it,
                                        viewModel.userTitle,
                                        viewModel.userSubtitle
                                    )
                                }.apply {
                                    if (!this) state = false
                                }
                                Command.setOwnerAndSELinux(
                                    "user",
                                    packageName,
                                    "${Path.getUserPath(userId)}/${packageName}",
                                    userId,
                                    contextSELinux
                                ) {
                                    setProcessingItem(
                                        it,
                                        viewModel.userTitle,
                                        viewModel.userSubtitle
                                    )
                                }

                                viewModel.userIsProcessing.set(false)
                            }
                            if (viewModel.userDeNeedProcessing.get()) {
                                viewModel.userDeIsProcessing.set(true)

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
                                    setProcessingItem(
                                        it,
                                        viewModel.userDeTitle,
                                        viewModel.userDeSubtitle
                                    )
                                }.apply {
                                    if (!this) state = false
                                }
                                Command.setOwnerAndSELinux(
                                    "user_de",
                                    packageName,
                                    "${Path.getUserDePath(userId)}/${packageName}",
                                    userId,
                                    contextSELinux
                                ) {
                                    setProcessingItem(
                                        it,
                                        viewModel.userDeTitle,
                                        viewModel.userDeSubtitle
                                    )
                                }

                                viewModel.userDeIsProcessing.set(false)
                            }
                            if (viewModel.dataNeedProcessing.get()) {
                                viewModel.dataIsProcessing.set(true)

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
                                    setProcessingItem(
                                        it,
                                        viewModel.dataTitle,
                                        viewModel.dataSubtitle
                                    )
                                }.apply {
                                    if (!this) state = false
                                }
                                Command.setOwnerAndSELinux(
                                    "data",
                                    packageName,
                                    "${Path.getDataPath(userId)}/${packageName}",
                                    userId,
                                    contextSELinux
                                ) {
                                    setProcessingItem(
                                        it,
                                        viewModel.dataTitle,
                                        viewModel.dataSubtitle
                                    )
                                }

                                viewModel.dataIsProcessing.set(false)
                            }
                            if (viewModel.obbNeedProcessing.get()) {
                                viewModel.obbIsProcessing.set(true)

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
                                    setProcessingItem(
                                        it,
                                        viewModel.obbTitle,
                                        viewModel.obbSubtitle
                                    )
                                }.apply {
                                    if (!this) state = false
                                }
                                Command.setOwnerAndSELinux(
                                    "obb",
                                    packageName,
                                    "${Path.getObbPath(userId)}/${packageName}",
                                    userId,
                                    contextSELinux
                                ) {
                                    setProcessingItem(
                                        it,
                                        viewModel.obbTitle,
                                        viewModel.obbSubtitle
                                    )
                                }

                                viewModel.obbIsProcessing.set(false)
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
                    }
                }
            }
        } else {
            finish()
        }
    }
}