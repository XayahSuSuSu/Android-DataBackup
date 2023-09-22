package com.xayah.databackup.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.xayah.databackup.module.GitHub
import com.xayah.librootservice.util.withIOContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject


/**
 * GitHub Api Release Entity
 */
data class Release(
    @SerializedName("html_url") val url: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("assets") val assets: List<Asset> = listOf(),
    @SerializedName("body") val body: String = ""
) {
    val content get() = body.replace(Regex("[*`]"), "")
}

/**
 * GitHub Api Asset Entity
 */
data class Asset(
    @SerializedName("browser_download_url") val url: String = ""
)

class ServerUtil @Inject constructor() {
    companion object {
        private const val apiReleases = "https://api.github.com/repos/XayahSuSuSu/Android-DataBackup/releases"
    }

    @Inject
    @GitHub
    lateinit var okHttpClient: OkHttpClient

    @Inject
    @GitHub
    lateinit var gson: Gson

    suspend fun getReleases(onSucceed: (releases: List<Release>) -> Unit, onFailed: () -> Unit) {
        withIOContext {
            try {
                val request = Request.Builder().url(apiReleases).build()
                okHttpClient.newCall(request).execute().use { response ->
                    val list: List<Release> =
                        gson.fromJson(response.body!!.string(), object : TypeToken<List<Release>>() {})
                    onSucceed(list)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onFailed()
            }
        }
    }
}
