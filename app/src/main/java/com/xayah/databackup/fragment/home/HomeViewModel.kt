package com.xayah.databackup.fragment.home

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.xayah.databackup.App
import com.xayah.databackup.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {
    val root = "${GlobalString.symbolDot} Root"
    val bin = "${GlobalString.symbolDot} Bin"
    val bashrc = "${GlobalString.symbolDot} Bashrc"
    var rootCheck = ObservableField(GlobalString.symbolCross)
    var binCheck = ObservableField(GlobalString.symbolCross)
    var bashrcCheck = ObservableField(GlobalString.symbolCross)
    var architectureCurrent = ObservableField("")
    var architectureSupport = ObservableField("${GlobalString.support}: arm64-v8a, x86_64")
    var versionCurrent = ObservableField(App.versionName)
    var versionLatest = ObservableField("")
    var downloadBtnVisible = ObservableBoolean(false)
    private var downloadLink = ObservableField("")
    var logText = ObservableField("")
    var dynamicColorsEnable = ObservableBoolean(false)

    private fun checkRoot(): String {
        Command.mkdir(Path.getExternalStorageDataBackupDirectory()).apply {
            if (!this) {
                Toast.makeText(
                    App.globalContext, GlobalString.backupDirCreateFailed, Toast.LENGTH_SHORT
                ).show()
            }
        }
        return if (Command.checkRoot()) GlobalString.symbolTick else GlobalString.symbolCross
    }

    private fun checkBin(): String {
        return if (Command.checkBin()) GlobalString.symbolTick else GlobalString.symbolCross
    }

    private fun checkBashrc(): String {
        return if (Command.checkBashrc()) GlobalString.symbolTick else GlobalString.symbolCross
    }

    private fun setArchitecture() {
        architectureCurrent.set(Command.getABI())
    }

    private fun setUpdate() {
        // 设置默认值
        versionLatest.set(GlobalString.fetching)
        downloadBtnVisible.set(false)
        CoroutineScope(Dispatchers.IO).launch {
            Server.releases({ releaseList ->
                val mReleaseList = releaseList.filter { !it.name.contains("Check") }
                if (mReleaseList.isEmpty()) {
                    versionLatest.set(GlobalString.fetchFailed)
                } else {
                    versionLatest.set("${GlobalString.latest}: ${mReleaseList[0].name}")
                    if (!versionLatest.get()!!.contains(versionCurrent.get()!!)) {
                        downloadBtnVisible.set(true)
                        downloadLink.set(mReleaseList[0].html_url)
                    }
                }
            }, { versionLatest.set(GlobalString.fetchFailed) })
        }
    }

    fun refresh(v: View? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            rootCheck.set(checkRoot())
            binCheck.set(checkBin())
            bashrcCheck.set(checkBashrc())
            setArchitecture()
            setUpdate()
            updateLogCard()
        }
    }

    fun initialize() {
        refresh()
        updateLogCard()
        setDynamicColorsCard()
    }

    private fun updateLogCard() {
        val logPath = Path.getShellLogPath()
        logText.set("${Command.countFile(logPath)} ${GlobalString.log}, ${Command.countSize(logPath)} ${GlobalString.size}\n${GlobalString.storedIn} $logPath")
    }

    fun onLogClearClick(v: View) {
        CoroutineScope(Dispatchers.IO).launch {
            Command.rm(Path.getShellLogPath())
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    v.context, GlobalString.success, Toast.LENGTH_SHORT
                ).show()
                refresh()
            }
        }
    }

    fun onLogSaveClick(v: View) {
        CoroutineScope(Dispatchers.IO).launch {
            // 保存日志
            Bashrc.writeToFile(
                App.logcat.toString(), "${Path.getShellLogPath()}/${App.openDate.replace(" ", "_")}"
            )
            App.logcat.clear()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    v.context, GlobalString.success, Toast.LENGTH_SHORT
                ).show()
                refresh()
            }
        }
    }

    private fun setDynamicColorsCard() {
        dynamicColorsEnable.set(App.globalContext.readIsDynamicColors())
    }

    fun onDynamicColorsEnableCheckedChanged(v: View, checked: Boolean) {
        dynamicColorsEnable.set(checked)
        App.globalContext.saveIsDynamicColors(dynamicColorsEnable.get())
    }

    fun onDownloadBtnClick(v: View) {
        val url = downloadLink.get() ?: ""
        if (url.isNotEmpty()) {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            v.context.startActivity(intent)
        }
    }
}