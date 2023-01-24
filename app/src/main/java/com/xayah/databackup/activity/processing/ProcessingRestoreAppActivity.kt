package com.xayah.databackup.activity.processing

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.lifecycle.viewModelScope
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
                        appName = i.appName,
                        packageName = i.packageName,
                        app = i.restoreList[i.restoreIndex].app,
                        data = i.restoreList[i.restoreIndex].data,
                        appIcon = i.appIcon
                    )
                    if (task.appIcon == null) {
                        i.appIconString?.apply {
                            if (this.isNotEmpty()) {
                                try {
                                    val img = Base64.decode(this.toByteArray(), Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(img, 0, img.size)
                                    val drawable: Drawable =
                                        BitmapDrawable(App.globalContext.resources, bitmap)
                                    task.appIcon = drawable
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
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
                            viewModel.appName.set(i.appName)
                            viewModel.packageName.set(i.packageName)
                            viewModel.appVersion.set(i.restoreList[i.restoreIndex].versionName)
                            viewModel.appIcon.set(i.appIcon)

                            val packageName = viewModel.packageName.get()!!
                            val userId = App.globalContext.readRestoreUser()
                            val compressionType = App.globalContext.readCompressionType()
                            val date = i.restoreList[i.restoreIndex].date
                            val inPath = "${Path.getBackupDataSavePath()}/${packageName}/${date}"
                            val suffix = Command.getSuffixByCompressionType(compressionType)
                            val userPath = "${inPath}/user.$suffix"
                            val userDePath = "${inPath}/user_de.$suffix"
                            val dataPath = "${inPath}/data.$suffix"
                            val obbPath = "${inPath}/obb.$suffix"

                            // 设置适配器
                            viewModel.mAdapterItems.apply {
                                processingItemMap.value.clear()
                                if (i.restoreList[i.restoreIndex].app) {
                                    // 检查是否备份APK
                                    processingItemMap.value[ProcessingItemTypeAPK] =
                                        ProcessingItem.APK()
                                }
                                if (i.restoreList[i.restoreIndex].data) {
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
                                // 恢复应用
                                Command.installAPK(
                                    inPath,
                                    packageName,
                                    userId,
                                    i.restoreList[i.restoreIndex].versionCode.toString()
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
                                    App.logcat.addLine("${packageName}: Not installed")
                                }
                            }

                            if (processingItemMap.value.containsKey(ProcessingItemTypeUSER)) {
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
                                    userId
                                ) {
                                    setProcessingItem(
                                        it,
                                        processingItemMap.value[ProcessingItemTypeUSER]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
                                }
                            }
                            if (processingItemMap.value.containsKey(ProcessingItemTypeUSERDE)) {
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
                                    userId
                                ) {
                                    setProcessingItem(
                                        it,
                                        processingItemMap.value[ProcessingItemTypeUSERDE]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
                                }
                            }
                            if (processingItemMap.value.containsKey(ProcessingItemTypeDATA)) {
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
                                    userId
                                ) {
                                    setProcessingItem(
                                        it,
                                        processingItemMap.value[ProcessingItemTypeDATA]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
                                }
                            }
                            if (processingItemMap.value.containsKey(ProcessingItemTypeOBB)) {
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
                                    userId
                                ) {
                                    setProcessingItem(
                                        it,
                                        processingItemMap.value[ProcessingItemTypeOBB]
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
                                }
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