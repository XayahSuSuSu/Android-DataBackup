package com.xayah.databackup.service.restore.rustic

import android.Manifest
import android.content.pm.PackageManager
import com.xayah.databackup.App
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.RestoreCategory
import com.xayah.databackup.entity.restore.RestoreEvent
import com.xayah.databackup.entity.restore.RestoreProgressCallback
import com.xayah.databackup.entity.restore.RestoreRequest
import com.xayah.databackup.entity.restore.RestoreSource
import com.xayah.databackup.entity.restore.RestoreTask
import com.xayah.databackup.entity.restore.pathsFor
import com.xayah.databackup.entity.rustic.requireFullSnapshotId
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.restore.RestoreCallLogsHelper
import com.xayah.databackup.service.restore.RestoreContactsHelper
import com.xayah.databackup.service.restore.RestoreHelper
import com.xayah.databackup.service.restore.toRestoreProgressCallback
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

                    RestoreCategory.Contacts -> restoreContacts(
                        repositoryPath = repositoryPath,
                        password = password,
                        snapshotId = snapshotId,
                        contactIds = task.ids,
                        callback = callback,
                    )

                    RestoreCategory.CallLogs -> restoreCallLogs(
                        repositoryPath = repositoryPath,
                        password = password,
                        snapshotId = snapshotId,
                        callLogIds = task.ids,
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

    /**
     * Restores selected contact records from a Rustic snapshot.
     *
     * Imports records as local contacts for the app's current user without overwriting existing contacts.
     * Requires WRITE_CONTACTS permission. The root service reads the snapshot, and the app writes to Contacts Provider.
     * Allocates new raw contact and data IDs; source IDs, account/sync metadata and group memberships are not imported.
     * Skips deleted records and records with no remaining data rows after filtering group memberships and photos.
     * Repeated selection keys are imported once per call, but repeated calls can create duplicate contacts.
     * Each contact is inserted in one transaction. Earlier imports are not rolled back if restoration fails.
     *
     * @param repositoryPath Path to the Rustic repository on the device.
     * @param password Password used to access the repository.
     * @param snapshotId Full 64-character hexadecimal snapshot ID.
     * @param contactIds Non-empty list of restore inventory keys in the form `contact:<index>`,
     * where `index` is the zero-based record position in the snapshot, not a source-device contact ID.
     * @param callback Synchronously receives restore progress and results for each record.
     * @return Distinct inventory keys of skipped records in selection order. Empty if no records were
     * skipped, or all selected keys if no records contain restorable data.
     * @throws IllegalArgumentException If the snapshot ID, selection or selected contact backup data is invalid.
     * @throws IllegalStateException If WRITE_CONTACTS permission is missing, the root service is unavailable,
     * or shared contact restore setup fails.
     */
    private suspend fun restoreContacts(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        contactIds: List<String>,
        callback: RestoreProgressCallback,
    ): List<String> = withContext(Dispatchers.IO) {
        requireFullSnapshotId(snapshotId)
        require(contactIds.isNotEmpty()) { "No contacts selected" }
        check(App.application.checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            "Contacts restore requires WRITE_CONTACTS permission"
        }
        val metadataPath = PathHelper.getRusticSnapshotMetadataFilePath(PathHelper.getBackupContactsConfigFileRelativePath())
        val serializedFiles = RemoteRootService.readRusticSnapshotTextFiles(repositoryPath, password, snapshotId, listOf(metadataPath))
        RestoreContactsHelper(App.application.contentResolver).restore(serializedFiles, metadataPath, contactIds, callback)
    }

    /**
     * Restores selected call logs from a Rustic snapshot for the app's current user.
     *
     * Requires READ_CALL_LOG and WRITE_CALL_LOG. The root service reads the snapshot, and the app writes to CallLogProvider.
     * Imports portable fields supported by the target Android version without source IDs or SIM/account associations.
     * Skips duplicates, voicemail and unsupported call types. Restored calls are marked read.
     * Earlier imports are not rolled back if restoration fails.
     *
     * @param repositoryPath Path to the Rustic repository on the device.
     * @param password Password used to access the repository.
     * @param snapshotId Full 64-character hexadecimal snapshot ID.
     * @param callLogIds Non-empty list of inventory keys in the form `call:<index>`, where `index` is the
     * zero-based record position in the snapshot, not a source-device call ID.
     * @param callback Synchronously receives restore progress and results for each record.
     * @return Distinct inventory keys of skipped records in selection order. Empty if none were skipped.
     * @throws IllegalArgumentException If the snapshot ID, selection or selected call log data is invalid.
     * @throws IllegalStateException If permissions are missing, the root service is unavailable or restoration fails.
     */
    private suspend fun restoreCallLogs(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        callLogIds: List<String>,
        callback: RestoreProgressCallback,
    ): List<String> = withContext(Dispatchers.IO) {
        requireFullSnapshotId(snapshotId)
        require(callLogIds.isNotEmpty()) { "No call logs selected" }
        check(listOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG).all {
            App.application.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }) { "Call logs restore requires READ_CALL_LOG and WRITE_CALL_LOG permissions" }
        val metadataPath = PathHelper.getRusticSnapshotMetadataFilePath(PathHelper.getBackupCallLogsConfigFileRelativePath())
        val serializedFiles = RemoteRootService.readRusticSnapshotTextFiles(repositoryPath, password, snapshotId, listOf(metadataPath))
        RestoreCallLogsHelper(App.application.contentResolver).restore(serializedFiles, metadataPath, callLogIds, callback)
    }
}
