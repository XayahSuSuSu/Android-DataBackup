package com.xayah.databackup.service.backup

import com.xayah.databackup.database.entity.App
import com.xayah.databackup.entity.backup.BackupAppSources
import com.xayah.databackup.entity.backup.BackupSkippedSource
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.backup.BackupSourcePath
import com.xayah.databackup.util.PathHelper

/**
 * Lists APK and data paths for the selected app backup options. [getCandidatePaths] does not check
 * whether paths exist; [getAppSources] checks them, skips missing paths, and fails if selected internal CE data is missing.
 */
class BackupAppSourceHelper {
    companion object {
        /** Cache roots managed by installd in both credential-encrypted and device-encrypted app data. */
        val INTERNAL_CACHE_DIR_NAMES = listOf("cache", "code_cache")

        /** Cache root returned by Context.getExternalCacheDir(). */
        val EXTERNAL_CACHE_DIR_NAMES = listOf("cache")
    }

    fun getCandidatePaths(app: App, apkPaths: List<String>): List<BackupSourcePath> = buildList {
        if (app.option.apk) {
            apkPaths.filter { it.isNotBlank() }.forEach { add(BackupSourcePath(it, BackupSourceCategory.Apk)) }
        }
        if (app.option.internalData) {
            add(BackupSourcePath(PathHelper.getAppUserDir(app.userId, app.packageName), BackupSourceCategory.InternalData))
            add(BackupSourcePath(PathHelper.getAppUserDeDir(app.userId, app.packageName), BackupSourceCategory.InternalData))
        }
        if (app.option.externalData) {
            add(BackupSourcePath(PathHelper.getAppDataDir(app.userId, app.packageName), BackupSourceCategory.ExternalData))
        }
        if (app.option.additionalData) {
            add(BackupSourcePath(PathHelper.getAppObbDir(app.userId, app.packageName), BackupSourceCategory.AdditionalData))
            add(BackupSourcePath(PathHelper.getAppMediaDir(app.userId, app.packageName), BackupSourceCategory.AdditionalData))
        }
    }.distinctBy(BackupSourcePath::path)

    /**
     * Returns the cache directories inside a data source that can be omitted from a backup.
     * Internal data restores recreate `cache` and `code_cache` with system ownership, permissions and quota attributes;
     * apps recreate external caches through the platform. Paths may not exist.
     */
    fun getCachePaths(source: BackupSourcePath): List<String> = when (source.category) {
        BackupSourceCategory.InternalData -> INTERNAL_CACHE_DIR_NAMES.map { "${source.path}/$it" }
        BackupSourceCategory.ExternalData -> EXTERNAL_CACHE_DIR_NAMES.map { "${source.path}/$it" }
        else -> emptyList()
    }

    suspend fun getAppSources(
        app: App,
        apkPaths: List<String>,
        exists: suspend (String) -> Boolean,
    ): BackupAppSources {
        val appUserDir = PathHelper.getAppUserDir(app.userId, app.packageName)
        val candidates = getCandidatePaths(app, apkPaths)

        val included = mutableListOf<BackupSourcePath>()
        val skipped = mutableListOf<BackupSkippedSource>()
        for (source in candidates) {
            if (exists(source.path)) {
                included += source
                continue
            }

            check(app.option.internalData.not() || source.path != appUserDir) {
                "Internal CE data path does not exist for ${app.packageName}: $appUserDir"
            }
            skipped += BackupSkippedSource(
                path = source.path,
                category = source.category,
                reason = BackupSkippedSource.REASON_NOT_FOUND,
            )
        }

        return BackupAppSources(
            packageName = app.packageName,
            userId = app.userId,
            info = app.info.copy(),
            option = app.option.copy(),
            storage = app.storage.copy(),
            included = included,
            skipped = skipped,
        )
    }
}
