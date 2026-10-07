package com.xayah.databackup.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.restore.RestoreSource
import com.xayah.databackup.entity.restore.RestoreSourceInfo
import com.xayah.databackup.entity.restore.RestoreState
import com.xayah.databackup.entity.rustic.RusticSnapshot
import com.xayah.databackup.rootservice.ICallback
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.restore.rustic.RusticRestoreMetadataHelper
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Provides privileged Rustic repository and snapshot operations through [RemoteRootService]. */
class RusticRepository {
    private val mSnapshotListAdapter = Moshi.Builder().build().adapter<List<RusticSnapshot>>()

    suspend fun loadRestoreState(config: BackupConfig, snapshotId: String): RestoreState {
        return withContext(Dispatchers.IO) {
            val backend = checkNotNull(config.backupBackend as? BackupBackend.Rustic)
            val repository = PathHelper.getBackupRepoDir(config.path)
            val snapshot = listSnapshots(repository, backend.password).single { it.id == snapshotId }
            val reader = RusticRestoreMetadataHelper()
            val manifestPath = PathHelper.getRusticSnapshotMetadataFilePath(PathHelper.getRusticManifestFileRelativePath())
            val manifestFiles = readSnapshotTextFiles(repository, backend.password, snapshot.id, listOf(manifestPath))
            val manifest = reader.deserializeManifest(manifestFiles.getValue(manifestPath))
            val paths = reader.getStructuredPaths(manifest)
            val files = if (paths.isEmpty()) {
                emptyMap()
            } else {
                readSnapshotTextFiles(
                    repositoryPath = repository,
                    password = backend.password,
                    snapshotId = snapshot.id,
                    paths = paths.map(PathHelper::getRusticSnapshotMetadataFilePath),
                ).mapKeys { (path, _) -> path.removePrefix("${PathHelper.getRusticSnapshotMetadataDir()}/") }
            }
            val inventory = reader.deserializeInventory(manifest, files)
            RestoreState(
                loading = false,
                config = config,
                source = RestoreSource.Rustic(repository, backend.password, snapshot.id),
                sourceInfo = RestoreSourceInfo(snapshot.id, snapshot.createdAt, snapshot.summary?.totalBytesProcessed),
                inventory = inventory,
                selected = inventory.allIds,
                appParts = inventory.availableAppParts,
            )
        }
    }

    suspend fun readSnapshotTextFiles(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        paths: List<String>,
    ): Map<String, String> {
        val serialized = RemoteRootService.readRusticSnapshotTextFiles(repositoryPath, password, snapshotId, paths)
        return requireNotNull(Moshi.Builder().build().adapter<Map<String, String>>().fromJson(serialized))
    }

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
        if (RemoteRootService.exists(repositoryPath) &&
            RemoteRootService.listFilePaths(repositoryPath, listFiles = true, listDirs = true).isNotEmpty()
        ) {
            throw IllegalStateException("Rustic repository config is missing from a non-empty directory.")
        }
        if (RemoteRootService.mkdirs(repositoryPath).not()) {
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
        sourcePaths: Map<String, String>,
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

    suspend fun deleteSnapshot(repositoryPath: String, password: String, snapshotId: String): List<RusticSnapshot> {
        // Invalidate before mutation so a failed cache write cannot resurrect a deleted snapshot.
        val cachePath = PathHelper.getRusticSnapshotsCacheFile(PathHelper.getParentPath(repositoryPath))
        if (RemoteRootService.exists(cachePath)) {
            check(RemoteRootService.deleteRecursively(cachePath)) { "Failed to invalidate snapshot cache" }
        }
        val serialized = RemoteRootService.deleteRusticSnapshot(repositoryPath, password, snapshotId)
        return cacheSnapshots(repositoryPath, serialized)
    }

    suspend fun listSnapshots(repositoryPath: String, password: String): List<RusticSnapshot> {
        val serialized = RemoteRootService.listRusticSnapshots(repositoryPath = repositoryPath, password = password)
        return cacheSnapshots(repositoryPath, serialized)
    }

    private suspend fun cacheSnapshots(repositoryPath: String, serialized: String): List<RusticSnapshot> {
        val snapshots = requireNotNull(mSnapshotListAdapter.fromJson(serialized)) { "Missing snapshot list" }
        runCatching {
            RemoteRootService.writeText(
                PathHelper.getRusticSnapshotsCacheFile(PathHelper.getParentPath(repositoryPath)),
                mSnapshotListAdapter.toJson(snapshots),
            )
        }.onFailure { error ->
            if (error is CancellationException || error !is Exception) throw error
            // Cache failures must not discard freshly loaded metadata.
        }
        return snapshots
    }

    suspend fun readCachedSnapshots(repositoryPath: String): List<RusticSnapshot>? {
        val cachePath = PathHelper.getRusticSnapshotsCacheFile(PathHelper.getParentPath(repositoryPath))
        return runCatching {
            if (RemoteRootService.exists(cachePath)) mSnapshotListAdapter.fromJson(RemoteRootService.readText(cachePath)) else null
        }.getOrElse { error ->
            if (error is CancellationException || error !is Exception) throw error
            null
        }
    }
}
