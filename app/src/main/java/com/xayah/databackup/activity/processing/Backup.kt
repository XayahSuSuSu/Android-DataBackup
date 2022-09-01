package com.xayah.databackup.activity.processing

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.data.*
import com.xayah.databackup.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class Backup(private val viewModel: ProcessingViewModel) {
    private lateinit var dataBinding: DataBinding

    // 应用备份列表
    private val _appInfoBackupList by lazy {
        MutableLiveData(mutableListOf<AppInfoBackup>())
    }
    private var appInfoBackupList
        get() = _appInfoBackupList.value!!.filter { it.infoBase.app || it.infoBase.data }
            .toMutableList()
        set(value) = _appInfoBackupList.postValue(value)
    private val appInfoBackupListNum
        get() = run {
            val appInfoBaseNum = AppInfoBaseNum(0, 0)
            for (i in appInfoBackupList) {
                if (i.infoBase.app) appInfoBaseNum.appNum++
                if (i.infoBase.data) appInfoBaseNum.dataNum++
            }
            appInfoBaseNum
        }

    // 应用恢复列表
    private val _appInfoRestoreList by lazy {
        MutableLiveData(mutableListOf<AppInfoRestore>())
    }
    private var appInfoRestoreList
        get() = _appInfoRestoreList.value!!
        set(value) = _appInfoRestoreList.postValue(value)

    // 媒体备份列表
    private val _mediaInfoBackupList by lazy {
        MutableLiveData(mutableListOf<MediaInfo>())
    }
    private var mediaInfoBackupList
        get() = _mediaInfoBackupList.value!!.filter { it.data }.toMutableList()
        set(value) = _mediaInfoBackupList.postValue(value)

    // 媒体恢复列表
    private val _mediaInfoRestoreList by lazy {
        MutableLiveData(mutableListOf<MediaInfo>())
    }
    private var mediaInfoRestoreList
        get() = _mediaInfoRestoreList.value!!
        set(value) = _mediaInfoRestoreList.postValue(value)

    // 备份历史列表
    private val _backupInfoList by lazy {
        MutableLiveData(mutableListOf<BackupInfo>())
    }
    private var backupInfoList
        get() = _backupInfoList.value!!
        set(value) = _backupInfoList.postValue(value)


    private var successNum = 0
    private var failedNum = 0

    init {
        viewModel.viewModelScope.launch {
            // 加载列表
            loadAllList()
            dataBinding = viewModel.dataBinding.apply {
                onBackupClick = { v ->
                    if (viewModel.isMedia) onBackupMediaClick(v)
                    else onBackupAppClick(v)
                }
                btnText.set(GlobalString.backup)
            }
            if (viewModel.isMedia) initializeMedia()
            else initializeApp()
            viewModel.dataBinding.isReady.set(true)
            viewModel.dataBinding.isFinished.set(false)
        }
    }

    private fun initializeApp() {
        dataBinding.progressMax.set(appInfoBackupList.size)
        dataBinding.totalTip.set(GlobalString.ready)
        appInfoBackupListNum.apply {
            dataBinding.totalProgress.set("${GlobalString.selected} ${this.appNum} ${GlobalString.application}, ${this.dataNum} ${GlobalString.data}")
        }
    }

    private fun initializeMedia() {
        dataBinding.progressMax.set(mediaInfoBackupList.size)
        dataBinding.totalTip.set(GlobalString.ready)
        dataBinding.totalProgress.set("${GlobalString.selected} ${mediaInfoBackupList.size} ${GlobalString.data}")
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
        viewModel.viewModelScope.launch {
            val startTime = Command.getDate()
            val startSize = Command.countSize(App.globalContext.readBackupSavePath())
            if (!dataBinding.isFinished.get()) CoroutineScope(Dispatchers.IO).launch {
                dataBinding.isProcessing.set(true)
                dataBinding.totalTip.set(GlobalString.backupProcessing)
                // 备份自身
                if (App.globalContext.readIsBackupItself())
                    Command.backupItself(
                        "com.xayah.databackup",
                        App.globalContext.readBackupSavePath(),
                        App.globalContext.readBackupUser()
                    )
                for ((index, i) in appInfoBackupList.withIndex()) {
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
                    } else {
                        dataBinding.isBackupUser.set(false)
                        dataBinding.isBackupData.set(false)
                        dataBinding.isBackupObb.set(false)
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
                            // 检测是否生成压缩包
                            Command.ls("${outPutPath}/apk.tar*").apply {
                                // 后续若直接令state = this会导致state非正常更新
                                if (!this) state = false
                            }
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
                            // 检测是否生成压缩包
                            Command.ls("${outPutPath}/user.tar*").apply {
                                if (!this) state = false
                            }
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
                            // 检测是否生成压缩包
                            Command.ls("${outPutPath}/data.tar*").apply {
                                if (!this) state = false
                            }
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
                            // 检测是否生成压缩包
                            Command.ls("${outPutPath}/obb.tar*").apply {
                                if (!this) state = false
                            }
                        }
                        dataBinding.processingObb.set(false)
                        initializeSizeAndSpeed()
                    }
                    if (state) {
                        successNum += 1
                        Command.addOrUpdateList(
                            AppInfoRestore(null, i.infoBase),
                            appInfoRestoreList as MutableList<Any>
                        ) {
                            (it as AppInfoRestore).infoBase.packageName == i.infoBase.packageName
                        }
                    } else failedNum += 1
                    dataBinding.progress.set(index + 1)
                }
                val endTime = Command.getDate()
                val endSize = Command.countSize(App.globalContext.readBackupSavePath())
                backupInfoList.add(
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
                saveRestoreList()
                dataBinding.totalTip.set(GlobalString.backupFinished)
                dataBinding.totalProgress.set("$successNum ${GlobalString.success}, $failedNum ${GlobalString.failed}, ${appInfoBackupList.size} ${GlobalString.total}")
                dataBinding.isProcessing.set(false)
                dataBinding.isFinished.set(true)
                dataBinding.btnText.set(GlobalString.finish)
                Bashrc.writeToFile(
                    App.logcat.toString(),
                    "${Path.getShellLogPath()}/backup_app_log_${LocalDateTime.now()}"
                )
            }
            else {
                v.context.getActivity()?.finish()
            }
        }
    }

    private fun onBackupMediaClick(v: View) {
        viewModel.viewModelScope.launch {
            val startTime = Command.getDate()
            val startSize = Command.countSize(App.globalContext.readBackupSavePath())
            val outPutPath = Path.getBackupMediaSavePath()

            if (!dataBinding.isFinished.get()) CoroutineScope(Dispatchers.IO).launch {
                dataBinding.isProcessing.set(true)
                dataBinding.totalTip.set(GlobalString.backupProcessing)
                for ((index, i) in mediaInfoBackupList.withIndex()) {
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
                            "tar",
                            "media",
                            i.name,
                            outPutPath,
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
                            // 检测是否生成压缩包
                            Command.ls("${outPutPath}/${i.name}.tar*").apply {
                                if (!this) state = false
                            }
                        }
                        dataBinding.processingData.set(false)
                        initializeSizeAndSpeed()
                    }
                    if (state) {
                        successNum += 1
                        Command.addOrUpdateList(i, mediaInfoRestoreList as MutableList<Any>) {
                            (it as MediaInfo).path == i.path
                        }
                    } else failedNum += 1
                    dataBinding.progress.set(index + 1)
                }
                val endTime = Command.getDate()
                val endSize = Command.countSize(App.globalContext.readBackupSavePath())
                backupInfoList.add(
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
                saveRestoreList()
                dataBinding.totalTip.set(GlobalString.backupFinished)
                dataBinding.totalProgress.set("$successNum ${GlobalString.success}, $failedNum ${GlobalString.failed}, ${mediaInfoBackupList.size} ${GlobalString.total}")
                dataBinding.isProcessing.set(false)
                dataBinding.isFinished.set(true)
                dataBinding.btnText.set(GlobalString.finish)
                Bashrc.writeToFile(
                    App.logcat.toString(),
                    "${Path.getShellLogPath()}/backup_media_log_${LocalDateTime.now()}"
                )
            }
            else {
                v.context.getActivity()?.finish()
            }
        }
    }

    private suspend fun loadAllList() {
        backupInfoList = Loader.loadBackupInfoList()
        appInfoBackupList = Loader.loadAppInfoBackupList()
        appInfoRestoreList = Loader.loadAppInfoRestoreList()
        mediaInfoBackupList = Loader.loadMediaInfoBackupList()
        mediaInfoRestoreList = Loader.loadMediaInfoRestoreList()
    }

    private suspend fun saveRestoreList() {
        if (!viewModel.isRestore) {
            JSON.saveBackupInfoList(backupInfoList)
            if (!viewModel.isMedia) {
                JSON.saveAppInfoRestoreList(appInfoRestoreList)
            } else {
                JSON.saveMediaInfoRestoreList(mediaInfoRestoreList)
            }
        }
    }
}