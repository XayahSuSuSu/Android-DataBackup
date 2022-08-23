package com.xayah.databackup.activity.processing

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.data.AppInfoBaseNum
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class Restore(private val viewModel: ProcessingViewModel) {
    private lateinit var dataBinding: DataBinding

    // 应用恢复列表
    private val _appInfoRestoreList by lazy {
        MutableLiveData(mutableListOf<AppInfoRestore>())
    }
    private var appInfoRestoreList
        get() = _appInfoRestoreList.value!!
        set(value) = _appInfoRestoreList.postValue(value)
    private val appInfoRestoreListNum
        get() = run {
            val appInfoBaseNum = AppInfoBaseNum(0, 0)
            for (i in appInfoRestoreList) {
                if (i.infoBase.app) appInfoBaseNum.appNum++
                if (i.infoBase.data) appInfoBaseNum.dataNum++
            }
            appInfoBaseNum
        }
    private val appInfoRestoreListTotalNum
        get() = run {
            max(appInfoRestoreListNum.appNum, appInfoRestoreListNum.dataNum)
        }

    // 媒体恢复列表
    private val _mediaInfoRestoreList by lazy {
        MutableLiveData(mutableListOf<MediaInfo>())
    }
    private var mediaInfoRestoreList
        get() = _mediaInfoRestoreList.value!!
        set(value) = _mediaInfoRestoreList.postValue(value)
    private val mediaInfoRestoreListNum
        get() = run {
            var num = 0
            for (i in mediaInfoRestoreList) {
                if (i.data) num++
            }
            num
        }

    private var successNum = 0
    private var failedNum = 0

    init {
        viewModel.viewModelScope.launch {
            // 加载列表
            loadAllList()
            dataBinding = viewModel.dataBinding.apply {
                onRestoreClick = { v ->
                    if (viewModel.isMedia) onRestoreMediaClick(v)
                    else onRestoreAppClick(v)
                }
                btnText.set(GlobalString.restore)
            }
            if (viewModel.isMedia) initializeMedia()
            else initializeApp()
        }
    }

    private fun initializeApp() {
        dataBinding.progressMax.set(appInfoRestoreListTotalNum)
        if (appInfoRestoreListTotalNum == 0) {
            dataBinding.btnText.set(GlobalString.finish)
        }
        dataBinding.totalTip.set(GlobalString.ready)
        appInfoRestoreListNum.apply {
            dataBinding.totalProgress.set("${GlobalString.selected} ${this.appNum} ${GlobalString.application}, ${this.dataNum} ${GlobalString.data}")
        }
    }

    private fun initializeMedia() {
        dataBinding.progressMax.set(mediaInfoRestoreListNum)
        if (mediaInfoRestoreListNum == 0) {
            dataBinding.btnText.set(GlobalString.finish)
        }
        dataBinding.totalTip.set(GlobalString.ready)
        mediaInfoRestoreListNum.apply {
            dataBinding.totalProgress.set("${GlobalString.selected} $this ${GlobalString.data}")
        }

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

    private fun onRestoreAppClick(v: View) {
        if (successNum + failedNum != appInfoRestoreListTotalNum) CoroutineScope(Dispatchers.IO).launch {
            dataBinding.isProcessing.set(true)
            dataBinding.totalTip.set(GlobalString.restoreProcessing)
            for ((index, i) in appInfoRestoreList.withIndex()) {
                if (!i.infoBase.app && !i.infoBase.data)
                    continue

                // 准备备份卡片数据
                dataBinding.appName.set(i.infoBase.appName)
                dataBinding.packageName.set(i.infoBase.packageName)
                dataBinding.appVersion.set(i.infoBase.versionName)
                dataBinding.appIcon.set(i.appIcon)
                dataBinding.isBackupApk.set(i.infoBase.app)

                val packageName = dataBinding.packageName.get()!!
                val userId = App.globalContext.readRestoreUser()
                val inPath = "${Path.getBackupDataSavePath()}/${packageName}"
                val userPath = "${Path.getBackupDataSavePath()}/${packageName}/user.tar*"
                val dataPath = "${Path.getBackupDataSavePath()}/${packageName}/data.tar*"
                val obbPath = "${Path.getBackupDataSavePath()}/${packageName}/obb.tar*"
                if (i.infoBase.data) {
                    Command.ls(userPath).apply { dataBinding.isBackupUser.set(this) }
                    Command.ls(dataPath).apply { dataBinding.isBackupData.set(this) }
                    Command.ls(obbPath).apply { dataBinding.isBackupObb.set(this) }
                } else {
                    dataBinding.isBackupUser.set(false)
                    dataBinding.isBackupData.set(false)
                    dataBinding.isBackupObb.set(false)
                }

                // 开始恢复
                var state = true // 该任务是否成功完成
                if (dataBinding.isBackupApk.get()) {
                    // 恢复应用
                    dataBinding.processingApk.set(true)
                    Command.installAPK(
                        inPath, packageName, userId, i.infoBase.versionCode.toString()
                    ) { setSizeAndSpeed(it) }.apply {
                        state = this
                    }
                    dataBinding.processingApk.set(false)
                    if (!state) {
                        failedNum += 1
                        continue
                    }
                    initializeSizeAndSpeed()
                }

                if (dataBinding.isBackupUser.get()) {
                    // 恢复User
                    dataBinding.processingUser.set(true)
                    val inputPath = "${inPath}/user.tar*"
                    Command.decompress(
                        Command.getCompressionTypeByPath(inputPath),
                        "user",
                        inputPath,
                        packageName,
                        Path.getUserPath(userId)
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                    }
                    Command.setOwnerAndSELinux(
                        "user", packageName, "${Path.getUserPath(userId)}/${packageName}", userId
                    )
                    dataBinding.processingUser.set(false)
                    initializeSizeAndSpeed()
                }
                if (dataBinding.isBackupData.get()) {
                    // 恢复Data
                    dataBinding.processingData.set(true)
                    val inputPath = "${inPath}/data.tar*"
                    Command.decompress(
                        Command.getCompressionTypeByPath(inputPath),
                        "data",
                        inputPath,
                        packageName,
                        Path.getDataPath(userId)
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                    }
                    Command.setOwnerAndSELinux(
                        "data", packageName, "${Path.getDataPath(userId)}/${packageName}", userId
                    )
                    dataBinding.processingData.set(false)
                    initializeSizeAndSpeed()
                }
                if (dataBinding.isBackupObb.get()) {
                    // 恢复Obb
                    dataBinding.processingObb.set(true)
                    val inputPath = "${inPath}/obb.tar*"
                    Command.decompress(
                        Command.getCompressionTypeByPath(inputPath),
                        "obb",
                        inputPath,
                        packageName,
                        Path.getObbPath(userId)
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                    }
                    Command.setOwnerAndSELinux(
                        "obb", packageName, "${Path.getObbPath(userId)}/${packageName}", userId
                    )
                    dataBinding.processingObb.set(false)
                    initializeSizeAndSpeed()
                }
                if (state) successNum += 1
                else failedNum += 1
                dataBinding.progress.set(index + 1)
            }
            dataBinding.totalTip.set(GlobalString.restoreFinished)
            dataBinding.totalProgress.set("$successNum ${GlobalString.success}, $failedNum ${GlobalString.failed}, $appInfoRestoreListTotalNum ${GlobalString.total}")
            dataBinding.isProcessing.set(false)
            dataBinding.btnText.set(GlobalString.finish)
        }
        else {
            v.context.getActivity()?.finish()
        }
    }

    private fun onRestoreMediaClick(v: View) {
        if (successNum + failedNum != mediaInfoRestoreListNum) CoroutineScope(Dispatchers.IO).launch {
            dataBinding.isProcessing.set(true)
            dataBinding.totalTip.set(GlobalString.restoreProcessing)
            for ((index, i) in mediaInfoRestoreList.withIndex()) {
                if (!i.data)
                    continue

                // 准备备份卡片数据
                dataBinding.appName.set(i.name)
                dataBinding.packageName.set(i.path)
                dataBinding.isBackupData.set(i.data)

                val inPath = Path.getBackupMediaSavePath()

                // 开始恢复
                var state = true // 该任务是否成功完成
                if (dataBinding.isBackupData.get()) {
                    // 恢复Data
                    dataBinding.processingData.set(true)
                    // 恢复目录
                    val inputPath = "${inPath}/${i.name}.tar*"
                    Command.decompress(
                        Command.getCompressionTypeByPath(inputPath),
                        "media",
                        inputPath,
                        i.name,
                        i.path.replace("/${i.name}", "")
                    ) { setSizeAndSpeed(it) }.apply {
                        if (!this) state = false
                    }
                    dataBinding.processingData.set(false)
                    initializeSizeAndSpeed()
                }
                if (state) successNum += 1
                else failedNum += 1
                dataBinding.progress.set(index + 1)
            }
            dataBinding.totalTip.set(GlobalString.restoreFinished)
            dataBinding.totalProgress.set("$successNum ${GlobalString.success}, $failedNum ${GlobalString.failed}, $mediaInfoRestoreListNum ${GlobalString.total}")
            dataBinding.isProcessing.set(false)
            dataBinding.btnText.set(GlobalString.finish)
        }
        else {
            v.context.getActivity()?.finish()
        }
    }

    private suspend fun loadAllList() {
        withContext(Dispatchers.IO) {
            appInfoRestoreList = Loader.loadAppInfoRestoreList()
            mediaInfoRestoreList = Loader.loadMediaInfoRestoreList()
        }
    }
}