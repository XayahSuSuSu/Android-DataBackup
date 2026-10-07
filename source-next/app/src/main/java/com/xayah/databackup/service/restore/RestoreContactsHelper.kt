package com.xayah.databackup.service.restore

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import androidx.annotation.WorkerThread
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.entity.restore.RestoreProgressCallback
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Restores contact records read from backup metadata through Contacts Provider without rolling back earlier imports.
 * The caller must provide the snapshot metadata and an app ContentResolver with WRITE_CONTACTS permission.
 * Inserts local contacts in the app's current user, with each raw contact and its data rows in one transaction.
 * Links data rows to newly allocated raw contact IDs without overwriting existing contacts.
 * Omits source IDs, account/sync metadata, group memberships and photos; repeated calls can create duplicates.
 * Skips deleted records and records with no restorable data rows, and returns their inventory keys.
 *
 * see [VCardEntry.java](https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:frameworks/opt/vcard/java/com/android/vcard/VCardEntry.java)
 */
internal class RestoreContactsHelper(private val mResolver: ContentResolver) {
    @WorkerThread
    suspend fun restore(
        serialized: String,
        path: String,
        contactIds: List<String>,
        callback: RestoreProgressCallback,
    ): List<String> {
        // Validate every selected record before the first write. Do not expose contact JSON in errors.
        val contacts = runCatching {
            val files = requireNotNull(Moshi.Builder().build().adapter<Map<String, String>>().fromJson(serialized))
            prepareContacts(requireNotNull(files[path]), contactIds)
        }.getOrElse { e ->
            LogHelper.e(TAG, "restore", "", e)
            throw IllegalArgumentException("Invalid contacts backup or selection")
        }
        val skipped = mutableListOf<String>()
        for ((id, prepared) in contacts) {
            currentCoroutineContext().ensureActive()
            val result = restoreRecord(id, callback) {
                val contact = prepared.getOrThrow()
                if (contact == null) {
                    true
                } else {
                    mResolver.applyBatch(ContactsContract.AUTHORITY, buildOperations(contact))
                    false
                }
            }
            if (result == true) {
                skipped.add(id)
            }
        }
        return skipped
    }

    internal data class ContactRestoreData(val rawContact: FieldMap, val data: List<FieldMap>)

    companion object {
        private const val TAG = "RestoreContactsHelper"
        private val rawColumns = setOf(RawContacts.STARRED, RawContacts.CUSTOM_RINGTONE, RawContacts.SEND_TO_VOICEMAIL)
        private val dataColumns = (1..15).map { "data$it" }.toSet() + setOf(Data.MIMETYPE, Data.IS_PRIMARY, Data.IS_SUPER_PRIMARY)

        private fun prepareContacts(content: String, contactIds: List<String>): Map<String, Result<ContactRestoreData?>> {
            require(contactIds.isNotEmpty()) { "No contacts selected" }
            val moshi = Moshi.Builder().build()
            val records =
                requireNotNull(moshi.adapter<List<Contact>>().fromJson(content)).mapIndexed { index, contact -> "contact:$index" to contact }.toMap()
            return contactIds.distinct().associateWith { id ->
                runCatching {
                    val record = requireNotNull(records[id]) { "Unknown contact record" }
                    val raw = requireNotNull(moshi.adapter<FieldMap>().fromJson(requireNotNull(record.rawContact)))
                    if ((raw[RawContacts.DELETED] as? Number)?.toInt() == 1) {
                        null
                    } else {
                        val data = requireNotNull(moshi.adapter<List<FieldMap>>().fromJson(requireNotNull(record.data)))
                            .mapNotNull { row ->
                                val mimeType = row[Data.MIMETYPE] as? String
                                require(!mimeType.isNullOrBlank()) { "Missing contact MIME type" }
                                // Group IDs and display-photo file IDs refer to the source provider only.
                                // Photos are absent because backup's cursor reader does not serialize blobs.
                                if (mimeType == CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE ||
                                    mimeType == CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                                ) null else sanitize(row, dataColumns)
                            }
                        if (data.isEmpty()) null else ContactRestoreData(sanitize(raw, rawColumns), data)
                    }
                }
            }
        }

        private fun sanitize(fields: FieldMap, columns: Set<String>): FieldMap = fields.filterKeys { it in columns }.mapValues { (_, value) ->
            when (value) {
                is String -> value
                is Number -> {
                    // Moshi's Any adapter decodes cursor integer values as Double.
                    val number = value.toDouble()
                    require(number.isFinite() && number == value.toLong().toDouble()) { "Invalid contact integer" }
                    value.toLong()
                }

                else -> throw IllegalArgumentException("Unsupported contact field type")
            }
        }

        internal fun buildOperations(contact: ContactRestoreData): ArrayList<ContentProviderOperation> {
            val operations = arrayListOf(
                ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                    .withValues(contentValues(contact.rawContact))
                    .withValue(RawContacts.ACCOUNT_NAME, null)
                    .withValue(RawContacts.ACCOUNT_TYPE, null)
                    .build()
            )
            contact.data.forEach { row ->
                operations.add(
                    ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValues(contentValues(row))
                        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                        .build()
                )
            }
            return operations
        }

        private fun contentValues(fields: FieldMap): ContentValues = ContentValues().apply {
            fields.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Long -> put(key, value)
                    else -> error("Unsupported prepared contact field")
                }
            }
        }
    }
}
