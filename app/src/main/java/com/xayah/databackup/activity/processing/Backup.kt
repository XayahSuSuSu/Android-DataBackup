package com.xayah.databackup.activity.processing

import android.graphics.Bitmap
import android.util.Base64
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.adapter.ProcessingTaskAdapter
import com.xayah.databackup.data.*
import com.xayah.databackup.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class Backup(private val viewModel: ProcessingViewModel) {
    private lateinit var dataBinding: DataBinding

    // 应用备份列表
    private val appInfoBackupList
        get() = App.appInfoBackupList.value.filter { it.infoBase.app || it.infoBase.data }
            .toMutableList()
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
    private val appInfoRestoreList
        get() = App.appInfoRestoreList.value.filter { it.infoBase.app || it.infoBase.data }
            .toMutableList()

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
                btnDesc.set(GlobalString.clickTheRightBtnToStart)
            }
            if (viewModel.isMedia) initializeMedia()
            else initializeApp()
            viewModel.dataBinding.isReady.set(true)
            viewModel.dataBinding.isFinished.postValue(false)
        }
    }

    private fun initializeApp() {
        viewModel.mAdapter.apply {
            val adapterList = mutableListOf<Any>()
            for (i in appInfoBackupList) adapterList.add(i.infoBase)
            register(ProcessingTaskAdapter())
            items = adapterList
            notifyDataSetChanged()
        }
        dataBinding.progressMax.set(appInfoBackupList.size)
        dataBinding.progressText.set("${GlobalString.progress}: ${dataBinding.progress.get()}/${dataBinding.progressMax.get()}")
        dataBinding.totalTip.set(GlobalString.ready)
        appInfoBackupListNum.apply {
            dataBinding.totalProgress.set("${GlobalString.selected} ${this.appNum} ${GlobalString.application}, ${this.dataNum} ${GlobalString.data}, ${App.globalContext.readBackupUser()} ${GlobalString.backupUser}")
        }
    }

    private fun initializeMedia() {
        viewModel.mAdapter.apply {
            val adapterList = mutableListOf<Any>()
            for (i in mediaInfoBackupList) adapterList.add(
                AppInfoBase(
                    i.name, i.path, "", -1, app = false, data = true, null, ""
                )
            )
            register(ProcessingTaskAdapter())
            items = adapterList
            notifyDataSetChanged()
        }
        dataBinding.progressMax.set(mediaInfoBackupList.size)
        dataBinding.progressText.set("${GlobalString.progress}: ${dataBinding.progress.get()}/${dataBinding.progressMax.get()}")
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
            if (!dataBinding.isFinished.value!!) CoroutineScope(Dispatchers.IO).launch {
                dataBinding.isProcessing.set(true)
                dataBinding.totalTip.set(GlobalString.backupProcessing)

                // 获取默认输入法和无障碍
                val keyboard = Bashrc.getKeyboard()
                val services = Bashrc.getAccessibilityServices()

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
                    dataBinding.appIcon.set(i.infoBase.appIcon)
                    dataBinding.isBackupApk.set(i.infoBase.app)

                    if (App.globalContext.readIsBackupIcon()) {
                        // 保存应用图标
                        i.infoBase.appIcon?.apply {
                            try {
                                val stream = ByteArrayOutputStream()
                                toBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
                                i.infoBase.appIconString =
                                    Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    val packageName = dataBinding.packageName.get()!!
                    val userId = App.globalContext.readBackupUser()
                    val compressionType = App.globalContext.readCompressionType()
                    val outPutPath = "${Path.getBackupDataSavePath()}/${packageName}"
                    val userPath = "${Path.getUserPath()}/${packageName}"
                    val userDePath = "${Path.getUserDePath()}/${packageName}"
                    val dataPath = "${Path.getDataPath()}/${packageName}"
                    val obbPath = "${Path.getObbPath()}/${packageName}"
                    if (i.infoBase.data) {
                        Command.ls(userPath).apply { dataBinding.isBackupUser.set(this) }
                        Command.ls(userDePath).apply { dataBinding.isBackupUserDe.set(this) }
                        Command.ls(dataPath).apply { dataBinding.isBackupData.set(this) }
                        Command.ls(obbPath).apply { dataBinding.isBackupObb.set(this) }
                    } else {
                        dataBinding.isBackupUser.set(false)
                        dataBinding.isBackupUserDe.set(false)
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
                    if (dataBinding.isBackupUserDe.get()) {
                        // 备份User_de
                        dataBinding.processingUserDe.set(true)
                        Command.compress(
                            compressionType,
                            "user_de",
                            packageName,
                            outPutPath,
                            Path.getUserDePath(),
                            i.userSize
                        ) { setSizeAndSpeed(it) }.apply {
                            if (!this) state = false
                            // 保存user_de大小
                            else i.userSize = Command.countSize(userDePath, 1)
                            // 检测是否生成压缩包
                            Command.ls("${outPutPath}/user_de.tar*").apply {
                                if (!this) state = false
                            }
                        }
                        dataBinding.processingUserDe.set(false)
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
                            AppInfoRestore(i.infoBase),
                            appInfoRestoreList as MutableList<Any>
                        ) {
                            (it as AppInfoRestore).infoBase.packageName == i.infoBase.packageName
                        }
                        viewModel.successList.add(i.infoBase)
                    } else {
                        failedNum += 1
                        viewModel.failedList.add(i.infoBase)
                    }
                    dataBinding.progress.set(index + 1)
                    dataBinding.progressText.set("${GlobalString.progress}: ${dataBinding.progress.get()}/${dataBinding.progressMax.get()}")
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
                dataBinding.totalProgress.set("${successNum + failedNum} ${GlobalString.total}")
                dataBinding.isProcessing.set(false)
                dataBinding.isFinished.postValue(true)
                dataBinding.btnText.set(GlobalString.finish)
                dataBinding.btnDesc.set(GlobalString.clickTheRightBtnToFinish)

                // 恢复默认输入法和无障碍
                keyboard.apply {
                    if (this.first) Bashrc.setKeyboard(this.second)
                }
                services.apply {
                    if (this.first) Bashrc.setAccessibilityServices(this.second)
                }

                Bashrc.moveLogToOut()
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

            if (!dataBinding.isFinished.value!!) CoroutineScope(Dispatchers.IO).launch {
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
                        viewModel.successList.add(viewModel.mAdapter.items[index] as AppInfoBase)
                    } else {
                        failedNum += 1
                        viewModel.failedList.add(viewModel.mAdapter.items[index] as AppInfoBase)
                    }
                    dataBinding.progress.set(index + 1)
                    dataBinding.progressText.set("${GlobalString.progress}: ${dataBinding.progress.get()}/${dataBinding.progressMax.get()}")
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
                dataBinding.totalProgress.set("${successNum + failedNum} ${GlobalString.total}")
                dataBinding.isProcessing.set(false)
                dataBinding.isFinished.postValue(true)
                dataBinding.btnText.set(GlobalString.finish)
                dataBinding.btnDesc.set(GlobalString.clickTheRightBtnToFinish)
                Bashrc.moveLogToOut()
            }
            else {
                v.context.getActivity()?.finish()
            }
        }
    }

    private suspend fun loadAllList() {
        backupInfoList = Loader.loadBackupInfoList()
        mediaInfoBackupList = Loader.loadMediaInfoBackupList()
        mediaInfoRestoreList = Loader.loadMediaInfoRestoreList()
    }

    private suspend fun saveRestoreList() {
        if (!viewModel.isRestore) {
            JSON.saveBackupInfoList(backupInfoList)
            if (!viewModel.isMedia) {
                JSON.saveAppInfoBackupList(App.appInfoBackupList.value)
                JSON.saveAppInfoRestoreList(App.appInfoRestoreList.value)
            } else {
                JSON.saveMediaInfoBackupList(_mediaInfoBackupList.value!!)
                JSON.saveMediaInfoRestoreList(mediaInfoRestoreList)
            }
        }
    }
}