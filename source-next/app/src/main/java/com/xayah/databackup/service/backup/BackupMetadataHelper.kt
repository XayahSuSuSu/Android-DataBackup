package com.xayah.databackup.service.backup

import com.xayah.databackup.entity.backup.BackupStagedFile
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Writes backup metadata through the root service and reads each file back to verify its contents.
 */
class BackupMetadataHelper {
    suspend fun writeFiles(directoryPath: String, files: List<BackupStagedFile>) {
        for ((relativePath, content) in files) {
            currentCoroutineContext().ensureActive()
            val filePath = "$directoryPath/$relativePath"
            check(RemoteRootService.mkdirs(PathHelper.getParentPath(filePath))) { "Cannot create backup metadata directory" }
            RemoteRootService.writeText(filePath, content)
            check(RemoteRootService.exists(filePath) && RemoteRootService.readText(filePath) == content) {
                "Cannot verify backup metadata write"
            }
        }
    }
}
