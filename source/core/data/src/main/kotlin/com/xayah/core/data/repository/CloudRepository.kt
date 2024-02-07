package com.xayah.core.data.repository

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import com.xayah.core.database.dao.CloudDao
import com.xayah.core.datastore.readCloudActivatedAccountName
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.network.client.CloudClient
import com.xayah.core.network.client.getCloud
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromVector
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.model.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    val cloudsMenuItems = clouds.map { c ->
        c.map {
            ActionMenuItem(
                title = StringResourceToken.fromString(it.name),
                icon = ImageVectorToken.fromVector(Icons.Rounded.AccountCircle),
                enabled = true,
                secondaryMenu = listOf(),
                dismissOnClick = true,
                onClick = null
            )
        }
    }

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
        deleteAfterDownloaded: Boolean = true,
        onDownloaded: suspend (path: String) -> Unit,
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
                onDownloaded("$dstDir/${PathUtil.getFileName(src)}")
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

    suspend fun getClient(name: String? = null): Pair<CloudClient, CloudEntity> {
        val entity = queryByName(name ?: context.readCloudActivatedAccountName().first())
        val client = entity?.getCloud()?.apply { connect() } ?: throw NullPointerException("Client is null.")
        return client to entity
    }

    suspend fun withClient(name: String? = null, block: suspend (client: CloudClient, entity: CloudEntity) -> Unit) = run {
        val (client, entity) = getClient(name)
        block(client, entity)
        client.disconnect()
    }

    suspend fun withActivatedClients(block: suspend (clients: List<Pair<CloudClient, CloudEntity>>) -> Unit) = run {
        val clients: MutableList<Pair<CloudClient, CloudEntity>> = mutableListOf()
        cloudDao.queryActivated().forEach {
            clients.add(it.getCloud().apply { connect() } to it)
        }
        block(clients)
        clients.forEach { it.first.disconnect() }
    }
}
