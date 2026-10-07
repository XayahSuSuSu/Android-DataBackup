package com.xayah.databackup.service.restore.archive

import android.content.pm.ApplicationInfo
import android.system.Os
import android.system.OsConstants
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.service.restore.InstallApkHelper
import com.xayah.databackup.service.restore.RestoreExternalDataHelper
import com.xayah.databackup.service.restore.RestoreInternalDataHelper
import com.xayah.databackup.util.PathHelper
import java.io.File
import java.util.UUID

/**
 * Restores one selected part of an application's Archive backup per invocation.
 * Must run in the root process; the caller must serialize restores and provide a callback to stop the app.
 *
 * Streams selected Zstandard archives through [ArchiveTarHelper] for validation before installing APKs
 * or modifying app data, then streams them again for extraction. Sources must remain unchanged during restore.
 * Delegates installation to [InstallApkHelper] and data restoration to [RestoreInternalDataHelper]
 * or [RestoreExternalDataHelper], which handle destination preparation and metadata repair.
 * App data is restored in place without rollback. Attempts to remove the temporary directory after
 * either success or failure, without following symbolic links during cleanup.
 */
internal class ArchiveRestoreAppHelper(private val mCacheDir: File) {
    suspend fun restore(
        archivePath: String,
        packageName: String,
        paths: List<String>,
        app: ApplicationInfo?,
        seInfo: String?,
        installer: InstallApkHelper,
        stopApp: () -> Unit,
    ) {
        val categoryByArchivePath = ArchiveRestorePaths.getAppArchivePaths(archivePath, packageName)
        require(paths.isNotEmpty() && paths.distinct().size == paths.size && categoryByArchivePath.keys.containsAll(paths)) {
            "Invalid app archive selection"
        }
        val categories = paths.map { categoryByArchivePath.getValue(it) }.toSet()
        require(categories.size == 1) { "Restore one app part at a time" }
        val category = categories.single()
        val archiveType = when (category) {
            BackupSourceCategory.Apk -> ArchiveTarType.Apk
            BackupSourceCategory.InternalData -> ArchiveTarType.InternalData
            BackupSourceCategory.ExternalData, BackupSourceCategory.AdditionalData -> ArchiveTarType.ExternalData
            else -> error("Unsupported app part")
        }
        val stagingDir = File(PathHelper.getCacheDir("restore-archive", mCacheDir), UUID.randomUUID().toString())
        check(stagingDir.mkdir()) { "Cannot create archive staging directory" }
        val result = runCatching {
            val tarHelperByArchivePath = paths.mapIndexed { index, archiveFilePath ->
                val sourceArchiveFile = File(archiveFilePath)
                check(OsConstants.S_ISREG(Os.lstat(sourceArchiveFile.path).st_mode)) { "Archive is not a regular file" }
                val archiveWorkDir = File(stagingDir, index.toString())
                check(archiveWorkDir.mkdir()) { "Cannot create archive working directory" }
                archiveFilePath to ArchiveTarHelper(
                    mArchive = sourceArchiveFile,
                    mWorkDir = archiveWorkDir,
                    mPackageName = packageName,
                    mArchiveType = archiveType,
                ).apply { validate() }
            }.toMap()
            tarHelperByArchivePath.values.forEach { it.validateSource() }
            when (category) {
                BackupSourceCategory.Apk -> {
                    val apkExtractDir = File(stagingDir, "apk")
                    check(apkExtractDir.mkdir())
                    tarHelperByArchivePath.values.single().extract(apkExtractDir)
                    installer.install(packageName, checkNotNull(apkExtractDir.listFiles()).toList())
                }
                BackupSourceCategory.InternalData -> {
                    val archivePathByIsCe = mapOf(
                        true to PathHelper.getBackupAppsUserFilePath(archivePath, packageName),
                        false to PathHelper.getBackupAppsUserDeFilePath(archivePath, packageName),
                    ).filterValues { it in tarHelperByArchivePath }
                    RestoreInternalDataHelper().restore(
                        checkNotNull(app), checkNotNull(seInfo), archivePathByIsCe,
                        sourceUid = { tarHelperByArchivePath.getValue(it).sourceUid },
                        extract = { archiveFilePath, targetDir -> tarHelperByArchivePath.getValue(archiveFilePath).extract(targetDir) },
                        stopApp = stopApp,
                    )
                }
                BackupSourceCategory.ExternalData, BackupSourceCategory.AdditionalData -> {
                    val archivePathByDataDirName = mapOf(
                        PathHelper.EXTERNAL_DATA_DIR_NAME to PathHelper.getBackupAppsDataFilePath(archivePath, packageName),
                        PathHelper.EXTERNAL_OBB_DIR_NAME to PathHelper.getBackupAppsObbFilePath(archivePath, packageName),
                        PathHelper.EXTERNAL_MEDIA_DIR_NAME to PathHelper.getBackupAppsMediaFilePath(archivePath, packageName),
                    ).filterValues { it in tarHelperByArchivePath }
                    RestoreExternalDataHelper().restore(
                        archivePath, checkNotNull(app), archivePathByDataDirName,
                        validate = { check(it in tarHelperByArchivePath) },
                        extract = { archiveFilePath, targetDir -> tarHelperByArchivePath.getValue(archiveFilePath).extract(targetDir) },
                        stopApp = stopApp,
                    )
                }
            }
        }
        runCatching { deleteStaging(stagingDir) }.onFailure { cleanupError ->
            result.exceptionOrNull()?.addSuppressed(cleanupError) ?: throw cleanupError
        }
        result.getOrThrow()
    }

    private fun deleteStaging(file: File) {
        if (OsConstants.S_ISDIR(Os.lstat(file.path).st_mode)) checkNotNull(file.listFiles()).forEach(::deleteStaging)
        check(file.delete()) { "Failed to clean archive staging directory" }
    }
}
