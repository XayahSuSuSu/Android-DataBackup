package com.xayah.core.data.repository

import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.model.MediaBackupEntity
import com.xayah.core.database.model.MediaBackupEntityUpsert
import com.xayah.core.datastore.ConstantUtil
import com.xayah.librootservice.service.RemoteRootService
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class MediaBackupRepository @Inject constructor(
    private val mediaDao: MediaDao,
    private val rootService: RemoteRootService,
) {
    val medium = mediaDao.observeBackupMedium().distinctUntilChanged()
    val selectedMedium = mediaDao.observeBackupSelected().distinctUntilChanged()

    private suspend fun upsertBackup(items: List<MediaBackupEntityUpsert>) = mediaDao.upsertBackup(items)
    suspend fun upsertBackup(item: MediaBackupEntity) = mediaDao.upsertBackup(item)
    suspend fun batchSelectOp(selected: Boolean, pathList: List<String>) = mediaDao.batchSelectOp(selected, pathList)

    suspend fun updateDefaultMedium() {
        upsertBackup(ConstantUtil.DefaultMediaList.map { (name, path) -> MediaBackupEntityUpsert(path = path, name = name) })
    }

    suspend fun refreshMedia(entity: MediaBackupEntity) {
        val sizeBytes = rootService.calculateSize(entity.path)
        if (entity.sizeBytes != sizeBytes) {
            upsertBackup(entity.copy(sizeBytes = sizeBytes))
        }
    }

    fun getKeyPredicate(key: String): (MediaBackupEntity) -> Boolean = { mediaBackup ->
        mediaBackup.name.lowercase().contains(key.lowercase()) || mediaBackup.path.lowercase().contains(key.lowercase())
    }
}
