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
        sourcePaths: List<String>,
        tags: List<String> = emptyList(),
        callback: Any? = null,
    ): String {
        return nativeCreateSnapshot(repositoryPath, password, sourcePaths.toTypedArray(), tags.toTypedArray(), callback)
    }

    fun restoreSnapshot(repositoryPath: String, password: String, snapshotId: String, destinationPath: String) {
        nativeRestoreSnapshot(repositoryPath, password, snapshotId, destinationPath)
    }

    fun checkRepository(repositoryPath: String, password: String) {
        nativeCheckRepository(repositoryPath, password)
    }

    private external fun nativeInitLogger()
    private external fun nativeInitRepository(repositoryPath: String, password: String)
    private external fun nativeRepositoryExists(repositoryPath: String): Boolean
    private external fun nativeValidateRepository(repositoryPath: String, password: String)
    private external fun nativeCreateSnapshot(
        repositoryPath: String,
        password: String,
        sourcePaths: Array<String>,
        tags: Array<String>,
        callback: Any?,
    ): String

    private external fun nativeRestoreSnapshot(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        destinationPath: String,
    )

    private external fun nativeCheckRepository(repositoryPath: String, password: String)
}
