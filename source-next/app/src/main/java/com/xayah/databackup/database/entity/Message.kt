package com.xayah.databackup.database.entity

import android.provider.Telephony
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.room.Entity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.App
import com.xayah.databackup.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object MessageConstant {
    const val APPLICATION_SMIL = "application/smil"
    const val TEXT_PLAIN = "text/plain"
    const val INVALID_ADDR = "insert-address-token"
}

@JsonClass(generateAdapter = true)
@Entity(tableName = "messages_sms", primaryKeys = ["id"])
data class Sms(
    var id: Long,
    var config: String?, // JSON
    var selected: Boolean,
)

data class SmsDeserialized(
    var id: Long,
    var config: FiledMap,
    var selected: Boolean,
) {
    val address: String by lazy {
        config.getOrDefault(Telephony.Sms.ADDRESS, App.application.getString(R.string.unknown)).toString()
    }

    val body: String by lazy {
        config.getOrDefault(Telephony.Sms.BODY, App.application.getString(R.string.unknown)).toString()
    }
}

fun Flow<List<Sms>>.deserializeSms(): Flow<List<SmsDeserialized>> = map { flow ->
    val moshi: Moshi = Moshi.Builder().build()
    flow.map {
        val config = it.config?.let { json -> moshi.adapter<FiledMap>().fromJson(json) }
        SmsDeserialized(
            id = it.id,
            config = config ?: mapOf(),
            selected = it.selected
        )
    }
}

@JsonClass(generateAdapter = true)
@Entity(tableName = "messages_mms", primaryKeys = ["id"])
data class Mms(
    var id: Long,
    var pdu: String?,  // JSON
    var addr: String?, // JSON
    var part: String?, // JSON
    var selected: Boolean,
)

data class MmsDeserialized(
    var id: Long,
    var pdu: FiledMap,
    var addr: List<FiledMap>,
    var part: List<FiledMap>,
    var selected: Boolean,
) {
    val iconMod = "[ICON]"

    val address: String by lazy {
        addr.forEach { p ->
            val addr = p.getOrDefault(Telephony.Mms.Addr.ADDRESS, "").toString()
            if (addr.isNotEmpty() && addr != MessageConstant.INVALID_ADDR) {
                return@lazy addr
            }
        }
        return@lazy App.application.getString(R.string.unknown)
    }

    val body: AnnotatedString by lazy {
        buildAnnotatedString {
            part.forEach { p ->
                val contentType = p.getOrDefault(Telephony.Mms.Part.CONTENT_TYPE, "").toString()
                if (contentType != MessageConstant.TEXT_PLAIN && contentType != MessageConstant.APPLICATION_SMIL) {
                    appendInlineContent(id = iconMod)
                } else if (contentType == MessageConstant.TEXT_PLAIN) {
                    val text = p.getOrDefault(Telephony.Mms.Part.TEXT, App.application.getString(R.string.unknown)).toString()
                    append(text)
                }
            }
        }
    }
}

fun Flow<List<Mms>>.deserializeMms(): Flow<List<MmsDeserialized>> = map { flow ->
    val moshi: Moshi = Moshi.Builder().build()
    flow.map {
        val pdu = it.pdu?.let { json -> moshi.adapter<FiledMap>().fromJson(json) }
        val addr = it.addr?.let { json -> moshi.adapter<List<FiledMap>>().fromJson(json) }
        val part = it.part?.let { json -> moshi.adapter<List<FiledMap>>().fromJson(json) }
        MmsDeserialized(
            id = it.id,
            pdu = pdu ?: mapOf(),
            addr = addr ?: listOf(),
            part = part ?: listOf(),
            selected = it.selected
        )
    }
}
