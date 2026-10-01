package com.xayah.databackup.data.rustic

import android.net.wifi.WifiConfiguration
import android.provider.ContactsContract
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.adapter.WifiConfigurationAdapter
import com.xayah.databackup.data.restore.AppRestoreParts
import com.xayah.databackup.data.restore.RestoreCategory
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.CallLogDeserialized
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.ContactDeserialized
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.database.entity.Info
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.MmsDeserialized
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.database.entity.NetworkUnmarshalled
import com.xayah.databackup.database.entity.Option
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.database.entity.SmsDeserialized
import com.xayah.databackup.database.entity.Storage
import com.xayah.databackup.util.PathHelper

/** Keys identify records within this snapshot, independently of device database IDs. */
data class RusticRestoreInventory(
    val usersMap: Map<Int, String> = emptyMap(),
    val apps: Map<String, App> = emptyMap(),
    val files: Map<String, String> = emptyMap(),
    val networks: Map<String, NetworkUnmarshalled> = emptyMap(),
    val contacts: Map<String, ContactDeserialized> = emptyMap(),
    val callLogs: Map<String, CallLogDeserialized> = emptyMap(),
    val sms: Map<String, SmsDeserialized> = emptyMap(),
    val mms: Map<String, MmsDeserialized> = emptyMap(),
    val availableAppParts: Map<String, Set<RusticSourceCategory>> = emptyMap(),
    val appSources: Map<String, List<RusticSourcePath>> = emptyMap(),
) {
    fun ids(category: RestoreCategory): Set<String> = when (category) {
        RestoreCategory.Apps -> apps.keys
        RestoreCategory.Networks -> networks.keys
        RestoreCategory.Contacts -> contacts.keys
        RestoreCategory.CallLogs -> callLogs.keys
        RestoreCategory.Messages -> sms.keys + mms.keys
    }

    val allIds: Set<String> get() = RestoreCategory.entries.flatMap { ids(it) }.toSet()
    val totalCount: Int get() = apps.size + files.size + networks.size + contacts.size + callLogs.size + sms.size + mms.size
}

/** Reads restore metadata from the selected snapshot. */
class RusticRestoreInventoryReader {
    private val mMoshi = Moshi.Builder().build()
    private val mWifiAdapter by lazy { Moshi.Builder().add(WifiConfigurationAdapter()).build().adapter<WifiConfiguration>() }
    private val mRecordsAdapter = mMoshi.adapter<List<Map<String, Any?>>>()
    private val mFieldsAdapter = mMoshi.adapter<Map<String, Any?>>()

    fun manifest(content: String): RusticBackupManifest = requireNotNull(mMoshi.adapter<RusticBackupManifest>().fromJson(content)).also {
            require(it.schemaVersion == RusticBackupManifest.CURRENT_SCHEMA_VERSION) { "Unsupported snapshot manifest version" }
        }

    fun structuredPaths(manifest: RusticBackupManifest): List<String> {
        val supported = listOf(
            PathHelper.getBackupNetworksConfigFileRelativePath(),
            PathHelper.getBackupContactsConfigFileRelativePath(),
            PathHelper.getBackupCallLogsConfigFileRelativePath(),
            PathHelper.getBackupMessagesSmsConfigFileRelativePath(),
            PathHelper.getBackupMessagesMmsConfigFileRelativePath(),
        )
        require(manifest.structuredFiles.all { it in supported }) { "Unsupported snapshot data file" }
        return manifest.structuredFiles.distinct()
    }

    fun inventory(manifest: RusticBackupManifest, files: Map<String, String>): RusticRestoreInventory {
        structuredPaths(manifest).forEach { require(files.containsKey(it)) { "Missing snapshot data file" } }
        val apps = manifest.apps.filter { it.included.isNotEmpty() }
            .distinctBy { it.packageName to it.userId }
            .associateBy { RestoreRecordId.app(it.userId, it.packageName) }
        return RusticRestoreInventory(
            usersMap = apps.values.associate { it.userId to it.userName },
            apps = apps.mapValues { (_, app) ->
                App(
                    packageName = app.packageName,
                    userId = app.userId,
                    info = Info(
                        label = app.label.ifBlank { app.packageName },
                        versionName = app.versionName,
                        flags = app.flags,
                        firstInstallTime = app.firstInstallTime,
                        lastUpdateTime = app.lastUpdateTime,
                        versionCode = app.versionCode
                    ),
                    option = Option(apk = false, internalData = false, externalData = false, additionalData = false),
                    storage = Storage(
                        apkBytes = app.apkBytes,
                        internalDataBytes = app.internalDataBytes,
                        externalDataBytes = app.externalDataBytes,
                        additionalDataBytes = app.additionalDataBytes,
                    ),
                )
            },
            availableAppParts = apps.mapValues { (_, app) ->
                app.included.map { it.category }.filter { it in AppRestoreParts }.toSet()
            },
            appSources = apps.mapValues { (_, app) -> app.included },
            files = manifest.included.filter { it.category == RusticSourceCategory.File }.distinctBy { it.path }
                .associate { RestoreRecordId.file(it.path) to it.path },
            networks = records<Network>(manifest, files, PathHelper.getBackupNetworksConfigFileRelativePath())
                .mapIndexed { index, record ->
                    RestoreRecordId.network(index) to NetworkUnmarshalled(
                        id = record.id,
                        ssid = record.ssid.ifBlank { fallbackTitle(index) }.removeSurrounding("\""),
                        preSharedKey = record.preSharedKey?.removeSurrounding("\""),
                        selected = false,
                        config1 = record.config1?.let { mWifiAdapter.fromJson(it) },
                        config2 = record.config2?.let { mWifiAdapter.fromJson(it) },
                    )
                }.toMap(),
            contacts = records<Contact>(manifest, files, PathHelper.getBackupContactsConfigFileRelativePath())
                .mapIndexed { index, record ->
                    val values = fields(record.rawContact)
                    val nameKey = ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY
                    val name = values[nameKey]?.toString().orEmpty().ifBlank { fallbackTitle(index) }
                    RestoreRecordId.contact(index) to ContactDeserialized(
                        id = record.id,
                        rawContact = values + (nameKey to name),
                        data = nestedRecords(record.data),
                        selected = false,
                    )
                }.toMap(),
            callLogs = records<CallLog>(manifest, files, PathHelper.getBackupCallLogsConfigFileRelativePath())
                .mapIndexed { index, record ->
                    RestoreRecordId.callLog(index) to CallLogDeserialized(id = record.id, call = fields(record.call), selected = false)
                }.toMap(),
            sms = records<Sms>(manifest, files, PathHelper.getBackupMessagesSmsConfigFileRelativePath())
                .mapIndexed { index, record ->
                    RestoreRecordId.sms(index) to SmsDeserialized(id = record.id, config = fields(record.config), selected = false)
                }.toMap(),
            mms = records<Mms>(manifest, files, PathHelper.getBackupMessagesMmsConfigFileRelativePath())
                .mapIndexed { index, record ->
                    RestoreRecordId.mms(index) to MmsDeserialized(
                        id = record.id,
                        pdu = fields(record.pdu),
                        addr = nestedRecords(record.addr),
                        part = nestedRecords(record.part),
                        selected = false,
                    )
                }.toMap(),
        )
    }

    private inline fun <reified T> records(manifest: RusticBackupManifest, files: Map<String, String>, path: String): List<T> =
        if (path in manifest.structuredFiles) requireNotNull(mMoshi.adapter<List<T>>().fromJson(files.getValue(path))) else emptyList()

    private fun fields(content: String?): FieldMap = clean(content?.let { mFieldsAdapter.fromJson(it) }.orEmpty())

    private fun nestedRecords(content: String?): List<FieldMap> =
        content?.let { mRecordsAdapter.fromJson(it) }.orEmpty().map(::clean)

    private fun clean(values: Map<String, Any?>): FieldMap = values.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

    private fun fallbackTitle(index: Int): String = "#${index + 1}"
}

private object RestoreRecordId {
    fun app(userId: Int, packageName: String): String = "app:$userId:$packageName"
    fun file(path: String): String = "file:$path"
    fun network(index: Int): String = "network:$index"
    fun contact(index: Int): String = "contact:$index"
    fun callLog(index: Int): String = "call:$index"
    fun sms(index: Int): String = "sms:$index"
    fun mms(index: Int): String = "mms:$index"
}
