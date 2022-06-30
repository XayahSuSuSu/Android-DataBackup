package com.xayah.databackup.activity.backup.processing.app

import android.graphics.drawable.Drawable
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel
import com.xayah.databackup.App
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupProcessingViewModel : ViewModel() {
    var mAppInfoList: MutableList<AppInfoBackup> = mutableListOf()
    var mMediaInfoList: MutableList<MediaInfo> = mutableListOf()
    var isMedia = false
    var appName = ObservableField("")
    var packageName = ObservableField("")
    var appVersion = ObservableField("")
    var appIcon = ObservableField<Drawable>()
    var size = ObservableField("0")
    var sizeUnit = ObservableField("Mib")
    var speed = ObservableField("0")
    var speedUnit = ObservableField("Mib/s")
    var isProcessing = ObservableBoolean(false)
    var backupBtnText = ObservableField(GlobalString.backup)
    var totalTip = ObservableField("")
    var totalProgress = ObservableField("")
    var progressMax = ObservableInt(0)
    var progress = ObservableInt(0)
    var isBackupApk = ObservableBoolean(false)
    var isBackupUser = ObservableBoolean(false)
    var isBackupData = ObservableBoolean(false)
    var isBackupObb = ObservableBoolean(false)
    var processingApk = ObservableBoolean(false)
    var processingUser = ObservableBoolean(false)
    var processingData = ObservableBoolean(false)
    var processingObb = ObservableBoolean(false)
    var successNum = 0
    var failedNum = 0

    fun initialize(mIsMedia: Boolean) {
        isMedia = mIsMedia
        if (isMedia) initializeMedia()
        else initializeApp()
    }

    private fun initializeApp() {
        mAppInfoList = Command.getCachedAppInfoBackupList(App.globalContext, true)
        progressMax.set(mAppInfoList.size)
        totalTip.set(GlobalString.ready)
        Command.getCachedAppInfoBackupListNum().apply {
            totalProgress.set("${GlobalString.selected} ${this.appNum} ${GlobalString.application}, ${this.dataNum} ${GlobalString.data}")
        }
    }

    private fun initializeMedia() {
        mMediaInfoList = Command.getCachedMediaInfoList(true)
        progressMax.set(mMediaInfoList.size)
        totalTip.set(GlobalString.ready)
        totalProgress.set("${GlobalString.selected} ${mMediaInfoList.size} ${GlobalString.data}")
    }


    private fun setSizeAndSpeed(src: String?) {
        try {
            val newSrc = src?.replace("[", "")?.replace("]", "")
            val sizeSrc = newSrc?.split(" ")?.filter { item -> item != "" }?.get(0)
            val speedSrc = newSrc?.split(" ")?.filter { item -> item != "" }?.get(2)
                ?.replace(" ", "")?.replace("]", "")
            size.set(sizeSrc?.filter { item -> item.isDigit() || item == '.' })
            sizeUnit.set(sizeSrc?.filter { item -> item.isLetter() })
            speed.set(speedSrc?.filter { item -> item.isDigit() || item == '.' })
            speedUnit.set(speedSrc?.filter { item -> item.isLetter() || item == '/' })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializeSizeAndSpeed() {
        size.set("0")
        sizeUnit.set("Mib")
        speed.set("0")
        speedUnit.set("Mib/s")
    }

    fun onBackupClick(v: View) {
        if (isMedia) onBackupMediaClick(v)
        else onBackupAppClick(v)
    }

    private fun onBackupAppClick(v: View) {
        if (successNum + failedNum != mAppInfoList.size)
            CoroutineScope(Dispatchers.IO).launch {
                isProcessing.set(true)
                totalTip.set(GlobalString.backupProcessing)
                for ((index, i) in mAppInfoList.withIndex()) {
                    // 准备备份卡片数据
                    appName.set(i.infoBase.appName)
                    packageName.set(i.infoBase.packageName)
                    appVersion.set(i.infoBase.versionName)
                    appIcon.set(i.appIcon)
                    isBackupApk.set(i.infoBase.app)

                    val packageName = packageName.get()!!
                    val userId = App.globalContext.readBackupUser()
                    val compressionType = App.globalContext.readCompressionType()
                    val outPutPath =
                        "${App.globalContext.readBackupSavePath()}/backup/data/${packageName}"
                    val userPath = "${Path.getUserPath(userId)}/${packageName}"
                    val dataPath = "${Path.getDataPath(userId)}/${packageName}"
                    val obbPath = "${Path.getObbPath(userId)}/${packageName}"
                    if (i.infoBase.data) {
                        Command.ls(userPath).apply { isBackupUser.set(this) }
                        Command.ls(dataPath).apply { isBackupData.set(this) }
                        Command.ls(obbPath).apply { isBackupObb.set(this) }
                    }

                    // 开始备份
                    var state = true // 该任务是否成功完成
                    if (isBackupApk.get()) {
                        // 备份应用
                        processingApk.set(true)
                        Command.compressAPK(
                            compressionType, packageName, outPutPath, userId, ""
                        ) { setSizeAndSpeed(it) }.apply {
                            if (!this)
                                state = false
                        }
                        processingApk.set(false)
                        initializeSizeAndSpeed()
                    }
                    if (isBackupUser.get()) {
                        // 备份User
                        processingUser.set(true)
                        Command.compress(
                            compressionType,
                            "user",
                            packageName,
                            outPutPath,
                            Path.getUserPath(userId),
                            ""
                        ) { setSizeAndSpeed(it) }.apply {
                            if (!this)
                                state = false
                        }
                        processingUser.set(false)
                        initializeSizeAndSpeed()
                    }
                    if (isBackupData.get()) {
                        // 备份Data
                        processingData.set(true)
                        Command.compress(
                            compressionType,
                            "data",
                            packageName,
                            outPutPath,
                            Path.getUserPath(userId),
                            ""
                        ) { setSizeAndSpeed(it) }.apply {
                            if (!this)
                                state = false
                        }
                        processingData.set(false)
                        initializeSizeAndSpeed()
                    }
                    if (isBackupObb.get()) {
                        // 备份Obb
                        processingObb.set(true)
                        Command.compress(
                            compressionType,
                            "obb",
                            packageName,
                            outPutPath,
                            Path.getUserPath(userId),
                            ""
                        ) { setSizeAndSpeed(it) }.apply {
                            if (!this)
                                state = false
                        }
                        processingObb.set(false)
                        initializeSizeAndSpeed()
                    }
                    if (state)
                        successNum += 1
                    else
                        failedNum += 1
                    progress.set(index + 1)
                }
                totalTip.set(GlobalString.backupFinished)
                totalProgress.set("$successNum ${GlobalString.success}, $failedNum ${GlobalString.failed}, ${mAppInfoList.size} ${GlobalString.total}")
                isProcessing.set(false)
                backupBtnText.set(GlobalString.finish)
            }
    }

    private fun onBackupMediaClick(v: View) {
        if (successNum + failedNum != mMediaInfoList.size)
            CoroutineScope(Dispatchers.IO).launch {
                isProcessing.set(true)
                totalTip.set(GlobalString.backupProcessing)
                for ((index, i) in mMediaInfoList.withIndex()) {
                    // 准备备份卡片数据
                    appName.set(i.name)
                    packageName.set(i.path)
                    isBackupData.set(i.data)

                    // 开始备份
                    var state = true // 该任务是否成功完成
                    if (isBackupData.get()) {
                        // 备份Data
                        processingData.set(true)
                        // 备份目录
                        Command.compress(
                            App.globalContext.readCompressionType(),
                            "media",
                            "media",
                            Path.getBackupMediaSavePath(),
                            i.path,
                            i.size
                        ) {
                            setSizeAndSpeed(it)
                        }.apply {
                            if (!this)
                                state = false
                        }
                        processingData.set(false)
                        initializeSizeAndSpeed()
                    }
                    if (state)
                        successNum += 1
                    else
                        failedNum += 1
                    progress.set(index + 1)
                }
                totalTip.set(GlobalString.backupFinished)
                totalProgress.set("$successNum ${GlobalString.success}, $failedNum ${GlobalString.failed}, ${mMediaInfoList.size} ${GlobalString.total}")
                isProcessing.set(false)
                backupBtnText.set(GlobalString.finish)
            }
    }
}