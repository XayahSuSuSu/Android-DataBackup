package com.xayah.databackup.ui.activity.operation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import com.xayah.databackup.ui.activity.main.router.currentRoute
import com.xayah.databackup.ui.activity.operation.router.OperationNavHost
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.rememberSlotScope
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.IntentUtil
import com.xayah.databackup.util.readKeepScreenOn
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OperationActivity : ComponentActivity() {
    @ExperimentalFoundationApi
    @ExperimentalLayoutApi
    @ExperimentalAnimationApi
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val route = intent.getStringExtra(IntentUtil.ExtraRoute) ?: OperationRoutes.PackageBackup.route
        setContent {
            DataBackupTheme {
                val slotScope = rememberSlotScope()
                val currentRoute = slotScope.navController.currentRoute()
                LaunchedEffect(currentRoute) {
                    when (currentRoute) {
                        OperationRoutes.PackageBackupProcessing.route, OperationRoutes.PackageRestoreProcessing.route -> {
                            if (readKeepScreenOn()) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }

                        else -> {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                }

                CompositionLocalProvider(LocalSlotScope provides slotScope) {
                    OperationNavHost(route)
                }
            }
        }
    }
}
