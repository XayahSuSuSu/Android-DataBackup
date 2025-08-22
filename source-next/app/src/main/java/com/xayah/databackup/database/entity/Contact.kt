package com.xayah.databackup.database.entity

import android.provider.ContactsContract
import androidx.room.Entity
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.App
import com.xayah.databackup.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "contacts", primaryKeys = ["id"])
data class Contact(
    var id: Long,
    var rawContact: String?, // JSON
    var data: String?,       // JSON
    var selected: Boolean,
)

data class ContactDeserialized(
    var id: Long,
    var rawContact: FiledMap,
    var data: List<FiledMap>,
    var selected: Boolean,
) {
    val displayName: String by lazy {
        rawContact.getOrDefault(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY, App.application.getString(R.string.unknown)).toString()
    }
}

fun Flow<List<Contact>>.deserialize(): Flow<List<ContactDeserialized>> = map { flow ->
    val moshi: Moshi = Moshi.Builder().build()
    flow.map {
        val rawContact = it.rawContact?.let { json -> moshi.adapter<FiledMap>().fromJson(json) }
        val data = it.data?.let { json -> moshi.adapter<List<FiledMap>>().fromJson(json) }
        ContactDeserialized(
            id = it.id,
            rawContact = rawContact ?: mapOf(),
            data = data ?: listOf(),
            selected = it.selected
        )
    }
}
