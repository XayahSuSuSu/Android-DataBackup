package com.xayah.databackup.compose.ui.activity.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import com.xayah.databackup.compose.ui.activity.settings.components.SettingsScaffold
import com.xayah.databackup.compose.ui.theme.DataBackupTheme

@ExperimentalMaterial3Api
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DataBackupTheme {
                SettingsScaffold { finish() }
            }
        }
    }
}

