package com.xayah.databackup.util

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.xayah.databackup.data.Release
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


class Server {
    companion object {
        private const val releasesApi =
            "https://api.github.com/repos/XayahSuSuSu/Android-DataBackup/releases"
    }

    private val client = OkHttpClient()

    suspend fun releases(
        successCallback: (releaseList: MutableList<Release>) -> Unit,
        failedCallback: () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val request: Request = Request.Builder()
                    .url(releasesApi)
                    .build()
                client.newCall(request).execute().use { response ->
                    response.body?.apply {
                        // 解析response.body
                        try {
                            val jsonArray = JsonParser.parseString(this.string()).asJsonArray
                            val mReleaseList = mutableListOf<Release>()
                            for (i in jsonArray) {
                                try {
                                    val item = Gson().fromJson(i, Release::class.java)
                                    mReleaseList.add(item)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    failedCallback()
                                }
                            }
                            successCallback(mReleaseList)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            failedCallback()
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                failedCallback()
            }
        }
    }
}