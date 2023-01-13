package com.xayah.databackup.activity.processing

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.adapter.ProcessingTaskAdapter
import com.xayah.databackup.databinding.ActivityProcessingBinding
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.view.fastInitialize
import com.xayah.databackup.view.util.setWithConfirm

abstract class ProcessingBaseActivity : AppCompatActivity() {
    companion object {
        fun setSizeAndSpeed(viewModel: ProcessingBaseViewModel, src: String?) {
            try {
                when (src) {
                    "install apk finished" -> {
                        // 安装应用中
                        viewModel.size.set("0")
                        viewModel.sizeUnit.set("")
                        viewModel.speed.set(GlobalString.installing)
                        viewModel.speedUnit.set("")
                    }
                    "testing" -> {
                        // 安装应用中
                        viewModel.size.set("0")
                        viewModel.sizeUnit.set("")
                        viewModel.speed.set(GlobalString.testing)
                        viewModel.speedUnit.set("")
                    }
                    else -> {
                        val newSrc = src?.replace("[", "")?.replace("]", "")
                        val sizeSrc = newSrc?.split(" ")?.filter { item -> item != "" }?.get(0)
                        val speedSrc =
                            newSrc?.split(" ")?.filter { item -> item != "" }?.get(2)
                                ?.replace(" ", "")
                                ?.replace("]", "")
                        viewModel.size.set(sizeSrc?.filter { item -> item.isDigit() || item == '.' })
                        viewModel.sizeUnit.set(sizeSrc?.filter { item -> item.isLetter() })
                        viewModel.speed.set(speedSrc?.filter { item -> item.isDigit() || item == '.' })
                        viewModel.speedUnit.set(speedSrc?.filter { item -> item.isLetter() || item == '/' })
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun initializeSizeAndSpeed(viewModel: ProcessingBaseViewModel) {
            viewModel.size.set("0")
            viewModel.sizeUnit.set("Mib")
            viewModel.speed.set("0")
            viewModel.speedUnit.set("Mib/s")
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

    override fun onBackPressed() {
        if (viewModel.isProcessing.get()) {
            MaterialAlertDialogBuilder(this).apply {
                setWithConfirm(GlobalString.confirmExit) {
                    super.onBackPressed()
                }
            }
        } else {
            super.onBackPressed()
        }
    }
}