package com.xayah.databackup.service.restore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.provider.CallLog.Calls
import androidx.annotation.WorkerThread
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Restores selected Rustic call logs through CallLogProvider for the app's current user.
 * Requires READ_CALL_LOG and WRITE_CALL_LOG. Validates all selected metadata before the first write.
 * The caller must serialize restores because duplicate checks and inserts are separate provider operations.
 * Skips duplicate and unsupported records, returning their inventory keys in selection order.
 * Inserts new rows without replacing existing calls; earlier inserts are not rolled back on failure.
 *
 * @see [CallLogBackupAgent.java](https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:packages/providers/CallLogProvider/src/com/android/calllogbackup/CallLogBackupAgent.java)
 * @see [CallLogProvider.java](https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:packages/providers/ContactsProvider/src/com/android/providers/contacts/CallLogProvider.java)
 */
internal class RestoreCallLogsHelper(private val resolver: ContentResolver) {
    @WorkerThread
    suspend fun restore(serialized: String, path: String, callLogIds: List<String>): List<String> {
        // Validate every selected record before the first write. Do not expose call log JSON in errors.
        val callLogs = runCatching {
            val files = requireNotNull(Moshi.Builder().build().adapter<Map<String, String>>().fromJson(serialized))
            CallLogRestorePreparer.prepare(requireNotNull(files[path]), callLogIds)
        }.getOrElse { e ->
            LogHelper.e(TAG, "restore", "", e)
            throw IllegalArgumentException("Invalid call logs backup or selection")
        }
        val skipped = mutableListOf<String>()
        for ((id, callLog) in callLogs) {
            currentCoroutineContext().ensureActive()
            if (callLog == null) {
                skipped.add(id)
                continue
            }
            runCatching {
                if (callLogExists(callLog)) {
                    skipped.add(id)
                } else {
                    val inserted = checkNotNull(resolver.insert(Calls.CONTENT_URI, contentValues(callLog))) { "Call log insertion failed" }
                    check(ContentUris.parseId(inserted) > 0) { "Call log insertion was rejected" }
                }
            }.onFailure {
                LogHelper.e(TAG, "restore", "", it)
                throw IllegalStateException("Call logs restore failed; some calls may already have been restored")
            }
        }
        return skipped
    }

    private fun callLogExists(fields: FieldMap): Boolean {
        // Compare date, number, type, duration and presentation to distinguish calls with the same
        // timestamp and number, including calls whose number is empty or withheld.
        val columns = listOf(Calls.DATE, Calls.NUMBER, Calls.TYPE, Calls.DURATION, Calls.NUMBER_PRESENTATION)
        return checkNotNull(
            resolver.query(
                Calls.CONTENT_URI, arrayOf(Calls._ID), columns.joinToString(" AND ") { "$it = ?" },
                columns.map { fields.getValue(it).toString() }.toTypedArray(), null,
            )
        ) { "Call log duplicate query failed" }.use { it.moveToFirst() }
    }

    companion object {
        private const val TAG = "RestoreCallLogsHelper"

        private fun contentValues(fields: FieldMap): ContentValues = ContentValues().apply {
            fields.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Long -> put(key, value)
                    else -> error("Unsupported prepared call log field")
                }
            }
        }
    }
}
