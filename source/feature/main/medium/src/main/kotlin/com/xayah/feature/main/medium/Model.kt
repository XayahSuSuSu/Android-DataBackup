package com.xayah.feature.main.medium

import android.content.Context

fun countBackups(context: Context, count: Int) = run {
    String.format(context.resources.getQuantityString(R.plurals.backups, count), count)
}
