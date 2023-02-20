package com.xayah.databackup.compose.ui.activity.guide.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.LoadingState
import com.xayah.databackup.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class EnvItem(
    @StringRes val itemId: Int,
    var cardState: LoadingState,
    val onCheck: suspend () -> LoadingState
)

/**
 * 检查Root
 */
private suspend fun checkRootAccess(): LoadingState {
    return if (Command.checkRoot()) LoadingState.Success else LoadingState.Failed
}

@SuppressLint("SetWorldWritable", "SetWorldReadable")
fun File.setPermissions() {
    this.setExecutable(true, false)
    this.setWritable(true, false)
    this.setReadable(true, false)
}

/**
 * 释放二进制文件
 */
private suspend fun binRelease(context: Context): LoadingState {
    // 环境目录
    val filesDirectory = Path.getAppInternalFilesPath()
    val binPath = "${filesDirectory}/bin"
    val binVersionPath = "${filesDirectory}/bin/version"
    val binZipPath = "${filesDirectory}/bin.zip"
    var checkBin: Boolean

    withContext(Dispatchers.IO) {
        val bin = File(binPath)
        // 环境检测与释放
        val version = String(context.assets.open("bin/version").readBytes())
        val localVersion = try {
            File(binVersionPath).readText()
        } catch (e: Exception) {
            ""
        }
        if (version > localVersion) {
            bin.deleteRecursively()
        }
        if (!bin.exists()) {
            Command.releaseAssets(
                context, "bin/${Command.getABI()}/bin.zip", "bin.zip"
            )
            Command.unzipByZip4j(binZipPath, binPath)
            context.saveAppVersion(App.versionName)
        }
        File(binPath).listFiles()?.apply {
            for (i in this) {
                i.setPermissions()
            }
        }
        checkBin = Command.checkBin().apply {
            // 环境检测通过后删除压缩文件
            if (this)
                Command.rm(binZipPath)
        }
    }

    return if (checkBin) LoadingState.Success else LoadingState.Failed
}

/**
 * 检查Bashrc环境
 */
private suspend fun checkBashrc(): LoadingState {
    return if (Bashrc.checkBashrc()) LoadingState.Success else LoadingState.Failed
}

/**
 * 检查存储管理权限
 */
@OptIn(ExperimentalPermissionsApi::class)
private fun checkStorageManagementPermission(
    context: Context,
    readPermissionState: PermissionState?,
    writePermissionState: PermissionState?,
): LoadingState {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (Environment.isExternalStorageManager()) {
            return LoadingState.Success
        } else {
            context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            })
        }
    } else {
        var writeGranted = false
        var readGranted = false
        writePermissionState?.apply {
            if (status.isGranted) {
                writeGranted = true
            }
            launchPermissionRequest()
        }
        readPermissionState?.apply {
            if (status.isGranted) {
                readGranted = true
            }
            launchPermissionRequest()
        }
        if (writeGranted && readGranted)
            return LoadingState.Success
    }
    return LoadingState.Loading
}

/**
 * 检查应用使用详情权限
 */
private fun checkPackageUsageStatsPermission(context: Context): LoadingState {
    if (context.checkPackageUsageStatsPermission()) {
        // 已获取权限
        context.saveIsSupportUsageAccess(true)
        return LoadingState.Success
    } else {
        try {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.parse("package:${App.globalContext.packageName}")
            })
        } catch (e: Exception) {
            context.saveIsSupportUsageAccess(false)
            return LoadingState.Success
        }
    }
    return LoadingState.Loading
}

@OptIn(ExperimentalPermissionsApi::class)
@ExperimentalMaterial3Api
@Composable
fun Env(onPass: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (permissionDialog, setPermissionDialog) = remember { mutableStateOf(false) }
    val (fitPermissionNum, setFitPermissionNum) = remember { mutableStateOf(0) }
    var readPermissionState: PermissionState? = null
    var writePermissionState: PermissionState? = null
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        readPermissionState = rememberPermissionState(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        writePermissionState = rememberPermissionState(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    val envList = remember {
        mutableStateListOf(
            EnvItem(
                itemId = R.string.grant_root_access,
                cardState = LoadingState.Loading,
                onCheck = {
                    checkRootAccess()
                }
            ),
            EnvItem(
                itemId = R.string.release_prebuilt_binaries,
                cardState = LoadingState.Loading,
                onCheck = {
                    binRelease(context)
                }
            ),
            EnvItem(
                itemId = R.string.activate_bashrc,
                cardState = LoadingState.Loading,
                onCheck = {
                    checkBashrc()
                }
            ),
            EnvItem(
                itemId = R.string.check_storage_management_permission,
                cardState = LoadingState.Loading,
                onCheck = {
                    checkStorageManagementPermission(
                        context,
                        readPermissionState,
                        writePermissionState
                    )
                }
            ),
            EnvItem(
                itemId = R.string.check_package_usage_stats_permission,
                cardState = LoadingState.Loading,
                onCheck = {
                    checkPackageUsageStatsPermission(context)
                }
            )
        )
    }

    GuideScaffold(
        title = stringResource(id = R.string.environment_detection),
        icon = Icons.Rounded.CheckCircle,
        showBtnIcon = fitPermissionNum == envList.size,
        nextBtnIcon = Icons.Rounded.ArrowForward,
        onNextBtnClick = {
            onPass()
        },
        items = {
            items(count = envList.size, key = { envList[it].itemId }) {
                EnvCard(
                    item = stringResource(id = envList[it].itemId),
                    cardState = envList[it].cardState,
                    onCardClick = {
                        scope.launch {
                            val state = envList[it].onCheck().apply {
                                if (this == LoadingState.Success)
                                    setFitPermissionNum(fitPermissionNum + 1)
                            }
                            envList[it] = envList[it].copy(cardState = state)
                        }
                    }
                )
            }
        }
    )

    if (permissionDialog) {
        AlertDialog(
            onDismissRequest = {
                setPermissionDialog(false)
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null
                )
            },
            title = {
                Text(
                    text = stringResource(id = R.string.error)
                )
            },
            text = {
                Text(
                    text = "${stringResource(R.string.path_permission_error)}:" + "\n" +
                            "rwxrwxrwx(777): ${Path.getAppInternalFilesPath()}/bin"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        setPermissionDialog(false)
                    }
                ) {
                    Text(stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                            ClipData.newPlainText(
                                "PermissionCmd",
                                "su; chmod 777 ${Path.getAppInternalFilesPath()}/bin/*"
                            )
                        )
                        setPermissionDialog(false)
                    }
                ) {
                    Text(stringResource(R.string.copy_grant_command))
                }
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }
}
