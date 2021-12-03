package com.xayah.databackup.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.ConsoleActivity
import com.xayah.databackup.model.AppInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ShellUtil(private val mContext: Context) {
    private val scriptVersion = "V11.9"
    private val appListFileName = "应用列表.txt"
    private val logFileName = "执行状态日志.txt"
    private val generateAppListScriptName = "生成应用列表.sh"

    var isSuccess = false

    val console = mutableListOf<String>()

    private val filesPath: String = mContext.getExternalFilesDir(null)!!.absolutePath
    private val sdcardPath: String =
        filesPath.replace("/Android/data/com.xayah.databackup/files", "")
    private val scriptPath: String = "$sdcardPath/DataBackup/scripts"

    val appListFilePath = "$scriptPath/$appListFileName"

    fun extractAssets() {
        try {
            val assets = File(filesPath, "$scriptVersion.zip")
            if (!assets.exists()) {
                val outStream = FileOutputStream(assets)
                val inputStream = mContext.resources.assets.open("$scriptVersion.zip")
                val buffer = ByteArray(1024)
                var byteCount: Int
                while (inputStream.read(buffer).also { byteCount = it } != -1) {
                    outStream.write(buffer, 0, byteCount)
                }
                outStream.flush()
                inputStream.close()
                outStream.close()
            }
            if (!ls("$sdcardPath/DataBackup/scripts"))
                unzip("$filesPath/$scriptVersion.zip", "$sdcardPath/DataBackup/scripts")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun ls(path: String): Boolean {
        return Shell.su("ls -i $path").exec().isSuccess
    }

    fun unzip(filePath: String, out: String) {
        if (mkdir(out))
            Shell.su("unzip $filePath -d $out").exec()
    }

    fun mkdir(path: String): Boolean {
        return Shell.su("mkdir -p $path").exec().isSuccess
    }

    fun rm(path: String): Boolean {
        return Shell.su("rm $path").exec().isSuccess
    }

    fun generateAppList() {
        rm("$scriptPath/$appListFileName")
        rm("$scriptPath/$logFileName")
        GlobalScope.launch() {
            isSuccess =
                Shell.su("sh $scriptPath/$generateAppListScriptName").to(console)
                    .exec().isSuccess
        }
    }

    fun onGenerateAppList() {
        val intent = Intent(mContext, ConsoleActivity::class.java)
        intent.putExtra("type", "generateAppList")
        (mContext as Activity).startActivityForResult(intent, 1)
    }

    fun countLine(path: String): Int {
        var out = 2
        try {
            out = Shell.su("wc -l $path").exec().out.joinToString().split(" ")[0].toInt()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return out
    }

    fun getAppPackages(): MutableList<String> {
        return Shell.su("tail -n +3 $appListFilePath").exec().out
    }

    fun readLine(line: Int, path: String): String {
        return Shell.su("sed -n '${line}p' $path").exec().out.joinToString()
    }

    fun writeLine(line: Int, content: String, path: String): Boolean {
        return Shell.su("sed -i \"${line}c $content\" $path").exec().isSuccess
    }

    fun banAppByIndex(line: Int): Boolean {
        val oldLine = readLine(line + 3, appListFilePath)
        val newLine = "#$oldLine"
        return writeLine(line + 3, newLine, appListFilePath)
    }

    fun allowAppByIndex(line: Int): Boolean {
        val oldLine = readLine(line + 3, appListFilePath)
        val newLine = oldLine.replace("#", "")
        return writeLine(line + 3, newLine, appListFilePath)
    }

    fun limitAppByIndex(line: Int): Boolean {
        val oldLine = readLine(line + 3, appListFilePath).split(" ")
        val newLine = oldLine.joinToString(separator = "! ")
        return writeLine(line + 3, newLine, appListFilePath)
    }

    fun unLimitAppByIndex(line: Int): Boolean {
        val oldLine = readLine(line + 3, appListFilePath)
        val newLine = oldLine.replace("!", "")
        return writeLine(line + 3, newLine, appListFilePath)
    }

    fun writeFile(content: String, path: String): Boolean {
        return Shell.su("echo \"$content\" > $path").exec().isSuccess
    }

    fun saveAppList(appList: MutableList<AppInfo>) {
        var outPut = mutableListOf<String>()
        for (i in appList) {
            outPut.add((if (i.ban) "#" else "") + i.appName + (if (i.onlyApp) "!" else "") + " " + i.appPackage)
        }
        val head = Shell.su("head -2 $appListFilePath").exec().out
        outPut = (head + outPut) as MutableList<String>
        writeFile(outPut.joinToString(separator = "\n"), appListFilePath)
    }

    fun countSelected(): Int {
        val appList = Shell.su("tail -n +3 $appListFilePath").exec().out
        return countLine(appListFilePath) - 2 - appList.joinToString().count { it == '#' }
    }
}