package com.xayah.databackup.compose.ui.activity.processing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import com.xayah.databackup.R
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
        val that = this
        setContent {
            DataBackupTheme {
                // 是否完成
                val allDone = remember { mutableStateOf(false) }
                val (exitConfirmDialog, setExitConfirmDialog) = remember { mutableStateOf(false) }
                LaunchedEffect(null) {
                    onBackPressedDispatcher.addCallback(that, object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            if (allDone.value.not()) {
                                setExitConfirmDialog(true)
                            } else {
                                finish()
                            }
                        }
                    })
                }



                when (type) {
                    TypeBackupApp -> {
                        BackupApp(allDone) { finish() }
                    }
                    TypeBackupMedia -> {
                        BackupMedia(allDone) { finish() }
                    }
                    TypeRestoreApp -> {
                        RestoreApp(allDone) { finish() }
                    }
                    TypeRestoreMedia -> {
                        RestoreMedia(allDone) { finish() }
                    }
                }

                if (exitConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            setExitConfirmDialog(false)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null
                            )
                        },
                        title = {
                            Text(
                                text = stringResource(id = R.string.tips)
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.confirm_exit)
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    setExitConfirmDialog(false)
                                    finish()
                                }
                            ) {
                                Text(stringResource(id = R.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    setExitConfirmDialog(false)
                                }
                            ) {
                                Text(stringResource(id = R.string.cancel))
                            }
                        },
                        properties = DialogProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
                    )
                }
            }
        }
    }
}
