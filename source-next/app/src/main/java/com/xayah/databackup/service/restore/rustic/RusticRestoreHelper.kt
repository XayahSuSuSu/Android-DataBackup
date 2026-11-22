package com.xayah.databackup.service.restore.rustic

import com.xayah.databackup.App
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.RestoreCategory
import com.xayah.databackup.entity.restore.RestoreEvent
import com.xayah.databackup.entity.restore.RestoreProgressCallback
import com.xayah.databackup.entity.restore.RestoreRequest
import com.xayah.databackup.entity.restore.RestoreSource
import com.xayah.databackup.entity.restore.RestoreTask
import com.xayah.databackup.entity.restore.pathsFor
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.restore.RestoreHelper
import com.xayah.databackup.service.restore.toRestoreProgressCallback

internal class RusticRestoreHelper : RestoreHelper {
    override suspend fun restore(request: RestoreRequest, task: RestoreTask, onEvent: (RestoreEvent) -> Unit): List<String> {
        val source = checkNotNull(request.source as? RestoreSource.Rustic) { "Rustic restore requires a Rustic source." }
        val repositoryPath = source.repositoryPath
        val password = source.password
        val snapshotId = source.snapshotId
        return when (task) {
            is RestoreTask.App -> {
                val app = task.source
                val apkPaths = app.pathsFor(BackupSourceCategory.Apk)
                val internalDataPaths = app.pathsFor(BackupSourceCategory.InternalData)
                if (apkPaths.isNotEmpty()) {
                    onEvent(RestoreEvent.AppPartStarted(task, setOf(BackupSourceCategory.Apk)))
                    RemoteRootService.restoreRusticAppApk(
                        repositoryPath = repositoryPath,
                        password = password,
                        snapshotId = snapshotId,
                        packageName = app.packageName,
                        userId = app.userId,
                        apkPaths = apkPaths
                    )
                    onEvent(RestoreEvent.AppPartCompleted(task, setOf(BackupSourceCategory.Apk)))
                }
                if (internalDataPaths.isNotEmpty()) {
                    onEvent(RestoreEvent.AppPartStarted(task, setOf(BackupSourceCategory.InternalData)))
                    RemoteRootService.restoreRusticAppInternalData(
                        repositoryPath = repositoryPath,
                        password = password,
                        snapshotId = snapshotId,
                        packageName = app.packageName,
                        userId = app.userId,
                        sourceUserId = app.userId,
                        internalDataPaths = internalDataPaths,
                    )
                    onEvent(RestoreEvent.AppPartCompleted(task, setOf(BackupSourceCategory.InternalData)))
                }
                for (category in listOf(BackupSourceCategory.ExternalData, BackupSourceCategory.AdditionalData)) {
                    val externalDataPaths = app.pathsFor(category)
                    if (externalDataPaths.isEmpty()) {
                        continue
                    }
                    val parts = setOf(category)
                    onEvent(RestoreEvent.AppPartStarted(task, parts))
                    RemoteRootService.restoreRusticAppExternalData(
                        repositoryPath = repositoryPath,
                        password = password,
                        snapshotId = snapshotId,
                        packageName = app.packageName,
                        userId = app.userId,
                        sourceUserId = app.userId,
                        externalDataPaths = externalDataPaths,
                    )
                    onEvent(RestoreEvent.AppPartCompleted(task, parts))
                }
                emptyList()
            }

            is RestoreTask.Records -> {
                val callback = task.toRestoreProgressCallback(onEvent)
                when (task.category) {
                    RestoreCategory.Apps -> error("App restores require an App task")
                    RestoreCategory.Networks -> RemoteRootService.restoreRusticNetworks(
                        repositoryPath = repositoryPath,
                        password = password,
                        snapshotId = snapshotId,
                        networkIds = task.ids,
                        callback = callback,
                    )

                    RestoreCategory.Contacts -> RemoteRootService.restoreRusticContacts(
                        repositoryPath = repositoryPath,
                        password = password,
                        snapshotId = snapshotId,
                        ids = task.ids,
                        callback = callback,
                    )

                    RestoreCategory.CallLogs -> RemoteRootService.restoreRusticCallLogs(
                        repositoryPath = repositoryPath,
                        password = password,
                        snapshotId = snapshotId,
                        ids = task.ids,
                        callback = callback,
                    )

                    RestoreCategory.Messages -> RemoteRootService.restoreRusticMessages(
                        repositoryPath = repositoryPath,
                        password = password,
                        snapshotId = snapshotId,
                        messageIds = task.ids,
                        callback = callback,
                    )
                }
            }
        }
    }
}
