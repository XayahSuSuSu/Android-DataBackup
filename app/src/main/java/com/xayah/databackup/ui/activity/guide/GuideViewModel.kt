package com.xayah.databackup.ui.activity.guide

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.xayah.databackup.App
import com.xayah.databackup.data.GuideType
import com.xayah.databackup.data.LoadingState
import com.xayah.databackup.ui.activity.guide.components.ItemEnvironment
import com.xayah.databackup.ui.activity.guide.components.ItemUpdate
import com.xayah.databackup.util.Path
import com.xayah.databackup.util.Server
import com.xayah.databackup.util.appReleaseList
import com.xayah.databackup.util.command.Command
import com.xayah.databackup.util.saveAppVersion
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
        val binZipPath = "${filesDirectory}/bin.zip"
        var checkBin: Boolean

        withContext(Dispatchers.IO) {
            val bin = File(binPath)
            // 环境检测与释放
            bin.deleteRecursively()
            File(binZipPath).deleteRecursively()
            if (!bin.exists()) {
                Command.releaseAssets(
                    context, "bin/bin.zip", "bin.zip"
                )
                Command.unzip(binZipPath, binPath)
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
                    File(binZipPath).deleteRecursively()
            }
        }

        return if (checkBin) LoadingState.Success else LoadingState.Failed
    }
}
