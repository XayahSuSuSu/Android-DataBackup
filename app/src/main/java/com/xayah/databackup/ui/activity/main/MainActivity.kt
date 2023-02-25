package com.xayah.databackup.ui.activity.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.ui.activity.main.components.MainScaffold
import com.xayah.databackup.ui.activity.settings.components.initializeBackupDirectory
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.Logcat
import com.xayah.librootservice.RootService
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.viewModelScope.launch {
            RootService.getInstance().initialize(this@MainActivity) {
                initializeBackupDirectory()
                Logcat.getInstance().init()
                viewModel.isRemoteFileInitialized.targetState = true
            }
        }

        setContent {
            DataBackupTheme {
                val isInitialized = remember {
                    viewModel.isRemoteFileInitialized
                }
                MainScaffold(isInitialized)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        RootService.getInstance().destroy()
    }
}
