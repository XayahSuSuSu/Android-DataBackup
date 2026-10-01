package com.xayah.databackup.service.restore.rustic

import android.net.wifi.WifiConfiguration
import android.provider.ContactsContract
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.adapter.WifiConfigurationAdapter
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
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.AppRestoreParts
import com.xayah.databackup.entity.restore.RestoreInventory
import com.xayah.databackup.entity.rustic.RusticBackupManifest
import com.xayah.databackup.util.PathHelper

/** Reads restore metadata from the selected snapshot. */
class RusticRestoreMetadataHelper {
    private val mMoshi = Moshi.Builder().build()
    private val mWifiAdapter by lazy { Moshi.Builder().add(WifiConfigurationAdapter()).build().adapter<WifiConfiguration>() }
    private val mRecordsAdapter = mMoshi.adapter<List<Map<String, Any?>>>()
    private val mFieldsAdapter = mMoshi.adapter<Map<String, Any?>>()

    fun deserializeManifest(content: String): RusticBackupManifest {
        val manifest = requireNotNull(mMoshi.adapter<RusticBackupManifest>().fromJson(content))
        require(manifest.schemaVersion == RusticBackupManifest.CURRENT_SCHEMA_VERSION) { "Unsupported snapshot manifest version" }
        return manifest
    }

    fun getStructuredPaths(manifest: RusticBackupManifest): List<String> {
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

    fun deserializeInventory(manifest: RusticBackupManifest, files: Map<String, String>): RestoreInventory {
        getStructuredPaths(manifest).forEach { require(files.containsKey(it)) { "Missing snapshot data file" } }
        val apps = manifest.apps.filter { it.included.isNotEmpty() }
            .distinctBy { it.packageName to it.userId }
            .associateBy { RestoreRecordId.app(it.userId, it.packageName) }
        return RestoreInventory(
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
            files = manifest.included.filter { it.category == BackupSourceCategory.File }.distinctBy { it.path }
                .associate { RestoreRecordId.file(it.path) to it.path },
            networks = deserializeRecords<Network>(manifest, files, PathHelper.getBackupNetworksConfigFileRelativePath())
                .mapIndexed { index, record ->
                    RestoreRecordId.network(index) to NetworkUnmarshalled(
                        id = record.id,
                        ssid = record.ssid.ifBlank { getFallbackTitle(index) }.removeSurrounding("\""),
                        preSharedKey = record.preSharedKey?.removeSurrounding("\""),
                        selected = false,
                        config1 = record.config1?.let { mWifiAdapter.fromJson(it) },
                        config2 = record.config2?.let { mWifiAdapter.fromJson(it) },
                    )
                }.toMap(),
            contacts = deserializeRecords<Contact>(manifest, files, PathHelper.getBackupContactsConfigFileRelativePath())
                .mapIndexed { index, record ->
                    val values = deserializeFields(record.rawContact)
                    val nameKey = ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY
                    val name = values[nameKey]?.toString().orEmpty().ifBlank { getFallbackTitle(index) }
                    RestoreRecordId.contact(index) to ContactDeserialized(
                        id = record.id,
                        rawContact = values + (nameKey to name),
                        data = deserializeNestedRecords(record.data),
                        selected = false,
                    )
                }.toMap(),
            callLogs = deserializeRecords<CallLog>(manifest, files, PathHelper.getBackupCallLogsConfigFileRelativePath())
                .mapIndexed { index, record ->
                    RestoreRecordId.callLog(index) to CallLogDeserialized(id = record.id, call = deserializeFields(record.call), selected = false)
                }.toMap(),
            sms = deserializeRecords<Sms>(manifest, files, PathHelper.getBackupMessagesSmsConfigFileRelativePath())
                .mapIndexed { index, record ->
                    RestoreRecordId.sms(index) to SmsDeserialized(id = record.id, config = deserializeFields(record.config), selected = false)
                }.toMap(),
            mms = deserializeRecords<Mms>(manifest, files, PathHelper.getBackupMessagesMmsConfigFileRelativePath())
                .mapIndexed { index, record ->
                    RestoreRecordId.mms(index) to MmsDeserialized(
                        id = record.id,
                        pdu = deserializeFields(record.pdu),
                        addr = deserializeNestedRecords(record.addr),
                        part = deserializeNestedRecords(record.part),
                        selected = false,
                    )
                }.toMap(),
        )
    }

    private inline fun <reified T> deserializeRecords(manifest: RusticBackupManifest, files: Map<String, String>, path: String): List<T> =
        if (path in manifest.structuredFiles) requireNotNull(mMoshi.adapter<List<T>>().fromJson(files.getValue(path))) else emptyList()

    private fun deserializeFields(content: String?): FieldMap = removeNullFields(content?.let { mFieldsAdapter.fromJson(it) }.orEmpty())

    private fun deserializeNestedRecords(content: String?): List<FieldMap> =
        content?.let { mRecordsAdapter.fromJson(it) }.orEmpty().map(::removeNullFields)

    private fun removeNullFields(values: Map<String, Any?>): FieldMap = values.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

    private fun getFallbackTitle(index: Int): String = "#${index + 1}"
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
