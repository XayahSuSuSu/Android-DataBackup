package com.xayah.databackup.entity.restore

import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.CallLogDeserialized
import com.xayah.databackup.database.entity.ContactDeserialized
import com.xayah.databackup.database.entity.MmsDeserialized
import com.xayah.databackup.database.entity.NetworkUnmarshalled
import com.xayah.databackup.database.entity.SmsDeserialized
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.backup.BackupSourcePath

/** Keys identify records within this backup, independently of device database IDs. */
data class RestoreInventory(
    val usersMap: Map<Int, String> = emptyMap(),
    val apps: Map<String, App> = emptyMap(),
    val files: Map<String, String> = emptyMap(),
    val networks: Map<String, NetworkUnmarshalled> = emptyMap(),
    val contacts: Map<String, ContactDeserialized> = emptyMap(),
    val callLogs: Map<String, CallLogDeserialized> = emptyMap(),
    val sms: Map<String, SmsDeserialized> = emptyMap(),
    val mms: Map<String, MmsDeserialized> = emptyMap(),
    val availableAppParts: Map<String, Set<BackupSourceCategory>> = emptyMap(),
    val appSources: Map<String, List<BackupSourcePath>> = emptyMap(),
) {
    fun getIds(category: RestoreCategory): Set<String> = when (category) {
        RestoreCategory.Apps -> apps.keys
        RestoreCategory.Networks -> networks.keys
        RestoreCategory.Contacts -> contacts.keys
        RestoreCategory.CallLogs -> callLogs.keys
        RestoreCategory.Messages -> sms.keys + mms.keys
    }

    val allIds: Set<String> get() = RestoreCategory.entries.flatMap { getIds(it) }.toSet()
    val totalCount: Int get() = apps.size + files.size + networks.size + contacts.size + callLogs.size + sms.size + mms.size
}
