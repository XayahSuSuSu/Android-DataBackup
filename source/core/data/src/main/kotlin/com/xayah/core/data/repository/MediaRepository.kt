package com.xayah.core.data.repository

import android.content.Context
import com.xayah.core.data.R
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataState
import com.xayah.core.model.OpType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.MediaEntityWithCount
import com.xayah.core.model.database.MediaExtraInfo
import com.xayah.core.model.database.MediaIndexInfo
import com.xayah.core.model.database.MediaInfo
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.ConfigsMediaRestoreName
import com.xayah.core.util.DateUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val mediaDao: MediaDao,
    private val cloudRepository: CloudRepository,
    private val pathUtil: PathUtil,
) {
    companion object {
        private const val TAG = "MediaRepository"
    }

    internal fun log(onMsg: () -> String): String = run {
        val msg = onMsg()
        LogUtil.log { TAG to msg }
        msg
    }

    private val archivesMediumDir get() = pathUtil.getLocalBackupArchivesMediumDir()
    private val localBackupSaveDir get() = context.localBackupSaveDir()
    fun getMedium() = mediaDao.queryFlow().distinctUntilChanged()
    fun getMedium(opType: OpType) = mediaDao.queryFlow(opType).distinctUntilChanged()
    val activatedCount = mediaDao.countActivatedFlow().distinctUntilChanged()

    suspend fun query(opType: OpType, preserveId: Long, cloud: String, backupDir: String) = mediaDao.query(opType, preserveId, cloud, backupDir)
    suspend fun query(opType: OpType, name: String, cloud: String, backupDir: String) = mediaDao.query(opType, name, cloud, backupDir)
    suspend fun query(opType: OpType, preserveId: Long, name: String, cloud: String, backupDir: String) = mediaDao.query(opType, preserveId, name, cloud, backupDir)
    suspend fun query(opType: OpType, preserveId: Long, name: String, ct: CompressionType, cloud: String, backupDir: String) = mediaDao.query(opType, preserveId, name, ct, cloud, backupDir)
    fun queryFlow(name: String, opType: OpType, preserveId: Long) = mediaDao.queryFlow(name = name, opType = opType, preserveId = preserveId)
    fun queryFlow(name: String, opType: OpType) = mediaDao.queryFlow(name = name, opType = opType)
    suspend fun getMedia(opType: OpType, name: String, preserveId: Long, ct: CompressionType, cloud: String, backupDir: String) =
        mediaDao.query(opType, preserveId, name, ct, cloud, backupDir)

    fun getKeyPredicate(key: String): (MediaEntityWithCount) -> Boolean = { m ->
        m.entity.name.lowercase().contains(key.lowercase()) || m.entity.path.lowercase().contains(key.lowercase())
    }

    suspend fun refreshFromLocalMedia(name: String) {
        refreshFromLocal(path = "$archivesMediumDir/$name")
    }

    suspend fun refreshFromCloudMedia(name: String) {
        runCatching { refreshFromCloud(name = name) }.onFailure(rootService.onFailure)
    }

    private suspend fun refreshFromLocal(path: String) {
        val paths = rootService.walkFileTree(path)
        paths.forEach {
            val fileName = PathUtil.getFileName(it.pathString)
            if (fileName == ConfigsMediaRestoreName) {
                runCatching {
                    val stored = rootService.readProtoBuf<MediaEntity>(it.pathString).also { p ->
                        p?.extraInfo?.activated = false
                        p?.indexInfo?.backupDir = localBackupSaveDir
                    }
                    if (stored != null) {
                        query(stored.indexInfo.opType, stored.preserveId, stored.name, stored.indexInfo.compressionType, "", localBackupSaveDir).also { m ->
                            if (m == null)
                                mediaDao.upsert(stored)
                        }
                    }
                }
            }
        }
    }

    private suspend fun refreshFromCloud(name: String) {
        cloudRepository.withActivatedClients { clients ->
            clients.forEach { (client, entity) ->
                val remote = entity.remote
                val remoteArchivesMediumDir = pathUtil.getCloudRemoteArchivesMediumDir(remote)
                val src = "$remoteArchivesMediumDir/$name"
                if (client.exists(src)) {
                    val paths = client.walkFileTree(src)
                    val tmpDir = pathUtil.getCloudTmpDir()
                    paths.forEach {
                        val fileName = PathUtil.getFileName(it.pathString)
                        val preserveId = PathUtil.getFileName(PathUtil.getParentPath(it.pathString))
                        if (fileName == ConfigsMediaRestoreName) {
                            runCatching {
                                cloudRepository.download(client = client, src = it.pathString, dstDir = tmpDir) { path ->
                                    val stored = rootService.readProtoBuf<MediaEntity>(path).also { p ->
                                        p?.extraInfo?.activated = false
                                        p?.indexInfo?.cloud = entity.name
                                        p?.indexInfo?.backupDir = remote
                                        p?.indexInfo?.preserveId = preserveId.toLongOrNull() ?: 0
                                    }
                                    if (stored != null) {
                                        getMedia(stored.indexInfo.opType, stored.name, stored.preserveId, stored.indexInfo.compressionType, entity.name, remote).also { m ->
                                            if (m == null)
                                                mediaDao.upsert(stored)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun refresh(topBarState: MutableStateFlow<TopBarState>) = run {
        val title = topBarState.value.title
        topBarState.emit(
            TopBarState(
                title = StringResourceToken.fromStringId(R.string.updating),
                indeterminate = true
            )
        )
        val medium = query(opType = OpType.BACKUP, preserveId = 0, "", "")
        medium.forEach { m ->
            mediaDao.upsert(m.copy(mediaInfo = m.mediaInfo.copy(displayBytes = rootService.calculateSize(m.path))))
        }
        topBarState.emit(TopBarState(progress = 1f, title = StringResourceToken.fromStringId(R.string.updating), indeterminate = true))
        refreshFromLocal(path = archivesMediumDir)
        topBarState.emit(TopBarState(progress = 1f, title = title, indeterminate = false))
    }

    private fun renameDuplicateMedia(name: String): String {
        val nameList = name.split("_").toMutableList()
        val index = nameList.first().toIntOrNull()
        if (index == null) {
            nameList.add("0")
        } else {
            nameList[nameList.lastIndex] = (index + 1).toString()
        }
        return nameList.joinToString(separator = "_")
    }

    suspend fun addMedia(pathList: List<String>): String {
        val customMediaList = mutableListOf<MediaEntity>()
        var failedCount = 0
        pathList.forEach { pathString ->
            if (pathString.isNotEmpty()) {
                if (pathString == context.localBackupSaveDir()) {
                    failedCount++
                    log { context.getString(R.string.backup_dir_as_media_error) }
                    return@forEach
                }
                var name = PathUtil.getFileName(pathString)
                query(opType = OpType.BACKUP, preserveId = 0, cloud = "", backupDir = "").forEach {
                    if (it.name == name && it.path != pathString) name = renameDuplicateMedia(name)
                }
                customMediaList.forEach {
                    if (it.name == name && it.path != pathString) name = renameDuplicateMedia(name)
                }
                val exists = query(opType = OpType.BACKUP, preserveId = 0, name = name, cloud = "", backupDir = "") != null
                if (exists) {
                    failedCount++
                    log { "$name:${pathString} has already existed." }
                } else customMediaList.add(
                    MediaEntity(
                        id = 0,
                        indexInfo = MediaIndexInfo(
                            opType = OpType.BACKUP,
                            name = name,
                            compressionType = CompressionType.TAR,
                            preserveId = 0,
                            cloud = "",
                            backupDir = ""
                        ),
                        mediaInfo = MediaInfo(
                            path = pathString,
                            dataState = DataState.Selected,
                            dataBytes = 0,
                            displayBytes = 0,
                        ),
                        extraInfo = MediaExtraInfo(
                            labels = listOf(),
                            activated = false
                        ),
                    )
                )
            }
        }
        mediaDao.upsert(customMediaList)
        return "${context.getString(R.string.succeed)}: ${customMediaList.size}, ${context.getString(R.string.failed)}: $failedCount"
    }

    suspend fun upsert(item: MediaEntity) = mediaDao.upsert(item)
    suspend fun clearActivated() = mediaDao.clearActivated()

    fun getArchiveDst(dstDir: String, ct: CompressionType) = "${dstDir}/media.${ct.suffix}"

    private suspend fun calculateArchiveSize(m: MediaEntity) = rootService.calculateSize(
        getArchiveDst("${archivesMediumDir}/${m.archivesPreserveRelativeDir}", m.indexInfo.compressionType)
    )

    private suspend fun calculateDataSize(m: MediaEntity) = run {
        val src = m.path
        if (rootService.exists(src)) rootService.calculateSize(src)
        else 0
    }

    suspend fun updateLocalMediaDataSize(opType: OpType, preserveId: Long, name: String) {
        query(opType, preserveId, name, "", "").also {
            if (it != null) {
                it.mediaInfo.displayBytes = calculateDataSize(it)
                upsert(it)
            }
        }
    }

    suspend fun updateLocalMediaArchivesSize(opType: OpType, name: String) {
        query(opType, name, "", "").onEach {
            it.mediaInfo.displayBytes = calculateArchiveSize(it)
            upsert(it)
        }
    }

    suspend fun updateCloudMediaArchivesSize(opType: OpType, name: String) {
        cloudRepository.withActivatedClients { clients ->
            clients.forEach { (client, entity) ->
                updateCloudMediaArchivesSize(opType, name, client, entity)
            }
        }
    }

    private fun calculateCloudArchiveSize(client: CloudClient, m: MediaEntity, archivesMediumDir: String) = run {
        val src = getArchiveDst("${archivesMediumDir}/${m.archivesPreserveRelativeDir}", m.indexInfo.compressionType)
        if (client.exists(src)) client.size(src)
        else 0
    }

    suspend fun updateCloudMediaArchivesSize(opType: OpType, name: String, client: CloudClient, entity: CloudEntity) {
        val remote = entity.remote
        val remoteArchivesMediumDir = pathUtil.getCloudRemoteArchivesMediumDir(remote)
        query(opType, name, entity.name, entity.remote).onEach {
            runCatching { it.mediaInfo.displayBytes = calculateCloudArchiveSize(client, it, remoteArchivesMediumDir) }.onFailure(rootService.onFailure)
            upsert(it)
        }
    }

    suspend fun preserve(m: MediaEntity) {
        val parent = archivesMediumDir
        val preserveId = DateUtil.getTimestamp()
        val isSuccess = if (m.indexInfo.cloud.isEmpty()) {
            val src = "${parent}/${m.archivesPreserveRelativeDir}"
            val dst = "${parent}/${m.archivesRelativeDir}/${preserveId}"
            rootService.renameTo(src, dst)
        } else {
            runCatching {
                cloudRepository.withClient(m.indexInfo.cloud) { client, entity ->
                    val remote = entity.remote
                    val remoteArchivesMediumsDir = pathUtil.getCloudRemoteArchivesMediumDir(remote)
                    val src = "${remoteArchivesMediumsDir}/${m.archivesPreserveRelativeDir}"
                    val dst = "${remoteArchivesMediumsDir}/${m.archivesRelativeDir}/${preserveId}"
                    client.renameTo(src, dst)
                }
            }.onFailure(rootService.onFailure).isSuccess
        }

        if (isSuccess) upsert(m.copy(indexInfo = m.indexInfo.copy(preserveId = preserveId)))
    }

    suspend fun delete(m: MediaEntity) {
        val isSuccess = if (m.indexInfo.cloud.isEmpty()) {
            val src = "${archivesMediumDir}/${m.archivesPreserveRelativeDir}"
            rootService.deleteRecursively(src)
        } else {
            runCatching {
                cloudRepository.withClient(m.indexInfo.cloud) { client, entity ->
                    val remote = entity.remote
                    val remoteArchivesMediumDir = pathUtil.getCloudRemoteArchivesMediumDir(remote)
                    val src = "${remoteArchivesMediumDir}/${m.archivesPreserveRelativeDir}"
                    client.deleteRecursively(src)
                }
            }.onFailure(rootService.onFailure).isSuccess
        }

        if (isSuccess) mediaDao.delete(m)
    }

    fun getLocationPredicate(index: Int, accountList: List<CloudEntity>): (MediaEntityWithCount) -> Boolean = { m ->
        when (index) {
            0 -> m.entity.indexInfo.cloud.isEmpty()
            else -> m.entity.indexInfo.cloud == accountList.getOrNull(index - 1)?.name
        }
    }

}
