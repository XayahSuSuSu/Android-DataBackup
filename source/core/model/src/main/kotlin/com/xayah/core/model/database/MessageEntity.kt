package com.xayah.core.model.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.CompressionType
import com.xayah.core.model.MMSMessageBox
import com.xayah.core.model.MessageType
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.SMSMessageBox
import kotlinx.serialization.Serializable

interface BaseMessageExtraInfo

@Serializable
data class MessageIndexInfo(
    var opType: OpType,
    var name: String,
    var compressionType: CompressionType,
    var preserveId: Long,
    var cloud: String,
    var backupDir: String,
    var messageType: MessageType,
)

data class MMSExtraInfo(
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
) : BaseMessageExtraInfo

data class SMSExtraInfo(
    val creator: String,
    val errorCode: Long,
    val person: Long,
    val protocol: Long,
    val replyPathPresent: Long,
    val serviceCenter: String,
    val type: SMSMessageBox,
) : BaseMessageExtraInfo

@Serializable
data class MessageBaseInfo(
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

@Serializable
@Entity
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @ColumnInfo(defaultValue = "0") var processingIndex: Int = 0,
    @Embedded(prefix = "indexInfo_") var indexInfo: MessageIndexInfo,
    @Embedded(prefix = "messageBaseInfo_") var messageBaseInfo: MessageBaseInfo,
    @Embedded(prefix = "messageExtraInfo_") var messageExtraInfo: BaseMessageExtraInfo,
)
