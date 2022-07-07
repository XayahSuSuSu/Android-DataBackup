package com.xayah.databackup.activity.processing

import android.view.View
import com.xayah.databackup.App
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.data.BackupInfo
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Backup {
    lateinit var dataBinding: DataBinding
    var mAppInfoBackupList: MutableList<AppInfoBackup> = mutableListOf()
    var mAppInfoRestoreList: MutableList<AppInfoRestore> = mutableListOf()
    var mMediaInfoBackupList: MutableList<MediaInfo> = mutableListOf()
    var mMediaInfoRestoreList: MutableList<MediaInfo> = mutableListOf()
    var mBackupInfoList: MutableList<BackupInfo> = mutableListOf()
    var isMedia = false
    var successNum = 0
    var failedNum = 0

    fun initialize(mDataBinding: DataBinding, mIsMedia: Boolean) {
        dataBinding = mDataBinding.apply {
            onBackupClick = { v ->
                if (isMedia) onBackupMediaClick(v)
                else onBackupAppClick(v)
            }
            btnText.set(GlobalString.backup)
        }
        isMedia = mIsMedia

        mBackupInfoList = Command.getCachedBackupInfoList()
        if (isMedia) initializeMedia()
        else initializeApp()
    }

    private fun initializeApp() {
        mAppInfoBackupList = Command.getCachedAppInfoBackupList(App.globalContext, true)
        mAppInfoRestoreList = Command.getCachedAppInfoRestoreList()
        dataBinding.progressMax.set(mAppInfoBackupList.size)
        dataBinding.totalTip.set(GlobalString.ready)
        Command.getCachedAppInfoBackupListNum().apply {
            dataBinding.totalProgress.set("${GlobalString.selected} ${this.appNum} ${GlobalString.application}, ${this.dataNum} ${GlobalString.data}")
        }
    }

    private fun initializeMedia() {
        mMediaInfoBackupList = Command.getCachedMediaInfoBackupList(true)
        mMediaInfoRestoreList = Command.getCachedMediaInfoRestoreList()
        dataBinding.progressMax.set(mMediaInfoBackupList.size)
        dataBinding.totalTip.set(GlobalString.ready)
        dataBinding.totalProgress.set("${GlobalString.selected} ${mMediaInfoBackupList.size} ${GlobalString.data}")
    }

    private fun setSizeAndSpeed(src: String?) {
        try {
            val newSrc = src?.replace("[", "")?.replace("]", "")
            val sizeSrc = newSrc?.split(" ")?.filter { item -> item != "" }?.get(0)
            val speedSrc =
                newSrc?.split(" ")?.filter { item -> item != "" }?.get(2)?.replace(" ", "")
                    ?.replace("]", "")
            dataBinding.size.set(sizeSrc?.filter { item -> item.isDigit() || item == '.' })
            dataBinding.sizeUnit.set(sizeSrc?.filter { item -> item.isLetter() })
            dataBinding.speed.set(speedSrc?.filter { item -> item.isDigit() || item == '.' })
            dataBinding.speedUnit.set(speedSrc?.filter { item -> item.isLetter() || item == '/' })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializeSizeAndSpeed() {
        dataBinding.size.set("0")
        dataBinding.sizeUnit.set("Mib")
        dataBinding.speed.set("0")
        dataBinding.speedUnit.set("Mib/s")
    }

    private fun onBackupAppClick(v: View) {
        val startTime = Command.getDate()
        val startSize = Command.countSize(Path.getExternalStorageDataBackupDirectory())
        if (successNum + failedNum != mAppInfoBackupList.size) CoroutineScope(Dispatchers.IO).launch {
            dataBinding.isProcessing.set(true)
            dataBinding.totalTip.set(GlobalString.backupProcessing)
            for ((index, i) in mAppInfoBackupList.withIndex()) {
                // 准备备份卡片数据
                dataBinding.appName.set(i.infoBase.appName)
                dataBinding.packageName.set(i.infoBase.packageName)
                dataBinding.appVersion.set(i.infoBase.versionName)
                dataBinding.appIcon.set(i.appIcon)
                dataBinding.isBackupApk.set(i.infoBase.app)

                val packageName = dataBinding.packageName.get()!!
                val userId = App.globalContext.readBackupUser()
                val compressionType = App.globalContext.readCompressionType()
                val outPutPath = "${Path.getBackupDataSavePath()}/${packageName}"
                val userPath = "${Path.getUserPath()}/${packageName}"
                val dataPath = "${Path.getDataPath()}/${packageName}"
                val obbPath = "${Path.getObbPath()}/${packageName}"
                if (i.infoBase.data) {
                    Command.ls(userPath).apply { dataBinding.isBackupUser.set(this) }
                    Command.ls(dataPath).apply { dataBinding.isBackupData.set(this) }
                    Command.ls(obbPath).apply { dataBinding.isBackupObb.set(this) }
                }

                // 开始备份
                var state = true // 该任务是否成功完成
                if (dataBinding.isBackupApk.get()) {
                    // 备份应用
                    dataBinding.processingApk.set(true)
                    Command.compressAPK(
                        compressionType, packageName, outPutPath, userId, i.appSize
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                        // 保存apk大小
                        else i.appSize = Command.countSize(
                            Bashrc.getAPKPath(i.infoBase.packageName, userId).second, 1
                        )
                    }
                    dataBinding.processingApk.set(false)
                    initializeSizeAndSpeed()
                }
                if (dataBinding.isBackupUser.get()) {
                    // 备份User
                    dataBinding.processingUser.set(true)
                    Command.compress(
                        compressionType,
                        "user",
                        packageName,
                        outPutPath,
                        Path.getUserPath(),
                        i.userSize
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                        // 保存user大小
                        else i.userSize = Command.countSize(userPath, 1)
                    }
                    dataBinding.processingUser.set(false)
                    initializeSizeAndSpeed()
                }
                if (dataBinding.isBackupData.get()) {
                    // 备份Data
                    dataBinding.processingData.set(true)
                    Command.compress(
                        compressionType,
                        "data",
                        packageName,
                        outPutPath,
                        Path.getDataPath(),
                        i.dataSize
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                        // 保存data大小
                        else i.dataSize = Command.countSize(dataPath, 1)
                    }
                    dataBinding.processingData.set(false)
                    initializeSizeAndSpeed()
                }
                if (dataBinding.isBackupObb.get()) {
                    // 备份Obb
                    dataBinding.processingObb.set(true)
                    Command.compress(
                        compressionType,
                        "obb",
                        packageName,
                        outPutPath,
                        Path.getObbPath(),
                        i.obbSize
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                        // 保存obb大小
                        else i.obbSize = Command.countSize(obbPath, 1)
                    }
                    dataBinding.processingObb.set(false)
                    initializeSizeAndSpeed()
                }
                if (state) {
                    successNum += 1
                    addOrUpdate(
                        AppInfoRestore(null, i.infoBase),
                        mAppInfoRestoreList as MutableList<Any>
                    ) {
                        (it as AppInfoRestore).infoBase.packageName == i.infoBase.packageName
                    }
                } else failedNum += 1
                dataBinding.progress.set(index + 1)
            }
            val endTime = Command.getDate()
            val endSize = Command.countSize(Path.getExternalStorageDataBackupDirectory())
            mBackupInfoList.add(
                BackupInfo(
                    Command.getVersion(),
                    startTime,
                    endTime,
                    startSize,
                    endSize,
                    "app",
                    App.globalContext.readBackupUser()
                )
            )
            saveBackupInfoList() // 更新备份信息
            saveAppInfoBackupList() // 更新备份大小
            saveAppInfoRestoreList() //保存恢复信息
            dataBinding.totalTip.set(GlobalString.backupFinished)
            dataBinding.totalProgress.set("$successNum ${GlobalString.success}, $failedNum ${GlobalString.failed}, ${mAppInfoBackupList.size} ${GlobalString.total}")
            dataBinding.isProcessing.set(false)
            dataBinding.btnText.set(GlobalString.finish)
        }
    }

    private fun onBackupMediaClick(v: View) {
        val startTime = Command.getDate()
        val startSize = Command.countSize(Path.getExternalStorageDataBackupDirectory())
        if (successNum + failedNum != mMediaInfoBackupList.size) CoroutineScope(Dispatchers.IO).launch {
            dataBinding.isProcessing.set(true)
            dataBinding.totalTip.set(GlobalString.backupProcessing)
            for ((index, i) in mMediaInfoBackupList.withIndex()) {
                // 准备备份卡片数据
                dataBinding.appName.set(i.name)
                dataBinding.packageName.set(i.path)
                dataBinding.isBackupData.set(i.data)

                // 开始备份
                var state = true // 该任务是否成功完成
                if (dataBinding.isBackupData.get()) {
                    // 备份Data
                    dataBinding.processingData.set(true)
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
                    dataBinding.processingData.set(false)
                    initializeSizeAndSpeed()
                }
                if (state) {
                    successNum += 1
                    addOrUpdate(i, mMediaInfoRestoreList as MutableList<Any>) {
                        (it as MediaInfo).path == i.path
                    }
                } else failedNum += 1
                dataBinding.progress.set(index + 1)
            }
            val endTime = Command.getDate()
            val endSize = Command.countSize(Path.getExternalStorageDataBackupDirectory())
            mBackupInfoList.add(
                BackupInfo(
                    Command.getVersion(),
                    startTime,
                    endTime,
                    startSize,
                    endSize,
                    "media",
                    App.globalContext.readBackupUser()
                )
            )
            saveBackupInfoList() // 更新备份信息
            saveMediaInfoBackupList() // 更新备份大小
            saveMediaInfoRestoreList() // 保存备份信息
            dataBinding.totalTip.set(GlobalString.backupFinished)
            dataBinding.totalProgress.set("$successNum ${GlobalString.success}, $failedNum ${GlobalString.failed}, ${mMediaInfoBackupList.size} ${GlobalString.total}")
            dataBinding.isProcessing.set(false)
            dataBinding.btnText.set(GlobalString.finish)
        }
    }

    private fun saveAppInfoBackupList() {
        val appInfoBackupList = Command.getCachedAppInfoBackupList(App.globalContext, false)
        for (i in mAppInfoBackupList) {
            addOrUpdate(i, appInfoBackupList as MutableList<Any>) {
                (it as AppInfoBackup).infoBase.packageName == i.infoBase.packageName
            }
        }
        JSON.writeJSONToFile(
            JSON.entityArrayToJsonArray(appInfoBackupList as MutableList<Any>),
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
        val mediaInfoBackupList = Command.getCachedMediaInfoBackupList(false)
        for (i in mMediaInfoBackupList) {
            addOrUpdate(i, mediaInfoBackupList as MutableList<Any>) {
                (it as MediaInfo).path == i.path
            }
        }
        JSON.writeJSONToFile(
            JSON.entityArrayToJsonArray(mediaInfoBackupList as MutableList<Any>),
            Path.getMediaInfoBackupListPath()
        )
    }

    private fun saveMediaInfoRestoreList() {
        JSON.writeJSONToFile(
            JSON.entityArrayToJsonArray(mMediaInfoRestoreList as MutableList<Any>),
            Path.getMediaInfoRestoreListPath()
        )
    }

    private fun addOrUpdate(item: Any, dst: MutableList<Any>, callback: (item: Any) -> Boolean) {
        val tmp = dst.find { callback(it) }
        val tmpIndex = dst.indexOf(tmp)
        if (tmpIndex == -1)
            dst.add(item)
        else
            dst[tmpIndex] = item
    }

    private fun saveBackupInfoList() {
        JSON.writeJSONToFile(
            JSON.entityArrayToJsonArray(mBackupInfoList as MutableList<Any>),
            Path.getBackInfoListPath()
        )
    }
}