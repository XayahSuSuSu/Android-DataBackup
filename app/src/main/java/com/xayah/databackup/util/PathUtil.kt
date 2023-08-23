package com.xayah.databackup.util

import android.annotation.SuppressLint
import android.content.Context
import com.xayah.databackup.DataBackupApplication
import java.nio.file.Paths
import kotlin.io.path.pathString

fun Context.filesPath(): String = filesDir.path

fun Context.binPath(): String = "${filesPath()}/bin"

fun Context.binArchivePath(): String = "${filesPath()}/bin.zip"

fun Context.extendPath(): String = "${filesPath()}/extend"

fun Context.iconPath(): String = "${filesPath()}/icon"

@SuppressLint("SdCardPath")
object PathUtil {
    fun getParentPath(path: String): String = Paths.get(path).parent.pathString
    private fun getBackupSavePath(): String = DataBackupApplication.application.readBackupSavePath()
    fun getTreePath(): String = "${getBackupSavePath()}/tree"

    fun getPackageUserPath(userId: Int): String = "/data/user/${userId}"
}
