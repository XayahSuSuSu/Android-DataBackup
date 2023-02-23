package com.xayah.databackup.util

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.xayah.databackup.App
import com.xayah.databackup.data.Release
import com.xayah.databackup.data.UpdateChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 过滤Release列表, 仅读取应用版本
 */
fun MutableList<Release>.appReleaseList(): MutableList<Release> {
    return this.filter {
        it.name.contains("App ") &&
                when (App.globalContext.readUpdateChannel()) {
                    UpdateChannel.Stable -> {
                        it.name.contains("-").not()
                    }
                    UpdateChannel.Test -> {
                        true
                    }
                }
    }.toMutableList()
}

class Server {
    object Instance {
        val instance = Server()
    }

    companion object {
        fun getInstance() = Instance.instance

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

    suspend fun download(
        url: String,
        savePath: String,
        onDownload: (current: Int, total: Int) -> Unit,
        onSuccess: () -> Unit,
        onFailed: () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val request: Request = Request.Builder()
                    .url(url)
                    .build()
                client.newCall(request).execute().use { response ->
                    val total = response.headers["content-length"]
                    val file = File(savePath)
                    if (file.exists()) file.delete()
                    val inputStream = response.body?.byteStream()
                    val fileOutputStream = FileOutputStream(file, true)
                    val buffer = ByteArray(2048)
                    var count = 0
                    var length: Int?
                    while (inputStream?.read(buffer).also { length = it } != -1) {
                        length?.apply {
                            count += this
                            fileOutputStream.write(buffer, 0, this)
                            total?.apply {
                                onDownload(count, this.toInt())
                            }
                        }
                    }
                    fileOutputStream.flush()
                    fileOutputStream.flush()
                    inputStream?.close()
                    fileOutputStream.close()
                    onSuccess()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                onFailed()
            }
        }
    }
}