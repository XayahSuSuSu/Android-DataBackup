package com.xayah.databackup.service.restore.rustic

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.Info
import com.xayah.databackup.database.entity.Option
import com.xayah.databackup.database.entity.Storage
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.AppRestoreParts
import com.xayah.databackup.entity.restore.RestoreInventory
import com.xayah.databackup.entity.rustic.RusticBackupManifest
import com.xayah.databackup.service.restore.RestoreMetadataHelper
import com.xayah.databackup.service.restore.RestoreRecordId
import com.xayah.databackup.util.PathHelper

/** Reads restore metadata from the selected snapshot. */
class RusticRestoreMetadataHelper {
    private val mMoshi = Moshi.Builder().build()

    fun deserializeManifest(content: String): RusticBackupManifest {
        val manifest = requireNotNull(mMoshi.adapter<RusticBackupManifest>().fromJson(content))
        require(manifest.schemaVersion == RusticBackupManifest.CURRENT_SCHEMA_VERSION) { "Unsupported snapshot manifest version" }
        return manifest
    }

    fun getStructuredPaths(manifest: RusticBackupManifest): List<String> {
        val supported = listOf(
            PathHelper.getBackupNetworksConfigFileRelativePath(),
            PathHelper.getBackupContactsConfigFileRelativePath(),
            PathHelper.getBackupCallLogsConfigFileRelativePath(),
            PathHelper.getBackupMessagesSmsConfigFileRelativePath(),
            PathHelper.getBackupMessagesMmsConfigFileRelativePath(),
        )
        require(manifest.structuredFiles.all { it in supported }) { "Unsupported snapshot data file" }
        return manifest.structuredFiles.distinct()
    }

    fun deserializeInventory(manifest: RusticBackupManifest, files: Map<String, String>): RestoreInventory {
        getStructuredPaths(manifest).forEach { require(files.containsKey(it)) { "Missing snapshot data file" } }
        val apps = manifest.apps.filter { it.included.isNotEmpty() }
            .distinctBy { it.packageName to it.userId }
            .associateBy { RestoreRecordId.getAppId(it.userId, it.packageName) }
        return RestoreMetadataHelper().deserializeInventory(files.filterKeys { it in manifest.structuredFiles }).copy(
            usersMap = apps.values.associate { it.userId to it.userName },
            apps = apps.mapValues { (_, app) ->
                App(
                    packageName = app.packageName,
                    userId = app.userId,
                    info = Info(
                        label = app.label.ifBlank { app.packageName },
                        versionName = app.versionName,
                        flags = app.flags,
                        firstInstallTime = app.firstInstallTime,
                        lastUpdateTime = app.lastUpdateTime,
                        versionCode = app.versionCode
                    ),
                    option = Option(apk = false, internalData = false, externalData = false, additionalData = false),
                    storage = Storage(
                        apkBytes = app.apkBytes,
                        internalDataBytes = app.internalDataBytes,
                        externalDataBytes = app.externalDataBytes,
                        additionalDataBytes = app.additionalDataBytes,
                    ),
                )
            },
            availableAppParts = apps.mapValues { (_, app) ->
                app.included.map { it.category }.filter { it in AppRestoreParts }.toSet()
            },
            appSources = apps.mapValues { (_, app) -> app.included },
            files = manifest.included.filter { it.category == BackupSourceCategory.File }.distinctBy { it.path }
                .associate { RestoreRecordId.getFileId(it.path) to it.path },
        )
    }
}
