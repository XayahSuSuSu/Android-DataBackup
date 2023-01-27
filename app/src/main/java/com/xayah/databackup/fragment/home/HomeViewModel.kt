package com.xayah.databackup.fragment.home

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.util.*
import com.xayah.databackup.view.fastInitialize
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    val root = "${GlobalString.symbolDot} Root"
    val bin = "${GlobalString.symbolDot} Bin"
    val bashrc = "${GlobalString.symbolDot} Bashrc"
    var rootCheck = ObservableField(GlobalString.symbolCross)
    var binCheck = ObservableField(GlobalString.symbolCross)
    var bashrcCheck = ObservableField(GlobalString.symbolCross)
    var architectureCurrent = ObservableField("")
    var architectureSupport = ObservableField(
        "${GlobalString.support}: ${
            App.globalContext.resources.assets.list("bin")!!.toList().joinToString(separator = ", ")
        }"
    )
    var versionCurrent = ObservableField(App.versionName)
    var versionLatest = ObservableField("")
    var downloadBtnVisible = ObservableBoolean(false)
    private var downloadLink = ObservableField("")
    var logText = ObservableField("")
    var dynamicColorsEnable = ObservableBoolean(false)
    var compressionType = ObservableField(App.globalContext.readCompressionType())

    var onResume = {}

    init {
        onResume = {
            refresh()
        }
    }

    fun onChangeCompressionType(v: View) {
        viewModelScope.launch {
            val context = v.context
            val items = arrayOf("tar", "zstd", "lz4")
            val choice = items.indexOf(compressionType.get())

            ListPopupWindow(context).apply {
                fastInitialize(v, items, choice)
                setOnItemClickListener { _, _, position, _ ->
                    dismiss()
                    context.saveCompressionType(items[position])
                    compressionType.set(items[position])
                }
                show()
            }
        }
    }

    private suspend fun checkRoot(): String {
        return if (Command.checkRoot()) GlobalString.symbolTick else GlobalString.symbolCross
    }

    private suspend fun checkBin(): String {
        return if (Command.checkBin()) GlobalString.symbolTick else GlobalString.symbolCross
    }

    private suspend fun checkBashrc(): String {
        return if (Command.checkBashrc()) GlobalString.symbolTick else GlobalString.symbolCross
    }

    private fun setArchitecture() {
        architectureCurrent.set(Command.getABI())
    }

    private fun setUpdate() {
        // 设置默认值
        versionLatest.set(GlobalString.fetching)
        downloadBtnVisible.set(false)
        viewModelScope.launch {
            Server.getInstance().releases({ releaseList ->
                val mReleaseList = releaseList.appReleaseList()
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
        viewModelScope.launch {
            rootCheck.set(checkRoot())
            binCheck.set(checkBin())
            bashrcCheck.set(checkBashrc())
            setArchitecture()
            setUpdate()
            updateLogCard()
            setDynamicColorsCard()
        }
    }

    private suspend fun updateLogCard() {
        val logPath = Path.getShellLogPath()
        logText.set("${Command.countFile(logPath)} ${GlobalString.log}, ${Command.countSize(logPath)} ${GlobalString.size}\n${GlobalString.storedIn} $logPath")
    }

    fun onLogClearClick(v: View) {
        viewModelScope.launch {
            Command.rm(Path.getShellLogPath())
            Toast.makeText(
                v.context, GlobalString.success, Toast.LENGTH_SHORT
            ).show()
            refresh()
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

    fun onOpenSourceLinkBtnClick(v: View) {
        val uri = Uri.parse("https://github.com/XayahSuSuSu/Android-DataBackup")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        v.context.startActivity(intent)
    }
}