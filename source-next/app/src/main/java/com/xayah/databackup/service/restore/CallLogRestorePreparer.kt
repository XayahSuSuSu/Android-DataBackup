package com.xayah.databackup.service.restore

import android.os.Build
import android.provider.CallLog.Calls
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.FieldMap

/**
 * Parses selected call log records from backup metadata and prepares fields for the target Android version.
 * Validates restorable fields, omits source IDs and device-local references, and applies restore defaults.
 * Returns successful null results for voicemail and unsupported call types; does not read or write provider data.
 */
internal object CallLogRestorePreparer {
    private val mMoshi = Moshi.Builder().build()
    private val mRecordsAdapter = mMoshi.adapter<List<CallLog>>()
    private val mFieldsAdapter = mMoshi.adapter<FieldMap>()
    private val mKeyPattern = Regex("call:(0|[1-9][0-9]*)")

    /**
     * Prepares distinct selected records in selection order, keyed by their inventory IDs.
     * Successful results contain null for voicemail and call types unsupported by the target Android version.
     * Invalid selections or malformed fields are captured as failures for the affected records.
     */
    fun prepare(content: String, ids: List<String>): Map<String, Result<FieldMap?>> {
        require(ids.isNotEmpty()) { "No call logs selected" }
        val records = requireNotNull(mRecordsAdapter.fromJson(content)) { "Missing call log records" }
        return ids.distinct().associateWith { id ->
            runCatching {
                require(mKeyPattern.matches(id)) { "Invalid call log key" }
                val index = requireNotNull(id.substringAfter(':').toIntOrNull()) { "Invalid call log index" }
                val record = requireNotNull(records.getOrNull(index)) { "Unknown call log record" }
                val fields = requireNotNull(mFieldsAdapter.fromJson(requireNotNull(record.call) { "Missing call log JSON" })) { "Missing call log fields" }
                prepareFields(fields)
            }
        }
    }

    private fun prepareFields(fields: FieldMap): FieldMap? {
        val type = sanitizeInteger(fields[Calls.TYPE], Calls.TYPE)
        // Skip voicemail entries because the backup contains call metadata but no voicemail audio.
        val types = setOf(Calls.INCOMING_TYPE, Calls.OUTGOING_TYPE, Calls.MISSED_TYPE, Calls.REJECTED_TYPE, Calls.BLOCKED_TYPE)
        if (type !in types.map(Int::toLong) &&
            !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && type == Calls.ANSWERED_EXTERNALLY_TYPE.toLong())
        ) {
            return null
        }

        // Keep portable fields only; omit source account associations, contact references,
        // voicemail/composer URIs, migration flags and UUIDs.
        val strings = buildSet {
            addAll(setOf(Calls.NUMBER, Calls.POST_DIAL_DIGITS, Calls.VIA_NUMBER))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // https://cs.android.com/android/platform/superproject/+/android-10.0.0_r1:frameworks/base/core/java/android/provider/CallLog.java;l=583
                add(Calls.CALL_SCREENING_APP_NAME)
                // https://cs.android.com/android/platform/superproject/+/android-10.0.0_r1:frameworks/base/core/java/android/provider/CallLog.java;l=574
                add(Calls.CALL_SCREENING_COMPONENT_NAME)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r1:frameworks/base/core/java/android/provider/CallLog.java;l=1392
                add(Calls.SUBJECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                // https://cs.android.com/android/platform/superproject/+/android-15.0.0_r1:frameworks/base/core/java/android/provider/CallLog.java;l=1012
                add(Calls.ASSERTED_DISPLAY_NAME)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                // https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:packages/providers/ContactsProvider/src/com/android/providers/contacts/CallLogProvider.java;l=270
                add(Calls.PREFERRED_DISPLAY_NAME)
            }
        }
        val numbers = buildSet {
            addAll(setOf(Calls.DATE, Calls.DURATION, Calls.TYPE, Calls.NUMBER_PRESENTATION, Calls.FEATURES, Calls.DATA_USAGE))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // https://cs.android.com/android/platform/superproject/+/android-10.0.0_r1:frameworks/base/core/java/android/provider/CallLog.java;l=604
                add(Calls.BLOCK_REASON)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r1:frameworks/base/core/java/android/provider/CallLog.java;l=1383
                add(Calls.MISSED_REASON)
                // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r1:frameworks/base/core/java/android/provider/CallLog.java;l=1418
                add(Calls.PRIORITY)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                // https://cs.android.com/android/platform/superproject/+/android-15.0.0_r1:frameworks/base/core/java/android/provider/CallLog.java;l=1006
                add(Calls.IS_BUSINESS_CALL)
            }
        }
        val values = fields.filterKeys { it in strings || it in numbers }.mapValues { (key, value) ->
            if (key in strings) {
                require(value is String) { "Invalid call log text field: $key" }
                value
            } else {
                sanitizeInteger(value, key)
            }
        }.toMutableMap()
        listOf(Calls.DATE, Calls.DURATION).forEach { key ->
            require(values[key] is Long && (values.getValue(key) as Long) >= 0) { "Invalid call log field: $key" }
        }
        buildList {
            add(Calls.FEATURES)
            add(Calls.DATA_USAGE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Calls.BLOCK_REASON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Calls.MISSED_REASON)
        }.forEach { key ->
            values[key]?.let { require((it as Long) >= 0) { "Invalid call log field: $key" } }
        }
        values[Calls.FEATURES]?.let { require((it as Long) <= Int.MAX_VALUE) { "Invalid call log features" } }
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Calls.PRIORITY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) add(Calls.IS_BUSINESS_CALL)
        }.forEach { key ->
            values[key]?.let { require(it == 0L || it == 1L) { "Invalid call log field: $key" } }
        }

        val number = values[Calls.NUMBER] as? String ?: ""
        var presentation = values[Calls.NUMBER_PRESENTATION] as? Long ?: Calls.PRESENTATION_ALLOWED.toLong()
        require(presentation in Calls.PRESENTATION_ALLOWED.toLong()..PRESENTATION_UNAVAILABLE.toLong()) {
            "Invalid call log presentation"
        }
        if (presentation == PRESENTATION_UNAVAILABLE.toLong() && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            presentation = Calls.PRESENTATION_UNKNOWN.toLong()
        }
        if (number.isEmpty() && presentation == Calls.PRESENTATION_ALLOWED.toLong()) {
            presentation = Calls.PRESENTATION_UNKNOWN.toLong()
        }
        // Clear the number when its presentation does not allow it to be displayed.
        values[Calls.NUMBER] = if (presentation == Calls.PRESENTATION_ALLOWED.toLong()) number else ""
        values[Calls.NUMBER_PRESENTATION] = presentation
        // Mark restored calls as read and not new.
        values[Calls.NEW] = 0L
        values[Calls.IS_READ] = 1L
        // Override the provider's default of 1 to keep restored calls specific to this user.
        values[ADD_FOR_ALL_USERS] = 0L
        return values
    }

    private fun sanitizeInteger(value: Any?, key: String): Long {
        require(value is Number) { "Invalid call log numeric field: $key" }
        val number = value.toDouble()
        // Moshi's Any adapter decodes cursor integers as Double; reject fractional and overflowing values.
        require(
            number.isFinite() && number >= Long.MIN_VALUE.toDouble() && number < Long.MAX_VALUE.toDouble() && number == value.toLong().toDouble()
        ) { "Invalid call log integer field: $key" }
        return value.toLong()
    }

    // https://cs.android.com/android/platform/superproject/+/android-14.0.0_r1:frameworks/base/core/java/android/provider/CallLog.java;l=944
    private const val PRESENTATION_UNAVAILABLE = 5

    // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:frameworks/base/core/java/android/provider/CallLog.java;l=458
    private const val ADD_FOR_ALL_USERS = "add_for_all_users"
}
