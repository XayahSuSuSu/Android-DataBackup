package com.xayah.databackup.ui.activity.crash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.ui.component.CrashScaffold
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.rememberSlotScope
import com.xayah.databackup.ui.theme.DataBackupTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CrashActivity : ComponentActivity() {
    @ExperimentalLayoutApi
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DataBackupTheme {
                val slotScope = rememberSlotScope()
                CompositionLocalProvider(LocalSlotScope provides slotScope) {
                    val viewModel = hiltViewModel<CrashViewModel>()
                    viewModel.setText(intent.getStringExtra("crashInfo") ?: "")
                    CrashScaffold(viewModel = viewModel)
                }
            }
        }
    }
}
