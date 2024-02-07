package com.xayah.core.service.util

import android.content.Context
import com.xayah.core.common.util.toLineString
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.readCleanRestoring
import com.xayah.core.model.OperationState
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.Tar
import com.xayah.core.util.model.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MediumRestoreUtil @Inject constructor(
    @ApplicationContext val context: Context,
    private val rootService: RemoteRootService,
    private val taskDao: TaskDao,
    private val mediaRepository: MediaRepository,
    private val cloudRepository: CloudRepository,
) {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    internal fun log(onMsg: () -> String): String = run {
        val msg = onMsg()
        LogUtil.log { TAG to msg }
        msg
    }

    private fun MediaEntity.getDataSelected() = dataSelected

    private suspend fun TaskDetailMediaEntity.updateInfo(
        state: OperationState? = null,
        bytes: Long? = null,
        log: String? = null,
    ) = run {
        dataInfo.also {
            if (state != null) it.state = state
            if (bytes != null) it.bytes = bytes
            if (log != null) it.log = log
        }
        taskDao.upsert(this)
    }

    suspend fun restoreData(m: MediaEntity, t: TaskDetailMediaEntity, srcDir: String): ShellResult = run {
        log { "Restoring..." }

        val name = m.name
        val ct = m.indexInfo.compressionType
        val src = mediaRepository.getArchiveDst(dstDir = srcDir, ct = ct)
        val dst = m.path
        val dstDir = PathUtil.getParentPath(dst)
        var isSuccess: Boolean
        val out = mutableListOf<String>()

        if (m.getDataSelected().not()) {
            isSuccess = true
            t.updateInfo(state = OperationState.SKIP)
        } else {
            // Return if the archive doesn't exist.
            if (rootService.exists(src)) {
                val sizeBytes = rootService.calculateSize(src)
                t.updateInfo(state = OperationState.PROCESSING, bytes = sizeBytes)

                // Decompress the archive.
                Tar.decompress(
                    exclusionList = listOf(),
                    clear = if (context.readCleanRestoring().first()) "--recursive-unlink" else "",
                    m = false,
                    src = src,
                    dst = dstDir,
                    extra = ct.decompressPara
                ).also { result ->
                    isSuccess = result.isSuccess
                    out.addAll(result.out)
                }
            } else {
                isSuccess = false
                out.add(log { "Not exist: $src" })
                t.updateInfo(state = OperationState.ERROR, log = out.toLineString())
                return@run ShellResult(code = -1, input = listOf(), out = out)
            }
            t.updateInfo(state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = out.toLineString())
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    private fun TaskDetailMediaEntity.getLog() = dataInfo.log

    suspend fun download(client: CloudClient, m: MediaEntity, t: TaskDetailMediaEntity, srcDir: String, dstDir: String, onDownloaded: suspend (path: String) -> Unit) = run {
        val ct = m.indexInfo.compressionType
        val src = mediaRepository.getArchiveDst(dstDir = srcDir, ct = ct)

        if (m.getDataSelected().not()) {
            t.updateInfo(state = OperationState.SKIP)
        } else {
            t.updateInfo(state = OperationState.DOWNLOADING)

            if (client.exists(src)) {
                cloudRepository.download(client = client, src = src, dstDir = dstDir, onDownloaded = onDownloaded).apply {
                    t.updateInfo(state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = t.getLog() + "\n${outString}")
                }
            } else {
                t.updateInfo(state = OperationState.ERROR, log = log { "Not exist: $src" })
            }
        }
    }
}
