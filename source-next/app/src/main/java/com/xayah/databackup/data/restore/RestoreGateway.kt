package com.xayah.databackup.data.restore

import com.xayah.databackup.data.rustic.RusticSourceCategory
import com.xayah.databackup.rootservice.RemoteRootService

internal fun interface RestoreGateway {
    /**
     * Reports record and app-part outcomes as they complete, and returns the skipped IDs from [RestoreTask.ids].
     */
    suspend fun restore(request: RestoreRequest, task: RestoreTask, onEvent: (RestoreEvent) -> Unit): List<String>
}

internal class RusticRestoreGateway : RestoreGateway {
    override suspend fun restore(request: RestoreRequest, task: RestoreTask, onEvent: (RestoreEvent) -> Unit): List<String> {
        val repository = request.repositoryPath
        val password = request.password
        val snapshot = request.snapshotId
        return when (task) {
            is RestoreTask.App -> {
                val app = task.source
                val apkPaths = app.pathsFor(RusticSourceCategory.Apk)
                val internalPaths = app.pathsFor(RusticSourceCategory.InternalData)
                if (apkPaths.isNotEmpty()) {
                    onEvent(RestoreEvent.AppPartStarted(task, setOf(RusticSourceCategory.Apk)))
                    RemoteRootService.restoreRusticAppApk(
                        repositoryPath = repository,
                        password = password,
                        snapshotId = snapshot,
                        packageName = app.packageName,
                        userId = app.userId,
                        apkPaths = apkPaths
                    )
                    onEvent(RestoreEvent.AppPartCompleted(task, setOf(RusticSourceCategory.Apk)))
                }
                if (internalPaths.isNotEmpty()) {
                    onEvent(RestoreEvent.AppPartStarted(task, setOf(RusticSourceCategory.InternalData)))
                    RemoteRootService.restoreRusticAppInternalData(
                        repositoryPath = repository,
                        password = password,
                        snapshotId = snapshot,
                        packageName = app.packageName,
                        userId = app.userId,
                        sourceUserId = app.userId,
                        internalDataPaths = internalPaths,
                    )
                    onEvent(RestoreEvent.AppPartCompleted(task, setOf(RusticSourceCategory.InternalData)))
                }
                for (category in listOf(RusticSourceCategory.ExternalData, RusticSourceCategory.AdditionalData)) {
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
