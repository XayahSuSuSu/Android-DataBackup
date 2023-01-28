package com.xayah.databackup.activity.processing

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.adapter.ProcessingItemAdapter
import com.xayah.databackup.adapter.ProcessingTaskAdapter
import com.xayah.databackup.data.ProcessingItem
import com.xayah.databackup.data.ProcessingTask
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalObject
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProcessingRestoreMediaActivity : ProcessingBaseActivity() {
    lateinit var viewModel: ProcessingBaseViewModel

    /**
     * 全局单例对象
     */
    private val globalObject = GlobalObject.getInstance()

    // 媒体列表
    private val mediaInfoRestoreMap
        get() = globalObject.mediaInfoRestoreMap.value.values.toList()
            .filter { if (it.detailRestoreList.isNotEmpty()) it.detailRestoreList[it.restoreIndex].data else false }
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
                for (i in mediaInfoRestoreMap) processingTaskList.value.add(
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
            viewModel.btnText.set(GlobalString.restore)
            viewModel.btnDesc.set(GlobalString.clickTheRightBtnToStart)
            viewModel.progressMax.set(mediaInfoRestoreMap.size)
            viewModel.progressText.set("${GlobalString.progress}: ${viewModel.progress.get()}/${viewModel.progressMax.get()}")
            viewModel.totalTip.set(GlobalString.ready)
            mediaInfoRestoreMap.size.apply {
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
                        for ((index, i) in mediaInfoRestoreMap.withIndex()) {
                            // 准备备份卡片数据
                            viewModel.appName.set(i.name)
                            viewModel.packageName.set(i.path)

                            val inPath =
                                "${Path.getBackupMediaSavePath()}/${i.name}/${i.detailRestoreList[i.restoreIndex].date}"

                            // 开始恢复
                            var state = true // 该任务是否成功完成
                            if (i.detailRestoreList[i.restoreIndex].data) {
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

                                // 恢复目录
                                val inputPath = "${inPath}/${i.name}.tar"
                                Command.decompress(
                                    Command.getCompressionTypeByPath(inputPath),
                                    "media",
                                    inputPath,
                                    i.name,
                                    i.path.replace("/${i.name}", "")
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
                                }

                                processingItem.isProcessing = false
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
                    }
                }
            }
        } else {
            finish()
        }
    }
}