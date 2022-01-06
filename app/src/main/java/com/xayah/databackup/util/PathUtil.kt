package com.xayah.databackup.util

import android.content.Context

class PathUtil(private val mContext: Context) {
    val DATA_PATH: String = mContext.filesDir.path.replace("/files", "")
    val SCRIPT_PATH: String = "$DATA_PATH/scripts"
    val BIN_SH_PATH: String = "$DATA_PATH/scripts/tools/bin/bin.sh"
}