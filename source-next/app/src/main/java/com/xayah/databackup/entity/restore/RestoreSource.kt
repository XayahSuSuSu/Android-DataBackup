package com.xayah.databackup.entity.restore

import com.xayah.databackup.entity.rustic.requireFullSnapshotId

/** Execution parameters captured before device writes. Never log source credentials. */
sealed interface RestoreSource {
    class Archive(val path: String) : RestoreSource

    class Rustic(val repositoryPath: String, val password: String, val snapshotId: String) : RestoreSource {
        init {
            requireFullSnapshotId(snapshotId)
        }
    }
}

/** Display metadata independent of a backend's wire format. */
data class RestoreSourceInfo(val id: String, val createdAt: Long, val totalBytes: Long?)
