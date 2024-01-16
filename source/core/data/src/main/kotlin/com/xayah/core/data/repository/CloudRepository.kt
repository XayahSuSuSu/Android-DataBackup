package com.xayah.core.data.repository

import android.content.Context
import androidx.annotation.StringRes
import com.xayah.core.database.dao.CloudDao
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.model.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class CloudRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val cloudDao: CloudDao,
) {
    private fun log(msg: () -> String): String = run {
        LogUtil.log { "CloudRepository" to msg() }
        msg()
    }

    fun getString(@StringRes resId: Int) = context.getString(resId)
    suspend fun upsert(item: CloudEntity) = cloudDao.upsert(item)
    suspend fun queryByName(name: String) = cloudDao.queryByName(name)

    val clouds = cloudDao.queryFlow().distinctUntilChanged()

    suspend fun delete(entity: CloudEntity) = cloudDao.delete(entity)

    suspend fun upload(client: CloudClient, src: String, dstDir: String): ShellResult = run {
        log { "Uploading..." }

        var isSuccess = true
        val out = mutableListOf<String>()

        runCatching {
            client.upload(src = src, dst = dstDir)
            out.add("Upload succeed.")
        }.onFailure {
            isSuccess = false
            if (it.localizedMessage != null)
                out.add(log { it.localizedMessage!! })
        }

        rootService.deleteRecursively(src).also { result ->
            isSuccess = isSuccess and result
            if (result.not()) out.add(log { "Failed to delete $src." })
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    suspend fun download(
        client: CloudClient,
        src: String,
        dstDir: String,
        onDownloaded: suspend () -> Unit,
        deleteAfterDownloaded: Boolean = true,
    ): ShellResult =
        run {
            log { "Downloading..." }

            var code = 0
            val out = mutableListOf<String>()
            rootService.deleteRecursively(dstDir)
            rootService.mkdirs(dstDir)
            PathUtil.setFilesDirSELinux(context)

            runCatching {
                client.download(src = src, dst = dstDir)
                out.add("Download succeed.")
            }.onFailure {
                code = -2
                if (it.localizedMessage != null)
                    out.add(log { it.localizedMessage!! })
            }

            if (code == 0) {
                onDownloaded()
            } else {
                out.add(log { "Failed to download $src." })
            }
            if (deleteAfterDownloaded)
                rootService.deleteRecursively(dstDir).also { result ->
                    code = if (result) code else -1
                    if (result.not()) out.add(log { "Failed to delete $dstDir." })
                }

            ShellResult(code = code, input = listOf(), out = out)
    }
}
