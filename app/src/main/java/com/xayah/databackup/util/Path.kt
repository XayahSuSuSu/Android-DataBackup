package com.xayah.databackup.util

import android.content.Context

class Path {
    companion object {
        fun getExternalFilesDir(context: Context): String {
            return context.filesDir.path
        }
    }
}