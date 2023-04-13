package com.xayah.databackup.ui.activity.processing

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.ui.activity.processing.action.onBackupAppProcessing
import com.xayah.databackup.ui.activity.processing.action.onBackupMediaProcessing
import com.xayah.databackup.ui.activity.processing.action.onRestoreAppProcessing
import com.xayah.databackup.ui.activity.processing.action.onRestoreMediaProcessing
import com.xayah.databackup.ui.activity.processing.components.EndPageBottomSheet
import com.xayah.databackup.ui.activity.processing.components.ProcessingScaffold
import com.xayah.databackup.ui.components.ConfirmDialog
import com.xayah.databackup.ui.components.IconButton
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.GlobalObject
import com.xayah.databackup.util.readKeepTheScreenOn

@ExperimentalMaterial3Api
class ProcessingActivity : ComponentActivity() {
    /**
     * 全局单例对象
     */
    private val globalObject = GlobalObject.getInstance()

    private fun onProcessing(viewModel: ProcessingViewModel, type: String) {
        when (type) {
            TypeBackupApp -> {
                onBackupAppProcessing(
                    viewModel = viewModel,
                    context = this,
                    globalObject = globalObject
                )
            }
            TypeBackupMedia -> {
                onBackupMediaProcessing(
                    viewModel = viewModel,
                    context = this,
                    globalObject = globalObject
                )
            }
            TypeRestoreApp -> {
                onRestoreAppProcessing(
                    viewModel = viewModel,
                    context = this,
                    globalObject = globalObject
                )
            }
            TypeRestoreMedia -> {
                onRestoreMediaProcessing(
                    viewModel = viewModel,
                    context = this,
                    globalObject = globalObject
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (readKeepTheScreenOn()) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val viewModel = ViewModelProvider(this)[ProcessingViewModel::class.java]
        viewModel.listType = intent.getStringExtra(TypeActivityTag) ?: TypeBackupApp
        val type = viewModel.listType
        val that = this

        setContent {
            DataBackupTheme(
                content = {
                    // 是否完成
                    val exitConfirmDialog = remember { mutableStateOf(false) }
                    LaunchedEffect(null) {
                        onBackPressedDispatcher.addCallback(that, object : OnBackPressedCallback(true) {
                            override fun handleOnBackPressed() {
                                if (viewModel.allDone.currentState.not()) {
                                    exitConfirmDialog.value = true
                                } else {
                                    finish()
                                }
                            }
                        })
                    }

                    ProcessingScaffold(
                        viewModel = viewModel,
                        actions = {
                            val openBottomSheet = remember { mutableStateOf(false) }
                            EndPageBottomSheet(isOpen = openBottomSheet, viewModel = viewModel)
                            IconButton(icon = Icons.Rounded.Menu) {
                            openBottomSheet.value = true
                        }
                    }) { finish() }

                ConfirmDialog(
                    isOpen = exitConfirmDialog,
                    icon = Icons.Rounded.Info,
                    title = stringResource(id = R.string.tips),
                    content = {
                        Text(
                            text = stringResource(R.string.confirm_exit)
                        )
                    }) {
                    viewModel.topBarTitle.value = getString(R.string.cancelling)
                    viewModel.isCancel.value = true
                }
                },
                onRootServiceInitialized = {
                    onProcessing(viewModel, type)
                })
        }
    }
}
