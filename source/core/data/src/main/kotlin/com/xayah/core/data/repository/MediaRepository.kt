package com.xayah.core.data.repository

import android.content.Context
import com.xayah.core.data.R
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.MediaExtraInfo
import com.xayah.core.model.database.MediaIndexInfo
import com.xayah.core.model.database.MediaInfo
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.Collator
import javax.inject.Inject

class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val cloudRepository: CloudRepository,
    private val mediaDao: MediaDao,
    private val pathUtil: PathUtil,
) {
    companion object {
        private const val TAG = "PackageRepository"
    }

    private fun log(onMsg: () -> String): String = run {
        val msg = onMsg()
        LogUtil.log { TAG to msg }
        msg
    }

    fun queryFlow(opType: OpType, blocked: Boolean) = mediaDao.queryFlow(opType, blocked).distinctUntilChanged()
    fun queryFlow(name: String, opType: OpType, preserveId: Long) = mediaDao.queryFlow(name, opType, preserveId).distinctUntilChanged()
    suspend fun upsert(item: MediaEntity) = mediaDao.upsert(item)
    suspend fun upsert(items: List<MediaEntity>) = mediaDao.upsert(items)
    suspend fun delete(item: MediaEntity) = mediaDao.delete(item)
    suspend fun queryActivated(opType: OpType) = mediaDao.queryActivated(opType)
    suspend fun query(opType: OpType, preserveId: Long, name: String, cloud: String, backupDir: String) = mediaDao.query(opType, preserveId, name, cloud, backupDir)
    suspend fun query(opType: OpType, preserveId: Long, cloud: String, backupDir: String) = mediaDao.query(opType, preserveId, cloud, backupDir)

    fun getArchiveDst(dstDir: String, ct: CompressionType) = "${dstDir}/${DataType.MEDIA_MEDIA.type}.${ct.suffix}"

    fun getKeyPredicateNew(key: String): (MediaEntity) -> Boolean = { m ->
        m.name.lowercase().contains(key.lowercase()) || m.path.lowercase().contains(key.lowercase())
    }

    private fun sortByDataSizeNew(type: SortType): Comparator<MediaEntity> = when (type) {
        SortType.ASCENDING -> {
            compareBy { m -> m.displayStatsBytes }
        }

        SortType.DESCENDING -> {
            compareByDescending { m -> m.displayStatsBytes }
        }
    }

    private fun sortByAlphabetNew(type: SortType): Comparator<MediaEntity> = Comparator { m1, m2 ->
        if (m1 != null && m2 != null) {
            when (type) {
                SortType.ASCENDING -> {
                    Collator.getInstance().let { collator ->
                        collator.getCollationKey(m1.name).compareTo(collator.getCollationKey(m2.name))
                    }
                }

                SortType.DESCENDING -> {
                    Collator.getInstance().let { collator ->
                        collator.getCollationKey(m2.name).compareTo(collator.getCollationKey(m1.name))
                    }
                }
            }
        } else {
            0
        }
    }

    fun getSortComparatorNew(sortIndex: Int, sortType: SortType): Comparator<in MediaEntity> = when (sortIndex) {
        1 -> sortByDataSizeNew(sortType)
        else -> sortByAlphabetNew(sortType)
    }

    suspend fun refresh() = run {
        // Add default medium for first time
        if (mediaDao.count() == 0L) {
            ConstantUtil.DefaultMediaList.forEach { (name, path) ->
                upsert(
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
                            labels = listOf(),
                            blocked = false,
                            activated = true,
                            existed = true,
                        ),
                    )
                )
            }
        }

        val medium = mediaDao.query(opType = OpType.BACKUP, blocked = false)
        medium.forEach { m ->
            val size = rootService.calculateSize(m.path)
            val existed = rootService.exists(m.path)
            mediaDao.upsert(m.copy(mediaInfo = m.mediaInfo.copy(displayBytes = size), extraInfo = m.extraInfo.copy(existed = existed, activated = m.extraInfo.activated && existed)))
        }
    }

    suspend fun updateLocalMediaSize(name: String, opType: OpType, preserveId: Long) {
        query(opType = opType, preserveId = preserveId, name = name, cloud = "", backupDir = "").also {
            if (it != null) {
                it.extraInfo.existed = rootService.exists(it.path)
                if (it.extraInfo.existed) {
                    it.mediaInfo.displayBytes = rootService.calculateSize(it.path)
                }
                upsert(it)
            }
        }
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
                            dataBytes = 0,
                            displayBytes = 0,
                        ),
                        extraInfo = MediaExtraInfo(
                            labels = listOf(),
                            activated = true,
                            blocked = false,
                            existed = true
                        ),
                    )
                )
            }
        }
        mediaDao.upsert(customMediaList)
        return "${context.getString(R.string.succeed)}: ${customMediaList.size}, ${context.getString(R.string.failed)}: $failedCount"
    }
}
