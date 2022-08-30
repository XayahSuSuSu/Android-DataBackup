package com.xayah.databackup.activity.processing

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.databinding.ActivityProcessingBinding
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.view.util.setWithConfirm

class ProcessingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProcessingBinding
    private lateinit var viewModel: ProcessingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityProcessingBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[ProcessingViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        viewModel.apply {
            isMedia = intent.getBooleanExtra("isMedia", false)
            isRestore = intent.getBooleanExtra("isRestore", false)
            initialize()
        }
    }

    override fun onBackPressed() {
        if (viewModel.dataBinding.isProcessing.get()) {
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