package com.xayah.databackup.data.rustic

import com.xayah.databackup.rootservice.ICallback
import com.xayah.databackup.rootservice.RemoteRootService

/** Provides privileged Rustic repository and filesystem operations through [RemoteRootService]. */
class RusticBackupGateway {
    suspend fun exists(path: String): Boolean = RemoteRootService.exists(path)

    suspend fun isDirectoryEmpty(path: String): Boolean = RemoteRootService.listFilePaths(path, listFiles = true, listDirs = true).isEmpty()

    suspend fun packageSourcePaths(packageName: String, userId: Int): List<String> = RemoteRootService.getPackageSourceDir(packageName, userId)

    suspend fun createDirectory(path: String): Boolean = RemoteRootService.mkdirs(path)

    suspend fun writeText(path: String, content: String) {
        RemoteRootService.writeText(path, content)
    }

    suspend fun deleteRecursively(path: String): Boolean = RemoteRootService.deleteRecursively(path)

    suspend fun initRepository(repositoryPath: String, password: String) {
        RemoteRootService.initRusticRepository(repositoryPath, password)
    }

    suspend fun repositoryExists(repositoryPath: String): Boolean = RemoteRootService.rusticRepositoryExists(repositoryPath)

    suspend fun validateRepository(repositoryPath: String, password: String) {
        RemoteRootService.validateRusticRepository(repositoryPath, password)
    }

    suspend fun prepareRepository(repositoryPath: String, password: String) {
        if (repositoryExists(repositoryPath)) {
            // Validate the repository config and credentials without performing a full integrity check.
            validateRepository(repositoryPath, password)
            return
        }
        if (exists(repositoryPath) && isDirectoryEmpty(repositoryPath).not()) {
            throw IllegalStateException("Rustic repository config is missing from a non-empty directory.")
        }
        if (createDirectory(repositoryPath).not()) {
            throw IllegalStateException("Failed to create Rustic repository directory.")
        }
        initRepository(repositoryPath, password)
        if (repositoryExists(repositoryPath).not()) {
            throw IllegalStateException("Rustic repository initialization did not create a repository config.")
        }
    }

    suspend fun createSnapshot(
        repositoryPath: String,
        password: String,
        sourcePaths: List<String>,
        tags: List<String>,
        onProgress: (Long, Long, Float) -> Unit,
    ): String {
        return RemoteRootService.createRusticSnapshot(
            repositoryPath = repositoryPath,
            password = password,
            sourcePaths = sourcePaths,
            tags = tags,
            callback = object : ICallback.Stub() {
                override fun onProgress(bytesWritten: Long, speed: Long, progress: Float) {
                    onProgress(bytesWritten, speed, progress)
                }
            },
        )
    }
}
