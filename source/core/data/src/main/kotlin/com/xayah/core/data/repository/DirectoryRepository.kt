package com.xayah.core.data.repository

import android.content.Context
import com.xayah.core.common.util.toSpaceString
import com.xayah.core.data.R
import com.xayah.core.database.dao.DirectoryDao
import com.xayah.core.database.model.DirectoryEntity
import com.xayah.core.database.model.DirectoryUpsertEntity
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.datastore.saveBackupSaveParentPath
import com.xayah.core.datastore.saveBackupSavePath
import com.xayah.core.datastore.saveRestoreSaveParentPath
import com.xayah.core.datastore.saveRestoreSavePath
import com.xayah.core.model.OpType
import com.xayah.core.model.StorageType
import com.xayah.core.util.command.PreparationUtil
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.name
import kotlin.io.path.pathString

class DirectoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val directoryDao: DirectoryDao,
    private val rootService: RemoteRootService,
) {
    val directories = directoryDao.queryActiveDirectoriesFlow().distinctUntilChanged()

    private suspend fun resetDir(type: OpType) = selectDir(
        type = type,
        parent = ConstantUtil.DefaultPathParent,
        path = ConstantUtil.DefaultPath,
        id = when (type) {
            OpType.BACKUP -> 1
            OpType.RESTORE -> 2
        }
    )

    suspend fun deleteDir(type: OpType, entity: DirectoryEntity) = run {
        if (entity.selected) resetDir(type = type)
        directoryDao.delete(entity)
    }

    suspend fun addDir(type: OpType, pathList: List<String>) {
        val customDirList = mutableListOf<DirectoryUpsertEntity>()
        pathList.forEach { pathString ->
            if (pathString.isNotEmpty()) {
                val path = Paths.get(pathString)
                val parent = path.parent.pathString
                val child = path.name

                // Custom storage
                val dir = DirectoryUpsertEntity(
                    id = directoryDao.queryId(parent = parent, child = child, type = type),
                    title = context.getString(R.string.custom_storage),
                    parent = parent,
                    child = child,
                    opType = type,
                    storageType = StorageType.CUSTOM,
                )
                customDirList.add(dir)
            }
        }

        directoryDao.upsert(customDirList)
    }

    suspend fun selectDir(type: OpType, entity: DirectoryEntity) = run {
        selectDir(type, entity.parent, entity.path, entity.id)
    }

    private suspend fun selectDir(type: OpType, parent: String, path: String, id: Long) = run {
        when (type) {
            OpType.BACKUP -> {
                context.saveBackupSaveParentPath(parent)
                context.saveBackupSavePath(path)
            }

            OpType.RESTORE -> {
                context.saveRestoreSaveParentPath(parent)
                context.saveRestoreSavePath(path)
            }
        }
        directoryDao.select(type = type, id = id)
    }

    suspend fun update(opType: OpType) {
        withIOContext {
            // Inactivate all directories
            directoryDao.updateActive(active = false)

            // Internal storage
            val internalDirs = mutableListOf<DirectoryUpsertEntity>()
            val internalDirectory = DirectoryUpsertEntity(
                id = 1,
                title = context.getString(R.string.internal_storage),
                parent = ConstantUtil.DefaultPathParent,
                child = ConstantUtil.DefaultPathChild,
                opType = OpType.BACKUP,
                storageType = StorageType.INTERNAL,
            )
            internalDirs.add(internalDirectory)
            internalDirs.add(internalDirectory.copy(id = 2, opType = OpType.RESTORE))
            directoryDao.upsert(internalDirs)

            // External storage
            val externalList = PreparationUtil.listExternalStorage().out
            val externalDirs = mutableListOf<DirectoryUpsertEntity>()
            for (storageItem in externalList) {
                // e.g. /mnt/media_rw/E7F9-FA61
                runCatching {
                    val child = ConstantUtil.DefaultPathChild
                    externalDirs.add(
                        DirectoryUpsertEntity(
                            id = directoryDao.queryId(parent = storageItem, child = child, opType),
                            title = context.getString(R.string.external_storage),
                            parent = storageItem,
                            child = child,
                            opType = opType,
                            storageType = StorageType.EXTERNAL,
                            active = true,
                        )
                    )
                }
            }
            directoryDao.upsert(externalDirs)

            // Activate backup/restore directories except external directories
            directoryDao.updateActive(type = opType, excludeType = StorageType.EXTERNAL, active = true)

            // Read statFs of each storage
            directoryDao.queryActiveDirectories().forEach { entity ->
                val parent = entity.parent
                entity.error = ""
                val statFs = rootService.readStatFs(parent)
                entity.availableBytes = statFs.availableBytes
                entity.totalBytes = statFs.totalBytes
                if (entity.storageType == StorageType.EXTERNAL) {
                    val tags = mutableListOf<String>()
                    val type = PreparationUtil.getExternalStorageType(parent).out.firstOrNull() ?: ""
                    tags.add(type)
                    // Check the format
                    val supported = type.lowercase() in ConstantUtil.SupportedExternalStorageFormat
                    if (supported.not()) {
                        tags.add(context.getString(R.string.unsupported_format))
                        entity.error = "${context.getString(R.string.supported_format)}: ${ConstantUtil.SupportedExternalStorageFormat.toSpaceString()}"
                        entity.enabled = false
                    } else {
                        entity.error = ""
                        entity.enabled = true
                    }
                    entity.tags = tags
                }

                directoryDao.upsert(entity)
            }

            val selectedDirectory = directoryDao.querySelectedByDirectoryType(type = opType)
            if (selectedDirectory == null || (selectedDirectory.storageType == StorageType.EXTERNAL && selectedDirectory.enabled.not()) || selectedDirectory.active.not()) {
                resetDir(type = opType)
            }
        }
    }

    suspend fun countActiveDirectories() = directoryDao.countActiveDirectories()
}
