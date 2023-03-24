package com.xayah.databackup.ui.activity.guide

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.xayah.databackup.App
import com.xayah.databackup.data.GuideType
import com.xayah.databackup.data.LoadingState
import com.xayah.databackup.ui.activity.guide.components.ItemEnvironment
import com.xayah.databackup.ui.activity.guide.components.ItemUpdate
import com.xayah.databackup.util.*
import com.xayah.databackup.util.command.Command
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class GuideViewModel : ViewModel() {
    val initType: MutableState<GuideType> = mutableStateOf(GuideType.Introduction)

    val loadingState: MutableState<LoadingState> = mutableStateOf(LoadingState.Loading)
    val updateList by lazy {
        mutableStateOf(listOf<ItemUpdate>())
    }

    val isPermissionDialogOpen: MutableState<Boolean> = mutableStateOf(false)
    val showFinishBtn: MutableState<Boolean> = mutableStateOf(false)
    val environmentList by lazy {
        MutableStateFlow(SnapshotStateList<ItemEnvironment>())
    }

    fun checkAllGranted() {
        var allGranted = true
        for (i in environmentList.value) {
            if (i.cardState.value == LoadingState.Loading || i.cardState.value == LoadingState.Failed) {
                allGranted = false
            }
        }
        showFinishBtn.value = allGranted
    }

    private val mutex = Mutex()

    suspend fun getUpdateList(onSuccess: () -> Unit, onFailed: () -> Unit) {
        mutex.withLock {
            Server.getInstance().releases(
                successCallback = { releaseList ->
                    val appReleaseList = releaseList.appReleaseList()
                    val itemUpdateList = mutableListOf<ItemUpdate>()
                    for (i in appReleaseList) {
                        itemUpdateList.add(
                            ItemUpdate(
                                i.name,
                                i.body.replace("* ", "").replace("*", ""),
                                i.html_url
                            )
                        )
                    }
                    updateList.value = itemUpdateList
                    onSuccess()
                },
                failedCallback = onFailed
            )
        }
    }

    /**
     * 设置权限
     */
    @SuppressLint("SetWorldWritable", "SetWorldReadable")
    fun File.setPermissions() {
        this.setExecutable(true, false)
        this.setWritable(true, false)
        this.setReadable(true, false)
    }

    /**
     * 检查Root
     */
    suspend fun checkRootAccess(): LoadingState {
        return if (Command.checkRoot()) LoadingState.Success else LoadingState.Failed
    }

    /**
     * 释放二进制文件
     */
    suspend fun binRelease(context: Context): LoadingState {
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
                File(binZipPath).deleteRecursively()
            }
            if (!bin.exists()) {
                Command.releaseAssets(
                    context, "bin/bin.zip", "bin.zip"
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
     * 检查存储管理权限
     */
    @OptIn(ExperimentalPermissionsApi::class)
    fun checkStorageManagementPermission(
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
    fun checkPackageUsageStatsPermission(context: Context): LoadingState {
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
}
