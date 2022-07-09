package com.xayah.databackup.fragment.home

import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.xayah.databackup.App
import com.xayah.databackup.data.Release
import com.xayah.databackup.util.Bashrc
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HomeViewModel : ViewModel() {
    val root = "${GlobalString.symbolDot} Root"
    val bin = "${GlobalString.symbolDot} Bin"
    val bashrc = "${GlobalString.symbolDot} Bashrc"
    var rootCheck = ObservableField(checkRoot())
    var binCheck = ObservableField(checkBin())
    var bashrcCheck = ObservableField(checkBashrc())
    var internalStorageString = ObservableField("")
    var internalStorageMaxValue = 100
    var internalStorageValue = ObservableInt(0)
    var otgString = ObservableField(GlobalString.notPluggedIn)
    var otgMaxValue = 100
    var otgValue = ObservableInt(0)
    var architectureCurrent = ObservableField("")
    var architectureSupport = ObservableField("${GlobalString.support}: arm64-v8a, x86_64")
    var versionCurrent = ObservableField(App.versionName)
    var versionLatest = ObservableField("")
    var downloadBtnVisible = ObservableBoolean(false)

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
        return if (Command.checkBin(App.globalContext)) GlobalString.symbolTick else GlobalString.symbolCross
    }

    private fun checkBashrc(): String {
        return if (Command.checkBashrc()) GlobalString.symbolTick else GlobalString.symbolCross
    }

    private fun setInternalStorage() {
        // 默认值
        internalStorageString.set(GlobalString.fetching)
        internalStorageValue.set(0)

        val space = Bashrc.getStorageSpace(Environment.getExternalStorageDirectory().path)
        val string = if (space.first) space.second else GlobalString.error
        internalStorageString.set(string)
        if (space.first) {
            try {
                internalStorageValue.set(string.split(" ").last().replace("%", "").toInt())
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                internalStorageString.set(GlobalString.fetchFailed)
            }
        } else {
            internalStorageString.set(GlobalString.fetchFailed)
        }
    }

    private fun setOTG() {
        // 默认值
        otgString.set(GlobalString.notPluggedIn)
        otgValue.set(0)
        // 检查OTG连接情况
        Bashrc.checkOTG().apply {
            val that = this
            if (that.first == 0) {
                val space = Bashrc.getStorageSpace(that.second)
                val string = if (space.first) space.second else GlobalString.error
                otgString.set(string)
                if (space.first) {
                    try {
                        otgValue.set(string.split(" ").last().replace("%", "").toInt())
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                }
            } else if (that.first == 1) {
                otgString.set(GlobalString.unsupportedFormat)
            }
        }
    }

    private fun setArchitecture() {
        architectureCurrent.set(Command.getABI())
    }

    private fun setUpdate() {
        // 设置默认值
        versionLatest.set(GlobalString.fetching)
        downloadBtnVisible.set(false)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request: Request = Request.Builder()
                    .url("https://api.github.com/repos/XayahSuSuSu/Android-DataBackup/releases")
                    .build()
                client.newCall(request).execute().use { response ->
                    response.body?.apply {
                        // 解析response.body
                        var jsonArray = JsonArray()
                        try {
                            jsonArray = JsonParser.parseString(this.string()).asJsonArray
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        val mBodyList = mutableListOf<Release>()
                        for (i in jsonArray) {
                            try {
                                val item = Gson().fromJson(i, Release::class.java)
                                if (item.name.contains("Check")) continue
                                mBodyList.add(item)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        if (mBodyList.isEmpty()) {
                            versionLatest.set(GlobalString.fetchFailed)
                        } else {
                            versionLatest.set("${GlobalString.latest}: ${mBodyList[0].name}")
                            if (!versionLatest.get()!!.contains(versionCurrent.get()!!)) {
                                downloadBtnVisible.set(true)
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                versionLatest.set(GlobalString.fetchFailed)
            }
        }
    }

    fun refresh(v: View? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            rootCheck.set(checkRoot())
            binCheck.set(checkBin())
            bashrcCheck.set(checkBashrc())
            setInternalStorage()
            setOTG()
            setArchitecture()
            setUpdate()
        }
    }

    fun initialize() {
        refresh()
//        saveLog()
    }

    private fun saveLog() {
        CoroutineScope(Dispatchers.IO).launch {
            val date = Command.getDate().replace(" ", "_")
            Command.mkdir(Path.getShellLogPath())
            Command.saveShellLog("${Path.getShellLogPath()}/${date}")
        }
    }
}