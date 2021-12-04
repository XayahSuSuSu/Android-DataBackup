package com.xayah.databackup.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.ConsoleActivity
import com.xayah.databackup.model.AppInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Shell(private val mContext: Context) {
    private val SCRIPT_VERSION = "V11.9"

    private val APP_LIST_FILE_NAME = "应用列表.txt"

    private val LOG_FILE_NAME = "执行状态日志.txt"

    private val GENERATE_APP_LIST_SCRIPT_NAME = "生成应用列表.sh"

    private val BACKUP_SCRIPT_NAME = "备份应用.sh"

    private val FILE_PATH: String = mContext.getExternalFilesDir(null)!!.absolutePath

    private val SDCARD_PATH: String =
        FILE_PATH.replace("/Android/data/com.xayah.databackup/files", "")

    val SCRIPT_PATH: String = "$SDCARD_PATH/DataBackup/scripts"

    val BACKUP_PATH: String = "$SDCARD_PATH/DataBackup/backups"

    val APP_LIST_FILE_PATH = "$SCRIPT_PATH/$APP_LIST_FILE_NAME"

    fun extractAssets() {
        try {
            val assets = File(FILE_PATH, "$SCRIPT_VERSION.zip")
            if (!assets.exists()) {
                val outStream = FileOutputStream(assets)
                val inputStream = mContext.resources.assets.open("$SCRIPT_VERSION.zip")
                val buffer = ByteArray(1024)
                var byteCount: Int
                while (inputStream.read(buffer).also { byteCount = it } != -1) {
                    outStream.write(buffer, 0, byteCount)
                }
                outStream.flush()
                inputStream.close()
                outStream.close()
            }
            if (!ShellUtil.ls("$SDCARD_PATH/DataBackup/scripts"))
                ShellUtil.unzip("$FILE_PATH/$SCRIPT_VERSION.zip", "$SDCARD_PATH/DataBackup/scripts")
            if (!ShellUtil.ls("$SDCARD_PATH/DataBackup/backups"))
                ShellUtil.mkdir("$SDCARD_PATH/DataBackup/backups")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun generateAppList(
        event: (String) -> Unit,
        finishedEvent: (Boolean?) -> Unit
    ) {
        ShellUtil.rm("$SCRIPT_PATH/$APP_LIST_FILE_NAME")
        ShellUtil.rm("$SCRIPT_PATH/$LOG_FILE_NAME")
        val callbackList: CallbackList<String> = object : CallbackList<String>() {
            override fun onAddElement(mString: String?) {
                if (mString != null) {
                    event(mString)
                }
            }
        }
        GlobalScope.launch() {
            Shell.su("cd $SCRIPT_PATH; sh $SCRIPT_PATH/$GENERATE_APP_LIST_SCRIPT_NAME")
                .to(callbackList)
                .submit { result: Shell.Result? ->
                    if (result != null) {
                        finishedEvent(result.isSuccess)
                    }
                }
        }
    }

    fun onGenerateAppList() {
        val intent = Intent(mContext, ConsoleActivity::class.java)
        intent.putExtra("type", "generateAppList")
        (mContext as Activity).startActivityForResult(intent, 1)
    }


    fun saveAppList(appList: MutableList<AppInfo>) {
        var outPut = mutableListOf<String>()
        for (i in appList) {
            outPut.add((if (i.ban) "#" else "") + i.appName + (if (i.onlyApp) "!" else "") + " " + i.appPackage)
        }
        val head = Shell.su("head -2 $APP_LIST_FILE_PATH").exec().out
        outPut = (head + outPut) as MutableList<String>
        ShellUtil.writeFile(outPut.joinToString(separator = "\n"), APP_LIST_FILE_PATH)
    }

    fun onBackup() {
        val intent = Intent(mContext, ConsoleActivity::class.java)
        intent.putExtra("type", "backup")
        (mContext as Activity).startActivityForResult(intent, 1)
    }

    fun backup(
        event: (String) -> Unit,
        finishedEvent: (Boolean?) -> Unit
    ) {
        ShellUtil.rm("$SCRIPT_PATH/$LOG_FILE_NAME")
        ShellUtil.replace("}&","}","$SCRIPT_PATH/$BACKUP_SCRIPT_NAME")
        val callbackList: CallbackList<String> = object : CallbackList<String>() {
            override fun onAddElement(mString: String?) {
                if (mString != null) {
                    event(mString)
                }
            }
        }
        GlobalScope.launch() {
            Shell.su("cd $SCRIPT_PATH; sh $SCRIPT_PATH/$BACKUP_SCRIPT_NAME")
                .to(callbackList)
                .submit { result: Shell.Result? ->
                    if (result != null) {
                        finishedEvent(result.isSuccess)
                    }
                }
        }
    }
}