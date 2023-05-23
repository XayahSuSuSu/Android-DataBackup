package com.xayah.databackup.util

import android.content.Context

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

object PathUtil
