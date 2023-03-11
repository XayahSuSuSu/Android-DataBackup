package com.xayah.databackup.ui.activity.list.telephony

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.xayah.databackup.R
import com.xayah.databackup.data.TypeActivityTag
import com.xayah.databackup.data.TypeBackupTelephony
import com.xayah.databackup.data.TypeRestoreTelephony
import com.xayah.databackup.ui.activity.list.telephony.components.TelephonyScaffold
import com.xayah.databackup.ui.activity.list.telephony.components.item.SmsBackupItem
import com.xayah.databackup.ui.activity.list.telephony.components.item.SmsRestoreItem
import com.xayah.databackup.ui.activity.list.telephony.util.Loader
import com.xayah.databackup.ui.activity.list.telephony.util.Processor
import com.xayah.databackup.ui.components.TextDialog
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.GlobalObject
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

        GlobalObject.initializeRootService {
            // Load list
            viewModel.viewModelScope.launch {
                when (type) {
                    TypeBackupTelephony -> {
                        Loader.smsBackupList(
                            viewModel = viewModel,
                            context = this@TelephonyActivity
                        )
                    }
                    TypeRestoreTelephony -> {
                        Loader.smsRestoreList(
                            viewModel = viewModel,
                            context = this@TelephonyActivity
                        )
                    }
                    else -> {
                    }
                }
            }
        }

        setContent {
            DataBackupTheme {
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

                val smsPermissionState = rememberPermissionState(
                    Manifest.permission.READ_SMS
                )
                LaunchedEffect(null) {
                    // Check permission
                    if (smsPermissionState.status.isGranted.not()) {
                        smsPermissionState.launchPermissionRequest()
                    }
                }

                TelephonyScaffold(
                    viewModel = viewModel,
                    isFabVisible = when (viewModel.tabRowState.value) {
                        0 -> false
                        1 -> false
                        2 -> true
                        3 -> false
                        else -> false
                    },
                    onConfirm = {
                        scope.launch {
                            if (isRoleHeld.not()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.not_the_default_sms_app_info),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                when (type) {
                                    TypeBackupTelephony -> {
                                        Processor.smsBackup(
                                            viewModel = viewModel,
                                            context = context
                                        )
                                    }
                                    TypeRestoreTelephony -> {
                                        Processor.smsRestore(
                                            viewModel = viewModel,
                                            context = context
                                        )
                                    }
                                    else -> {
                                    }
                                }
                            }
                        }
                    },
                    onFinish = { finish() },
                    content = {
                        when (viewModel.tabRowState.value) {
                            0 -> {}
                            1 -> {}
                            2 -> {
                                items(items = viewModel.smsList.value, itemContent = {
                                    when (type) {
                                        TypeBackupTelephony -> {
                                            SmsBackupItem(item = it)
                                        }
                                        TypeRestoreTelephony -> {
                                            SmsRestoreItem(item = it)
                                        }
                                        else -> {
                                        }
                                    }
                                })
                            }
                            3 -> {}
                        }
                    }
                )
            }
        }
    }
}
