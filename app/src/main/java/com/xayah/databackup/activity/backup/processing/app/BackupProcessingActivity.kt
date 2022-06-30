package com.xayah.databackup.activity.backup.processing.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.databinding.ActivityBackupProcessingBinding

class BackupProcessingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackupProcessingBinding
    private lateinit var viewModel: BackupProcessingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityBackupProcessingBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[BackupProcessingViewModel::class.java]
        binding.viewModel = viewModel
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        viewModel.initialize(intent.getBooleanExtra("isMedia", false))
    }
}