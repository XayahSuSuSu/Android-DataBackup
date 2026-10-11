package com.xayah.databackup.entity.restore

import com.xayah.databackup.entity.BackupBackendType
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.backup.BackupSourcePath
import kotlin.uuid.Uuid

/**
 * Captures the restore parameters and selected tasks. Apps are restored to their source user IDs.
 */
internal class RestoreRequest(
    val source: RestoreSource,
    tasks: List<RestoreTask>,
    val id: String = Uuid.random().toString(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    private val mTasks = tasks.deepCopy()
    val tasks: List<RestoreTask> get() = mTasks.deepCopy()
    val backendType: BackupBackendType = when (source) {
        is RestoreSource.Archive -> BackupBackendType.Archive
        is RestoreSource.Rustic -> BackupBackendType.Rustic
    }

    init {
        require(id.isNotBlank() && mTasks.isNotEmpty()) { "Empty restore request" }
        require(mTasks.flatMap { it.ids }.distinct().size == mTasks.sumOf { it.ids.size }) { "Repeated restore records" }
        require(mTasks.all { it.ids.isNotEmpty() }) { "Empty restore task" }
    }
}

private fun List<RestoreTask>.deepCopy(): List<RestoreTask> = map { task ->
    when (task) {
        is RestoreTask.App -> task.copy(source = task.source.copy(paths = task.source.paths.map { it.copy() }))
        is RestoreTask.Records -> task.copy(ids = task.ids.toList())
    }
}

internal data class RestoreAppSource(
    val packageName: String,
    val userId: Int,
    val paths: List<BackupSourcePath>,
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

internal fun RestoreState.toRestoreRequest(): RestoreRequest {
    check(!loading && !failed) { "Snapshot is not ready" }
    val source = checkNotNull(source)
    val inventory = checkNotNull(inventory)
    require(selected.isNotEmpty() && inventory.allIds.containsAll(selected)) { "Invalid restore selection" }
    val tasks = RestoreCategory.entries.flatMap { category ->
        val ids = inventory.getIds(category).filter { it in selected }
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
        source = source,
        tasks = tasks
    )
}

internal fun RestoreAppSource.pathsFor(vararg categories: BackupSourceCategory): List<String> =
    paths.filter { it.category in categories }.map { it.path }.distinct()
