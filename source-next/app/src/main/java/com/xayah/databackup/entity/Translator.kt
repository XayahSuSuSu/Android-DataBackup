package com.xayah.databackup.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Translator(
    val username: String = "",
    @SerialName("full_name")
    val fullName: String = "",
    @SerialName("date_joined")
    val dateJoined: String = "",
    @SerialName("change_count")
    val changeCount: Int = 0,
    val languages: Map<String, Int> = emptyMap(),
    val name: String? = null,
    val avatar: String? = null,
    val link: String? = null,
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: fullName.takeIf { it.isNotBlank() } ?: username
}
