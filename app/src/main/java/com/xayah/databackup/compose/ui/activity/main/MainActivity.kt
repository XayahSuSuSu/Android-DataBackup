package com.xayah.databackup.compose.ui.activity.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import com.xayah.databackup.compose.ui.activity.main.components.MainScaffold
import com.xayah.databackup.compose.ui.theme.DataBackupTheme

@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DataBackupTheme {
                MainScaffold()
            }
        }
    }
}

