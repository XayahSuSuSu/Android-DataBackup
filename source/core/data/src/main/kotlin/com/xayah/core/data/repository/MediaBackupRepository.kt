package com.xayah.core.data.repository

import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.model.MediaBackupEntity
import com.xayah.core.database.model.MediaBackupEntityUpsert
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.rootservice.service.RemoteRootService
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class MediaBackupRepository @Inject constructor(
    private val mediaDao: MediaDao,
    private val rootService: RemoteRootService,
) {
    val medium = mediaDao.observeBackupMedium().distinctUntilChanged()
    val selectedMedium = mediaDao.observeBackupSelected().distinctUntilChanged()

    suspend fun upsertBackup(items: List<MediaBackupEntityUpsert>) = mediaDao.upsertBackup(items)
    suspend fun upsertBackup(item: MediaBackupEntity) = mediaDao.upsertBackup(item)
    suspend fun batchSelectOp(selected: Boolean, pathList: List<String>) = mediaDao.batchSelectOp(selected, pathList)
    suspend fun queryAllBackup() = mediaDao.queryAllBackup()

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

    fun renameDuplicateMedia(name: String): String {
        val nameList = name.split("_").toMutableList()
        val index = nameList.first().toIntOrNull()
        if (index == null) {
            nameList.add("0")
        } else {
            nameList[nameList.lastIndex] = (index + 1).toString()
        }
        return nameList.joinToString(separator = "_")
    }
}
