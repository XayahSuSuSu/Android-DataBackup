package com.xayah.databackup.data.rustic

import com.xayah.databackup.database.entity.App
import com.xayah.databackup.util.PathHelper

/** Builds an app's backup source list from its enabled options and available paths. */
class RusticAppSourcePlanner {
    suspend fun plan(
        app: App,
        apkPaths: List<String>,
        exists: suspend (String) -> Boolean,
    ): RusticAppSourcePlan {
        val appUserDir = PathHelper.getAppUserDir(app.userId, app.packageName)
        val candidates = buildList {
            fun addSource(path: String, category: RusticSourceCategory) {
                if (path.isNotBlank()) add(RusticSourcePath(path, category))
            }

            with(app.option) {
                if (apk) {
                    apkPaths.forEach { addSource(it, RusticSourceCategory.Apk) }
                }
                if (internalData) {
                    // Back up both user-unlocked (CE) and direct-boot (DE) internal data.
                    addSource(appUserDir, RusticSourceCategory.InternalData)
                    addSource(PathHelper.getAppUserDeDir(app.userId, app.packageName), RusticSourceCategory.InternalData)
                }
                if (externalData) {
                    addSource(PathHelper.getAppDataDir(app.userId, app.packageName), RusticSourceCategory.ExternalData)
                }
                if (additionalData) {
                    addSource(PathHelper.getAppObbDir(app.userId, app.packageName), RusticSourceCategory.AdditionalData)
                    addSource(PathHelper.getAppMediaDir(app.userId, app.packageName), RusticSourceCategory.AdditionalData)
                }
            }
        }.distinctBy(RusticSourcePath::path)

        val included = mutableListOf<RusticSourcePath>()
        val skipped = mutableListOf<RusticSkippedSource>()
        for (source in candidates) {
            if (exists(source.path)) {
                included += source
                continue
            }

            check(app.option.internalData.not() || source.path != appUserDir) { "Internal CE data path does not exist for ${app.packageName}: $appUserDir" }
            skipped += RusticSkippedSource(
                path = source.path,
                category = source.category,
                reason = RusticSkippedSource.REASON_NOT_FOUND,
            )
        }

        return RusticAppSourcePlan(
            packageName = app.packageName,
            userId = app.userId,
            info = app.info.copy(),
            option = app.option.copy(),
            included = included,
            skipped = skipped,
        )
    }
}
