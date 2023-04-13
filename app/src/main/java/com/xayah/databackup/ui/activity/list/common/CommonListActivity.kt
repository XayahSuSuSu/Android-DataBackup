package com.xayah.databackup.ui.activity.list.common

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.ui.activity.list.common.components.ListScaffold
import com.xayah.databackup.ui.activity.list.common.components.content.*
import com.xayah.databackup.ui.components.IconButton
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
class CommonListActivity : ComponentActivity() {
    private lateinit var viewModel: CommonListViewModel
    private lateinit var type: String
    private lateinit var explorer: MaterialYouFileExplorer

    private suspend fun onInitialize() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel = ViewModelProvider(this)[CommonListViewModel::class.java]

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        })

        explorer = MaterialYouFileExplorer().apply {
            initialize(this@CommonListActivity)
        }

        type = intent.getStringExtra(TypeActivityTag) ?: TypeBackupApp
        setContent {
            DataBackupTheme(
                content = {
                    val isInitialized = viewModel.isInitialized
                    val progress = viewModel.progress
                    val onManifest by viewModel.onManifest.collectAsState()
                    ListScaffold(
                        isInitialized = isInitialized,
                        progress = progress.value,
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
                                        ) { finish() }
                                    }
                                    TypeBackupMedia -> {
                                        MediaBackupBottomSheet(
                                            isOpen = openBottomSheet,
                                            viewModel = viewModel,
                                            context = this@CommonListActivity,
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
                                IconButton(icon = Icons.Rounded.Menu) {
                                    openBottomSheet.value = true
                                }
                            }
                        }
                    },
                    content = {
                        if (onManifest) {
                            when (type) {
                                TypeBackupApp -> {
                                    onAppBackupManifest(viewModel, this@CommonListActivity)
                                }
                                TypeBackupMedia -> {
                                    onMediaBackupManifest(viewModel, this@CommonListActivity)
                                }
                                TypeRestoreApp -> {
                                    onAppRestoreManifest(viewModel, this@CommonListActivity)
                                }
                                TypeRestoreMedia -> {
                                    onMediaRestoreManifest(viewModel, this@CommonListActivity)
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
                },
                onRootServiceInitialized = {
                    onInitialize()
                })
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
