package com.xayah.databackup.data.rustic

import android.provider.Telephony
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper

class RusticBackupSourceCollector(
    private val mAppSourcePlanner: RusticAppSourcePlanner,
    private val mSerializer: RusticStructuredDataSerializer,
    private val mGateway: RusticBackupGateway,
) {
    companion object {
        private const val TAG = "RusticBackupSourceCollector"
    }

    /**
     * Builds the complete source list for a Rustic snapshot and writes generated JSON and
     * manifest files under [stagingPath]. Missing optional paths are returned as skipped sources.
     */
    suspend fun collect(
        selection: RusticBackupSelection,
        stagingPath: String,
        createdAt: Long,
    ): RusticCollectedSources {
        // Resolve each app's enabled options into existing source paths and missing optional paths.
        val appPlans = selection.apps.map { app ->
            mAppSourcePlanner.plan(
                app = app,
                apkPaths = if (app.option.apk) mGateway.packageSourcePaths(app.packageName, app.userId) else emptyList(),
                exists = mGateway::exists,
            )
        }
        val included = appPlans.flatMap { it.included }.toMutableList()
        val skipped = appPlans.flatMap { it.skipped }.toMutableList()

        // Add standalone user files and binary MMS attachments to the physical sources.
        selection.files.filter { it.selected }.forEach { file ->
            addPath(file.path, RusticSourceCategory.File, included, skipped)
        }
        extractMmsAttachmentPaths(selection).forEach { path ->
            addPath(path, RusticSourceCategory.MmsAttachment, included, skipped)
        }

        // Convert selected structured records into JSON files that can be stored in the snapshot.
        val stagedFiles = mSerializer.serialize(selection)
        // Describe both the structured files and physical sources for the future restore flow.
        val manifest = RusticBackupManifest(
            configUuid = selection.config.uuidString,
            createdAt = createdAt,
            structuredFiles = stagedFiles.map { it.relativePath },
            apps = appPlans.map { plan ->
                RusticAppManifest(
                    packageName = plan.packageName,
                    userId = plan.userId,
                    label = plan.info.label,
                    versionName = plan.info.versionName,
                    versionCode = plan.info.versionCode,
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
        val allStagedFiles = stagedFiles + RusticStagedFile(PathHelper.getRusticManifestFileRelativePath(), mSerializer.serializeManifest(manifest))

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
            if (mGateway.createDirectory(parent).not()) {
                throw IllegalStateException("Failed to create Rustic staging directory: $parent")
            }
            mGateway.writeText(destination, stagedFile.content)
        }

        // Add the staging directory so generated metadata and structured data are included in the snapshot.
        val sourcePaths = (included.map { it.path } + stagingPath).distinct()
        return RusticCollectedSources(sourcePaths, stagingPath, skipped.distinctBy { it.path })
    }

    /** Adds an existing non-blank path to [included], or records a missing path in [skipped]. */
    private suspend fun addPath(
        path: String,
        category: RusticSourceCategory,
        included: MutableList<RusticSourcePath>,
        skipped: MutableList<RusticSkippedSource>,
    ) {
        if (path.isBlank()) return
        if (mGateway.exists(path)) {
            included += RusticSourcePath(path, category)
        } else {
            skipped += RusticSkippedSource(
                path = path,
                category = category,
                reason = RusticSkippedSource.REASON_NOT_FOUND,
            )
        }
    }

    /**
     * Extracts non-blank attachment paths from serialized MMS parts. Malformed part data is
     * logged and skipped so one invalid MMS does not prevent other attachments from being backed up.
     */
    private fun extractMmsAttachmentPaths(selection: RusticBackupSelection): List<String> {
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
