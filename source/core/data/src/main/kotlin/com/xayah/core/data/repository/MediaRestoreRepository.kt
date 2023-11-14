package com.xayah.core.data.repository

import android.content.Context
import com.xayah.core.data.R
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.database.model.OperationMask
import com.xayah.core.datastore.readRestoreSavePath
import com.xayah.core.service.util.MediumBackupUtil
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.PathUtil
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import javax.inject.Inject

class MediaRestoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao,
    private val rootService: RemoteRootService,
    private val pathUtil: PathUtil,
    private val mediumBackupUtil: MediumBackupUtil,
) {
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

    suspend fun batchSelectOp(selected: Boolean, timestamp: Long, pathList: List<String>) = mediaDao.batchSelectOp(selected, timestamp, pathList)

    fun getKeyPredicate(key: String): (MediaRestoreEntity) -> Boolean = { mediaRestore ->
        mediaRestore.name.lowercase().contains(key.lowercase()) || mediaRestore.path.lowercase().contains(key.lowercase())
    }

    /**
     * Update sizeBytes, installed state.
     */
    suspend fun updateMediaState(entity: MediaRestoreEntity) = withIOContext {
        val timestampPath = "${pathUtil.getLocalRestoreArchivesMediumDir()}/${entity.name}/${entity.timestamp}"
        val sizeBytes = rootService.calculateSize(timestampPath)
        if (entity.sizeBytes != sizeBytes) {
            upsertRestore(entity.copy(sizeBytes = sizeBytes))
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun loadLocalConfig(topBarState: MutableStateFlow<TopBarState>) {
        val mediaRestoreList: MutableList<MediaRestoreEntity> = mutableListOf()
        runCatching {
            val configPath = mediumBackupUtil.getConfigsDst(dstDir = configsDir.first())
            val bytes = rootService.readBytes(configPath)
            mediaRestoreList.addAll(ProtoBuf.decodeFromByteArray<List<MediaRestoreEntity>>(bytes).toMutableList())
        }
        val mediumCount = (mediaRestoreList.size - 1).coerceAtLeast(1)

        // Get 1/10 of total count.
        val epoch: Int = ((mediumCount + 1) / 10).coerceAtLeast(1)

        mediaRestoreList.forEachIndexed { index, mediaInfo ->
            val mediaRestore = mediaDao.queryMedia(path = mediaInfo.path, timestamp = mediaInfo.timestamp, savePath = mediaInfo.savePath)
            val id = mediaRestore?.id ?: 0
            val selected = mediaRestore?.selected ?: false
            mediaDao.upsertRestore(mediaInfo.copy(id = id, selected = selected))

            if (index % epoch == 0)
                topBarState.emit(
                    TopBarState(
                        progress = index.toFloat() / mediumCount,
                        title = StringResourceToken.fromStringId(R.string.updating)
                    )
                )
        }
        topBarState.emit(TopBarState(progress = 1f, title = StringResourceToken.fromStringId(R.string.restore_list)))
    }
}
