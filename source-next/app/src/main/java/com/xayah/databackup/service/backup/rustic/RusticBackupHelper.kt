package com.xayah.databackup.service.backup.rustic

import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.data.RusticRepository
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.backup.BackupCategory
import com.xayah.databackup.entity.backup.BackupEvent
import com.xayah.databackup.entity.backup.BackupOutcome
import com.xayah.databackup.entity.backup.BackupRequest
import com.xayah.databackup.entity.backup.BackupResult
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.backup.BackupStage
import com.xayah.databackup.entity.backup.toInitialState
import com.xayah.databackup.entity.rustic.RusticSnapshot
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.backup.BackupAppSourceHelper
import com.xayah.databackup.service.backup.BackupHelper
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.SkipAppCache
import com.xayah.databackup.util.readBoolean
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class RusticBackupHelper(
    private val mSourceHelper: RusticBackupSourceHelper,
    private val mRusticRepo: RusticRepository,
    private val mAppSourceHelper: BackupAppSourceHelper,
) : BackupHelper {
    companion object {
        private const val TAG = "RusticBackupHelper"
    }

    override suspend fun backup(request: BackupRequest, onEvent: (BackupEvent) -> Unit): BackupResult {
        val selection = request.getSelection()
        RemoteRootService.requireBackupAccessForCurrentUser(selection.getSelectedRecordCategories())
        val backend = checkNotNull(selection.config.backupBackend as? BackupBackend.Rustic)
        val repositoryPath = PathHelper.getBackupRepoDir(selection.config.path)
        val stagingPath = PathHelper.getRusticStagingDir(selection.config.uuidString, request.createdAt)
        var isCleanupSuccessful = true
        val result = try {
            onEvent(BackupEvent.StageChanged(BackupStage.PrepareDestination))
            mRusticRepo.prepareRepository(repositoryPath, backend.password)
            currentCoroutineContext().ensureActive()
            onEvent(BackupEvent.StageChanged(BackupStage.CollectSources))
            val sources = mSourceHelper.collectSources(
                selection = selection,
                stagingPath = stagingPath,
                isAppCacheSkipped = application.readBoolean(SkipAppCache).first(),
                createdAt = request.createdAt,
            )
            currentCoroutineContext().ensureActive()
            onEvent(BackupEvent.StageChanged(BackupStage.WriteBackup))
            val snapshotId = mRusticRepo.createSnapshot(
                repositoryPath = repositoryPath,
                password = backend.password,
                sourcePaths = sources.sourcePaths,
                excludedPaths = sources.excludedPaths,
                tags = RusticSnapshot.createBackupTags(selection.config.uuidString),
            ) { bytes, speed, progress ->
                onEvent(BackupEvent.Progress(bytes, speed, progress))
            }
            check(snapshotId.isNotBlank()) { "Rustic returned an empty snapshot ID" }
            onEvent(BackupEvent.SnapshotCreated(snapshotId))
            currentCoroutineContext().ensureActive()
            // Report item outcomes only after the snapshot is saved successfully.
            val initialState = request.toInitialState()
            for (app in selection.apps.orEmpty()) {
                val plan = sources.apps.first { it.packageName == app.packageName && it.userId == app.userId }
                val paths = mAppSourceHelper.getCandidatePaths(app, emptyList()).groupBy { it.category }
                onEvent(BackupEvent.AppStarted(app.pkgUserKey))
                for ((category, index) in initialState.appsProgress.first { it.key == app.pkgUserKey }.parts) {
                    val isIncluded = if (category == BackupSourceCategory.Apk) {
                        plan.included.any { it.category == BackupSourceCategory.Apk }
                    } else {
                        val path = paths.getValue(category)[index].path
                        plan.included.any { it.path == path }
                    }
                    val outcome = if (isIncluded) BackupOutcome.Completed else BackupOutcome.Skipped
                    onEvent(BackupEvent.PartChanged(app.pkgUserKey, category, index, outcome))
                }
                onEvent(BackupEvent.AppCompleted(app.pkgUserKey))
            }
            for ((category) in initialState.categoryProgress.filter { it.isSelected }) {
                val outcome = if (category == BackupCategory.Messages && sources.hasFailures) {
                    BackupOutcome.Failed
                } else {
                    BackupOutcome.Completed
                }
                onEvent(BackupEvent.CategoryCompleted(category, outcome))
            }
            BackupResult(snapshotId, sources.includedCount, sources.skippedSources, sources.hasFailures)
        } finally {
            withContext(NonCancellable) {
                runCatching {
                    check(RemoteRootService.deleteRecursively(stagingPath))
                }.onFailure {
                    isCleanupSuccessful = false
                    // Cleanup must not hide the original failure or a successfully created snapshot.
                    LogHelper.w(TAG, "backup", "Failed to clean Rustic staging directory")
                }
            }
        }
        return result.copy(hasFailures = result.hasFailures || !isCleanupSuccessful)
    }
}
