package com.xayah.databackup.activity.backup.processing.app

import android.graphics.drawable.Drawable
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel
import com.xayah.databackup.App
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupProcessingViewModel : ViewModel() {
    var mAppInfoBackupList: MutableList<AppInfoBackup> = mutableListOf()
    var mAppInfoRestoreList: MutableList<AppInfoRestore> = mutableListOf()
    var mMediaInfoBackupList: MutableList<MediaInfo> = mutableListOf()
    var mMediaInfoRestoreList: MutableList<MediaInfo> = mutableListOf()
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
        mAppInfoBackupList = Command.getCachedAppInfoBackupList(App.globalContext, true)
        mAppInfoRestoreList = Command.getCachedAppInfoRestoreList()
        progressMax.set(mAppInfoBackupList.size)
        totalTip.set(GlobalString.ready)
        Command.getCachedAppInfoBackupListNum().apply {
            totalProgress.set("${GlobalString.selected} ${this.appNum} ${GlobalString.application}, ${this.dataNum} ${GlobalString.data}")
        }
    }

    private fun initializeMedia() {
        mMediaInfoBackupList = Command.getCachedMediaInfoList(true)
        progressMax.set(mMediaInfoBackupList.size)
        totalTip.set(GlobalString.ready)
        totalProgress.set("${GlobalString.selected} ${mMediaInfoBackupList.size} ${GlobalString.data}")
    }


    private fun setSizeAndSpeed(src: String?) {
        try {
            val newSrc = src?.replace("[", "")?.replace("]", "")
            val sizeSrc = newSrc?.split(" ")?.filter { item -> item != "" }?.get(0)
            val speedSrc =
                newSrc?.split(" ")?.filter { item -> item != "" }?.get(2)?.replace(" ", "")
                    ?.replace("]", "")
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
        if (successNum + failedNum != mAppInfoBackupList.size) CoroutineScope(Dispatchers.IO).launch {
            isProcessing.set(true)
            totalTip.set(GlobalString.backupProcessing)
            for ((index, i) in mAppInfoBackupList.withIndex()) {
                // 准备备份卡片数据
                appName.set(i.infoBase.appName)
                packageName.set(i.infoBase.packageName)
                appVersion.set(i.infoBase.versionName)
                appIcon.set(i.appIcon)
                isBackupApk.set(i.infoBase.app)

                val packageName = packageName.get()!!
                val userId = App.globalContext.readBackupUser()
                val compressionType = App.globalContext.readCompressionType()
                val outPutPath = "${Path.getBackupDataSavePath()}/${packageName}"
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
                        compressionType, packageName, outPutPath, userId, i.appSize
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                        // 保存apk大小
                        else i.appSize = Command.countSize(
                            Bashrc.getAPKPath(i.infoBase.packageName, userId).second, 1
                        )
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
                        i.userSize
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                        // 保存user大小
                        else i.userSize = Command.countSize(userPath, 1)
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
                        Path.getDataPath(userId),
                        i.dataSize
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                        // 保存data大小
                        else i.dataSize = Command.countSize(dataPath, 1)
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
                        Path.getObbPath(userId),
                        i.obbSize
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                        // 保存obb大小
                        else i.obbSize = Command.countSize(obbPath, 1)
                    }
                    processingObb.set(false)
                    initializeSizeAndSpeed()
                }
                if (state) {
                    successNum += 1
                    mAppInfoRestoreList.add(AppInfoRestore(null, i.infoBase))
                } else failedNum += 1
                progress.set(index + 1)
            }
            saveAppInfoBackupList() // 更新备份大小
            saveAppInfoRestoreList() //保存备份信息
            totalTip.set(GlobalString.backupFinished)
            totalProgress.set("$successNum ${GlobalString.success}, $failedNum ${GlobalString.failed}, ${mAppInfoBackupList.size} ${GlobalString.total}")
            isProcessing.set(false)
            backupBtnText.set(GlobalString.finish)
        }
    }

    private fun onBackupMediaClick(v: View) {
        if (successNum + failedNum != mMediaInfoBackupList.size) CoroutineScope(Dispatchers.IO).launch {
            isProcessing.set(true)
            totalTip.set(GlobalString.backupProcessing)
            for ((index, i) in mMediaInfoBackupList.withIndex()) {
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
                        if (!this) state = false
                        // 保存大小
                        else i.size = Command.countSize(
                            i.path, 1
                        )
                    }
                    processingData.set(false)
                    initializeSizeAndSpeed()
                }
                if (state) {
                    successNum += 1
                    mMediaInfoRestoreList.add(i)
                } else failedNum += 1
                progress.set(index + 1)
            }
            saveMediaInfoBackupList() // 更新备份大小
            saveMediaInfoRestoreList() // 保存备份信息
            totalTip.set(GlobalString.backupFinished)
            totalProgress.set("$successNum ${GlobalString.success}, $failedNum ${GlobalString.failed}, ${mMediaInfoBackupList.size} ${GlobalString.total}")
            isProcessing.set(false)
            backupBtnText.set(GlobalString.finish)
        }
    }

    private fun saveAppInfoBackupList() {
        JSON.writeJSONToFile(
            JSON.entityArrayToJsonArray(mAppInfoBackupList as MutableList<Any>),
            Path.getAppInfoBackupListPath()
        )
    }

    private fun saveAppInfoRestoreList() {
        JSON.writeJSONToFile(
            JSON.entityArrayToJsonArray(mAppInfoRestoreList as MutableList<Any>),
            Path.getAppInfoRestoreListPath()
        )
    }

    private fun saveMediaInfoBackupList() {
        JSON.writeJSONToFile(
            JSON.entityArrayToJsonArray(mMediaInfoBackupList as MutableList<Any>),
            Path.getMediaInfoBackupListPath()
        )
    }

    private fun saveMediaInfoRestoreList() {
        JSON.writeJSONToFile(
            JSON.entityArrayToJsonArray(mMediaInfoRestoreList as MutableList<Any>),
            Path.getMediaInfoRestoreListPath()
        )
    }
}