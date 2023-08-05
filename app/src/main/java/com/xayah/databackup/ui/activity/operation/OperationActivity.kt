package com.xayah.databackup.ui.activity.operation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import com.xayah.databackup.ui.activity.operation.router.OperationNavHost
import com.xayah.databackup.ui.component.rememberSlotScope
import com.xayah.databackup.ui.theme.DataBackupTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OperationActivity : ComponentActivity() {
    @ExperimentalLayoutApi
    @ExperimentalAnimationApi
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DataBackupTheme {
                val slotScope = rememberSlotScope()
                slotScope.OperationNavHost()
            }
        }
    }
}
