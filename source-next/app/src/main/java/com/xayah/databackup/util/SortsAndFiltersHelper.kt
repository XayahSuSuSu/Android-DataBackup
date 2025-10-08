package com.xayah.databackup.util

import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.CallLogDeserialized
import com.xayah.databackup.database.entity.ContactDeserialized
import com.xayah.databackup.database.entity.MmsDeserialized
import com.xayah.databackup.database.entity.NetworkUnmarshalled
import com.xayah.databackup.database.entity.SmsDeserialized

private fun filterUserId(app: App, userId: Int) = app.userId == userId

private fun filterApps(app: App, filterUserApps: Boolean, filterSystemApps: Boolean) =
    ((filterUserApps && app.isSystemApp.not()) || (filterSystemApps && app.isSystemApp))

private fun filterSearchText(app: App, searchText: String) =
    searchText.isEmpty()
            || app.info.label.lowercase().contains(searchText.lowercase())
            || app.packageName.lowercase().contains(searchText.lowercase())

private fun filterSearchText(network: NetworkUnmarshalled, searchText: String) =
    searchText.isEmpty()
            || network.ssid.lowercase().contains(searchText.lowercase())

private fun filterSearchText(contact: ContactDeserialized, searchText: String) =
    searchText.isEmpty()
            || contact.displayName.lowercase().contains(searchText.lowercase())

private fun filterSearchText(callLog: CallLogDeserialized, searchText: String) =
    searchText.isEmpty()
            || callLog.number.lowercase().contains(searchText.lowercase())

private fun filterSearchText(sms: SmsDeserialized, searchText: String) =
    searchText.isEmpty()
            || sms.body.lowercase().contains(searchText.lowercase())

private fun filterSearchText(mms: MmsDeserialized, searchText: String) =
    searchText.isEmpty()
            || mms.body.toString().contains(searchText.lowercase())

fun Iterable<App>.filterApp(searchText: String): List<App> = filter { filterSearchText(it, searchText) }

fun Iterable<App>.filterApp(userId: Int, filterUserApps: Boolean, filterSystemApps: Boolean): List<App> = filter {
    filterUserId(it, userId)
            && filterApps(it, filterUserApps, filterSystemApps)
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

fun Iterable<NetworkUnmarshalled>.filterNetwork(searchText: String): List<NetworkUnmarshalled> = filter {
    filterSearchText(it, searchText)
}

fun Iterable<ContactDeserialized>.filterContact(searchText: String): List<ContactDeserialized> = filter {
    filterSearchText(it, searchText)
}

fun Iterable<CallLogDeserialized>.filterCallLog(searchText: String): List<CallLogDeserialized> = filter {
    filterSearchText(it, searchText)
}

fun Iterable<SmsDeserialized>.filterSms(searchText: String): List<SmsDeserialized> = filter {
    filterSearchText(it, searchText)
}

fun Iterable<MmsDeserialized>.filterMms(searchText: String): List<MmsDeserialized> = filter {
    filterSearchText(it, searchText)
}
