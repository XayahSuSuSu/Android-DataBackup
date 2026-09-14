package com.xayah.libnative

object Rustic {
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
    fun restoreSnapshot(repositoryPath: String, password: String, snapshotId: String, destinationPath: String) {
        nativeRestoreSnapshot(repositoryPath, password, snapshotId, destinationPath)
    }

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
    )
    private external fun nativeListSnapshots(repositoryPath: String, password: String): String
    private external fun nativeCheckRepository(repositoryPath: String, password: String)
    private external fun nativeDeleteSnapshot(repositoryPath: String, password: String, snapshotId: String): String
}
