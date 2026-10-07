package com.xayah.databackup.service.restore.rustic

import android.content.pm.ApplicationInfo
import com.xayah.databackup.entity.rustic.requireFullSnapshotId
import com.xayah.databackup.service.restore.RestoreExternalDataHelper
import com.xayah.libnative.RusticWrapper

internal class RusticRestoreExternalDataHelper {
    suspend fun restore(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        app: ApplicationInfo,
        sources: Map<String, String>,
        stopApp: () -> Unit,
    ) {
        requireFullSnapshotId(snapshotId)
        RestoreExternalDataHelper().restore(
            repositoryPath = repositoryPath,
            app = app,
            sources = sources,
            validate = { RusticWrapper.validateExternalSnapshot(repositoryPath, password, "$snapshotId:$it") },
            extract = { path, target -> RusticWrapper.restoreExternalSnapshot(repositoryPath, password, "$snapshotId:$path", target.path) },
            stopApp = stopApp,
        )
    }
}
