package com.xayah.databackup.service.restore.rustic

import android.content.pm.ApplicationInfo
import com.xayah.databackup.entity.rustic.requireFullSnapshotId
import com.xayah.databackup.service.restore.RestoreInternalDataHelper
import com.xayah.libnative.RusticWrapper

internal class RusticRestoreInternalDataHelper {
    suspend fun restore(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        app: ApplicationInfo,
        seInfo: String,
        sources: Map<Boolean, String>,
        stopApp: () -> Unit,
    ) {
        requireFullSnapshotId(snapshotId)
        RestoreInternalDataHelper().restore(
            app = app,
            seInfo = seInfo,
            sources = sources,
            sourceUid = { RusticWrapper.readSnapshotDirectoryUid(repositoryPath, password, "$snapshotId:$it") },
            extract = { path, target ->
                RusticWrapper.restoreSnapshot(
                    repositoryPath = repositoryPath,
                    password = password,
                    snapshotId = "$snapshotId:$path",
                    destinationPath = target.path,
                    options = RusticWrapper.RestoreOptions(numericId = true, verifyExisting = true),
                )
            },
            stopApp = stopApp,
        )
    }
}
