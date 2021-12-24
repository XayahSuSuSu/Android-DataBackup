package com.xayah.databackup.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.ConsoleActivity
import com.xayah.databackup.R
import com.xayah.databackup.model.AppInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Shell(private val mContext: Context) {
    private val SCRIPT_VERSION = mContext.getString(R.string.script_version)

    private val APP_LIST_FILE_NAME = mContext.getString(R.string.script_app_list)

    private val LOG_FILE_NAME = mContext.getString(R.string.script_log)

    private val GENERATE_APP_LIST_SCRIPT_NAME =
        mContext.getString(R.string.script_generate_app_list)

    private val BACKUP_SCRIPT_NAME = mContext.getString(R.string.script_backup)

    private val RESTORE_SCRIPT_NAME = mContext.getString(R.string.script_restore)

    private val BACKUP_SETTINGS = "backup_settings.conf"

    private val FILE_PATH: String = mContext.getExternalFilesDir(null)!!.absolutePath

    private val DATA_PATH: String = mContext.filesDir.path.replace("/files", "")

    private val SDCARD_PATH: String =
        FILE_PATH.replace("/Android/data/com.xayah.databackup/files", "")

    val SCRIPT_PATH: String = "$DATA_PATH/scripts"

    val BACKUP_PATH: String = "/storage/emulated/0/Download"

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
            val currentVersion =
                mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionName
            val version = ShellUtil.readLine(0, "$DATA_PATH/version")
            if (version == "" || version != currentVersion) {
                ShellUtil.rm("$DATA_PATH/scripts")
                ShellUtil.unzip("$FILE_PATH/$SCRIPT_VERSION.zip", "$DATA_PATH/scripts")
                writeVersion()
            }

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
            outPut.add(
                (if (i.ban) "#" else "") + i.appName.replace(
                    " ",
                    ""
                ) + (if (i.onlyApp) "!" else "") + " " + i.appPackage
            )
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
        ShellUtil.replace("}&", "}", "$SCRIPT_PATH/$BACKUP_SCRIPT_NAME")
        ShellUtil.replace("pv", "pv -f", "$SCRIPT_PATH/$BACKUP_SCRIPT_NAME")
        val callbackList: CallbackList<String> = object : CallbackList<String>() {
            override fun onAddElement(mString: String?) {
                if (mString != null) {
                    event(mString)
                }
            }
        }
        GlobalScope.launch() {
            Shell.su("cd $SCRIPT_PATH; sh $SCRIPT_PATH/$BACKUP_SCRIPT_NAME")
                .to(callbackList, callbackList)
                .submit { result: Shell.Result? ->
                    if (result != null) {
                        finishedEvent(result.isSuccess)
                    }
                }
        }
    }

    fun restore(
        backupDir: String,
        event: (String) -> Unit,
        finishedEvent: (Boolean?) -> Unit
    ) {
        ShellUtil.replace("} &", "}", "$BACKUP_PATH/$backupDir/$RESTORE_SCRIPT_NAME")
        ShellUtil.replace("pv", "pv -f", "$BACKUP_PATH/$backupDir/$RESTORE_SCRIPT_NAME")
        val callbackList: CallbackList<String> = object : CallbackList<String>() {
            override fun onAddElement(mString: String?) {
                if (mString != null) {
                    event(mString)
                }
            }
        }
        GlobalScope.launch() {
            Shell.su("cd $BACKUP_PATH; sh $BACKUP_PATH/$backupDir/$RESTORE_SCRIPT_NAME")
                .to(callbackList)
                .submit { result: Shell.Result? ->
                    if (result != null) {
                        finishedEvent(result.isSuccess)
                    }
                }
        }
    }

    fun writeVersion(): Boolean {
        val versionName =
            mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionName
        return ShellUtil.writeFile(versionName, "$DATA_PATH/version")
    }

    fun readVersion(): String {
        return ShellUtil.readLine(0, "$DATA_PATH/version")
    }

    fun saveSettings() {
        val prefs = mContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val contentList = mutableListOf<String>()
        contentList.add("Lo=${prefs.getInt("Lo", 0)}")
        contentList.add(
            "Output_path=${
                prefs.getString(
                    "Output_path",
                    mContext.getString(R.string.settings_sumarry_output_path)
                )
            }"
        )
        contentList.add("USBdefault=${prefs.getInt("USBdefault", 0)}")
        contentList.add("Splist=${prefs.getInt("Splist", 0)}")
        contentList.add("Backup_user_data=${prefs.getInt("Backup_user_data", 1)}")
        contentList.add("Backup_obb_data=${prefs.getInt("Backup_obb_data", 1)}")
        contentList.add("backup_media=${prefs.getInt("backup_media", 0)}")
        contentList.add(
            "Custom_path=\"\n" + prefs.getString(
                "Custom_path",
                mContext.getString(R.string.settings_summary_custom_path)
            )
                    + "\n\""
        )
        contentList.add("Compression_method=zstd")
        ShellUtil.writeFile(
            contentList.joinToString(separator = "\n"),
            "$SCRIPT_PATH/$BACKUP_SETTINGS"
        )
    }

    fun autoUpdate(allow:Boolean){
        if (allow)
            ShellUtil.touch("$SCRIPT_PATH/tools/bin/update")
        else
            ShellUtil.rm("$SCRIPT_PATH/tools/bin/update")
    }

    fun checkAutoUpdate():Boolean{
        return ShellUtil.ls("$SCRIPT_PATH/tools/bin/update")
    }
}