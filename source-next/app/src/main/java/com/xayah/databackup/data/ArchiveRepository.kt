package com.xayah.databackup.data

import android.os.Process
import android.os.UserHandleHidden
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.Info
import com.xayah.databackup.database.entity.Option
import com.xayah.databackup.database.entity.Storage
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.backup.BackupSourcePath
import com.xayah.databackup.entity.restore.RestoreSource
import com.xayah.databackup.entity.restore.RestoreSourceInfo
import com.xayah.databackup.entity.restore.RestoreState
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.restore.RestoreMetadataHelper
import com.xayah.databackup.service.restore.RestoreRecordId
import com.xayah.databackup.service.restore.archive.ArchiveRestorePaths
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArchiveRepository {
    suspend fun loadRestoreState(config: BackupConfig): RestoreState = withContext(Dispatchers.IO) {
        require(config.path.isNotBlank()) { "Archive path is empty" }
        check(RemoteRootService.exists(config.path)) { "Archive is unavailable" }
        val files = buildMap {
            for (relative in ArchiveRestorePaths.structuredFileRelativePaths) {
                val path = "${config.path}/$relative"
                if (RemoteRootService.exists(path)) {
                    put(relative, RemoteRootService.readText(path))
                }
            }
        }
        // Archive keeps one backup per package; restore it to the user running DataBackup.
        val userId = UserHandleHidden.getUserId(Process.myUid())
        val apps = mutableMapOf<String, App>()
        val sources = mutableMapOf<String, List<BackupSourcePath>>()
        for (directory in RemoteRootService.listFilePaths(path = PathHelper.getBackupAppsDir(config.path), listFiles = false, listDirs = true)) {
            val packageName = PathHelper.getChildPath(directory.path)
            val paths = ArchiveRestorePaths.getAppArchivePaths(config.path, packageName).filterKeys { RemoteRootService.exists(it) }
            if (paths.isEmpty()) {
                continue
            }
            val id = RestoreRecordId.getAppId(userId, packageName)
            apps[id] = App(
                packageName = packageName,
                userId = userId,
                info = Info(label = packageName),
                option = Option(
                    apk = false,
                    internalData = false,
                    externalData = false,
                    additionalData = false
                ),
                storage = Storage()
            )
            sources[id] = paths.map { (path, category) -> BackupSourcePath(path, category) }
        }
        val inventory = RestoreMetadataHelper().deserializeInventory(files).copy(
            usersMap = mapOf(userId to userId.toString()),
            apps = apps,
            appSources = sources,
            availableAppParts = sources.mapValues { (_, paths) -> paths.map { it.category }.toSet() },
        )
        RestoreState(
            loading = false,
            config = config,
            source = RestoreSource.Archive(config.path),
            sourceInfo = RestoreSourceInfo(config.uuidString, config.updatedAt, null),
            inventory = inventory,
            selected = inventory.allIds,
            appParts = inventory.availableAppParts,
        )
    }
}
