package com.xayah.databackup.ui.activity.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.xayah.databackup.ui.activity.main.router.ScaffoldNavHost
import com.xayah.databackup.ui.activity.main.router.ScaffoldRoutes
import com.xayah.databackup.ui.activity.main.router.navigateAndPopBackStack
import com.xayah.databackup.ui.component.rememberSlotScope
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.command.EnvUtil.getCurrentAppVersionName
import com.xayah.databackup.util.readAppVersionName
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @ExperimentalLayoutApi
    @ExperimentalAnimationApi
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DataBackupTheme {
                val slotScope = rememberSlotScope()
                val scaffoldNavController = rememberNavController()
                LaunchedEffect(null) {
                    var route = ScaffoldRoutes.Main.route
                    if (getCurrentAppVersionName() > readAppVersionName()) {
                        // There is an update
                        route = ScaffoldRoutes.Guide.route
                    }
                    scaffoldNavController.navigateAndPopBackStack(route)
                }
                slotScope.ScaffoldNavHost(scaffoldNavController = scaffoldNavController)
            }
        }
    }
}
