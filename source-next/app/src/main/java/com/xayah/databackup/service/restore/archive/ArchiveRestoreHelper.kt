package com.xayah.databackup.service.restore.archive

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

internal class ArchiveRestoreHelper : RestoreHelper {
    override suspend fun restore(request: RestoreRequest, task: RestoreTask, onEvent: (RestoreEvent) -> Unit): List<String> {
        val source = checkNotNull(request.source as? RestoreSource.Archive) { "Archive restore requires an Archive source" }
        return when (task) {
            is RestoreTask.App -> {
                val parts = listOf(
                    BackupSourceCategory.Apk,
                    BackupSourceCategory.InternalData,
                    BackupSourceCategory.ExternalData,
                    BackupSourceCategory.AdditionalData,
                )
                for (part in parts) {
                    val paths = task.source.pathsFor(part)
                    if (paths.isEmpty()) {
                        continue
                    }
                    onEvent(RestoreEvent.AppPartStarted(task, setOf(part)))
                    RemoteRootService.restoreArchiveApp(source.path, task.source.packageName, task.source.userId, paths)
                    onEvent(RestoreEvent.AppPartCompleted(task, setOf(part)))
                }
                emptyList()
            }
            is RestoreTask.Records -> {
                val callback = RestoreProgressCallback { event ->
                    onEvent(when (event) {
                        is RestoreRecordEvent.Started -> RestoreEvent.RecordStarted(task, event.id)
                        is RestoreRecordEvent.Restored -> RestoreEvent.RecordCompleted(task, event.id, skipped = false)
                        is RestoreRecordEvent.Skipped -> RestoreEvent.RecordCompleted(task, event.id, skipped = true)
                        is RestoreRecordEvent.Failed -> RestoreEvent.RecordFailed(task, event.id)
                    })
                }
                when (task.category) {
                    RestoreCategory.Apps -> error("App restores require an App task")
                    RestoreCategory.Networks -> RemoteRootService.restoreArchiveNetworks(source.path, task.ids, callback)
                    RestoreCategory.Contacts -> RemoteRootService.restoreArchiveContacts(source.path, task.ids, callback)
                    RestoreCategory.CallLogs -> RemoteRootService.restoreArchiveCallLogs(source.path, task.ids, callback)
                    RestoreCategory.Messages -> RemoteRootService.restoreArchiveMessages(source.path, task.ids, callback)
                }
            }
        }
    }
}
