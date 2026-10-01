package com.xayah.databackup.data.restore

import com.xayah.databackup.data.rustic.RusticSourceCategory
import com.xayah.databackup.data.rustic.RusticSourcePath
import com.xayah.databackup.data.rustic.requireFullSnapshotId
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.util.PathHelper

/**
 * Captures the restore parameters and selected tasks. Apps are restored to their source user IDs.
 */
internal class RestoreRequest(
    val repositoryPath: String,
    val password: String,
    val snapshotId: String,
    val tasks: List<RestoreTask>,
)

internal data class RestoreAppSource(
    val packageName: String,
    val userId: Int,
    val paths: List<RusticSourcePath>,
)

/**
 * A restore task for one app or a batch of structured records.
 * User cancellation is checked before this task starts.
 */
internal sealed interface RestoreTask {
    val category: RestoreCategory
    val ids: List<String>

    data class App(
        val id: String,
        val source: RestoreAppSource,
    ) : RestoreTask {
        override val category = RestoreCategory.Apps
        override val ids = listOf(id)
    }

    data class Records(
        override val category: RestoreCategory,
        override val ids: List<String>,
    ) : RestoreTask {
        init {
            require(category != RestoreCategory.Apps) { "App restores require an App task" }
        }
    }
}

internal fun RestoreSessionState.toRestoreRequest(): RestoreRequest {
    check(!loading && !failed) { "Snapshot is not ready" }
    val config = checkNotNull(config)
    val backend = checkNotNull(config.backupBackend as? BackupBackend.Rustic)
    val snapshot = checkNotNull(snapshot)
    val inventory = checkNotNull(inventory)
    requireFullSnapshotId(snapshot.id)
    require(selected.isNotEmpty() && inventory.allIds.containsAll(selected)) { "Invalid restore selection" }
    val tasks = RestoreCategory.entries.flatMap { category ->
        val ids = inventory.ids(category).filter { it in selected }
        when {
            ids.isEmpty() -> emptyList()
            category != RestoreCategory.Apps -> listOf(RestoreTask.Records(category = category, ids = ids))
            else -> ids.map { id ->
                val app = inventory.apps.getValue(id)
                val parts = appParts[id].orEmpty()
                require(parts.isNotEmpty() && AppRestoreParts.containsAll(parts)) { "No restorable app parts selected" }
                val paths = inventory.appSources[id].orEmpty().filter { it.category in parts }.distinct()
                require(paths.map { it.category }.toSet() == parts) { "Selected app paths are missing from the snapshot" }
                RestoreTask.App(id = id, source = RestoreAppSource(app.packageName, app.userId, paths))
            }
        }
    }
    return RestoreRequest(
        repositoryPath = PathHelper.getBackupRepoDir(config.path),
        password = backend.password,
        snapshotId = snapshot.id,
        tasks = tasks
    )
}

internal fun RestoreAppSource.pathsFor(vararg categories: RusticSourceCategory): List<String> =
    paths.filter { it.category in categories }.map { it.path }.distinct()
