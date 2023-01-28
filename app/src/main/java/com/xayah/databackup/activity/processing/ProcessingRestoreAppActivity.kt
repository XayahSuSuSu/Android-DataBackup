package com.xayah.databackup.activity.processing

import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.io.SuFile
import com.xayah.databackup.App
import com.xayah.databackup.adapter.ProcessingItemAdapter
import com.xayah.databackup.adapter.ProcessingTaskAdapter
import com.xayah.databackup.data.*
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

    // Processing项目哈希表
    private val processingItemMap by lazy {
        MutableStateFlow(hashMapOf<String, ProcessingItem>())
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
                        val outPutIconPath =
                            "${Path.getBackupDataSavePath()}/${i.detailBase.packageName}/icon.png"
                        SuFile(outPutIconPath).apply {
                            val bytes = readBytes()
                            task.appIcon = (BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                .toDrawable(this@ProcessingRestoreAppActivity.resources))
                        }
                    }
                    processingTaskList.value.add(task)
                }
                register(ProcessingTaskAdapter())
                items = processingTaskList.value
                notifyDataSetChanged()
            }

            viewModel.mAdapterItems.apply {
                register(ProcessingItemAdapter())
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

                            // 设置适配器
                            viewModel.mAdapterItems.apply {
                                val size = processingItemMap.value.size
                                processingItemMap.value.clear()
                                clearProcessingItems(viewModel, size)
                                if (i.detailRestoreList[i.restoreIndex].selectApp) {
                                    // 检查是否备份APK
                                    processingItemMap.value[ProcessingItemTypeAPK] =
                                        ProcessingItem.APK()
                                }
                                if (i.detailRestoreList[i.restoreIndex].selectData) {
                                    // 检查是否备份数据
                                    Command.ls(userPath).apply {
                                        if (this)
                                            processingItemMap.value[ProcessingItemTypeUSER] =
                                                ProcessingItem.USER()
                                    }
                                    Command.ls(userDePath).apply {
                                        if (this)
                                            processingItemMap.value[ProcessingItemTypeUSERDE] =
                                                ProcessingItem.USERDE()
                                    }
                                    Command.ls(dataPath).apply {
                                        if (this)
                                            processingItemMap.value[ProcessingItemTypeDATA] =
                                                ProcessingItem.DATA()
                                    }
                                    Command.ls(obbPath).apply {
                                        if (this)
                                            processingItemMap.value[ProcessingItemTypeOBB] =
                                                ProcessingItem.OBB()
                                    }
                                }
                                items = processingItemMap.value.values.sortedBy { it.weight }
                                viewModel.viewModelScope.launch {
                                    refreshProcessingItems(viewModel)
                                }
                            }

                            // 开始恢复
                            var state = true // 该任务是否成功完成
                            if (processingItemMap.value.containsKey(ProcessingItemTypeAPK)) {
                                processingItemMap.value[ProcessingItemTypeAPK]?.isProcessing = true
                                refreshProcessingItems(viewModel)

                                // 恢复应用
                                Command.installAPK(
                                    inPath,
                                    packageName,
                                    userId,
                                    i.detailRestoreList[i.restoreIndex].versionCode.toString()
                                ) {
                                    setProcessingItem(
                                        it,
                                        processingItemMap.value[ProcessingItemTypeAPK]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
                                }.apply {
                                    state = this
                                }
                                if (!state) {
                                    viewModel.failedList.value.add(processingTaskList.value[index])
                                    continue
                                }

                                processingItemMap.value[ProcessingItemTypeAPK]?.isProcessing = false
                                refreshProcessingItems(viewModel)
                            }

                            // 判断是否安装该应用
                            Bashrc.findPackage(userId, packageName).apply {
                                if (!this.first) {
                                    // 未安装
                                    processingItemMap.value.remove(ProcessingItemTypeUSER)
                                    processingItemMap.value.remove(ProcessingItemTypeUSERDE)
                                    processingItemMap.value.remove(ProcessingItemTypeDATA)
                                    processingItemMap.value.remove(ProcessingItemTypeOBB)
                                    state = false
                                    Logcat.getInstance().addLine("${packageName}: Not installed")
                                }
                            }

                            if (processingItemMap.value.containsKey(ProcessingItemTypeUSER)) {
                                processingItemMap.value[ProcessingItemTypeUSER]?.isProcessing = true
                                refreshProcessingItems(viewModel)

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
                                        processingItemMap.value[ProcessingItemTypeUSER]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
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
                                        processingItemMap.value[ProcessingItemTypeUSER]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
                                }

                                processingItemMap.value[ProcessingItemTypeUSER]?.isProcessing =
                                    false
                                refreshProcessingItems(viewModel)
                            }
                            if (processingItemMap.value.containsKey(ProcessingItemTypeUSERDE)) {
                                processingItemMap.value[ProcessingItemTypeUSERDE]?.isProcessing =
                                    true
                                refreshProcessingItems(viewModel)

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
                                        processingItemMap.value[ProcessingItemTypeUSERDE]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
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
                                        processingItemMap.value[ProcessingItemTypeUSERDE]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
                                }

                                processingItemMap.value[ProcessingItemTypeUSERDE]?.isProcessing =
                                    false
                                refreshProcessingItems(viewModel)
                            }
                            if (processingItemMap.value.containsKey(ProcessingItemTypeDATA)) {
                                processingItemMap.value[ProcessingItemTypeDATA]?.isProcessing = true
                                refreshProcessingItems(viewModel)

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
                                        processingItemMap.value[ProcessingItemTypeDATA]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
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
                                        processingItemMap.value[ProcessingItemTypeDATA]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
                                }

                                processingItemMap.value[ProcessingItemTypeDATA]?.isProcessing =
                                    false
                                refreshProcessingItems(viewModel)
                            }
                            if (processingItemMap.value.containsKey(ProcessingItemTypeOBB)) {
                                processingItemMap.value[ProcessingItemTypeOBB]?.isProcessing = true
                                refreshProcessingItems(viewModel)

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
                                        processingItemMap.value[ProcessingItemTypeOBB]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
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
                                        processingItemMap.value[ProcessingItemTypeOBB]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
                                }

                                processingItemMap.value[ProcessingItemTypeOBB]?.isProcessing = false
                                refreshProcessingItems(viewModel)
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
                    }
                }
            }
        } else {
            finish()
        }
    }
}