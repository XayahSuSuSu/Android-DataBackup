package com.xayah.databackup.data

import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.rustic.RusticBackupEvent
import com.xayah.databackup.entity.rustic.RusticBackupResult
import com.xayah.databackup.entity.rustic.RusticBackupStage
import com.xayah.databackup.entity.rustic.RusticSnapshot
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.backup.rustic.RusticBackupSourceHelper
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Coordinates the Rustic backup workflow from source selection through snapshot creation and finalization. */
class RusticBackupProcessRepository(
    private val mBackupSelectionRepo: BackupSelectionRepository,
    private val mSourceHelper: RusticBackupSourceHelper,
    private val mRusticRepo: RusticRepository,
    private val mBackupConfigRepo: BackupConfigRepository,
) {
    companion object {
        private const val TAG = "RusticBackupProcessRepository"
    }

    suspend fun start(onEvent: (RusticBackupEvent) -> Unit): RusticBackupResult {
        val selection = mBackupSelectionRepo.getSelection()
        val backend = selection.config.backupBackend as? BackupBackend.Rustic ?: throw IllegalStateException("Current backup is not a Rustic backup.")
        val repositoryPath = PathHelper.getBackupRepoDir(selection.config.path)
        val createdAt = System.currentTimeMillis()
        val stagingPath = PathHelper.getRusticStagingDir(selection.config.uuidString, createdAt)

        try {
            onEvent(RusticBackupEvent.StageChanged(RusticBackupStage.PrepareRepository))
            mRusticRepo.prepareRepository(repositoryPath, backend.password)

            onEvent(RusticBackupEvent.StageChanged(RusticBackupStage.CollectSources))
            val collected = mSourceHelper.collectSources(selection, stagingPath, createdAt)

            onEvent(RusticBackupEvent.StageChanged(RusticBackupStage.CreateSnapshot))
            val snapshotId = mRusticRepo.createSnapshot(
                repositoryPath = repositoryPath,
                password = backend.password,
                sourcePaths = collected.sourcePaths,
                tags = RusticSnapshot.createBackupTags(selection.config.uuidString),
            ) { bytesDone, speed, progress ->
                onEvent(RusticBackupEvent.Progress(bytesDone, speed, progress))
            }.takeIf { it.isNotBlank() } ?: throw IllegalStateException("Rustic returned an empty snapshot ID.")

            onEvent(RusticBackupEvent.StageChanged(RusticBackupStage.FinalizeSnapshot))
            runCatching {
                mBackupConfigRepo.setupBackupConfig()
            }.onFailure { error ->
                if (error is CancellationException || error !is Exception) throw error
                throw IllegalStateException("Snapshot $snapshotId was created, but backup metadata could not be finalized.", error)
            }

            return RusticBackupResult(snapshotId, collected.includedCount, collected.skippedSources)
        } finally {
            // Staged metadata is temporary and must not survive a completed or failed backup.
            withContext(NonCancellable) {
                if (RemoteRootService.deleteRecursively(stagingPath).not()) {
                    LogHelper.w(TAG, "start", "Failed to clean Rustic staging directory.")
                }
            }
        }
    }
}
