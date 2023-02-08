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
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.activity.processing.action.onBackupAppProcessing
import com.xayah.databackup.compose.ui.activity.processing.action.onBackupMediaProcessing
import com.xayah.databackup.compose.ui.activity.processing.action.onRestoreAppProcessing
import com.xayah.databackup.compose.ui.activity.processing.action.onRestoreMediaProcessing
import com.xayah.databackup.compose.ui.activity.processing.components.ProcessingScaffold
import com.xayah.databackup.compose.ui.theme.DataBackupTheme
import com.xayah.databackup.data.*
import com.xayah.databackup.util.GlobalObject

@ExperimentalMaterial3Api
class ProcessingActivity : ComponentActivity() {
    /**
     * 全局单例对象
     */
    private val globalObject = GlobalObject.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val viewModel = ViewModelProvider(this)[ProcessingViewModel::class.java]
        val type = intent.getStringExtra(ProcessingActivityTag)
        val that = this

        when (type) {
            TypeBackupApp -> {
                onBackupAppProcessing(viewModel, context = this, globalObject = globalObject)
            }
            TypeBackupMedia -> {
                onBackupMediaProcessing(viewModel, context = this, globalObject = globalObject)
            }
            TypeRestoreApp -> {
                onRestoreAppProcessing(viewModel, context = this, globalObject = globalObject)
            }
            TypeRestoreMedia -> {
                onRestoreMediaProcessing(viewModel, context = this, globalObject = globalObject)
            }
        }

        setContent {
            DataBackupTheme {
                // 是否完成
                val (exitConfirmDialog, setExitConfirmDialog) = remember { mutableStateOf(false) }
                LaunchedEffect(null) {
                    onBackPressedDispatcher.addCallback(that, object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            if (viewModel.allDone.value.not()) {
                                setExitConfirmDialog(true)
                            } else {
                                finish()
                            }
                        }
                    })
                }

                ProcessingScaffold(viewModel) { finish() }

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
                                    viewModel.topBarTitle.value = getString(R.string.cancelling)
                                    viewModel.isCancel.value = true
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
