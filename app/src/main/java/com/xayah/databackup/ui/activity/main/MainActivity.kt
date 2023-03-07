package com.xayah.databackup.ui.activity.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.ui.activity.main.components.MainScaffold
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.GlobalObject
import com.xayah.librootservice.RootService

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        GlobalObject.initializeRootService {
            viewModel.isRemoteFileInitialized.targetState = true
        }

        setContent {
            DataBackupTheme {
                MainScaffold(viewModel.isRemoteFileInitialized)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        RootService.getInstance().destroy()
    }
}
