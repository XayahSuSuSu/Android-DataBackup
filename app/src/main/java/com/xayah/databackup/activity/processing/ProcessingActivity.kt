package com.xayah.databackup.activity.processing

import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.activity.AppCompatActivityBase
import com.xayah.databackup.databinding.ActivityProcessingBinding

class ProcessingActivity : AppCompatActivityBase() {
    private lateinit var binding: ActivityProcessingBinding
    private lateinit var viewModel: ProcessingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityProcessingBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[ProcessingViewModel::class.java]
        binding.viewModel = viewModel
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        viewModel.initialize(
            intent.getBooleanExtra("isRestore", false),
            intent.getBooleanExtra("isMedia", false)
        )
    }
}