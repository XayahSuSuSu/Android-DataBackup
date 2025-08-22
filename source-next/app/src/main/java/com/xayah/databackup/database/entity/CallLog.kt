package com.xayah.databackup.database.entity

import android.provider.CallLog.Calls
import androidx.room.Entity
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.util.DateUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "call_logs", primaryKeys = ["id"])
data class CallLog(
    var id: Long,
    var call: String?, // JSON
    var selected: Boolean,
)

data class CallLogDeserialized(
    var id: Long,
    var call: FiledMap,
    var selected: Boolean,
) {
    val number: String by lazy {
        call.getOrDefault(Calls.NUMBER, App.application.getString(R.string.unknown)).toString()
    }

    val type: Int by lazy {
        call.getOrDefault(Calls.TYPE, Calls.MISSED_TYPE).toString().toDoubleOrNull()?.toInt() ?: Calls.MISSED_TYPE
    }

    val subscriptionId: Int by lazy {
        call.getOrDefault(Calls.PHONE_ACCOUNT_ID, -1).toString().toDoubleOrNull()?.toInt() ?: -1
    }

    val geocodedLocation: String by lazy {
        call.getOrDefault(
            key = Calls.GEOCODED_LOCATION,
            defaultValue = App.application.getString(R.string.unknown)
        ).toString().ifEmpty(
            { App.application.getString(R.string.unknown) }
        )
    }

    val date: Long by lazy {
        call.getOrDefault(Calls.DATE, -1L).toString().toDoubleOrNull()?.toLong() ?: -1L
    }

    val description: String by lazy {
        val desc = mutableListOf<String>()
        if (subscriptionId != -1) {
            desc.add("SIM $subscriptionId")
        }
        desc.add(geocodedLocation)
        if (date != -1L) {
            desc.add(DateUtil.timestampToDate(date))
        }
        desc.joinToString(" | ")
    }
}

fun Flow<List<CallLog>>.deserialize(): Flow<List<CallLogDeserialized>> = map { flow ->
    val moshi: Moshi = Moshi.Builder().build()
    flow.map {
        val calls = it.call?.let { json -> moshi.adapter<FiledMap>().fromJson(json) }
        CallLogDeserialized(
            id = it.id,
            call = calls ?: mapOf(),
            selected = it.selected
        )
    }
}
