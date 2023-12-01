package com.xayah.core.data.repository

import android.content.Context
import androidx.annotation.StringRes
import com.xayah.core.data.R
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.datastore.readRestoreSavePath
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localRestoreSaveDir
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MediaRestoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao,
    private val rootService: RemoteRootService,
    private val pathUtil: PathUtil,
) {
    private fun log(msg: () -> String) = LogUtil.log { "MediaRestoreRepository" to msg() }

    fun getString(@StringRes resId: Int) = context.getString(resId)

    fun observeMedium(timestamp: Long) = mediaDao.queryMediumFlow(timestamp).distinctUntilChanged()
    val selectedMedium = mediaDao.observeActiveMedium().map { medium -> medium.filter { it.selected } }.distinctUntilChanged()

    val restoreSavePath = context.readRestoreSavePath().distinctUntilChanged()
    private val configsDir = restoreSavePath.map { pathUtil.getConfigsDir(it) }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTimestamps() = restoreSavePath.flatMapLatest { mediaDao.observeTimestamps(it).distinctUntilChanged() }.distinctUntilChanged()

    suspend fun upsertRestore(item: MediaRestoreEntity) = mediaDao.upsertRestore(item)
    suspend fun updateActive(active: Boolean) = mediaDao.updateRestoreActive(active = active)
    suspend fun updateActive(active: Boolean, timestamp: Long, savePath: String) =
        mediaDao.updateRestoreActive(active = active, timestamp = timestamp, savePath = savePath)

    suspend fun writeMediaProtoBuf(dst: String, onStoredList: suspend (MutableList<MediaRestoreEntity>) -> List<MediaRestoreEntity>) {
        val mediaRestoreList: MutableList<MediaRestoreEntity> = mutableListOf()
        runCatching {
            val storedList = rootService.readProtoBuf<List<MediaRestoreEntity>>(src = dst)
            mediaRestoreList.addAll(storedList!!)
        }

        rootService.writeProtoBuf(data = onStoredList(mediaRestoreList), dst = dst)
    }

    suspend fun deleteRestore(items: List<MediaRestoreEntity>) = withIOContext {
        items.forEach { item ->
            val path = "${pathUtil.getLocalRestoreArchivesMediumDir()}/${item.name}/${item.timestamp}"
            log { "Trying to delete: $path" }
            rootService.deleteRecursively(path = path)
        }
        rootService.clearEmptyDirectoriesRecursively(path = context.localRestoreSaveDir())

        val configsDst = PathUtil.getMediaRestoreConfigDst(dstDir = pathUtil.getLocalRestoreConfigsDir())
        writeMediaProtoBuf(configsDst) { storedList ->
            storedList.apply {
                items.forEach { item ->
                    removeIf { it.path == item.path && it.timestamp == item.timestamp && it.savePath == item.savePath }
                }
            }.toList()
        }

        mediaDao.deleteRestore(items)
    }

    suspend fun batchSelectOp(selected: Boolean, timestamp: Long, pathList: List<String>) = mediaDao.batchSelectOp(selected, timestamp, pathList)

    fun getKeyPredicate(key: String): (MediaRestoreEntity) -> Boolean = { mediaRestore ->
        mediaRestore.name.lowercase().contains(key.lowercase()) || mediaRestore.path.lowercase().contains(key.lowercase())
    }

    suspend fun loadLocalConfig() {
        val mediaRestoreList: MutableList<MediaRestoreEntity> = mutableListOf()
        runCatching {
            val configPath = PathUtil.getMediaRestoreConfigDst(dstDir = configsDir.first())
            if (rootService.exists(configPath)) {
                val storedList = rootService.readProtoBuf<List<MediaRestoreEntity>>(src = configPath)
                mediaRestoreList.addAll(storedList!!)
            }
        }
        mediaRestoreList.forEachIndexed { index, mediaInfo ->
            val mediaRestore = mediaDao.queryMedia(path = mediaInfo.path, timestamp = mediaInfo.timestamp, savePath = mediaInfo.savePath)
            val id = mediaRestore?.id ?: 0
            val selected = mediaRestore?.selected ?: false
            mediaDao.upsertRestore(mediaInfo.copy(id = id, selected = selected))
        }
    }

    /**
     * Update sizeBytes, installed state.
     */
    suspend fun update(topBarState: MutableStateFlow<TopBarState>) = withIOContext {
        val medium = mediaDao.queryAllRestore()
        val mediumCount = (medium.size - 1).coerceAtLeast(1)
        // Get 1/10 of total count.
        val epoch: Int = ((mediumCount + 1) / 10).coerceAtLeast(1)

        medium.forEachIndexed { index, entity ->
            val timestampPath = "${pathUtil.getLocalRestoreArchivesMediumDir()}/${entity.name}/${entity.timestamp}"
            val sizeBytes = rootService.calculateSize(timestampPath)
            entity.sizeBytes = sizeBytes
            if (entity.isExists.not()) {
                entity.selected = false
            }

            if (index % epoch == 0)
                topBarState.emit(
                    TopBarState(
                        progress = index.toFloat() / mediumCount,
                        title = StringResourceToken.fromStringId(R.string.updating)
                    )
                )
        }
        mediaDao.upsertRestore(medium)
        topBarState.emit(TopBarState(progress = 1f, title = StringResourceToken.fromStringId(R.string.restore_list)))
    }
}
