package com.xayah.databackup.service.restore.rustic

import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.RestoreCategory
import com.xayah.databackup.entity.restore.RestoreEvent
import com.xayah.databackup.entity.restore.RestoreProgressCallback
import com.xayah.databackup.entity.restore.RestoreRecordEvent
import com.xayah.databackup.entity.restore.RestoreRequest
import com.xayah.databackup.entity.restore.RestoreSource
import com.xayah.databackup.entity.restore.RestoreTask
import com.xayah.databackup.entity.restore.pathsFor
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.restore.RestoreHelper

internal class RusticRestoreHelper : RestoreHelper {
    override suspend fun restore(request: RestoreRequest, task: RestoreTask, onEvent: (RestoreEvent) -> Unit): List<String> {
        val source = checkNotNull(request.source as? RestoreSource.Rustic) { "Rustic restore requires a Rustic source." }
        val repository = source.repositoryPath
        val password = source.password
        val snapshot = source.snapshotId
        return when (task) {
            is RestoreTask.App -> {
                val app = task.source
                val apkPaths = app.pathsFor(BackupSourceCategory.Apk)
                val internalPaths = app.pathsFor(BackupSourceCategory.InternalData)
                if (apkPaths.isNotEmpty()) {
                    onEvent(RestoreEvent.AppPartStarted(task, setOf(BackupSourceCategory.Apk)))
                    RemoteRootService.restoreRusticAppApk(
                        repositoryPath = repository,
                        password = password,
                        snapshotId = snapshot,
                        packageName = app.packageName,
                        userId = app.userId,
                        apkPaths = apkPaths
                    )
                    onEvent(RestoreEvent.AppPartCompleted(task, setOf(BackupSourceCategory.Apk)))
                }
                if (internalPaths.isNotEmpty()) {
                    onEvent(RestoreEvent.AppPartStarted(task, setOf(BackupSourceCategory.InternalData)))
                    RemoteRootService.restoreRusticAppInternalData(
                        repositoryPath = repository,
                        password = password,
                        snapshotId = snapshot,
                        packageName = app.packageName,
                        userId = app.userId,
                        sourceUserId = app.userId,
                        internalDataPaths = internalPaths,
                    )
                    onEvent(RestoreEvent.AppPartCompleted(task, setOf(BackupSourceCategory.InternalData)))
                }
                for (category in listOf(BackupSourceCategory.ExternalData, BackupSourceCategory.AdditionalData)) {
                    val externalPaths = app.pathsFor(category)
                    if (externalPaths.isEmpty()) continue
                    val parts = setOf(category)
                    onEvent(RestoreEvent.AppPartStarted(task, parts))
                    RemoteRootService.restoreRusticAppExternalData(
                        repositoryPath = repository,
                        password = password,
                        snapshotId = snapshot,
                        packageName = app.packageName,
                        userId = app.userId,
                        sourceUserId = app.userId,
                        externalDataPaths = externalPaths,
                    )
                    onEvent(RestoreEvent.AppPartCompleted(task, parts))
                }
                emptyList()
            }

            is RestoreTask.Records -> {
                val callback = RestoreProgressCallback { event ->
                    onEvent(
                        when (event) {
                            is RestoreRecordEvent.Started -> RestoreEvent.RecordStarted(task, event.id)
                            is RestoreRecordEvent.Restored -> RestoreEvent.RecordCompleted(task, event.id, skipped = false)
                            is RestoreRecordEvent.Skipped -> RestoreEvent.RecordCompleted(task, event.id, skipped = true)
                            is RestoreRecordEvent.Failed -> RestoreEvent.RecordFailed(task, event.id)
                        }
                    )
                }
                when (task.category) {
                    RestoreCategory.Apps -> error("App restores require an App task")
                    RestoreCategory.Networks -> RemoteRootService.restoreRusticNetworks(
                        repositoryPath = repository,
                        password = password,
                        snapshotId = snapshot,
                        networkIds = task.ids,
                        callback = callback,
                    )

                    RestoreCategory.Contacts -> RemoteRootService.restoreRusticContacts(
                        repositoryPath = repository,
                        password = password,
                        snapshotId = snapshot,
                        contactIds = task.ids,
                        callback = callback,
                    )

                    RestoreCategory.CallLogs -> RemoteRootService.restoreRusticCallLogs(
                        repositoryPath = repository,
                        password = password,
                        snapshotId = snapshot,
                        callLogIds = task.ids,
                        callback = callback,
                    )

                    RestoreCategory.Messages -> RemoteRootService.restoreRusticMessages(
                        repositoryPath = repository,
                        password = password,
                        snapshotId = snapshot,
                        messageIds = task.ids,
                        callback = callback,
                    )
                }
            }
        }
    }
}
