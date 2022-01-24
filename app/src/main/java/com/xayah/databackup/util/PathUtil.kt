package com.xayah.databackup.util

import android.content.Context
import com.xayah.databackup.R

class PathUtil(private val mContext: Context) {
    private val GENERATE_APP_LIST_SCRIPT_NAME =
        mContext.getString(R.string.script_generate_app_list)
    private val BACKUP_SCRIPT_NAME = mContext.getString(R.string.script_backup)
    val APP_LIST_FILE_NAME = mContext.getString(R.string.script_app_list)
    private val BACKUP_SETTINGS_NAME = "backup_settings.conf"
    val DUMP_NAME = "DumpName.sh"
    val RESTORE_BACKUP_NAME = "Restorebackup.sh"

    val DATA_PATH: String = mContext.filesDir.path.replace("/files", "")
    val SCRIPT_PATH: String = "$DATA_PATH/scripts"
    val BIN_SH_PATH: String = "$DATA_PATH/scripts/tools/bin/bin.sh"

    val GENERATE_APP_LIST_SCRIPT_PATH = "$SCRIPT_PATH/$GENERATE_APP_LIST_SCRIPT_NAME"
    val APP_LIST_FILE_PATH = "$SCRIPT_PATH/$APP_LIST_FILE_NAME"
    val BACKUP_SCRIPT_PATH = "$SCRIPT_PATH/$BACKUP_SCRIPT_NAME"
    val BACKUP_SETTINGS_PATH = "$SCRIPT_PATH/$BACKUP_SETTINGS_NAME"
}