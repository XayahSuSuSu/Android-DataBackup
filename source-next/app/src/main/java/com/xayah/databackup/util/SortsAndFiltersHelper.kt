package com.xayah.databackup.util

import com.xayah.databackup.database.entity.App

fun Iterable<App>.filter(userId: Int, filterUserApps: Boolean, filterSystemApps: Boolean): List<App> = filter {
    it.userId == userId
            && ((filterUserApps && it.isSystemApp.not()) || (filterSystemApps && it.isSystemApp))
}

fun Iterable<App>.sortByA2Z(sortSequence: SortsSequence): List<App> =
    when (sortSequence) {
        SortsSequence.ASCENDING -> sortedBy { app -> app.info.label }
        SortsSequence.DESCENDING -> sortedByDescending { app -> app.info.label }
    }

fun Iterable<App>.sortByDataSize(sortSequence: SortsSequence): List<App> =
    when (sortSequence) {
        SortsSequence.ASCENDING -> sortedBy { app -> app.totalBytes }
        SortsSequence.DESCENDING -> sortedByDescending { app -> app.totalBytes }
    }

fun Iterable<App>.sortByInstallTime(sortSequence: SortsSequence): List<App> =
    when (sortSequence) {
        SortsSequence.ASCENDING -> sortedBy { app -> app.info.firstInstallTime }
        SortsSequence.DESCENDING -> sortedByDescending { app -> app.info.firstInstallTime }
    }

fun Iterable<App>.sortByUpdateTime(sortSequence: SortsSequence): List<App> =
    when (sortSequence) {
        SortsSequence.ASCENDING -> sortedBy { app -> app.info.lastUpdateTime }
        SortsSequence.DESCENDING -> sortedByDescending { app -> app.info.lastUpdateTime }
    }
