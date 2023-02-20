package com.xayah.databackup.compose.ui.activity.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.compose.ui.activity.list.components.ListScaffold
import com.xayah.databackup.compose.ui.activity.list.components.content.onAppBackupContent
import com.xayah.databackup.compose.ui.activity.list.components.content.onAppBackupInitialize
import com.xayah.databackup.compose.ui.activity.list.components.content.onAppBackupManifest
import com.xayah.databackup.compose.ui.activity.list.components.content.toAppBackupProcessing
import com.xayah.databackup.compose.ui.theme.DataBackupTheme
import com.xayah.databackup.data.*

@ExperimentalMaterial3Api
class ListActivity : ComponentActivity() {
    private lateinit var viewModel: ListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel = ViewModelProvider(this)[ListViewModel::class.java]

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        })

        val type = intent.getStringExtra(TypeActivityTag)
        setContent {
            DataBackupTheme {
                LaunchedEffect(null) {
                    when (type) {
                        TypeBackupApp -> {
                            onAppBackupInitialize(viewModel)
                        }
                        TypeBackupMedia -> {}
                        TypeRestoreApp -> {}
                        TypeRestoreMedia -> {}
                    }
                }

                val isInitialized by viewModel.isInitialized.collectAsState()
                val onManifest by viewModel.onManifest.collectAsState()
                ListScaffold(
                    isInitialized = isInitialized,
                    onManifest = onManifest,
                    content = {
                        if (onManifest) {
                            when (type) {
                                TypeBackupApp -> {
                                    onAppBackupManifest(viewModel, this@ListActivity)
                                }
                                TypeBackupMedia -> {}
                                TypeRestoreApp -> {}
                                TypeRestoreMedia -> {}
                            }
                        } else {
                            when (type) {
                                TypeBackupApp -> {
                                    onAppBackupContent(viewModel)
                                }
                                TypeBackupMedia -> {}
                                TypeRestoreApp -> {}
                                TypeRestoreMedia -> {}
                            }
                        }
                    },
                    onNext = {
                        if (onManifest) {
                            when (type) {
                                TypeBackupApp -> {
                                    toAppBackupProcessing(this)
                                }
                                TypeBackupMedia -> {}
                                TypeRestoreApp -> {}
                                TypeRestoreMedia -> {}
                            }

                        } else {
                            viewModel.onManifest.value = true
                        }
                    },
                    onFinish = {
                        onBack()
                    })
            }
        }
    }

    fun onBack() {
        if (viewModel.onManifest.value) {
            viewModel.onManifest.value = false
        } else {
            finish()
        }
    }
}
