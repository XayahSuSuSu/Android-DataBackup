package com.xayah.core.data.repository

import android.content.Context
import com.xayah.core.data.R
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataState
import com.xayah.core.model.OpType
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.MediaEntityWithCount
import com.xayah.core.model.database.MediaExtraInfo
import com.xayah.core.model.database.MediaIndexInfo
import com.xayah.core.model.database.MediaInfo
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

    private val archivesMediumDir by lazy { pathUtil.getLocalBackupArchivesMediumDir() }
    val medium = mediaDao.queryFlow().distinctUntilChanged()
    val activatedCount = mediaDao.countActivatedFlow().distinctUntilChanged()

    fun queryFlow(name: String, opType: OpType, preserveId: Long) = mediaDao.queryFlow(name = name, opType = opType, preserveId = preserveId)
    fun queryFlow(name: String, opType: OpType) = mediaDao.queryFlow(name = name, opType = opType)

    fun getKeyPredicate(key: String): (MediaEntityWithCount) -> Boolean = { m ->
        m.entity.name.lowercase().contains(key.lowercase()) || m.entity.path.lowercase().contains(key.lowercase())
    }

    suspend fun refreshFromLocalMedia(name: String) {
        refreshFromLocal(path = "$archivesMediumDir/$name")
    }

    private suspend fun refreshFromLocal(path: String) {
        val paths = rootService.walkFileTree(path)
        paths.forEach {
            val fileName = PathUtil.getFileName(it.pathString)
            if (fileName == ConfigsMediaRestoreName) {
                runCatching {
                    val stored = rootService.readProtoBuf<MediaEntity>(it.pathString).also { p ->
                        p?.extraInfo?.activated = false
                    }
                    if (stored != null) {
                        mediaDao.query(stored.indexInfo.opType, stored.preserveId, stored.name, stored.indexInfo.compressionType).also { m ->
                            if (m == null)
                                mediaDao.upsert(stored)
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
        val medium = mediaDao.query(opType = OpType.BACKUP, preserveId = 0)
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
                mediaDao.query(opType = OpType.BACKUP, preserveId = 0).forEach {
                    if (it.name == name && it.path != pathString) name = renameDuplicateMedia(name)
                }
                customMediaList.forEach {
                    if (it.name == name && it.path != pathString) name = renameDuplicateMedia(name)
                }
                val exists = mediaDao.query(opType = OpType.BACKUP, preserveId = 0, name = name) != null
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

    fun getArchiveDst(dstDir: String, ct: CompressionType) = "${dstDir}/media.${ct.suffix}"

    private suspend fun calculateArchiveSize(m: MediaEntity) = rootService.calculateSize(
        getArchiveDst("${archivesMediumDir}/${m.archivesPreserveRelativeDir}", m.indexInfo.compressionType)
    )

    private suspend fun calculateDataSize(m: MediaEntity) = run {
        val src = m.path
        if (rootService.exists(src)) rootService.calculateSize(src)
        else 0
    }

    private suspend fun queryMedium(opType: OpType, name: String) = mediaDao.query(opType, name)

    suspend fun updateMediaDataSize(opType: OpType, preserveId: Long, name: String) {
        mediaDao.query(opType, preserveId, name).also {
            if (it != null) {
                it.mediaInfo.displayBytes = calculateDataSize(it)
                upsert(it)
            }
        }
    }

    suspend fun updateMediaArchivesSize(opType: OpType, name: String) {
        queryMedium(opType, name).onEach {
            it.mediaInfo.displayBytes = calculateArchiveSize(it)
            upsert(it)
        }
    }

    suspend fun preserve(m: MediaEntity) {
        val preserveId = DateUtil.getTimestamp()
        val src = "${archivesMediumDir}/${m.archivesPreserveRelativeDir}"
        val dst = "${archivesMediumDir}/${m.archivesRelativeDir}/${preserveId}"
        rootService.renameTo(src, dst)
        upsert(m.copy(indexInfo = m.indexInfo.copy(preserveId = preserveId)))
    }

    suspend fun delete(m: MediaEntity) {
        val src = "${archivesMediumDir}/${m.archivesPreserveRelativeDir}"
        rootService.deleteRecursively(src)
        mediaDao.delete(m)
    }
}
