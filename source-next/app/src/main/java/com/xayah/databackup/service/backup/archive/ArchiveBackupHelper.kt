package com.xayah.databackup.service.backup.archive

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
import com.xayah.databackup.service.backup.BackupHelper
import com.xayah.databackup.service.backup.BackupMetadataHelper
import com.xayah.databackup.service.backup.BackupMmsSourceHelper
import com.xayah.databackup.service.backup.BackupSerializationHelper
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class ArchiveBackupHelper(
    private val mAppsHelper: ArchiveBackupAppsHelper,
    private val mSerializationHelper: BackupSerializationHelper,
    private val mMetadataHelper: BackupMetadataHelper,
    private val mMmsHelper: BackupMmsSourceHelper,
) : BackupHelper {
    companion object {
        private const val TAG = "ArchiveBackupHelper"
    }

    override suspend fun backup(request: BackupRequest, onEvent: (BackupEvent) -> Unit): BackupResult {
        check(request.backendType == BackupBackendType.Archive) { "Archive backup requires an Archive destination" }
        val selection = request.getSelection()
        val backupPath = selection.config.path
        var hasFailures = false
        val skippedSources = mutableListOf<BackupSkippedSource>()
        onEvent(BackupEvent.StageChanged(BackupStage.WriteBackup))
        if (selection.apps != null) {
            onEvent(BackupEvent.CategoryStarted(BackupCategory.Apps))
            for (app in selection.apps) {
                val hasAppFailures = mAppsHelper.backup(app, backupPath, onEvent)
                hasFailures = hasFailures || hasAppFailures
            }
            val outcome = if (hasFailures) BackupOutcome.Failed else BackupOutcome.Completed
            onEvent(BackupEvent.CategoryCompleted(BackupCategory.Apps, outcome))
        }
        // TODO File backup is not implemented.
        if (selection.files.orEmpty().isNotEmpty()) {
            hasFailures = true
            onEvent(BackupEvent.CategoryCompleted(BackupCategory.Files, BackupOutcome.Unprocessed))
        }
        val categories = buildList {
            if (selection.networks != null) {
                add(BackupCategory.Networks)
            }
            if (selection.contacts != null) {
                add(BackupCategory.Contacts)
            }
            if (selection.callLogs != null) {
                add(BackupCategory.CallLogs)
            }
            if (selection.sms != null || selection.mms != null) {
                add(BackupCategory.Messages)
            }
        }
        for (category in categories) {
            currentCoroutineContext().ensureActive()
            onEvent(BackupEvent.CategoryStarted(category))
            val isCompleted = runCatching {
                mMetadataHelper.writeFiles(backupPath, mSerializationHelper.serializeCategory(selection, category))
                var hasAttachmentFailures = false
                if (category == BackupCategory.Messages) {
                    val sources = mMmsHelper.getSources(selection.mms.orEmpty())
                    hasAttachmentFailures = sources.failedRecordIds.isNotEmpty()
                    val attachmentsPath = PathHelper.getBackupAppPartsDir(PathHelper.getBackupMessagesDir(backupPath))
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
