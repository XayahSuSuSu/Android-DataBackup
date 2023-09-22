package com.xayah.databackup.ui.activity.guide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.xayah.databackup.ui.activity.guide.page.GuideViewModel
import com.xayah.databackup.ui.activity.guide.router.GuideNavHost
import com.xayah.databackup.ui.component.GuideScaffold
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.rememberSlotScope
import com.xayah.databackup.ui.theme.DataBackupTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GuideActivity : ComponentActivity() {
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DataBackupTheme {
                val slotScope = rememberSlotScope()
                CompositionLocalProvider(LocalSlotScope provides slotScope) {
                    val navController = rememberNavController()
                    val viewModel = hiltViewModel<GuideViewModel>()
                    GuideScaffold(navController = navController, viewModel = viewModel) {
                        GuideNavHost(navController = navController, viewModel = viewModel)
                    }
                }
            }
        }
    }
}
