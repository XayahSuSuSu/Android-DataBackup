package com.xayah.databackup.activity.processing

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

class ProcessingBackupMediaActivity : ProcessingBaseActivity() {
    lateinit var viewModel: ProcessingBaseViewModel

    /**
     * 全局单例对象
     */
    private val globalObject = GlobalObject.getInstance()

    // 备份信息列表
    private val backupInfoList by lazy {
        MutableStateFlow(mutableListOf<BackupInfo>())
    }

    // 媒体列表
    private val mediaInfoBackupList
        get() = globalObject.mediaInfoBackupMap.value.values.toList()
            .filter { it.backupDetail.data }.toMutableList()

    // 任务列表
    private val processingTaskList by lazy {
        MutableStateFlow(mutableListOf<ProcessingTask>())
    }

    override fun initialize(viewModel: ProcessingBaseViewModel) {
        this.viewModel = viewModel
        viewModel.viewModelScope.launch {
            // 加载配置
            backupInfoList.emit(Command.getBackupInfoList())
            if (globalObject.mediaInfoBackupMap.value.isEmpty()) {
                globalObject.mediaInfoBackupMap.emit(Command.getMediaInfoBackupMap())
            }
            if (globalObject.mediaInfoRestoreMap.value.isEmpty()) {
                globalObject.mediaInfoRestoreMap.emit(Command.getMediaInfoRestoreMap())
            }

            // 设置适配器
            viewModel.mAdapter.apply {
                for (i in mediaInfoBackupList) processingTaskList.value.add(
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

            viewModel.mAdapterItems.apply {
                register(ProcessingItemAdapter())
            }

            // 设置备份状态
            viewModel.btnText.set(GlobalString.backup)
            viewModel.btnDesc.set(GlobalString.clickTheRightBtnToStart)
            viewModel.progressMax.set(mediaInfoBackupList.size)
            viewModel.progressText.set("${GlobalString.progress}: ${viewModel.progress.get()}/${viewModel.progressMax.get()}")
            viewModel.totalTip.set(GlobalString.ready)
            viewModel.totalProgress.set("${GlobalString.selected} ${mediaInfoBackupList.size} ${GlobalString.data}")
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

                        for ((index, i) in mediaInfoBackupList.withIndex()) {
                            val date =
                                if (App.globalContext.readBackupStrategy() == BackupStrategy.Cover) GlobalString.cover else App.getTimeStamp()
                            // 准备备份卡片数据
                            viewModel.appName.set(i.name)
                            viewModel.packageName.set(i.path)

                            val outPutPath = "${Path.getBackupMediaSavePath()}/${i.name}/${date}"

                            // 开始备份
                            var state = true // 该任务是否成功完成
                            if (i.backupDetail.data) {
                                val processingItem = ProcessingItem.DATA().apply {
                                    isProcessing = true
                                }
                                // 设置适配器
                                viewModel.mAdapterItems.apply {
                                    items = mutableListOf(processingItem)
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
                                }

                                // 备份目录
                                Command.compress(
                                    "tar",
                                    "media",
                                    i.name,
                                    outPutPath,
                                    i.path,
                                    i.backupDetail.size
                                ) {
                                    setProcessingItem(
                                        it,
                                        processingItem
                                    )
                                    viewModel.viewModelScope.launch {
                                        refreshProcessingItems(viewModel)
                                    }
                                }.apply {
                                    if (!this) state = false
                                    // 保存大小
                                    else i.backupDetail.size = Command.countSize(
                                        i.path, 1
                                    )
                                }

                                processingItem.isProcessing = false
                                refreshProcessingItems(viewModel)
                            }
                            i.backupDetail.date = date
                            if (state) {
                                val detail = MediaInfoDetailBase(
                                    data = false,
                                    size = i.backupDetail.size,
                                    date = i.backupDetail.date
                                )

                                if (globalObject.mediaInfoRestoreMap.value.containsKey(
                                        i.name
                                    ).not()
                                ) {
                                    globalObject.mediaInfoRestoreMap.value[i.name] =
                                        MediaInfoRestore().apply {
                                            this.name = i.name
                                            this.path = i.path
                                        }
                                }
                                val mediaInfoRestore =
                                    globalObject.mediaInfoRestoreMap.value[i.name]!!

                                val detailIndex =
                                    mediaInfoRestore.detailRestoreList.indexOfFirst { date == it.date }
                                if (detailIndex == -1) {
                                    // RestoreList中不存在该Item
                                    mediaInfoRestore.detailRestoreList.add(detail)
                                    mediaInfoRestore.restoreIndex++
                                } else {
                                    // RestoreList中已存在该Item
                                    mediaInfoRestore.detailRestoreList[detailIndex] = detail
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
                        GsonUtil.saveMediaInfoBackupMapToFile(globalObject.mediaInfoBackupMap.value)
                        GsonUtil.saveMediaInfoRestoreMapToFile(globalObject.mediaInfoRestoreMap.value)
                        GsonUtil.saveBackupInfoListToFile(backupInfoList.value)
                    }
                }
            }
        } else {
            finish()
        }
    }
}