package com.xayah.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * GitHub Api Release Entity
 */
data class Release(
    @SerializedName("html_url") val url: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("assets") val assets: List<Asset> = listOf(),
    @SerializedName("body") val body: String = "",
) {
    val content get() = body.replace(Regex("[*`]"), "")
}

/**
 * GitHub Api Asset Entity
 */
data class Asset(
    @SerializedName("browser_download_url") val url: String = "",
)