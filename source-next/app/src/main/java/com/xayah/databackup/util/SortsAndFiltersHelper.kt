package com.xayah.databackup.util

import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.NetworkUnmarshalled

fun Iterable<App>.filter(searchText: String, userId: Int, filterUserApps: Boolean, filterSystemApps: Boolean): List<App> = filter {
    filterUserId(it, userId)
            && filterApps(it, filterUserApps, filterSystemApps)
            && filterSearchText(it, searchText)
}

private fun filterUserId(app: App, userId: Int) = app.userId == userId

private fun filterApps(app: App, filterUserApps: Boolean, filterSystemApps: Boolean) =
    ((filterUserApps && app.isSystemApp.not()) || (filterSystemApps && app.isSystemApp))

private fun filterSearchText(app: App, searchText: String) =
    searchText.isEmpty()
            || app.info.label.lowercase().contains(searchText.lowercase())
            || app.packageName.lowercase().contains(searchText.lowercase())

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

fun Iterable<NetworkUnmarshalled>.filter(searchText: String): List<NetworkUnmarshalled> = filter {
    filterSearchText(it, searchText)
}

private fun filterSearchText(network: NetworkUnmarshalled, searchText: String) =
    searchText.isEmpty()
            || network.ssid.lowercase().contains(searchText.lowercase())
