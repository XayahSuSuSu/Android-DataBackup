package com.xayah.databackup.service.restore

import android.content.ClipDescription
import android.provider.Telephony
import android.telephony.SubscriptionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.database.entity.MessageConstant
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.service.restore.MessageRestorePreparer.prepare

/**
 * Prepares backup messages for restoration without writing to providers or accessing attachments.
 * Keeps portable provider columns, drops source identifiers and applies restore defaults.
 * Prepared records contain only validated String and Long field values and should be obtained through [prepare].
 */
internal object MessageRestorePreparer {
    data class SmsRecord(val values: FieldMap)
    data class Part(val values: FieldMap, val attachmentPath: String?)
    data class MmsRecord(val values: FieldMap, val addresses: List<FieldMap>, val parts: List<Part>)
    data class PreparedMessages(
        val sms: Map<String, SmsRecord>,
        val mms: Map<String, MmsRecord>,
        val skippedIds: Set<String>,
        val failures: Map<String, Exception>,
    )

    private val mMoshi = Moshi.Builder().build()
    private val mSmsAdapter = mMoshi.adapter<List<Sms>>()
    private val mMmsAdapter = mMoshi.adapter<List<Mms>>()
    private val mFieldsAdapter = mMoshi.adapter<FieldMap>()
    private val mRowsAdapter = mMoshi.adapter<List<FieldMap>>()
    private val mMessageKeyPattern = Regex("(sms|mms):(0|[1-9][0-9]*)")

    private val mSmsStrings = setOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.SUBJECT, Telephony.Sms.SERVICE_CENTER)
    private val mSmsNumbers = setOf(
        Telephony.Sms.DATE, Telephony.Sms.DATE_SENT, Telephony.Sms.TYPE, Telephony.Sms.READ, Telephony.Sms.SEEN,
        Telephony.Sms.STATUS, Telephony.Sms.LOCKED, Telephony.Sms.PROTOCOL, Telephony.Sms.REPLY_PATH_PRESENT
    )
    private val mMmsStrings = setOf(
        Telephony.Mms.SUBJECT, Telephony.Mms.CONTENT_TYPE, Telephony.Mms.CONTENT_LOCATION, Telephony.Mms.MESSAGE_CLASS,
        Telephony.Mms.MESSAGE_ID, Telephony.Mms.TRANSACTION_ID, Telephony.Mms.RESPONSE_TEXT, Telephony.Mms.RETRIEVE_TEXT
    )
    private val mMmsNumbers = setOf(
        Telephony.Mms.DATE, Telephony.Mms.DATE_SENT, Telephony.Mms.MESSAGE_BOX, Telephony.Mms.READ, Telephony.Mms.SEEN,
        Telephony.Mms.LOCKED, Telephony.Mms.SUBJECT_CHARSET, Telephony.Mms.MESSAGE_TYPE, Telephony.Mms.MMS_VERSION,
        Telephony.Mms.MESSAGE_SIZE, Telephony.Mms.EXPIRY, Telephony.Mms.PRIORITY, Telephony.Mms.READ_REPORT,
        Telephony.Mms.REPORT_ALLOWED, Telephony.Mms.RESPONSE_STATUS, Telephony.Mms.STATUS, Telephony.Mms.RETRIEVE_STATUS,
        Telephony.Mms.RETRIEVE_TEXT_CHARSET, Telephony.Mms.READ_STATUS, Telephony.Mms.CONTENT_CLASS,
        Telephony.Mms.DELIVERY_REPORT, Telephony.Mms.DELIVERY_TIME
    )
    private val mPartStrings = setOf(
        Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part.NAME, Telephony.Mms.Part.FILENAME, Telephony.Mms.Part.CONTENT_DISPOSITION,
        Telephony.Mms.Part.CONTENT_ID, Telephony.Mms.Part.CONTENT_LOCATION, Telephony.Mms.Part.TEXT
    )
    private val mPartNumbers = setOf(Telephony.Mms.Part.SEQ, Telephony.Mms.Part.CHARSET)

    val inlineTypes = setOf(ClipDescription.MIMETYPE_TEXT_PLAIN, ClipDescription.MIMETYPE_TEXT_HTML, MessageConstant.APP_SMIL)

    /**
     * Validates selected records before any provider writes. Malformed selected records are returned in [PreparedMessages.failures].
     * Unsent messages, notification/report PDUs and incomplete MMS parts are returned in [PreparedMessages.skippedIds].
     * Attachment paths are validated here; snapshot membership and file contents are checked by the restore helper.
     */
    fun prepare(smsJson: String?, mmsJson: String?, ids: List<String>): PreparedMessages {
        require(ids.isNotEmpty()) { "No messages selected" }
        val selectedIds = ids.distinct().associateWith { id ->
            require(mMessageKeyPattern.matches(id)) { "Invalid message key: $id" }
            requireNotNull(id.substringAfter(':').toIntOrNull()) { "Invalid message index: $id" }
        }
        val sms = if (selectedIds.keys.any { it.startsWith("sms:") }) {
            requireNotNull(mSmsAdapter.fromJson(requireNotNull(smsJson) { "Missing SMS metadata" })) { "Missing SMS records" }
        } else emptyList()
        val mms = if (selectedIds.keys.any { it.startsWith("mms:") }) {
            requireNotNull(mMmsAdapter.fromJson(requireNotNull(mmsJson) { "Missing MMS metadata" })) { "Missing MMS records" }
        } else emptyList()
        val preparedSms = linkedMapOf<String, SmsRecord>()
        val preparedMms = linkedMapOf<String, MmsRecord>()
        val skippedIds = linkedSetOf<String>()
        val failures = linkedMapOf<String, Exception>()
        selectedIds.forEach { (id, index) ->
            runCatching {
                if (id.startsWith("sms:")) {
                    val record = prepareSms(requireNotNull(sms.getOrNull(index)) { "Unknown SMS record" })
                    if (record == null) skippedIds.add(id) else preparedSms[id] = record
                } else {
                    val record = prepareMms(requireNotNull(mms.getOrNull(index)) { "Unknown MMS record" })
                    if (record == null) skippedIds.add(id) else preparedMms[id] = record
                }
            }.onFailure { error ->
                if (error !is Exception) throw error
                failures[id] = error
            }
        }
        return PreparedMessages(preparedSms, preparedMms, skippedIds, failures)
    }

    private fun prepareSms(record: Sms): SmsRecord? {
        val raw = requireNotNull(mFieldsAdapter.fromJson(requireNotNull(record.config) { "Missing SMS config" }))
        val values = sanitize(raw, mSmsStrings, mSmsNumbers).toMutableMap()
        require(values[Telephony.Sms.DATE] is Long && (values.getValue(Telephony.Sms.DATE) as Long) >= 0) { "Invalid SMS date" }
        require(values[Telephony.Sms.TYPE] is Long) { "Missing SMS type" }
        // Import completed messages only: queued/outbox records must not be sent again by a messaging app.
        val completedTypes = setOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toLong(), Telephony.Sms.MESSAGE_TYPE_SENT.toLong())
        if (values[Telephony.Sms.TYPE] !in completedTypes) {
            return null
        }
        values.putIfAbsent(Telephony.Sms.ADDRESS, UNKNOWN_SENDER)
        values.putIfAbsent(Telephony.Sms.BODY, "")
        applyDefaults(values)
        return SmsRecord(values)
    }

    private fun prepareMms(record: Mms): MmsRecord? {
        val raw = requireNotNull(mFieldsAdapter.fromJson(requireNotNull(record.pdu) { "Missing MMS PDU" }))
        val values = sanitize(raw, mMmsStrings, mMmsNumbers).toMutableMap()
        require(values[Telephony.Mms.DATE] is Long && (values.getValue(Telephony.Mms.DATE) as Long) >= 0) { "Invalid MMS date" }
        require(values[Telephony.Mms.MESSAGE_BOX] is Long && values[Telephony.Mms.MESSAGE_TYPE] is Long) { "Missing MMS type" }
        // Send.req and Retrieve.conf contain user content; notification/report PDUs are not complete messages.
        val completedBoxes = setOf(Telephony.Mms.MESSAGE_BOX_INBOX.toLong(), Telephony.Mms.MESSAGE_BOX_SENT.toLong())
        if (values[Telephony.Mms.MESSAGE_BOX] !in completedBoxes ||
            values[Telephony.Mms.MESSAGE_TYPE] !in setOf(MESSAGE_TYPE_SEND_REQ, MESSAGE_TYPE_RETRIEVE_CONF)
        ) {
            return null
        }
        val addresses = prepareAddresses(requireNotNull(record.addr) { "Missing MMS addresses" })
        val parts = prepareParts(requireNotNull(record.part) { "Missing MMS parts" })
        applyDefaults(values)
        val isTextOnly = parts.size <= 2 && parts.all {
            it.values[Telephony.Mms.Part.CONTENT_TYPE] in setOf(ClipDescription.MIMETYPE_TEXT_PLAIN, MessageConstant.APP_SMIL)
        }
        values[Telephony.Mms.TEXT_ONLY] = if (isTextOnly) 1L else 0L
        if (parts.isEmpty() || parts.any { it.values[Telephony.Mms.Part.CONTENT_TYPE] !in inlineTypes && it.attachmentPath == null }) {
            return null
        }
        return MmsRecord(values, addresses, parts)
    }

    private fun prepareAddresses(json: String): List<FieldMap> =
        requireNotNull(mRowsAdapter.fromJson(json)) { "Missing MMS address rows" }.map { row ->
            sanitize(row, setOf(Telephony.Mms.Addr.ADDRESS), setOf(Telephony.Mms.Addr.TYPE, Telephony.Mms.Addr.CHARSET)).also {
                require(it[Telephony.Mms.Addr.ADDRESS] is String) { "Invalid MMS address field: ${Telephony.Mms.Addr.ADDRESS}" }
                require(it[Telephony.Mms.Addr.TYPE] in setOf(ADDRESS_TYPE_BCC, ADDRESS_TYPE_CC, ADDRESS_TYPE_FROM, ADDRESS_TYPE_TO)) {
                    "Invalid MMS address field: ${Telephony.Mms.Addr.TYPE}"
                }
            }
        }

    private fun prepareParts(json: String): List<Part> =
        requireNotNull(mRowsAdapter.fromJson(json)) { "Missing MMS part rows" }.map { row ->
            val fields = sanitize(row, mPartStrings, mPartNumbers)
            require((fields[Telephony.Mms.Part.CONTENT_TYPE] as? String)?.isNotBlank() == true) {
                "Missing MMS content type: ${Telephony.Mms.Part.CONTENT_TYPE}"
            }
            val attachmentPath = row[Telephony.Mms.Part._DATA]?.let {
                require(it is String) { "Invalid MMS attachment field: ${Telephony.Mms.Part._DATA}" }
                it.takeIf(String::isNotBlank)
            }
            if (attachmentPath != null) validateAttachmentPath(attachmentPath)
            Part(fields, attachmentPath)
        }

    /** Matches Messaging's PduPersister fallback when the backup does not contain the source self number. */
    fun recipients(message: MmsRecord): Set<String> {
        fun addresses(types: Set<Long>) =
            message.addresses.filter { it[Telephony.Mms.Addr.TYPE] in types }.map { it.getValue(Telephony.Mms.Addr.ADDRESS) as String }
                .filter { it.isNotBlank() && it != MessageConstant.FROM_INSERT_ADDRESS_TOKEN_STR }.toSet()
        if (message.values[Telephony.Mms.MESSAGE_TYPE] == MESSAGE_TYPE_SEND_REQ) {
            return addresses(setOf(ADDRESS_TYPE_TO, ADDRESS_TYPE_CC, ADDRESS_TYPE_BCC))
        }
        val sender = addresses(setOf(ADDRESS_TYPE_FROM))
        val toCc = addresses(setOf(ADDRESS_TYPE_TO, ADDRESS_TYPE_CC))
        // With no source self number, one TO/CC address is assumed to be self; a group retains all participants.
        return sender + if (toCc.size == 1) emptySet() else toCc
    }

    fun validateAttachmentPath(path: String) {
        require(path.startsWith('/') && '\u0000' !in path && path.split('/').drop(1).all { it.isNotEmpty() && it != "." && it != ".." }) {
            "Invalid MMS attachment path"
        }
    }

    private fun applyDefaults(values: MutableMap<String, Any>) {
        // SMS and MMS share these column names.
        values.putIfAbsent(Telephony.Sms.READ, 1L)
        values.putIfAbsent(Telephony.Sms.SEEN, 1L)
        // The current backup has no source self-number mapping, so source sub_id is not portable.
        values[Telephony.Sms.SUBSCRIPTION_ID] = SubscriptionManager.INVALID_SUBSCRIPTION_ID.toLong()
    }

    /**
     * Keeps only allowlisted provider fields and validates their value types.
     * Text values remain unchanged; numeric values must be finite integers within the accepted Long range
     * and are normalized to Long. Required fields and message-specific rules are checked by the caller.
     *
     * @param row Source fields parsed from backup JSON; not modified.
     * @param strings Allowed columns whose values must be String.
     * @param numbers Allowed columns whose values must be Number and pass integer validation.
     * @return A new map containing only allowed fields with String or Long values.
     * @throws IllegalArgumentException If an allowed field has an invalid type or numeric value.
     */
    private fun sanitize(row: FieldMap, strings: Set<String>, numbers: Set<String>): FieldMap =
        row.filterKeys { it in strings || it in numbers }.mapValues { (key, value) ->
            if (key in strings) {
                require(value is String) { "Invalid message text field: $key" }
                value
            } else {
                require(value is Number) { "Invalid message numeric field: $key" }
                val number = value.toDouble()
                require(number.isFinite() && number >= Long.MIN_VALUE.toDouble() && number < Long.MAX_VALUE.toDouble() &&
                        number == value.toLong().toDouble()) { "Invalid message integer field: $key" }
                value.toLong()
            }
        }

    // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:packages/providers/TelephonyProvider/src/com/android/providers/telephony/TelephonyBackupAgent.java;l=166
    const val UNKNOWN_SENDER = "\u02bcUNKNOWN_SENDER!\u02bc"

    // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:frameworks/opt/telephony/src/java/com/google/android/mms/pdu/PduHeaders.java
    private const val ADDRESS_TYPE_BCC = 0x81L
    private const val ADDRESS_TYPE_CC = 0x82L
    private const val ADDRESS_TYPE_FROM = 0x89L
    private const val ADDRESS_TYPE_TO = 0x97L
    private const val MESSAGE_TYPE_SEND_REQ = 0x80L
    private const val MESSAGE_TYPE_RETRIEVE_CONF = 0x84L
}
