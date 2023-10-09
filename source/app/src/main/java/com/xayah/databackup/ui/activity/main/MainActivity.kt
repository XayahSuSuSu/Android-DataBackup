package com.xayah.databackup.ui.activity.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.ui.activity.main.page.MainViewModel
import com.xayah.databackup.ui.activity.main.router.MainNavHost
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.MainScaffold
import com.xayah.databackup.ui.component.rememberSlotScope
import com.xayah.databackup.ui.theme.DataBackupTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalLayoutApi
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DataBackupTheme {
                val slotScope = rememberSlotScope()
                CompositionLocalProvider(LocalSlotScope provides slotScope) {
                    val mainViewModel = hiltViewModel<MainViewModel>()
                    MainScaffold(viewModel = mainViewModel) {
                        MainNavHost(navController = LocalSlotScope.current!!.navController, viewModel = mainViewModel)
                    }
                }
            }
        }
    }
}
