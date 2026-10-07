package com.xayah.databackup.service.restore

import android.net.wifi.WifiConfiguration
import android.provider.ContactsContract
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.adapter.WifiConfigurationAdapter
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.CallLogDeserialized
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.ContactDeserialized
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.MmsDeserialized
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.database.entity.NetworkUnmarshalled
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.database.entity.SmsDeserialized
import com.xayah.databackup.entity.restore.RestoreInventory
import com.xayah.databackup.util.PathHelper

/** Decodes the structured JSON format shared by Archive and Rustic backups. */
class RestoreMetadataHelper {
    private val mMoshi = Moshi.Builder().build()
    private val mWifiAdapter by lazy { Moshi.Builder().add(WifiConfigurationAdapter()).build().adapter<WifiConfiguration>() }
    private val mRecordsAdapter = mMoshi.adapter<List<Map<String, Any?>>>()
    private val mFieldsAdapter = mMoshi.adapter<Map<String, Any?>>()

    fun deserializeInventory(files: Map<String, String>): RestoreInventory = RestoreInventory(
        networks = deserializeRecords<Network>(files, PathHelper.getBackupNetworksConfigFileRelativePath())
            .mapIndexed { index, record ->
                RestoreRecordId.getNetworkId(index) to NetworkUnmarshalled(
                    id = record.id,
                    ssid = record.ssid.ifBlank { getFallbackTitle(index) }.removeSurrounding("\""),
                    preSharedKey = record.preSharedKey?.removeSurrounding("\""),
                    selected = false,
                    config1 = record.config1?.let { mWifiAdapter.fromJson(it) },
                    config2 = record.config2?.let { mWifiAdapter.fromJson(it) },
                )
            }.toMap(),
        contacts = deserializeRecords<Contact>(files, PathHelper.getBackupContactsConfigFileRelativePath())
            .mapIndexed { index, record ->
                val values = deserializeFields(record.rawContact)
                val nameKey = ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY
                val name = values[nameKey]?.toString().orEmpty().ifBlank { getFallbackTitle(index) }
                RestoreRecordId.getContactId(index) to ContactDeserialized(
                    id = record.id,
                    rawContact = values + (nameKey to name),
                    data = deserializeNestedRecords(record.data),
                    selected = false,
                )
            }.toMap(),
        callLogs = deserializeRecords<CallLog>(files, PathHelper.getBackupCallLogsConfigFileRelativePath())
            .mapIndexed { index, record ->
                RestoreRecordId.getCallLogId(index) to CallLogDeserialized(
                    id = record.id,
                    call = deserializeFields(record.call),
                    selected = false,
                )
            }.toMap(),
        sms = deserializeRecords<Sms>(files, PathHelper.getBackupMessagesSmsConfigFileRelativePath())
            .mapIndexed { index, record ->
                RestoreRecordId.getSmsId(index) to SmsDeserialized(id = record.id, config = deserializeFields(record.config), selected = false)
            }.toMap(),
        mms = deserializeRecords<Mms>(files, PathHelper.getBackupMessagesMmsConfigFileRelativePath())
            .mapIndexed { index, record ->
                RestoreRecordId.getMmsId(index) to MmsDeserialized(
                    id = record.id,
                    pdu = deserializeFields(record.pdu),
                    addr = deserializeNestedRecords(record.addr),
                    part = deserializeNestedRecords(record.part),
                    selected = false,
                )
            }.toMap(),
    )

    private inline fun <reified T> deserializeRecords(files: Map<String, String>, path: String): List<T> =
        if (path in files) requireNotNull(mMoshi.adapter<List<T>>().fromJson(files.getValue(path))) else emptyList()

    private fun deserializeFields(content: String?): FieldMap = removeNullFields(content?.let { mFieldsAdapter.fromJson(it) }.orEmpty())

    private fun deserializeNestedRecords(content: String?): List<FieldMap> =
        content?.let { mRecordsAdapter.fromJson(it) }.orEmpty().map(::removeNullFields)

    private fun removeNullFields(values: Map<String, Any?>): FieldMap = values.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

    private fun getFallbackTitle(index: Int): String = "#${index + 1}"
}

internal object RestoreRecordId {
    fun getAppId(userId: Int, packageName: String): String = "app:$userId:$packageName"
    fun getFileId(path: String): String = "file:$path"
    fun getNetworkId(index: Int): String = "network:$index"
    fun getContactId(index: Int): String = "contact:$index"
    fun getCallLogId(index: Int): String = "call:$index"
    fun getSmsId(index: Int): String = "sms:$index"
    fun getMmsId(index: Int): String = "mms:$index"
}
