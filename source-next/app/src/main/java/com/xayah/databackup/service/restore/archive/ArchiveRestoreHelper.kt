package com.xayah.databackup.service.restore.archive

import android.Manifest
import android.content.pm.PackageManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
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
import com.xayah.databackup.service.restore.RestoreCallLogsHelper
import com.xayah.databackup.service.restore.RestoreContactsHelper
import com.xayah.databackup.service.restore.RestoreHelper
import com.xayah.databackup.service.restore.toRestoreProgressCallback
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ArchiveRestoreHelper : RestoreHelper {
    private val mMetadataAdapter = Moshi.Builder().build().adapter<Map<String, String>>()

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
                val callback = task.toRestoreProgressCallback(onEvent)
                when (task.category) {
                    RestoreCategory.Apps -> error("App restores require an App task")
                    RestoreCategory.Networks -> RemoteRootService.restoreArchiveNetworks(source.path, task.ids, callback)
                    RestoreCategory.Contacts -> restoreContacts(source.path, task.ids, callback)
                    RestoreCategory.CallLogs -> restoreCallLogs(source.path, task.ids, callback)
                    RestoreCategory.Messages -> RemoteRootService.restoreArchiveMessages(source.path, task.ids, callback)
                }
            }
        }
    }

    private suspend fun restoreContacts(archivePath: String, ids: List<String>, callback: RestoreProgressCallback): List<String> =
        withContext(Dispatchers.IO) {
            check(App.application.checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                "Contacts restore requires WRITE_CONTACTS permission"
            }
            val metadataPath = PathHelper.getBackupContactsConfigFilePath(archivePath)
            val serializedFiles = mMetadataAdapter.toJson(mapOf(metadataPath to RemoteRootService.readText(metadataPath)))
            RestoreContactsHelper(App.application.contentResolver).restore(serializedFiles, metadataPath, ids, callback)
        }

    private suspend fun restoreCallLogs(archivePath: String, ids: List<String>, callback: RestoreProgressCallback): List<String> =
        withContext(Dispatchers.IO) {
            check(
                listOf(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.WRITE_CALL_LOG
                ).all { App.application.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
            ) { "Call logs restore requires READ_CALL_LOG and WRITE_CALL_LOG permissions" }
            val metadataPath = PathHelper.getBackupCallLogsConfigFilePath(archivePath)
            val serializedFiles = mMetadataAdapter.toJson(mapOf(metadataPath to RemoteRootService.readText(metadataPath)))
            RestoreCallLogsHelper(App.application.contentResolver).restore(serializedFiles, metadataPath, ids, callback)
        }
}
