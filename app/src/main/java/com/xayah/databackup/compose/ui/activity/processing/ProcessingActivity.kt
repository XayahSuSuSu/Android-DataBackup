package com.xayah.databackup.compose.ui.activity.processing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import com.xayah.databackup.compose.ui.activity.processing.components.BackupApp
import com.xayah.databackup.compose.ui.activity.processing.components.BackupMedia
import com.xayah.databackup.compose.ui.activity.processing.components.RestoreApp
import com.xayah.databackup.compose.ui.activity.processing.components.RestoreMedia
import com.xayah.databackup.compose.ui.theme.DataBackupTheme
import com.xayah.databackup.data.*

@ExperimentalMaterial3Api
class ProcessingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val type = intent.getStringExtra(ProcessingActivityTag)

        setContent {
            DataBackupTheme {
                when (type) {
                    TypeBackupApp -> {
                        BackupApp { finish() }
                    }
                    TypeBackupMedia -> {
                        BackupMedia { finish() }
                    }
                    TypeRestoreApp -> {
                        RestoreApp { finish() }
                    }
                    TypeRestoreMedia -> {
                        RestoreMedia { finish() }
                    }
                }
            }
        }
    }
}
