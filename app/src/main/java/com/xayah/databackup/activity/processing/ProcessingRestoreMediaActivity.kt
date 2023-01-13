package com.xayah.databackup.activity.processing

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.adapter.ProcessingTaskAdapter
import com.xayah.databackup.data.ProcessingTask
import com.xayah.databackup.util.Bashrc
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProcessingRestoreMediaActivity : ProcessingBaseActivity() {
    lateinit var viewModel: ProcessingBaseViewModel

    // 媒体列表
    private val mediaInfoList
        get() = App.mediaInfoList.value.filter { if (it.restoreList.isNotEmpty()) it.restoreList[it.restoreIndex].data else false }
            .toMutableList()

    // 任务列表
    private val processingTaskList by lazy {
        MutableStateFlow(mutableListOf<ProcessingTask>())
    }

    override fun initialize(viewModel: ProcessingBaseViewModel) {
        this.viewModel = viewModel
        viewModel.viewModelScope.launch {
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
            viewModel.btnText.set(GlobalString.restore)
            viewModel.btnDesc.set(GlobalString.clickTheRightBtnToStart)
            viewModel.progressMax.set(mediaInfoList.size)
            viewModel.progressText.set("${GlobalString.progress}: ${viewModel.progress.get()}/${viewModel.progressMax.get()}")
            viewModel.totalTip.set(GlobalString.ready)
            mediaInfoList.size.apply {
                viewModel.totalProgress.set("${GlobalString.selected} $this ${GlobalString.data}")
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
                        for ((index, i) in mediaInfoList.withIndex()) {
                            // 准备备份卡片数据
                            viewModel.appName.set(i.name)
                            viewModel.packageName.set(i.path)
                            viewModel.isBackupData.set(i.restoreList[i.restoreIndex].data)

                            val inPath =
                                "${Path.getBackupMediaSavePath()}/${i.name}/${i.restoreList[i.restoreIndex].date}"

                            // 开始恢复
                            var state = true // 该任务是否成功完成
                            if (viewModel.isBackupData.get()) {
                                // 恢复Data
                                viewModel.processingData.set(true)
                                // 恢复目录
                                val inputPath = "${inPath}/${i.name}.tar*"
                                Command.decompress(
                                    Command.getCompressionTypeByPath(inputPath),
                                    "media",
                                    inputPath,
                                    i.name,
                                    i.path.replace("/${i.name}", "")
                                ) { setSizeAndSpeed(viewModel, it) }.apply {
                                    if (!this) state = false
                                }
                                viewModel.processingData.set(false)
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
                    }
                }
            }
        } else {
            finish()
        }
    }
}