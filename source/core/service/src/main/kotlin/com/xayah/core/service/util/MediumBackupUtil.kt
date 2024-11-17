package com.xayah.core.service.util

import android.content.Context
import com.xayah.core.common.util.toLineString
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.readCompatibleMode
import com.xayah.core.datastore.readFollowSymlinks
import com.xayah.core.model.DataType
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class MediumBackupUtil @Inject constructor(
    @ApplicationContext val context: Context,
    private val rootService: RemoteRootService,
    private val taskDao: TaskDao,
    private val mediaRepository: MediaRepository,
    private val commonBackupUtil: CommonBackupUtil,
    private val cloudRepository: CloudRepository,
) {
    companion object {
        private const val TAG = "MediumBackupUtil"
    }

    private fun log(onMsg: () -> String): String = run {
        val msg = onMsg()
        LogUtil.log { TAG to msg }
        msg
    }

    private val usePipe = runBlocking { context.readCompatibleMode().first() }

    private fun MediaEntity.getDataBytes() = mediaInfo.dataBytes

    private fun MediaEntity.setDataBytes(sizeBytes: Long) = run { mediaInfo.dataBytes = sizeBytes }
    private fun MediaEntity.setDisplayBytes(sizeBytes: Long) = run { mediaInfo.displayBytes = sizeBytes }

    private fun TaskDetailMediaEntity.getLog() = mediaInfo.log

    private suspend fun TaskDetailMediaEntity.updateInfo(
        state: OperationState? = null,
        bytes: Long? = null,
        log: String? = null,
        content: String? = null,
    ) = run {
        mediaInfo.also {
            if (state != null) it.state = state
            if (bytes != null) it.bytes = bytes
            if (log != null) it.log = log
            if (content != null) it.content = content
        }
        taskDao.upsert(this)
    }

    suspend fun backupMedia(m: MediaEntity, t: TaskDetailMediaEntity, r: MediaEntity?, dstDir: String): ShellResult = run {
        log { "Backing up ${DataType.MEDIA_MEDIA.type}..." }

        val name = m.name
        val ct = m.indexInfo.compressionType
        val dst = mediaRepository.getArchiveDst(dstDir = dstDir, ct = ct)
        var isSuccess: Boolean
        val out = mutableListOf<String>()
        val src = m.path
        val srcDir = PathUtil.getParentPath(src)

        // Check the existence of origin path.
        rootService.exists(src).also {
            if (it.not()) {
                isSuccess = false
                out.add(log { "Not exist: $src" })
                t.updateInfo(state = OperationState.ERROR, log = out.toLineString())
                return@run ShellResult(code = -1, input = listOf(), out = out)
            }
        }

        val sizeBytes = rootService.calculateSize(src)
        t.updateInfo(state = OperationState.PROCESSING, bytes = sizeBytes)
        if (rootService.exists(dst) && sizeBytes == r?.getDataBytes()) {
            isSuccess = true
            t.updateInfo(state = OperationState.SKIP)
            out.add(log { "Data has not changed." })
        } else {
            // Compress and test.
            Tar.compress(
                usePipe = usePipe,
                exclusionList = listOf(),
                h = if (context.readFollowSymlinks().first()) "-h" else "",
                srcDir = srcDir,
                src = PathUtil.getFileName(src),// the name is not always the actual file name of the source,but the src does contain
                dst = dst,
                extra = ct.compressPara
            ).also { result ->
                isSuccess = result.isSuccess
                out.addAll(result.out)
            }
            commonBackupUtil.testArchive(src = dst, ct = ct).also { result ->
                isSuccess = isSuccess and result.isSuccess
                out.addAll(result.out)
                if (result.isSuccess) {
                    m.setDataBytes(sizeBytes)
                    m.setDisplayBytes(rootService.calculateSize(dst))
                }
            }
        }

        t.updateInfo(state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = out.toLineString())

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    suspend fun upload(client: CloudClient, m: MediaEntity, t: TaskDetailMediaEntity, srcDir: String, dstDir: String) = run {
        val ct = m.indexInfo.compressionType
        val src = mediaRepository.getArchiveDst(dstDir = srcDir, ct = ct)
        t.updateInfo(state = OperationState.UPLOADING)

        var flag = true
        var progress = 0f
        with(CoroutineScope(coroutineContext)) {
            launch {
                while (flag) {
                    t.updateInfo(content = "${(progress * 100).toInt()}%")
                    delay(500)
                }
            }
        }

        cloudRepository.upload(client = client, src = src, dstDir = dstDir, onUploading = { read, total -> progress = read.toFloat() / total }).apply {
            flag = false
            t.updateInfo(state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = t.getLog() + "\n${outString}", content = "100%")
        }
    }
}
