package com.xayah.databackup.ui.activity.crash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.ui.activity.crash.components.CrashScaffold
import com.xayah.databackup.ui.theme.DataBackupTheme

@ExperimentalMaterial3Api
class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val viewModel = ViewModelProvider(this)[CrashViewModel::class.java]
        viewModel.initializeExplorer(this)
        viewModel.crashInfo = intent.getStringExtra("crashInfo") ?: ""

        setContent {
            DataBackupTheme {
                CrashScaffold(viewModel) {
                    viewModel.saveCrashInfo(this)
                }
            }
        }
    }
}

