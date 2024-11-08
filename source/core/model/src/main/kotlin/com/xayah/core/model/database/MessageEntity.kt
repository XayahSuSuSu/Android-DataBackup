package com.xayah.core.model.database

import com.xayah.core.model.MMSMessageBox
import com.xayah.core.model.MessageType
import com.xayah.core.model.SMSMessageBox
import kotlinx.serialization.Serializable

data class MMSExtra(
    val id: Long,
    val contentClass: Long,
    val contentLocation: String,
    val contentType: String,
    val deliveryReport: Long,
    val deliveryTime: Long,
    val expiry: Long,
    val messageBox: MMSMessageBox,
    val messageClass: String,
    val messageId: String,
    val messageSize: Long,
    val messageType: Long,
    val mmsVersion: Long,
    val priority: Long,
    val readReport: Long,
    val readStatus: Long,
    val reportAllowed: Long,
    val responseStatus: Long,
    val responseText: String,
    val retrieveStatus: Long,
    val retrieveText: String,
    val retrieveTextCharset: Long,
    val subjectCharset: Long,
    val textOnly: Long,
    val threadId: Long,
    val transactionId: String,

    // Mms.Part
    val filename: String,
)

data class SMSExtra(
    val creator: String,
    val errorCode: Long,
    val person: Long,
    val protocol: Long,
    val replyPathPresent: Long,
    val serviceCenter: String,
    val type: SMSMessageBox,
)

@Serializable
data class MessageInfo(
    val messageInfoType: MessageType,
    val address: String,
    val body: String, // SMS: body, MMS: text
    val date: Long,
    val dateSent: Long,
    val locked: Long,
    val read: Long,
    val seen: Long,
    val status: Long,
    val subject: String,
    val subscriptionId: Long,
)
