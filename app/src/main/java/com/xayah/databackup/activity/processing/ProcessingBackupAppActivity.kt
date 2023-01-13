package com.xayah.databackup.activity.processing

import android.graphics.Bitmap
import android.util.Base64
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.adapter.ProcessingTaskAdapter
import com.xayah.databackup.data.*
import com.xayah.databackup.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ProcessingBackupAppActivity : ProcessingBaseActivity() {
    lateinit var viewModel: ProcessingBaseViewModel

    // 备份信息列表
    private val backupInfoList by lazy {
        MutableStateFlow(mutableListOf<BackupInfo>())
    }

    // 应用备份列表
    private val appInfoBackupList
        get() = App.appInfoList.value.filter { it.backup.app || it.backup.data }
            .toMutableList()
    private val appInfoBackupListNum
        get() = run {
            val appInfoBaseNum = AppInfoBaseNum(0, 0)
            for (i in appInfoBackupList) {
                if (i.backup.app) appInfoBaseNum.appNum++
                if (i.backup.data) appInfoBaseNum.dataNum++
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
            // 加载备份列表
            backupInfoList.emit(Command.getCachedBackupInfoList())

            // 设置适配器
            viewModel.mAdapter.apply {
                for (i in appInfoBackupList) processingTaskList.value.add(
                    ProcessingTask(
                        appName = i.appName,
                        packageName = i.packageName,
                        app = i.backup.app,
                        data = i.backup.data,
                        appIcon = i.appIcon
                    )
                )
                register(ProcessingTaskAdapter())
                items = processingTaskList.value
                notifyDataSetChanged()
            }

            // 设置备份状态
            viewModel.btnText.set(GlobalString.backup)
            viewModel.btnDesc.set(GlobalString.clickTheRightBtnToStart)
            viewModel.progressMax.set(appInfoBackupList.size)
            viewModel.progressText.set("${GlobalString.progress}: ${viewModel.progress.get()}/${viewModel.progressMax.get()}")
            viewModel.totalTip.set(GlobalString.ready)
            appInfoBackupListNum.apply {
                viewModel.totalProgress.set("${GlobalString.selected} ${this.appNum} ${GlobalString.application}, ${this.dataNum} ${GlobalString.data}, ${App.globalContext.readBackupUser()} ${GlobalString.backupUser}")
            }
            viewModel.isReady.set(true)
            viewModel.isFinished.postValue(false)
        }
    }

    override fun onFabClick() {
        if (!viewModel.isFinished.value!!) {
            if (viewModel.isProcessing.get().not()) {
                viewModel.isProcessing.set(true)
                viewModel.totalTip.set(GlobalString.backupProcessing)
                viewModel.viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        // 记录开始时间戳
                        val startTime = App.getTimeStamp()
                        // 记录开始备份目录大小
                        val startSize = Command.countSize(App.globalContext.readBackupSavePath())

                        // 获取默认输入法和无障碍
                        val keyboard = Bashrc.getKeyboard()
                        val services = Bashrc.getAccessibilityServices()

                        // 备份自身
                        if (App.globalContext.readIsBackupItself())
                            Command.backupItself(
                                "com.xayah.databackup",
                                App.globalContext.readBackupSavePath(),
                                App.globalContext.readBackupUser()
                            )

                        for ((index, i) in appInfoBackupList.withIndex()) {
                            val date =
                                if (App.globalContext.readBackupStrategy() == BackupStrategy.Cover) GlobalString.cover else App.getTimeStamp()
                            // 准备备份卡片数据
                            viewModel.appName.set(i.appName)
                            viewModel.packageName.set(i.packageName)
                            viewModel.appVersion.set(i.backup.versionName)
                            viewModel.appIcon.set(i.appIcon)
                            viewModel.isBackupApk.set(i.backup.app)

                            if (App.globalContext.readIsBackupIcon()) {
                                // 保存应用图标
                                i.appIcon?.apply {
                                    try {
                                        val stream = ByteArrayOutputStream()
                                        toBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
                                        i.appIconString =
                                            Base64.encodeToString(
                                                stream.toByteArray(),
                                                Base64.DEFAULT
                                            )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            val packageName = viewModel.packageName.get()!!
                            val userId = App.globalContext.readBackupUser()
                            val compressionType = App.globalContext.readCompressionType()
                            val outPutPath = "${Path.getBackupDataSavePath()}/${packageName}/$date"
                            val userPath = "${Path.getUserPath()}/${packageName}"
                            val userDePath = "${Path.getUserDePath()}/${packageName}"
                            val dataPath = "${Path.getDataPath()}/${packageName}"
                            val obbPath = "${Path.getObbPath()}/${packageName}"
                            if (i.backup.data) {
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

                            // 开始备份
                            var state = true // 该任务是否成功完成
                            if (viewModel.isBackupApk.get()) {
                                // 备份应用
                                viewModel.processingApk.set(true)
                                Command.compressAPK(
                                    compressionType,
                                    packageName,
                                    outPutPath,
                                    userId,
                                    i.backup.appSize
                                ) { setSizeAndSpeed(viewModel, it) }.apply {
                                    if (!this) state = false
                                    // 保存apk大小
                                    else i.backup.appSize = Command.countSize(
                                        Bashrc.getAPKPath(i.packageName, userId).second, 1
                                    )
                                }
                                viewModel.processingApk.set(false)
                                initializeSizeAndSpeed(viewModel)
                            }
                            if (viewModel.isBackupUser.get()) {
                                // 备份User
                                viewModel.processingUser.set(true)
                                Command.compress(
                                    compressionType,
                                    "user",
                                    packageName,
                                    outPutPath,
                                    Path.getUserPath(),
                                    i.backup.userSize
                                ) { setSizeAndSpeed(viewModel, it) }.apply {
                                    if (!this) state = false
                                    // 保存user大小
                                    else i.backup.userSize = Command.countSize(userPath, 1)
                                }
                                viewModel.processingUser.set(false)
                                initializeSizeAndSpeed(viewModel)
                            }
                            if (viewModel.isBackupUserDe.get()) {
                                // 备份User_de
                                viewModel.processingUserDe.set(true)
                                Command.compress(
                                    compressionType,
                                    "user_de",
                                    packageName,
                                    outPutPath,
                                    Path.getUserDePath(),
                                    i.backup.userDeSize
                                ) { setSizeAndSpeed(viewModel, it) }.apply {
                                    if (!this) state = false
                                    // 保存user_de大小
                                    else i.backup.userDeSize = Command.countSize(userDePath, 1)
                                }
                                viewModel.processingUserDe.set(false)
                                initializeSizeAndSpeed(viewModel)
                            }
                            if (viewModel.isBackupData.get()) {
                                // 备份Data
                                viewModel.processingData.set(true)
                                Command.compress(
                                    compressionType,
                                    "data",
                                    packageName,
                                    outPutPath,
                                    Path.getDataPath(),
                                    i.backup.dataSize
                                ) { setSizeAndSpeed(viewModel, it) }.apply {
                                    if (!this) state = false
                                    // 保存data大小
                                    else i.backup.dataSize = Command.countSize(dataPath, 1)
                                }
                                viewModel.processingData.set(false)
                                initializeSizeAndSpeed(viewModel)
                            }
                            if (viewModel.isBackupObb.get()) {
                                // 备份Obb
                                viewModel.processingObb.set(true)
                                Command.compress(
                                    compressionType,
                                    "obb",
                                    packageName,
                                    outPutPath,
                                    Path.getObbPath(),
                                    i.backup.obbSize
                                ) { setSizeAndSpeed(viewModel, it) }.apply {
                                    if (!this) state = false
                                    // 保存obb大小
                                    else i.backup.obbSize = Command.countSize(obbPath, 1)
                                }
                                viewModel.processingObb.set(false)
                                initializeSizeAndSpeed(viewModel)
                            }
                            i.backup.date = date
                            if (state) {
                                val item = AppInfoItem(
                                    app = false,
                                    data = false,
                                    hasApp = true,
                                    hasData = true,
                                    versionName = i.backup.versionName,
                                    versionCode = i.backup.versionCode,
                                    appSize = i.backup.appSize,
                                    userSize = i.backup.userSize,
                                    userDeSize = i.backup.userDeSize,
                                    dataSize = i.backup.dataSize,
                                    obbSize = i.backup.obbSize,
                                    date = i.backup.date
                                )
                                val itemIndex = i.restoreList.indexOfFirst { date == it.date }
                                if (itemIndex == -1) {
                                    // RestoreList中不存在该Item
                                    i.restoreList.add(item)
                                    i.restoreIndex++
                                } else {
                                    // RestoreList中已存在该Item
                                    i.restoreList[itemIndex] = item
                                }
                                viewModel.successList.value.add(processingTaskList.value[index])
                            } else {
                                viewModel.failedList.value.add(processingTaskList.value[index])
                            }
                            viewModel.progress.set(index + 1)
                            viewModel.progressText.set("${GlobalString.progress}: ${viewModel.progress.get()}/${viewModel.progressMax.get()}")
                        }
                        val endTime = App.getTimeStamp()
                        val endSize = Command.countSize(App.globalContext.readBackupSavePath())
                        backupInfoList.value.add(
                            BackupInfo(
                                Command.getVersion(),
                                startTime,
                                endTime,
                                startSize,
                                endSize,
                                "app",
                                App.globalContext.readBackupUser()
                            )
                        )
                        viewModel.totalTip.set(GlobalString.backupFinished)
                        viewModel.totalProgress.set("${viewModel.successNum + viewModel.failedNum} ${GlobalString.total}")
                        viewModel.isProcessing.set(false)
                        viewModel.isFinished.postValue(true)
                        viewModel.btnText.set(GlobalString.finish)
                        viewModel.btnDesc.set(GlobalString.clickTheRightBtnToFinish)

                        // 恢复默认输入法和无障碍
                        keyboard.apply {
                            if (this.first) Bashrc.setKeyboard(this.second)
                        }
                        services.apply {
                            if (this.first) Bashrc.setAccessibilityServices(this.second)
                        }

                        // 保存列表数据
                        JSON.saveBackupInfoList(backupInfoList.value)
                        JSON.saveAppInfoList(App.appInfoList.value)
                        // 移动日志数据
                        Bashrc.moveLogToOut()
                    }
                }
            }
        } else {
            finish()
        }
    }
}