package com.xayah.databackup.activity.processing

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.adapter.ProcessingTaskAdapter
import com.xayah.databackup.data.BackupInfo
import com.xayah.databackup.data.BackupStrategy
import com.xayah.databackup.data.ProcessingTask
import com.xayah.databackup.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProcessingBackupMediaActivity : ProcessingBaseActivity() {
    lateinit var viewModel: ProcessingBaseViewModel

    // 备份信息列表
    private val backupInfoList by lazy {
        MutableStateFlow(mutableListOf<BackupInfo>())
    }

    // 媒体列表
    private val mediaInfoList
        get() = App.mediaInfoList.value.filter { it.backup.data }.toMutableList()

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
                for (i in mediaInfoList) processingTaskList.value.add(
                    ProcessingTask(
                        appName = i.name,
                        packageName = i.path,
                        app = false,
                        data = true,
                        appIcon = null
                    )
                )
                register(ProcessingTaskAdapter())
                items = processingTaskList.value
                notifyDataSetChanged()
            }

            // 设置备份状态
            viewModel.btnText.set(GlobalString.backup)
            viewModel.btnDesc.set(GlobalString.clickTheRightBtnToStart)
            viewModel.progressMax.set(mediaInfoList.size)
            viewModel.progressText.set("${GlobalString.progress}: ${viewModel.progress.get()}/${viewModel.progressMax.get()}")
            viewModel.totalTip.set(GlobalString.ready)
            viewModel.totalProgress.set("${GlobalString.selected} ${mediaInfoList.size} ${GlobalString.data}")
            viewModel.isReady.set(true)
            viewModel.isFinished.postValue(false)
        }
    }

    override fun onFabClick() {
        viewModel.viewModelScope.launch {
            // 记录开始时间戳
            val startTime = App.getTimeStamp()
            // 记录开始备份目录大小
            val startSize = Command.countSize(App.globalContext.readBackupSavePath())

            if (!viewModel.isFinished.value!!) withContext(Dispatchers.IO) {
                viewModel.isProcessing.set(true)
                viewModel.totalTip.set(GlobalString.backupProcessing)
                for ((index, i) in mediaInfoList.withIndex()) {
                    val date =
                        if (App.globalContext.readBackupStrategy() == BackupStrategy.Cover) GlobalString.cover else App.getTimeStamp()
                    // 准备备份卡片数据
                    viewModel.appName.set(i.name)
                    viewModel.packageName.set(i.path)
                    viewModel.isBackupData.set(i.backup.data)

                    val outPutPath = "${Path.getBackupMediaSavePath()}/${i.name}/${date}"

                    // 开始备份
                    var state = true // 该任务是否成功完成
                    if (viewModel.isBackupData.get()) {
                        // 备份Data
                        viewModel.processingData.set(true)
                        // 备份目录
                        Command.compress(
                            "tar",
                            "media",
                            i.name,
                            outPutPath,
                            i.path,
                            i.backup.size
                        ) {
                            setSizeAndSpeed(viewModel, it)
                        }.apply {
                            if (!this) state = false
                            // 保存大小
                            else i.backup.size = Command.countSize(
                                i.path, 1
                            )
                        }
                        viewModel.processingData.set(false)
                        initializeSizeAndSpeed(viewModel)
                    }
                    i.backup.date = date
                    if (state) {
                        i.restoreList.add(i.backup)
                        i.restoreIndex++
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
                        "media",
                        App.globalContext.readBackupUser()
                    )
                )
                viewModel.totalTip.set(GlobalString.backupFinished)
                viewModel.totalProgress.set("${viewModel.successNum + viewModel.failedNum} ${GlobalString.total}")
                viewModel.isProcessing.set(false)
                viewModel.isFinished.postValue(true)
                viewModel.btnText.set(GlobalString.finish)
                viewModel.btnDesc.set(GlobalString.clickTheRightBtnToFinish)

                // 保存列表数据
                App.saveMediaInfoList()
                JSON.saveBackupInfoList(backupInfoList.value)
                // 移动日志数据
                Bashrc.moveLogToOut()
            } else {
                finish()
            }
        }
    }
}