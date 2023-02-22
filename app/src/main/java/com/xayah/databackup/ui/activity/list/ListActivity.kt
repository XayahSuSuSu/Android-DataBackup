package com.xayah.databackup.ui.activity.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.ui.activity.list.components.ListScaffold
import com.xayah.databackup.ui.activity.list.components.content.*
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
class ListActivity : ComponentActivity() {
    private lateinit var viewModel: ListViewModel
    private lateinit var type: String
    private lateinit var explorer: MaterialYouFileExplorer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel = ViewModelProvider(this)[ListViewModel::class.java]

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        })

        explorer = MaterialYouFileExplorer().apply {
            initialize(this@ListActivity)
        }

        type = intent.getStringExtra(TypeActivityTag) ?: TypeBackupApp
        setContent {
            DataBackupTheme {
                LaunchedEffect(null) {
                    when (type) {
                        TypeBackupApp -> {
                            onAppBackupInitialize(viewModel)
                        }
                        TypeBackupMedia -> {
                            onMediaBackupInitialize(viewModel)
                        }
                        TypeRestoreApp -> {
                            onAppRestoreInitialize(viewModel)
                        }
                        TypeRestoreMedia -> {
                            onMediaRestoreInitialize(viewModel)
                        }
                    }
                }

                val isInitialized = viewModel.isInitialized
                val onManifest by viewModel.onManifest.collectAsState()
                ListScaffold(
                    isInitialized = isInitialized,
                    topBarTitle = when (type) {
                        TypeBackupApp -> {
                            stringResource(R.string.select_backup_app)
                        }
                        TypeBackupMedia -> {
                            stringResource(R.string.select_backup_media)
                        }
                        TypeRestoreApp -> {
                            stringResource(R.string.select_restore_app)
                        }
                        TypeRestoreMedia -> {
                            stringResource(R.string.select_restore_media)
                        }
                        else -> {
                            ""
                        }
                    },
                    onManifest = onManifest,
                    actions = {
                        if (onManifest.not()) {
                            Row {
                                val openBottomSheet = remember { mutableStateOf(false) }

                                when (type) {
                                    TypeBackupApp -> {
                                        AppBackupBottomSheet(
                                            isOpen = openBottomSheet,
                                            viewModel = viewModel
                                        )
                                    }
                                    TypeBackupMedia -> {
                                        MediaBackupBottomSheet(
                                            isOpen = openBottomSheet,
                                            viewModel = viewModel,
                                            context = this@ListActivity,
                                            explorer = explorer
                                        )
                                    }
                                    TypeRestoreApp -> {
                                        AppRestoreBottomSheet(
                                            isOpen = openBottomSheet,
                                            viewModel = viewModel
                                        )
                                    }
                                    TypeRestoreMedia -> {
                                        MediaRestoreBottomSheet(
                                            isOpen = openBottomSheet,
                                            viewModel = viewModel,
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    openBottomSheet.value = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Menu,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    },
                    content = {
                        if (onManifest) {
                            when (type) {
                                TypeBackupApp -> {
                                    onAppBackupManifest(viewModel, this@ListActivity)
                                }
                                TypeBackupMedia -> {
                                    onMediaBackupManifest(viewModel, this@ListActivity)
                                }
                                TypeRestoreApp -> {
                                    onAppRestoreManifest(viewModel, this@ListActivity)
                                }
                                TypeRestoreMedia -> {
                                    onMediaRestoreManifest(viewModel, this@ListActivity)
                                }
                            }
                        } else {
                            when (type) {
                                TypeBackupApp -> {
                                    onAppBackupContent(viewModel)
                                }
                                TypeBackupMedia -> {
                                    onMediaBackupContent(viewModel)
                                }
                                TypeRestoreApp -> {
                                    onAppRestoreContent(viewModel)
                                }
                                TypeRestoreMedia -> {
                                    onMediaRestoreContent(viewModel)
                                }
                            }
                        }
                    },
                    onNext = {
                        if (onManifest) {
                            when (type) {
                                TypeBackupApp -> {
                                    toAppBackupProcessing(this)
                                    finish()
                                }
                                TypeBackupMedia -> {
                                    toMediaBackupProcessing(this)
                                    finish()
                                }
                                TypeRestoreApp -> {
                                    toAppRestoreProcessing(this)
                                    finish()
                                }
                                TypeRestoreMedia -> {
                                    toMediaRestoreProcessing(this)
                                    finish()
                                }
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

    override fun onPause() {
        super.onPause()
        CoroutineScope(Dispatchers.IO).launch {
            // 保存日志
            when (type) {
                TypeBackupApp -> {
                    onAppBackupMapSave()
                }
                TypeBackupMedia -> {
                    onMediaBackupMapSave()
                }
                TypeRestoreApp -> {
                    onAppRestoreMapSave()
                }
                TypeRestoreMedia -> {
                    onMediaRestoreMapSave()
                }
            }
        }
    }
}
