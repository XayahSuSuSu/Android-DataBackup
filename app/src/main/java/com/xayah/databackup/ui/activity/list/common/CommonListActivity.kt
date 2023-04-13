package com.xayah.databackup.ui.activity.list.common

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.librootservice.RootService
import com.xayah.databackup.ui.activity.list.common.components.ListScaffold
import com.xayah.databackup.ui.activity.list.common.components.content.*
import com.xayah.databackup.ui.components.ConfirmDialog
import com.xayah.databackup.ui.components.EditTextDialog
import com.xayah.databackup.ui.components.IconButton
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.GlobalObject
import com.xayah.databackup.util.GsonUtil
import com.xayah.databackup.util.joinToLineString
import com.xayah.databackup.util.readBackupSavePath
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
                    val scope = rememberCoroutineScope()
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
                                            val isAddListDialogOpen = remember {
                                                mutableStateOf(false)
                                            }
                                            val isResultDialogOpen = remember {
                                                mutableStateOf(false)
                                            }
                                            var result by remember {
                                                mutableStateOf("")
                                            }
                                            ConfirmDialog(
                                                isOpen = isResultDialogOpen,
                                                icon = Icons.Rounded.Info,
                                                title = stringResource(R.string.finished),
                                                content = {
                                                    Text(text = result)
                                                },
                                                cancelable = false
                                            ) {}
                                            EditTextDialog(
                                                isOpen = isAddListDialogOpen,
                                                title = stringResource(id = R.string.add_list),
                                                label = stringResource(id = R.string.separated_by_newlines),
                                                defValue = stringArrayResource(id = R.array.default_media_path).toList().joinToLineString
                                            ) {
                                                scope.launch {
                                                    result = ""
                                                    val pathList = it.split("\n")
                                                    for (path in pathList) {
                                                        if (path.isEmpty()) continue
                                                        if (path == App.globalContext.readBackupSavePath()) {
                                                            result += "${getString(R.string.backup_dir)}: $path.\n"
                                                            continue
                                                        }
                                                        var name = path.split("/").last()
                                                        var isDuplicatePath = false
                                                        for (i in viewModel.mediaBackupList.value) {
                                                            if (path == i.path) {
                                                                isDuplicatePath = true
                                                                result += "${getString(R.string.duplicate)}: $path.\n"
                                                                break
                                                            }
                                                            if (name == i.name) {
                                                                name = renameDuplicateMedia(name)
                                                            }
                                                        }
                                                        if (isDuplicatePath) continue
                                                        if (RootService.getInstance().exists(path).not()) {
                                                            result += "${getString(R.string.not_exists)}: $path.\n"
                                                            continue
                                                        }
                                                        val mediaInfo = generateMediaInfoBackup(name, path)
                                                        viewModel.mediaBackupList.value.add(mediaInfo)
                                                        GlobalObject.getInstance().mediaInfoBackupMap.value[mediaInfo.name] = mediaInfo
                                                        GsonUtil.saveMediaInfoBackupMapToFile(GlobalObject.getInstance().mediaInfoBackupMap.value)
                                                        result += "${getString(R.string.added)}: $path.\n"
                                                    }
                                                    isResultDialogOpen.value = true
                                                }
                                            }
                                            MediaBackupBottomSheet(
                                                isOpen = openBottomSheet,
                                                isAddListDialogOpen = isAddListDialogOpen,
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
