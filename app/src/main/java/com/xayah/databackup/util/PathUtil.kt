package com.xayah.databackup.util

import android.content.Context
import com.xayah.databackup.DataBackupApplication

fun Context.filesPath(): String {
    return filesDir.path
}

fun Context.binPath(): String {
    return "${filesPath()}/bin"
}

fun Context.binArchivePath(): String {
    return "${filesPath()}/bin.zip"
}

fun Context.extendPath(): String {
    return "${filesPath()}/extend"
}

fun Context.iconPath(): String {
    return "${filesPath()}/icon"
}

object PathUtil {
    private fun getBackupSavePath(): String {
        return DataBackupApplication.application.readBackupSavePath()
    }

    fun getTreePath(): String {
        return "${getBackupSavePath()}/tree"
    }
}
