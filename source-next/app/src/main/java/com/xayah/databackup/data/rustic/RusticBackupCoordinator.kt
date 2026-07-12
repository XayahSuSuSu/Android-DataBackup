package com.xayah.databackup.data.rustic

import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper

/** Coordinates the Rustic backup workflow from source selection through snapshot creation and finalization. */
class RusticBackupCoordinator(
    private val mSelectionProvider: RusticBackupSelectionProvider,
    private val mSourceCollector: RusticBackupSourceCollector,
    private val mGateway: RusticBackupGateway,
    private val mBackupConfigRepo: BackupConfigRepository,
) {
    companion object {
        private const val TAG = "RusticBackupCoordinator"
        private const val SNAPSHOT_TAG = "databackup"
        private const val SNAPSHOT_CONFIG_TAG_PREFIX = "$SNAPSHOT_TAG:config:"
    }

    suspend fun start(onEvent: (RusticBackupEvent) -> Unit): RusticBackupResult {
        val selection = mSelectionProvider.getSelection()
        val backend = selection.config.backupBackend as? BackupBackend.Rustic ?: throw IllegalStateException("Current backup is not a Rustic backup.")
        val repositoryPath = PathHelper.getBackupRepoDir(selection.config.path)
        val createdAt = System.currentTimeMillis()
        val stagingPath = PathHelper.getRusticStagingDir(selection.config.uuidString, createdAt)

        try {
            onEvent(RusticBackupEvent.StageChanged(RusticBackupStage.PrepareRepository))
            if (mGateway.repositoryExists(repositoryPath)) {
                // Validate the repository config and credentials without performing a full integrity check.
                mGateway.validateRepository(repositoryPath, backend.password)
            } else {
                if (mGateway.exists(repositoryPath) && mGateway.isDirectoryEmpty(repositoryPath).not()) {
                    throw IllegalStateException("Rustic repository config is missing from a non-empty directory.")
                }
                if (mGateway.createDirectory(repositoryPath).not()) {
                    throw IllegalStateException("Failed to create Rustic repository directory.")
                }
                mGateway.initRepository(repositoryPath, backend.password)
            }

            onEvent(RusticBackupEvent.StageChanged(RusticBackupStage.CollectSources))
            val collected = mSourceCollector.collect(selection, stagingPath, createdAt)

            onEvent(RusticBackupEvent.StageChanged(RusticBackupStage.CreateSnapshot))
            val snapshotId = mGateway.createSnapshot(
                repositoryPath = repositoryPath,
                password = backend.password,
                sourcePaths = collected.sourcePaths,
                tags = listOf(
                    SNAPSHOT_TAG,
                    "$SNAPSHOT_CONFIG_TAG_PREFIX${selection.config.uuidString}",
                )
            ) { bytesDone, speed, progress ->
                onEvent(RusticBackupEvent.Progress(bytesDone, speed, progress))
            }.takeIf { it.isNotBlank() } ?: throw IllegalStateException("Rustic returned an empty snapshot ID.")

            onEvent(RusticBackupEvent.StageChanged(RusticBackupStage.FinalizeSnapshot))
            try {
                mBackupConfigRepo.setupBackupConfig()
            } catch (error: Throwable) {
                throw IllegalStateException("Snapshot $snapshotId was created, but backup metadata could not be finalized.", error)
            }

            return RusticBackupResult(snapshotId, collected.sourcePaths.size, collected.skippedSources)
        } finally {
            // Staged metadata is temporary and must not survive a completed or failed backup.
            if (mGateway.deleteRecursively(stagingPath).not()) {
                LogHelper.w(TAG, "start", "Failed to clean Rustic staging directory.")
            }
        }
    }
}
