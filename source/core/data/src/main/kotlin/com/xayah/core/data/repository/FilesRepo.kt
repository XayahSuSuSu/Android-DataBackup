package com.xayah.core.data.repository

import android.content.Context
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.datastore.di.DbDispatchers.Default
import com.xayah.core.datastore.di.Dispatcher
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataType
import com.xayah.core.model.File
import com.xayah.core.model.OpType
import com.xayah.core.model.database.LabelFileCrossRefEntity
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.MediaExtraInfo
import com.xayah.core.model.database.MediaIndexInfo
import com.xayah.core.model.database.MediaInfo
import com.xayah.core.model.database.asExternalModel
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.ConfigsMediaRestoreName
import com.xayah.core.util.DateUtil
import com.xayah.core.util.LogUtil
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
    companion object {
        private const val TAG = "FilesRepo"
    }

    private fun log(block: () -> String): String = block().also { LogUtil.log { TAG to it } }

    fun getFile(id: Long) = filesDao.queryFileFlow(id)

    fun getFiles(
        opType: OpType,
        listData: Flow<ListData>,
        refs: Flow<List<LabelFileCrossRefEntity>>,
        labels: Flow<Set<String>>,
        cloudName: String,
        backupDir: String
    ): Flow<List<File>> = combine(
        listData,
        refs,
        labels,
        when (opType) {
            OpType.BACKUP -> filesDao.queryFilesFlow(opType = opType, existed = true, blocked = false)
            OpType.RESTORE -> filesDao.queryFilesFlow(opType = opType, cloud = cloudName, backupDir = backupDir)
        }
    ) { lData, lRefs, lLabels, files ->
        val data = lData.castTo<ListData.Files>()
        files.asSequence()
            .filter(mediaRepo.getKeyPredicateNew(key = data.searchQuery))
            .filter { if (lLabels.isNotEmpty()) lRefs.find { ref -> it.path == ref.path && it.preserveId == ref.preserveId } != null else true }
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
                            lastBackupTime = 0,
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

    private fun renameDuplicateFile(name: String): String {
        val nameList = name.split("_").toMutableList()
        val index = nameList.first().toIntOrNull()
        if (index == null) {
            nameList.add("0")
        } else {
            nameList[nameList.lastIndex] = (index + 1).toString()
        }
        return nameList.joinToString(separator = "_")
    }

    suspend fun addFiles(pathList: List<String>) {
        val files = mutableListOf<MediaEntity>()
        var failure = 0
        pathList.forEach { pathString ->
            if (pathString.isNotEmpty()) {
                var name = PathUtil.getFileName(pathString)
                filesDao.query(opType = OpType.BACKUP, preserveId = 0, cloud = "", backupDir = "").forEach {
                    if (it.name == name && it.path != pathString) name = renameDuplicateFile(name)
                }
                files.forEach {
                    if (it.name == name && it.path != pathString) name = renameDuplicateFile(name)
                }
                val exists = filesDao.query(opType = OpType.BACKUP, preserveId = 0, name = name, cloud = "", backupDir = "") != null
                if (exists) {
                    failure++
                    log { "$name:${pathString} has already existed." }
                } else files.add(
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
                            dataBytes = 0,
                            displayBytes = 0,
                        ),
                        extraInfo = MediaExtraInfo(
                            lastBackupTime = 0,
                            activated = true,
                            blocked = false,
                            existed = true
                        ),
                    )
                )
            }
        }
        filesDao.upsert(files)
    }

    suspend fun calculateLocalFileSize(file: MediaEntity) {
        file.mediaInfo.displayBytes = rootService.calculateSize(file.path)
        filesDao.upsert(file)
    }

    private fun getArchiveSrc(dstDir: String, ct: CompressionType) = "${dstDir}/${DataType.MEDIA_MEDIA.type}.${ct.suffix}"

    suspend fun calculateLocalFileArchiveSize(file: MediaEntity) {
        file.mediaInfo.displayBytes = rootService.calculateSize(getArchiveSrc("${pathUtil.getLocalBackupFilesDir()}/${file.archivesRelativeDir}", file.indexInfo.compressionType))
        filesDao.upsert(file)
    }

    suspend fun setBlocked(id: Long, blocked: Boolean) {
        filesDao.setBlocked(id, blocked)
    }

    suspend fun delete(id: Long) {
        filesDao.delete(id)
    }

    suspend fun delete(ids: List<Long>) {
        filesDao.delete(ids)
    }

    suspend fun protectFile(cloudName: String?, file: MediaEntity) {
        if (cloudName.isNullOrEmpty().not()) {
            cloudName?.apply {
                protectCloudFile(this, file)
            }
        } else {
            protectLocalFile(file)
        }
    }

    private suspend fun protectLocalFile(file: MediaEntity) {
        val protectedFile = file.copy(indexInfo = file.indexInfo.copy(preserveId = DateUtil.getTimestamp()))
        val fileDir = pathUtil.getLocalBackupFilesDir()
        val src = "${fileDir}/${file.archivesRelativeDir}"
        val dst = "${fileDir}/${protectedFile.archivesRelativeDir}"
        rootService.writeJson(data = protectedFile, dst = PathUtil.getMediaRestoreConfigDst(src))
        rootService.renameTo(src, dst)
        filesDao.update(protectedFile)
    }

    private suspend fun protectCloudFile(cloudName: String, file: MediaEntity) = runCatching {
        cloudRepo.withClient(cloudName) { client, entity ->
            val protectedFile = file.copy(indexInfo = file.indexInfo.copy(preserveId = DateUtil.getTimestamp()))
            val remote = entity.remote
            val remoteFilesDir = pathUtil.getCloudRemoteFilesDir(remote)
            val src = "${remoteFilesDir}/${file.archivesRelativeDir}"
            val dst = "${remoteFilesDir}/${protectedFile.archivesRelativeDir}"
            val tmpDir = pathUtil.getCloudTmpDir()
            val tmpJsonPath = PathUtil.getMediaRestoreConfigDst(tmpDir)
            rootService.writeJson(data = protectedFile, dst = tmpJsonPath)
            cloudRepo.upload(client = client, src = tmpJsonPath, dstDir = src)
            rootService.deleteRecursively(tmpDir)
            client.renameTo(src, dst)
        }
    }.withLog()

    suspend fun deleteFile(cloudName: String?, file: MediaEntity) {
        if (cloudName.isNullOrEmpty().not()) {
            cloudName?.apply {
                deleteCloudFile(this, file)
            }
        } else {
            deleteLocalFile(file)
        }
    }

    private suspend fun deleteLocalFile(file: MediaEntity) {
        val filesDir = pathUtil.getLocalBackupFilesDir()
        val src = "${filesDir}/${file.archivesRelativeDir}"
        if (rootService.deleteRecursively(src)) {
            filesDao.delete(file.id)
        }
    }

    private suspend fun deleteCloudFile(cloudName: String, file: MediaEntity) = runCatching {
        cloudRepo.withClient(cloudName) { client, entity ->
            val remote = entity.remote
            val remoteFilesDir = pathUtil.getCloudRemoteFilesDir(remote)
            val src = "${remoteFilesDir}/${file.archivesRelativeDir}"
            if (client.exists(src)) {
                client.deleteRecursively(src)
                if (client.exists(src).not()) {
                    filesDao.delete(file.id)
                }
            }
        }
    }.withLog()
}
