package com.xayah.databackup.service.backup

import com.xayah.databackup.database.entity.App
import com.xayah.databackup.entity.backup.BackupAppSources
import com.xayah.databackup.entity.backup.BackupSkippedSource
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.backup.BackupSourcePath
import com.xayah.databackup.util.PathHelper

/** Builds an app's backup source list from its enabled options and available paths. */
class BackupAppSourceHelper {
    suspend fun getAppSources(
        app: App,
        apkPaths: List<String>,
        exists: suspend (String) -> Boolean,
    ): BackupAppSources {
        val appUserDir = PathHelper.getAppUserDir(app.userId, app.packageName)
        val candidates = buildList {
            fun addSource(path: String, category: BackupSourceCategory) {
                if (path.isNotBlank()) add(BackupSourcePath(path, category))
            }

            with(app.option) {
                if (apk) {
                    apkPaths.forEach { addSource(it, BackupSourceCategory.Apk) }
                }
                if (internalData) {
                    // Back up both user-unlocked (CE) and direct-boot (DE) internal data.
                    addSource(appUserDir, BackupSourceCategory.InternalData)
                    addSource(PathHelper.getAppUserDeDir(app.userId, app.packageName), BackupSourceCategory.InternalData)
                }
                if (externalData) {
                    addSource(PathHelper.getAppDataDir(app.userId, app.packageName), BackupSourceCategory.ExternalData)
                }
                if (additionalData) {
                    addSource(PathHelper.getAppObbDir(app.userId, app.packageName), BackupSourceCategory.AdditionalData)
                    addSource(PathHelper.getAppMediaDir(app.userId, app.packageName), BackupSourceCategory.AdditionalData)
                }
            }
        }.distinctBy(BackupSourcePath::path)

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
