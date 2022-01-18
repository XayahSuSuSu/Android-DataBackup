package com.xayah.databackup.util

import android.content.Context
import com.xayah.databackup.R

class PathUtil(private val mContext: Context) {
    val DATA_PATH: String = mContext.filesDir.path.replace("/files", "")
    val SCRIPT_PATH: String = "$DATA_PATH/scripts"
    val BIN_SH_PATH: String = "$DATA_PATH/scripts/tools/bin/bin.sh"
    val GENERATE_APP_LIST_SCRIPT_NAME = mContext.getString(R.string.script_generate_app_list)
    val BACKUP_SCRIPT_NAME = mContext.getString(R.string.script_backup)
}