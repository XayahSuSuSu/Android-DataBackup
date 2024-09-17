package com.xayah.core.data.repository

import android.content.Context
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.datastore.di.DbDispatchers.Default
import com.xayah.core.datastore.di.Dispatcher
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.CompressionType
import com.xayah.core.model.File
import com.xayah.core.model.OpType
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.MediaExtraInfo
import com.xayah.core.model.database.MediaIndexInfo
import com.xayah.core.model.database.MediaInfo
import com.xayah.core.model.database.asExternalModel
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.ConfigsMediaRestoreName
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import com.xayah.core.util.withLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class FilesRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(Default) private val defaultDispatcher: CoroutineDispatcher,
    private val filesDao: MediaDao,
    private val mediaRepo: MediaRepository,
    private val pathUtil: PathUtil,
    private val rootService: RemoteRootService,
    private val cloudRepo: CloudRepository,
) {
    fun getFiles(
        opType: OpType,
        listData: Flow<ListData>,
        cloudName: String,
        backupDir: String
    ): Flow<List<File>> = combine(
        listData,
        when (opType) {
            OpType.BACKUP -> filesDao.queryFilesFlow(opType = opType, existed = true, blocked = false)
            OpType.RESTORE -> filesDao.queryFilesFlow(opType = opType, cloud = cloudName, backupDir = backupDir)
        }
    ) { lData, files ->
        val data = lData.castTo<ListData.Files>()
        files.asSequence()
            .filter(mediaRepo.getKeyPredicateNew(key = data.searchQuery))
            .sortedWith(mediaRepo.getSortComparatorNew(sortIndex = data.sortIndex, sortType = data.sortType))
            .sortedByDescending { p -> p.extraInfo.activated }.toList()
            .map(MediaEntity::asExternalModel)
    }.flowOn(defaultDispatcher)

    fun countFiles(opType: OpType) = filesDao.countFilesFlow(opType = opType, existed = true, blocked = false)
    fun countSelectedFiles(opType: OpType) = filesDao.countActivatedFilesFlow(opType = opType, existed = true, blocked = false)

    suspend fun selectFile(id: Long, selected: Boolean) {
        filesDao.activateById(id, selected)
    }

    suspend fun selectAll(ids: List<Long>) {
        filesDao.activateByIds(ids, true)
    }

    suspend fun unselectAll(ids: List<Long>) {
        filesDao.activateByIds(ids, false)
    }

    suspend fun reverseAll(ids: List<Long>) {
        filesDao.reverseActivatedByIds(ids)
    }

    suspend fun blockSelected(ids: List<Long>) {
        filesDao.blockByIds(ids)
    }

    suspend fun deleteSelected(ids: List<Long>) {
        val filesDir = pathUtil.getLocalBackupFilesDir()
        val deletedIds = mutableListOf<Long>()
        ids.forEach {
            val file = filesDao.queryById(it)
            if (file != null) {
                val isSuccess = if (file.indexInfo.cloud.isEmpty()) {
                    val src = "${filesDir}/${file.archivesRelativeDir}"
                    rootService.deleteRecursively(src)
                } else {
                    runCatching {
                        cloudRepo.withClient(file.indexInfo.cloud) { client, entity ->
                            val remote = entity.remote
                            val remoteArchivesMediumDir = pathUtil.getCloudRemoteFilesDir(remote)
                            val src = "${remoteArchivesMediumDir}/${file.archivesRelativeDir}"
                            if (client.exists(src)) client.deleteRecursively(src)
                        }
                    }.withLog().isSuccess
                }
                if (isSuccess) deletedIds.add(file.id)
            }
        }
        filesDao.deleteByIds(deletedIds)
    }

    suspend fun initialize() {
        // Add default medium for first time
        if (filesDao.count() == 0L) {
            ConstantUtil.DefaultMediaList.forEach { (name, path) ->
                filesDao.upsert(
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
                            path = path,
                            dataBytes = 0,
                            displayBytes = 0,
                        ),
                        extraInfo = MediaExtraInfo(
                            blocked = false,
                            activated = true,
                            existed = true,
                        ),
                    )
                )
            }
        }

        val files = filesDao.query(opType = OpType.BACKUP, blocked = false)
        files.forEach { m ->
            val size = rootService.calculateSize(m.path)
            val existed = rootService.exists(m.path)
            filesDao.upsert(m.copy(mediaInfo = m.mediaInfo.copy(displayBytes = size), extraInfo = m.extraInfo.copy(existed = existed, activated = m.extraInfo.activated && existed)))
        }
    }

    suspend fun load(cloudName: String?, onLoad: suspend (cur: Int, max: Int, content: String) -> Unit) {
        if (cloudName.isNullOrEmpty().not()) {
            cloudName?.apply {
                loadCloudFiles(this, onLoad)
            }
        } else {
            loadLocalFiles(onLoad)
        }
    }

    private suspend fun loadLocalFiles(onLoad: suspend (cur: Int, max: Int, content: String) -> Unit) {
        val path = pathUtil.getLocalBackupFilesDir()
        val paths = rootService.walkFileTree(path)
        paths.forEachIndexed { index, pathParcelable ->
            val fileName = PathUtil.getFileName(pathParcelable.pathString)
            onLoad(index, paths.size, fileName)
            if (fileName == ConfigsMediaRestoreName) {
                runCatching {
                    rootService.readJson<MediaEntity>(pathParcelable.pathString).also { m ->
                        m?.id = 0
                        m?.extraInfo?.existed = true
                        m?.extraInfo?.activated = false
                        m?.indexInfo?.cloud = ""
                        m?.indexInfo?.backupDir = context.localBackupSaveDir()
                    }?.apply {
                        if (filesDao.query(indexInfo.opType, preserveId, name, indexInfo.compressionType, indexInfo.cloud, indexInfo.backupDir) == null) {
                            filesDao.upsert(this)
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadCloudFiles(cloud: String, onLoad: suspend (cur: Int, max: Int, content: String) -> Unit) = runCatching {
        cloudRepo.withClient(cloud) { client, entity ->
            val remote = entity.remote
            val src = pathUtil.getCloudRemoteFilesDir(remote)
            if (client.exists(src)) {
                val paths = client.walkFileTree(src)
                val tmpDir = pathUtil.getCloudTmpDir()
                paths.forEachIndexed { index, pathParcelable ->
                    val fileName = PathUtil.getFileName(pathParcelable.pathString)
                    onLoad(index, paths.size, fileName)
                    if (fileName == ConfigsMediaRestoreName) {
                        runCatching {
                            cloudRepo.download(client = client, src = pathParcelable.pathString, dstDir = tmpDir) { path ->
                                val stored = rootService.readJson<MediaEntity>(path).also { p ->
                                    p?.id = 0
                                    p?.extraInfo?.existed = true
                                    p?.extraInfo?.activated = false
                                    p?.indexInfo?.cloud = entity.name
                                    p?.indexInfo?.backupDir = remote
                                }?.apply {
                                    if (filesDao.query(indexInfo.opType, preserveId, name, indexInfo.compressionType, indexInfo.cloud, indexInfo.backupDir) == null) {
                                        filesDao.upsert(this)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }.withLog()
}
