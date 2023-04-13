package com.xayah.databackup.ui.activity.list.telephony

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.xayah.databackup.R
import com.xayah.databackup.data.TypeActivityTag
import com.xayah.databackup.data.TypeBackupTelephony
import com.xayah.databackup.data.TypeRestoreTelephony
import com.xayah.databackup.ui.activity.list.telephony.components.TelephonyPermission
import com.xayah.databackup.ui.activity.list.telephony.components.TelephonyScaffold
import com.xayah.databackup.ui.activity.list.telephony.components.item.*
import com.xayah.databackup.ui.activity.list.telephony.util.Loader
import com.xayah.databackup.ui.activity.list.telephony.util.Processor
import com.xayah.databackup.ui.components.LoadingDialog
import com.xayah.databackup.ui.components.TextDialog
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.makeShortToast
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
class TelephonyActivity : ComponentActivity() {
    private lateinit var viewModel: TelephonyViewModel

    private val isRoleHeld: Boolean
        get() = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(this) == packageName
        }
    private val requestRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.isRoleHolderDialogOpen.value = isRoleHeld.not()
        }

    /**
     * Set DataBackup as default SMS app
     */
    private fun requestRole() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            requestRoleLauncher.launch(
                getSystemService(RoleManager::class.java).createRequestRoleIntent(
                    RoleManager.ROLE_SMS
                )
            )
        } else {
            requestRoleLauncher.launch(Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            })
        }
    }

    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel = ViewModelProvider(this)[TelephonyViewModel::class.java]

        val type = intent.getStringExtra(TypeActivityTag) ?: TypeBackupTelephony

        if (type == TypeRestoreTelephony && isRoleHeld.not()) {
            viewModel.isRoleHolderDialogOpen.value = true
        }

        setContent {
            DataBackupTheme(
                content = {
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current

                    TextDialog(
                        isOpen = viewModel.isRoleHolderDialogOpen,
                        icon = Icons.Rounded.Warning,
                        title = stringResource(id = R.string.tips),
                        content = getString(R.string.not_the_default_sms_app_info),
                        confirmText = stringResource(R.string.confirm),
                        onConfirmClick = {
                            requestRole()
                        },
                        onDismissClick = {
                            finish()
                        },
                        showDismissBtn = true
                    )

                    val permissionsState = rememberMultiplePermissionsState(
                        listOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.WRITE_CONTACTS,
                            Manifest.permission.READ_CALL_LOG,
                            Manifest.permission.WRITE_CALL_LOG,
                        )
                    )

                    if (permissionsState.allPermissionsGranted.not()) {
                        TelephonyPermission {
                            finish()
                        }
                    } else {
                        val isLoadingDialogOpen = remember {
                            mutableStateOf(false)
                        }
                        LoadingDialog(isOpen = isLoadingDialogOpen)
                        TelephonyScaffold(
                            isInitialized = viewModel.isInitialized,
                            viewModel = viewModel,
                        title = stringResource(id = R.string.telephony) + when (type) {
                            TypeBackupTelephony -> {
                                stringResource(id = R.string.backup)
                            }
                            TypeRestoreTelephony -> {
                                stringResource(id = R.string.restore)
                            }
                            else -> {
                                ""
                            }
                        },
                        isFabVisible = when (viewModel.tabRowState.value) {
                            0, 1, 2, 3 -> true
                            else -> false
                        },
                        onConfirm = {
                            scope.launch {
                                isLoadingDialogOpen.value = true
                                when (type) {
                                    TypeBackupTelephony -> {
                                        when (viewModel.tabRowState.value) {
                                            0 -> {
                                                Processor.contactsBackup(
                                                    viewModel = viewModel,
                                                    context = context
                                                )
                                            }
                                            1 -> {
                                                Processor.callLogBackup(
                                                    viewModel = viewModel,
                                                    context = context
                                                )
                                            }
                                            2 -> {
                                                Processor.smsBackup(
                                                    viewModel = viewModel,
                                                    context = context
                                                )
                                            }
                                            3 -> {
                                                Processor.mmsBackup(
                                                    viewModel = viewModel,
                                                    context = context
                                                )
                                            }
                                        }
                                    }
                                    TypeRestoreTelephony -> {
                                        if (isRoleHeld.not()) {
                                            context.makeShortToast(context.getString(R.string.not_the_default_sms_app_info))
                                        } else {
                                            when (viewModel.tabRowState.value) {
                                                0 -> {
                                                    Processor.contactsRestore(
                                                        viewModel = viewModel,
                                                        context = context
                                                    )
                                                }
                                                1 -> {
                                                    Processor.callLogRestore(
                                                        viewModel = viewModel,
                                                        context = context
                                                    )
                                                }
                                                2 -> {
                                                    Processor.smsRestore(
                                                        viewModel = viewModel,
                                                        context = context
                                                    )
                                                }
                                                3 -> {
                                                    Processor.mmsRestore(
                                                        viewModel = viewModel,
                                                        context = context
                                                    )
                                                }
                                            }

                                        }
                                    }
                                }
                                isLoadingDialogOpen.value = false
                            }
                        },
                        onFinish = { finish() },
                        content = {
                            when (viewModel.tabRowState.value) {
                                0 -> {
                                    items(items = viewModel.contactsList.value, itemContent = {
                                        when (type) {
                                            TypeBackupTelephony -> {
                                                ContactsBackupItem(item = it)
                                            }
                                            TypeRestoreTelephony -> {
                                                ContactsRestoreItem(item = it)
                                            }
                                        }
                                    })
                                }
                                1 -> {
                                    items(items = viewModel.callLogList.value, itemContent = {
                                        when (type) {
                                            TypeBackupTelephony -> {
                                                CallLogBackupItem(item = it)
                                            }
                                            TypeRestoreTelephony -> {
                                                CallLogRestoreItem(item = it)
                                            }
                                        }
                                    })
                                }
                                2 -> {
                                    items(items = viewModel.smsList.value, itemContent = {
                                        when (type) {
                                            TypeBackupTelephony -> {
                                                SmsBackupItem(item = it)
                                            }
                                            TypeRestoreTelephony -> {
                                                SmsRestoreItem(item = it)
                                            }
                                        }
                                    })
                                }
                                3 -> {
                                    items(items = viewModel.mmsList.value, itemContent = {
                                        when (type) {
                                            TypeBackupTelephony -> {
                                                MmsBackupItem(item = it)
                                            }
                                            TypeRestoreTelephony -> {
                                                MmsRestoreItem(item = it)
                                            }
                                        }
                                    })
                                }
                            }
                        }
                        )
                    }

                },
                onRootServiceInitialized = {
                    // Load list
                    viewModel.viewModelScope.launch {
                        when (type) {
                            TypeBackupTelephony -> {
                                Loader.smsBackupList(
                                    viewModel = viewModel,
                                    context = this@TelephonyActivity
                                )
                                Loader.mmsBackupList(
                                    viewModel = viewModel,
                                    context = this@TelephonyActivity
                                )
                                Loader.contactsBackupList(
                                    viewModel = viewModel,
                                    context = this@TelephonyActivity
                                )
                                Loader.callLogBackupList(
                                    viewModel = viewModel,
                                    context = this@TelephonyActivity
                                )
                            }
                            TypeRestoreTelephony -> {
                                Loader.smsRestoreList(
                                    viewModel = viewModel,
                                    context = this@TelephonyActivity
                                )
                                Loader.mmsRestoreList(
                                    viewModel = viewModel,
                                    context = this@TelephonyActivity
                                )
                                Loader.contactsRestoreList(
                                    viewModel = viewModel,
                                    context = this@TelephonyActivity
                                )
                                Loader.callLogRestoreList(
                                    viewModel = viewModel,
                                    context = this@TelephonyActivity
                                )
                            }
                        }
                        viewModel.isInitialized.targetState = true
                    }
                })
        }
    }
}
