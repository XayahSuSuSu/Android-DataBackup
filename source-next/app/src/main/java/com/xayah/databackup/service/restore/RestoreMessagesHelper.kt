package com.xayah.databackup.service.restore

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.provider.Telephony
import android.security.keystore.KeyProperties
import android.system.Os
import android.system.OsConstants
import androidx.annotation.WorkerThread
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.data.rustic.RusticBackupManifest
import com.xayah.databackup.data.rustic.RusticSourceCategory
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.rootservice.RootContentResolver
import com.xayah.databackup.service.restore.MessageRestorePreparer.MmsRecord
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.libnative.Rustic
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

/**
 * Restores selected SMS and MMS records from a Rustic snapshot through Telephony Provider.
 * Must run in the root process with the caller's Binder identity cleared.
 * Validates metadata with [MessageRestorePreparer] before writing and restores only manifest-listed attachments.
 * Creates destination message records, resolves conversation threads and writes attachments through provider URIs.
 * Skips duplicates and records identified as unrestorable, returning their inventory keys on success.
 * On failure, attempts to remove the current partial MMS; previously restored messages remain intact.
 *
 * @see [TelephonyBackupAgent.java](https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:packages/providers/TelephonyProvider/src/com/android/providers/telephony/TelephonyBackupAgent.java)
 * @see [PduPersister.java](https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:frameworks/base/telephony/common/com/google/android/mms/pdu/PduPersister.java)
 */
internal class RestoreMessagesHelper(context: Context, private val mCacheDir: File) {
    companion object {
        private const val TAG = "RestoreMessagesHelper"
        private const val MMS_ADDR_PATH = "addr"
        private const val MMS_PART_PATH = "part"
    }

    private val mResolver = RootContentResolver(context)
    private val mConversationContext = object : ContextWrapper(context) {
        override fun getContentResolver() = mResolver
    }

    @WorkerThread
    fun restore(repositoryPath: String, password: String, snapshotId: String, messageIds: List<String>): List<String> {
        require(snapshotId.matches(Regex("[0-9a-fA-F]{64}"))) { "A full snapshot ID is required" }
        require(messageIds.isNotEmpty()) { "No messages selected" }
        fun getMetadataPath(relative: String) = PathHelper.getRusticSnapshotMetadataFilePath(relative)
        val smsPath = getMetadataPath(PathHelper.getBackupMessagesSmsConfigFileRelativePath())
        val mmsPath = getMetadataPath(PathHelper.getBackupMessagesMmsConfigFileRelativePath())
        val manifestPath = getMetadataPath(PathHelper.getRusticManifestFileRelativePath())
        val paths = buildList {
            if (messageIds.any { it.startsWith("sms:") }) add(smsPath)
            if (messageIds.any { it.startsWith("mms:") }) { add(mmsPath); add(manifestPath) }
        }
        val moshi = Moshi.Builder().build()
        val files = requireNotNull(moshi.adapter<Map<String, String>>().fromJson(
            Rustic.readSnapshotTextFiles(repositoryPath, password, snapshotId, paths)
        ))
        // Validate all selected metadata before any provider writes.
        val prepared = MessageRestorePreparer.prepare(files[smsPath], files[mmsPath], messageIds)
        val includedAttachments = if (messageIds.any { it.startsWith("mms:") }) {
            val manifest = requireNotNull(moshi.adapter<RusticBackupManifest>().fromJson(requireNotNull(files[manifestPath])))
            require(manifest.schemaVersion == RusticBackupManifest.CURRENT_SCHEMA_VERSION) { "Unsupported backup schema" }
            manifest.included.filter { it.category == RusticSourceCategory.MmsAttachment }.map { it.path }.toSet()
        } else emptySet()
        val skipped = prepared.skippedIds.toMutableSet()
        val restorableMms = prepared.mms.filter { (id, record) ->
            val complete = record.parts.all { it.attachmentPath == null || it.attachmentPath in includedAttachments }
            if (!complete) skipped.add(id)
            complete
        }
        val cacheDir = PathHelper.getCacheDir(PathHelper.CACHE_SUBDIR_RESTORE_MESSAGES, mCacheDir)
        val staging = File(cacheDir, UUID.randomUUID().toString())
        check(staging.mkdir()) { "Failed to create message staging directory: $staging" }
        val result = runCatching {
            // Restore only manifest-listed snapshot attachments, never the live source-device paths.
            val attachments = restorableMms.values.flatMap { it.parts }.mapNotNull { it.attachmentPath }.distinct()
                .mapIndexed { index, path ->
                    val file = File(staging, index.toString())
                    Rustic.restoreSnapshot(repositoryPath, password, "$snapshotId:$path", file.path, Rustic.RestoreOptions(noOwnership = true))
                    check(OsConstants.S_ISREG(Os.lstat(file.path).st_mode)) { "MMS attachment is not a regular file" }
                    path to file
                }.toMap()
            mResolver.allowTemporaryMessageWrites().use {
                prepared.sms.forEach { (id, record) ->
                    if (smsExists(record.values)) {
                        skipped.add(id)
                    } else {
                        val values = contentValues(record.values)
                        val address = (record.values[Telephony.Sms.ADDRESS] as String).ifBlank { MessageRestorePreparer.UNKNOWN_SENDER }
                        values.put(Telephony.Sms.THREAD_ID, getOrCreateThreadId(setOf(address)))
                        val inserted = checkNotNull(mResolver.insert(Telephony.Sms.CONTENT_URI, values)) { "SMS insertion failed" }
                        check(ContentUris.parseId(inserted) > 0) { "SMS insertion was rejected" }
                    }
                }
                restorableMms.forEach { (id, record) ->
                    if (mmsExists(record, attachments)) {
                        skipped.add(id)
                    } else {
                        insertMms(record, attachments)
                    }
                }
            }
        }

        runCatching {
            deleteStaging(staging)
        }.onFailure { cleanup ->
            result.exceptionOrNull()?.addSuppressed(cleanup) ?: throw cleanup
        }

        result.getOrThrow()
        return messageIds.distinct().filter { it in skipped }
    }

    private fun getOrCreateThreadId(recipients: Set<String>): Long =
        Telephony.Threads.getOrCreateThreadId(mConversationContext, recipients.ifEmpty { setOf(MessageRestorePreparer.UNKNOWN_SENDER) })

    private fun smsExists(values: FieldMap): Boolean {
        // Compare address and type as well as date and body to avoid treating messages
        // with different senders, recipients or message types as duplicates.
        val columns = listOf(Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.ADDRESS, Telephony.Sms.TYPE)
        return checkNotNull(
            mResolver.query(
                Telephony.Sms.CONTENT_URI, arrayOf(Telephony.Sms._ID), columns.joinToString(" AND ") { "$it = ?" },
                columns.map { values.getValue(it).toString() }.toTypedArray(), null
            )
        ) { "SMS duplicate query failed" }.use { it.moveToFirst() }
    }

    private fun mmsExists(record: MmsRecord, attachments: Map<String, File>): Boolean {
        // Compare addresses, part metadata and attachment hashes to distinguish MMS messages
        // with the same date and text but different recipients or attachments.
        checkNotNull(
            mResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf(Telephony.Mms._ID, Telephony.Mms.SUBJECT, Telephony.Mms.MESSAGE_TYPE, Telephony.Mms.MESSAGE_BOX),
                "${Telephony.Mms.DATE} = ?",
                arrayOf(record.values.getValue(Telephony.Mms.DATE).toString()), null
            )
        ) { "MMS duplicate query failed" }.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Mms._ID)
            val subjectIndex = cursor.getColumnIndexOrThrow(Telephony.Mms.SUBJECT)
            val messageTypeIndex = cursor.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_TYPE)
            val messageBoxIndex = cursor.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX)
            while (cursor.moveToNext()) {
                if (cursor.getString(subjectIndex).orEmpty() != (record.values[Telephony.Mms.SUBJECT] as? String).orEmpty() ||
                    cursor.getLong(messageTypeIndex) != record.values[Telephony.Mms.MESSAGE_TYPE] ||
                    cursor.getLong(messageBoxIndex) != record.values[Telephony.Mms.MESSAGE_BOX]
                ) {
                    continue
                }
                val uri = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, cursor.getLong(idIndex))
                val addresses = queryRows(
                    Uri.withAppendedPath(uri, MMS_ADDR_PATH),
                    listOf(Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.TYPE, Telephony.Mms.Addr.CHARSET),
                )
                if (addresses.map(::buildAddressKey).sorted() != record.addresses.map(::buildAddressKey).sorted()) {
                    continue
                }
                val columns = listOf(
                    Telephony.Mms.Part._ID, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part.SEQ, Telephony.Mms.Part.CHARSET,
                    Telephony.Mms.Part.NAME, Telephony.Mms.Part.FILENAME, Telephony.Mms.Part.CONTENT_DISPOSITION,
                    Telephony.Mms.Part.CONTENT_ID, Telephony.Mms.Part.CONTENT_LOCATION, Telephony.Mms.Part.TEXT,
                )
                val parts = queryRows(Uri.withAppendedPath(uri, MMS_PART_PATH), columns).toMutableList()
                if (parts.size != record.parts.size) {
                    continue
                }
                val matches = record.parts.all { part ->
                    val match = parts.indexOfFirst { row ->
                        val metadataMatches = columns.filterNot { it == Telephony.Mms.Part._ID }.all { column ->
                            row[column]?.toString().orEmpty() == part.values[column]?.toString().orEmpty()
                        }
                        metadataMatches && (part.attachmentPath == null ||
                                checkNotNull(
                                    mResolver.openInputStream(
                                        Uri.withAppendedPath(
                                            Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, MMS_PART_PATH),
                                            row.getValue(Telephony.Mms.Part._ID).toString(),
                                        )
                                    )
                                )
                                .use { input ->
                                    attachments.getValue(part.attachmentPath).inputStream().use {
                                        calculateSha256(input).contentEquals(calculateSha256(it))
                                    }
                                })
                    }
                    if (match < 0) false else { parts.removeAt(match); true }
                }
                if (matches) return true
            }
        }
        return false
    }

    private fun insertMms(record: MmsRecord, attachments: Map<String, File>) {
        // UriMatcher's numeric segments require positive IDs. Reserve an unused placeholder before writing.
        var placeholder: Long
        var partsUri: Uri
        do {
            placeholder = (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).coerceAtLeast(1)
            partsUri = Uri.withAppendedPath(ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, placeholder), MMS_PART_PATH)
        } while (queryRows(partsUri, listOf(Telephony.Mms.Part._ID)).isNotEmpty() ||
            queryRows(ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, placeholder), listOf(Telephony.Mms._ID)).isNotEmpty()
        )
        var messageUri: Uri? = null
        runCatching {
            val values = contentValues(record.values)
            values.put(Telephony.Mms.THREAD_ID, getOrCreateThreadId(MessageRestorePreparer.recipients(record)))
            record.parts.forEach { part ->
                val partUri = checkNotNull(mResolver.insert(partsUri, contentValues(part.values))) { "MMS part insertion failed" }
                check(ContentUris.parseId(partUri) > 0) { "MMS part insertion was rejected" }
                part.attachmentPath?.let { path ->
                    attachments.getValue(path).inputStream().use { input ->
                        checkNotNull(mResolver.openOutputStream(partUri)) { "Cannot open MMS part" }.use { output -> input.copyTo(output) }
                    }
                }
            }
            val inserted = checkNotNull(mResolver.insert(Telephony.Mms.CONTENT_URI, values)) { "MMS insertion failed" }
            check(ContentUris.parseId(inserted) > 0) { "MMS insertion was rejected" }
            messageUri = inserted
            val id = ContentUris.parseId(inserted)
            check(mResolver.update(partsUri, ContentValues().apply { put(Telephony.Mms.Part.MSG_ID, id) }, null, null) == record.parts.size) {
                "MMS part association failed"
            }
            record.addresses.forEach { address ->
                checkNotNull(mResolver.insert(Uri.withAppendedPath(inserted, MMS_ADDR_PATH), contentValues(address))) {
                    "MMS address insertion failed"
                }
            }
            // The PDU insert precedes the final address/link updates; refresh observers after completion too.
            mResolver.notifyChange(Telephony.MmsSms.CONTENT_URI, null)
        }.onFailure { failure ->
            // Provider deletes cascade to address/part rows and provider-owned files. Earlier messages remain intact.
            messageUri?.let { uri -> runCatching { mResolver.delete(uri, null, null) }.exceptionOrNull()?.let(failure::addSuppressed) }
            runCatching { mResolver.delete(partsUri, null, null) }.exceptionOrNull()?.let(failure::addSuppressed)
            LogHelper.e(TAG, "insertMms", "", failure)
            throw IllegalStateException("MMS restore failed; some messages may already have been restored")
        }
    }

    private fun queryRows(uri: Uri, columns: List<String>): List<FieldMap> =
        checkNotNull(mResolver.query(uri, columns.toTypedArray(), null, null, null)) { "MMS query failed" }.use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(buildMap {
                    columns.forEachIndexed { index, column ->
                        if (!cursor.isNull(index)) put(column, cursor.getString(index))
                    }
                })
            }
        }

    private fun buildAddressKey(row: FieldMap): String =
        listOf(Telephony.Mms.Addr.TYPE, Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.CHARSET).joinToString("\u0000") {
            row[it]?.toString().orEmpty()
        }

    private fun contentValues(fields: FieldMap) = ContentValues().apply {
        fields.forEach { (key, value) ->
            when (value) {
                is String -> put(key, value)
                is Long -> put(key, value)
                else -> error("Invalid prepared message field")
            }
        }
    }

    private fun calculateSha256(input: InputStream): ByteArray {
        val digest = MessageDigest.getInstance(KeyProperties.DIGEST_SHA256)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val size = input.read(buffer)
            if (size < 0) break
            digest.update(buffer, 0, size)
        }
        return digest.digest()
    }

    private fun deleteStaging(file: File) {
        if (OsConstants.S_ISDIR(Os.lstat(file.path).st_mode)) {
            checkNotNull(file.listFiles()).forEach(::deleteStaging)
        }
        check(file.delete()) { "Cannot clean message staging files" }
    }
}
