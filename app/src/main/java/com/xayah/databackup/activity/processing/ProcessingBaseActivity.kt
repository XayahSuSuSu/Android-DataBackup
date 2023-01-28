package com.xayah.databackup.activity.processing

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.adapter.ProcessingTaskAdapter
import com.xayah.databackup.data.*
import com.xayah.databackup.databinding.ActivityProcessingBinding
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.view.fastInitialize
import com.xayah.databackup.view.util.setWithConfirm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class ProcessingBaseActivity : AppCompatActivity() {
    companion object {
        /**
         * 刷新Processing项目
         */
        suspend fun refreshProcessingItems(viewModel: ProcessingBaseViewModel) {
            withContext(Dispatchers.Main) {
                viewModel.mAdapterItems.notifyDataSetChanged()
            }
        }

        /**
         * 刷新Processing项目
         */
        suspend fun clearProcessingItems(viewModel: ProcessingBaseViewModel, size: Int) {
            withContext(Dispatchers.Main) {
                viewModel.mAdapterItems.notifyItemRangeRemoved(0, size)
            }
        }

        /**
         * 根据String信息设置ProcessingItem
         */
        fun setProcessingItem(
            src: String?,
            processingItem: ProcessingItem?
        ) {
            try {
                when (src) {
                    ProcessFinished -> {
                        // 完成
                        processingItem?.title = GlobalString.finished
                    }
                    ProcessSkip -> {
                        // 跳过
                        processingItem?.subtitle = GlobalString.noChangeAndSkip
                    }
                    ProcessCompressing -> {
                        // 压缩中
                        processingItem?.title = GlobalString.compressing
                    }
                    ProcessDecompressing -> {
                        // 解压中
                        processingItem?.title = GlobalString.decompressing
                    }
                    ProcessTesting -> {
                        // 测试中
                        processingItem?.title = GlobalString.testing
                    }
                    ProcessSettingSELinux -> {
                        // 设置SELinux中
                        processingItem?.title = GlobalString.settingSELinux
                    }
                    ProcessInstallingApk -> {
                        // 安装APK中
                        processingItem?.title = GlobalString.installing
                    }
                    else -> {
                        src?.apply {
                            // Total bytes written: 74311680 (71MiB, 238MiB/s)
                            try {
                                "\\((.*?)\\)".toRegex().find(src)?.apply {
                                    // (71MiB, 238MiB/s)
                                    val newSrc = this.value
                                        .replace("(", "")
                                        .replace(")", "")
                                        .replace(",", "")
                                        .trim()
                                    val info = newSrc.split(" ")
                                    processingItem?.subtitle =
                                        "${GlobalString.size}: ${info[0]}, ${GlobalString.speed}: ${info[1]}"
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private lateinit var binding: ActivityProcessingBinding
    private lateinit var viewModel: ProcessingBaseViewModel

    abstract fun initialize(viewModel: ProcessingBaseViewModel)

    abstract fun onFabClick()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityProcessingBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[ProcessingBaseViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
        setSupportActionBar(binding.bottomAppBar)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.isProcessing.get()) {
                    MaterialAlertDialogBuilder(this@ProcessingBaseActivity).apply {
                        setWithConfirm(GlobalString.confirmExit) {
                            finish()
                        }
                    }
                } else {
                    finish()
                }
            }
        })

        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        binding.recyclerView.apply {
            adapter = viewModel.mAdapter
            fastInitialize(true)
        }
        binding.recyclerViewSuccess.apply {
            adapter = viewModel.mAdapterSuccess
            fastInitialize(true)
        }
        binding.recyclerViewFailed.apply {
            adapter = viewModel.mAdapterFailed
            fastInitialize(true)
        }
        binding.recyclerViewItems.apply {
            adapter = viewModel.mAdapterItems
            fastInitialize()
        }


        viewModel.apply {
            isFinished.observe(this@ProcessingBaseActivity) {
                successProgress.set("${successList.value.size} ${GlobalString.success}")
                mAdapterSuccess.apply {
                    register(ProcessingTaskAdapter())
                    items = successList.value
                    notifyDataSetChanged()
                }
                mAdapterFailed.apply {
                    failedProgress.set("${failedList.value.size} ${GlobalString.failed}")
                    register(ProcessingTaskAdapter())
                    items = failedList.value
                    notifyDataSetChanged()
                }
            }
        }

        initialize(viewModel)
        // `initialize()`之后继承的Activity才持有viewModel
        binding.floatingActionButton.setOnClickListener {
            onFabClick()
        }
    }
}