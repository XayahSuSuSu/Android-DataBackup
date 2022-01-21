package com.xayah.databackup.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Shell(private val mContext: Context) {
    private val SCRIPT_VERSION = mContext.getString(R.string.script_version)

    val APP_LIST_FILE_NAME = mContext.getString(R.string.script_app_list)

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
            if (version == "" /*|| version != currentVersion*/) {
                ShellUtil.rm("$DATA_PATH/scripts")
                ShellUtil.unzip("$FILE_PATH/$SCRIPT_VERSION.zip", "$DATA_PATH/scripts")
                writeVersion()
            }

        } catch (e: IOException) {
            e.printStackTrace()
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

    fun close() {
        Shell.getShell().close()
    }

    fun checkRestoreScript(path: String): Boolean {
        return ShellUtil.ls(path + RESTORE_SCRIPT_NAME)
    }

//    fun writeInfo(): Boolean {
//        val prefs = mContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
//        return ShellUtil.writeFile(
//            "${
//                prefs.getString(
//                    "info",
//                    mContext.getString(R.string.settings_sumarry_info)
//                )
//            }_${System.currentTimeMillis()}",
//            prefs.getString(
//                "Output_path",
//                mContext.getString(R.string.settings_sumarry_output_path)
//            ) + "/Backup_${
//                prefs.getString(
//                    "Compression_method",
//                    mContext.getString(R.string.settings_summary_compression_method_zstd)
//                ).toString().replace(Regex("\\((.+?)\\)"), "")
//            }/.config"
//        )
//    }

//    fun getInfo(path: String): BackupInfo {
//        val info = ShellUtil.readLine(0, "$path/.config").split("_")
//        if (info.size == 1) {
//            return BackupInfo(
//                mContext.getString(R.string.restore_not_named),
//                mContext.getString(R.string.restore_not_timed),
//                path
//            )
//        } else {
//            val name = info[0]
//            val time = DataUtil.getFormatDate(info[1].toLong())
//            return BackupInfo(name, time, path)
//        }
//    }
}