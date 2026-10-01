package com.xayah.databackup.service.backup.rustic

import android.provider.Telephony
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.entity.backup.BackupSelection
import com.xayah.databackup.entity.backup.BackupSkippedSource
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.backup.BackupSourcePath
import com.xayah.databackup.entity.backup.BackupSources
import com.xayah.databackup.entity.backup.BackupStagedFile
import com.xayah.databackup.entity.rustic.RusticAppManifest
import com.xayah.databackup.entity.rustic.RusticBackupManifest
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.backup.BackupAppSourceHelper
import com.xayah.databackup.service.backup.BackupSerializationHelper
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper

class RusticBackupSourceHelper(
    private val mAppSourceHelper: BackupAppSourceHelper,
    private val mSerializationHelper: BackupSerializationHelper,
) {
    companion object {
        private const val TAG = "RusticBackupSourceHelper"
    }

    /**
     * Builds the complete source list for a Rustic snapshot and writes generated JSON and
     * manifest files under [stagingPath]. Missing optional paths are returned as skipped sources.
     */
    suspend fun collectSources(
        selection: BackupSelection,
        stagingPath: String,
        createdAt: Long,
    ): BackupSources {
        // Resolve each app's enabled options into existing source paths and missing optional paths.
        val appPlans = selection.apps.map { app ->
            mAppSourceHelper.getAppSources(
                app = app,
                apkPaths = if (app.option.apk) RemoteRootService.getPackageSourceDir(app.packageName, app.userId) else emptyList(),
                exists = RemoteRootService::exists,
            )
        }
        val usersMap = if (appPlans.isEmpty()) emptyMap() else RemoteRootService.getUsers().associate { it.id to it.name }
        val included = appPlans.flatMap { it.included }.toMutableList()
        val skipped = appPlans.flatMap { it.skipped }.toMutableList()

        // Add standalone user files and binary MMS attachments to the physical sources.
        selection.files.filter { it.selected }.forEach { file ->
            addPath(file.path, BackupSourceCategory.File, included, skipped)
        }
        extractMmsAttachmentPaths(selection).forEach { path ->
            addPath(path, BackupSourceCategory.MmsAttachment, included, skipped)
        }

        // Convert selected structured records into JSON files that can be stored in the snapshot.
        val stagedFiles = mSerializationHelper.serialize(selection)
        // Describe both the structured files and physical sources for the future restore flow.
        val manifest = RusticBackupManifest(
            configUuid = selection.config.uuidString,
            createdAt = createdAt,
            structuredFiles = stagedFiles.map { it.relativePath },
            apps = appPlans.map { plan ->
                RusticAppManifest(
                    packageName = plan.packageName,
                    userId = plan.userId,
                    userName = usersMap[plan.userId] ?: plan.userId.toString(),
                    label = plan.info.label,
                    versionName = plan.info.versionName,
                    versionCode = plan.info.versionCode,
                    flags = plan.info.flags,
                    firstInstallTime = plan.info.firstInstallTime,
                    lastUpdateTime = plan.info.lastUpdateTime,
                    apkBytes = plan.storage.apkBytes,
                    internalDataBytes = plan.storage.internalDataBytes,
                    externalDataBytes = plan.storage.externalDataBytes,
                    additionalDataBytes = plan.storage.additionalDataBytes,
                    apk = plan.option.apk,
                    internalData = plan.option.internalData,
                    externalData = plan.option.externalData,
                    additionalData = plan.option.additionalData,
                    included = plan.included,
                    skipped = plan.skipped,
                )
            },
            included = included.distinctBy { it.path },
            skipped = skipped.distinctBy { it.path },
        )
        val allStagedFiles = stagedFiles + BackupStagedFile(
            PathHelper.getRusticManifestFileRelativePath(),
            Moshi.Builder().build().adapter<RusticBackupManifest>().toJson(manifest),
        )

        // A selected structured category remains a valid source even when it has no records.
        val hasStructuredData = selection.networks != null || selection.contacts != null ||
                selection.callLogs != null || selection.sms != null || selection.mms != null
        if (included.isEmpty() && hasStructuredData.not()) {
            throw IllegalStateException("No backup sources were selected.")
        }

        // Materialize generated JSON and the manifest under the temporary staging root.
        allStagedFiles.forEach { stagedFile ->
            val destination = "$stagingPath/${stagedFile.relativePath}"
            val parent = PathHelper.getParentPath(destination)
            if (RemoteRootService.mkdirs(parent).not()) {
                throw IllegalStateException("Failed to create Rustic staging directory: $parent")
            }
            RemoteRootService.writeText(destination, stagedFile.content)
        }

        // Ordinary sources keep their paths; generated metadata uses a fixed snapshot directory.
        val sourcePaths = included.associate { it.path to it.path }
        return BackupSources(
            sourcePaths = sourcePaths + (stagingPath to PathHelper.getRusticSnapshotMetadataDir()),
            includedCount = sourcePaths.size,
            skippedSources = skipped.distinctBy { it.path },
        )
    }

    /** Adds an existing non-blank path to [included], or records a missing path in [skipped]. */
    private suspend fun addPath(
        path: String,
        category: BackupSourceCategory,
        included: MutableList<BackupSourcePath>,
        skipped: MutableList<BackupSkippedSource>,
    ) {
        if (path.isBlank()) return
        if (RemoteRootService.exists(path)) {
            included += BackupSourcePath(path, category)
        } else {
            skipped += BackupSkippedSource(
                path = path,
                category = category,
                reason = BackupSkippedSource.REASON_NOT_FOUND,
            )
        }
    }

    /**
     * Extracts non-blank attachment paths from serialized MMS parts. Malformed part data is
     * logged and skipped so one invalid MMS does not prevent other attachments from being backed up.
     */
    private fun extractMmsAttachmentPaths(selection: BackupSelection): List<String> {
        val moshi = Moshi.Builder().build()
        return selection.mms.orEmpty().flatMap { mms ->
            // MMS parts store attachment paths in the platform _data field.
            runCatching { moshi.adapter<List<FieldMap>>().fromJson(mms.part.orEmpty()).orEmpty() }
                .onFailure { LogHelper.e(TAG, "extractMmsAttachmentPaths", "mms: $mms", it) }
                .getOrDefault(emptyList())
                .mapNotNull { it[Telephony.Mms.Part._DATA]?.toString()?.takeIf(String::isNotBlank) }
        }
    }
}
