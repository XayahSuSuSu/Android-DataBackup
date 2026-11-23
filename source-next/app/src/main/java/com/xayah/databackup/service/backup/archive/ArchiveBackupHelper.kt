package com.xayah.databackup.service.backup.archive

import android.os.Process
import android.os.UserHandleHidden
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.entity.BackupBackendType
import com.xayah.databackup.entity.backup.BackupCategory
import com.xayah.databackup.entity.backup.BackupEvent
import com.xayah.databackup.entity.backup.BackupOutcome
import com.xayah.databackup.entity.backup.BackupRequest
import com.xayah.databackup.entity.backup.BackupResult
import com.xayah.databackup.entity.backup.BackupSkippedSource
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.backup.BackupStage
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.archive.ArchiveRecordPaths
import com.xayah.databackup.service.backup.BackupHelper
import com.xayah.databackup.service.backup.BackupMetadataHelper
import com.xayah.databackup.service.backup.BackupMmsSourceHelper
import com.xayah.databackup.service.backup.BackupSerializationHelper
import com.xayah.databackup.util.ArchiveCompressionSetting
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.MaxParallelAppBackups
import com.xayah.databackup.util.MinParallelAppBackups
import com.xayah.databackup.util.ParallelAppBackups
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.SkipAppCache
import com.xayah.databackup.util.readBoolean
import com.xayah.databackup.util.readEnum
import com.xayah.databackup.util.readInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ArchiveBackupHelper(
    private val mAppsHelper: ArchiveBackupAppsHelper,
    private val mSerializationHelper: BackupSerializationHelper,
    private val mMetadataHelper: BackupMetadataHelper,
    private val mMmsHelper: BackupMmsSourceHelper,
) : BackupHelper {
    companion object {
        private const val TAG = "ArchiveBackupHelper"
    }

    /**
     * Archives apps with at most [parallelism] apps in flight, starting them in selection order.
     * Each app writes its own archive files, and the event consumer is thread-safe; tar and Zstandard spend much of
     * their time waiting on storage for small files, so overlapping apps shortens the run. A failure of one app is
     * reported for that app only; cancellation stops every app.
     *
     * @return true if any app reported a failure.
     */
    private suspend fun backupApps(
        apps: List<App>,
        backupPath: String,
        options: ArchiveBackupOptions,
        parallelism: Int,
        onEvent: (BackupEvent) -> Unit,
    ): Boolean = coroutineScope {
        val permits = Semaphore(parallelism)
        apps.map { app ->
            async {
                permits.withPermit {
                    mAppsHelper.backup(app, backupPath, options, onEvent)
                }
            }
        }.awaitAll().any { it }
    }

    override suspend fun backup(request: BackupRequest, onEvent: (BackupEvent) -> Unit): BackupResult {
        check(request.backendType == BackupBackendType.Archive) { "Archive backup requires an Archive destination" }
        val selection = request.getSelection()
        val backupPath = selection.config.path
        val recordUserId = UserHandleHidden.getUserId(Process.myUid())
        val recordPaths = ArchiveRecordPaths.getRelativePaths(recordUserId)
        var hasFailures = false
        val skippedSources = mutableListOf<BackupSkippedSource>()
        onEvent(BackupEvent.StageChanged(BackupStage.WriteBackup))
        if (selection.apps != null) {
            onEvent(BackupEvent.CategoryStarted(BackupCategory.Apps))
            val options = ArchiveBackupOptions(
                isAppCacheSkipped = application.readBoolean(SkipAppCache).first(),
                compression = application.readEnum(ArchiveCompressionSetting).first(),
            )
            val parallelism = if (options.compression.canRunInParallel) {
                application.readInt(ParallelAppBackups).first().coerceIn(MinParallelAppBackups, MaxParallelAppBackups)
            } else {
                1
            }
            hasFailures = backupApps(selection.apps, backupPath, options, parallelism, onEvent)
            val outcome = if (hasFailures) BackupOutcome.Failed else BackupOutcome.Completed
            onEvent(BackupEvent.CategoryCompleted(BackupCategory.Apps, outcome))
        }
        // TODO File backup is not implemented.
        if (selection.files.orEmpty().isNotEmpty()) {
            hasFailures = true
            onEvent(BackupEvent.CategoryCompleted(BackupCategory.Files, BackupOutcome.Unprocessed))
        }
        for (category in selection.getSelectedRecordCategories()) {
            currentCoroutineContext().ensureActive()
            onEvent(BackupEvent.CategoryStarted(category))
            val isCompleted = runCatching {
                RemoteRootService.requireBackupAccessForCurrentUser(listOf(category))
                val files = mSerializationHelper.serializeCategory(selection, category).map { file ->
                    file.copy(relativePath = recordPaths.getValue(file.relativePath))
                }
                mMetadataHelper.writeFiles(backupPath, files)
                var hasAttachmentFailures = false
                if (category == BackupCategory.Messages) {
                    val sources = mMmsHelper.getSources(selection.mms.orEmpty())
                    hasAttachmentFailures = sources.failedRecordIds.isNotEmpty()
                    val attachmentsPath = ArchiveRecordPaths.getMmsAttachmentsDir(backupPath, recordUserId)
                    check(RemoteRootService.mkdirs(attachmentsPath)) { "Cannot create attachment directory" }
                    for (sourcePath in sources.paths) {
                        currentCoroutineContext().ensureActive()
                        if (RemoteRootService.exists(sourcePath)) {
                            val outputPath = "$attachmentsPath/${PathHelper.getChildPath(sourcePath)}"
                            if (!RemoteRootService.copyRecursively(sourcePath, outputPath, true)) {
                                hasAttachmentFailures = true
                            }
                        } else {
                            skippedSources += BackupSkippedSource(
                                path = sourcePath,
                                category = BackupSourceCategory.MmsAttachment,
                                reason = BackupSkippedSource.REASON_NOT_FOUND
                            )
                        }
                    }
                }
                !hasAttachmentFailures
            }.getOrElse { error ->
                currentCoroutineContext().ensureActive()
                if (error is CancellationException || error !is Exception) {
                    throw error
                }
                LogHelper.w(TAG, "backup", "Backup category failed: category=$category, error=${error.javaClass.simpleName}")
                false
            }
            hasFailures = hasFailures || !isCompleted
            val outcome = if (isCompleted) BackupOutcome.Completed else BackupOutcome.Failed
            onEvent(BackupEvent.CategoryCompleted(category, outcome))
        }
        return BackupResult(hasFailures = hasFailures, skippedSources = skippedSources)
    }
}
