package com.xayah.libnative

import androidx.annotation.Keep

object Rustic {
    @Keep
    data class RestoreOptions(
        val delete: Boolean = false,
        val numericId: Boolean = false,
        val noOwnership: Boolean = false,
        val verifyExisting: Boolean = false,
    )

    fun initLogger() = nativeInitLogger()

    fun initRepository(repositoryPath: String, password: String) {
        nativeInitRepository(repositoryPath, password)
    }

    fun repositoryExists(repositoryPath: String): Boolean {
        return nativeRepositoryExists(repositoryPath)
    }

    fun validateRepository(repositoryPath: String, password: String) {
        nativeValidateRepository(repositoryPath, password)
    }

    /** Creates a snapshot and returns its full 64-character hexadecimal ID. */
    fun createSnapshot(
        repositoryPath: String,
        password: String,
        sourcePaths: Map<String, String>,
        tags: List<String> = emptyList(),
        callback: Any? = null,
    ): String {
        val paths = sourcePaths.toList()
        return nativeCreateSnapshot(
            repositoryPath = repositoryPath,
            password = password,
            sourcePaths = paths.map { it.first }.toTypedArray(),
            snapshotPaths = paths.map { it.second }.toTypedArray(),
            tags = tags.toTypedArray(),
            callback = callback,
        )
    }

    /** [snapshotId] accepts a snapshot ID or `snapshotId:path` to restore a single file or directory. */
    fun restoreSnapshot(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        destinationPath: String,
        options: RestoreOptions = RestoreOptions(),
    ) {
        nativeRestoreSnapshot(repositoryPath, password, snapshotId, destinationPath, options)
    }

    /** Validates snapshot paths and entry types before the caller clears external destination contents. */
    fun validateExternalSnapshot(repositoryPath: String, password: String, snapshotId: String) {
        nativeValidateExternalSnapshot(repositoryPath, password, snapshotId)
    }

    /**
     * Restores external data without importing source ownership, modes, xattrs or hardlink relationships.
     * The caller must validate the snapshot and prepare the destination before calling,
     * then repair ownership, permissions, ACLs, quota project IDs and SELinux labels
     * according to the destination Android system, even if restoration fails.
     */
    fun restoreExternalSnapshot(repositoryPath: String, password: String, snapshotId: String, destinationPath: String) {
        nativeRestoreExternalSnapshot(repositoryPath, password, snapshotId, destinationPath)
    }

    /** Returns the snapshot directory's original numeric UID; fails if the selected node is not a directory. */
    fun readSnapshotDirectoryUid(repositoryPath: String, password: String, snapshotId: String): Int =
        nativeReadSnapshotDirectoryUid(repositoryPath, password, snapshotId)

    fun deleteSnapshot(repositoryPath: String, password: String, snapshotId: String): String {
        return nativeDeleteSnapshot(repositoryPath, password, snapshotId)
    }

    fun listSnapshots(repositoryPath: String, password: String): String {
        return nativeListSnapshots(repositoryPath, password)
    }

    fun checkRepository(repositoryPath: String, password: String) {
        nativeCheckRepository(repositoryPath, password)
    }

    fun readSnapshotTextFiles(repositoryPath: String, password: String, snapshotId: String, paths: List<String>): String =
        nativeReadSnapshotTextFiles(repositoryPath, password, snapshotId, paths.toTypedArray())

    private external fun nativeReadSnapshotTextFiles(repositoryPath: String, password: String, snapshotId: String, paths: Array<String>): String
    private external fun nativeInitLogger()
    private external fun nativeInitRepository(repositoryPath: String, password: String)
    private external fun nativeRepositoryExists(repositoryPath: String): Boolean
    private external fun nativeValidateRepository(repositoryPath: String, password: String)
    private external fun nativeCreateSnapshot(
        repositoryPath: String,
        password: String,
        sourcePaths: Array<String>,
        snapshotPaths: Array<String>,
        tags: Array<String>,
        callback: Any?,
    ): String

    private external fun nativeRestoreSnapshot(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        destinationPath: String,
        options: RestoreOptions,
    )

    private external fun nativeValidateExternalSnapshot(repositoryPath: String, password: String, snapshotId: String)
    private external fun nativeRestoreExternalSnapshot(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        destinationPath: String,
    )

    private external fun nativeReadSnapshotDirectoryUid(repositoryPath: String, password: String, snapshotId: String): Int
    private external fun nativeListSnapshots(repositoryPath: String, password: String): String
    private external fun nativeCheckRepository(repositoryPath: String, password: String)
    private external fun nativeDeleteSnapshot(repositoryPath: String, password: String, snapshotId: String): String
}
