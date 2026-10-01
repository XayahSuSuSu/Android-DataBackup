package com.xayah.databackup.entity.restore

import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.backup.BackupSourceCategory

/**
 * Restore setup state owned by a restore navigation entry.
 *
 * Converted into a [RestoreRequest] before execution. Restore progress and results are tracked separately.
 *
 * @property loading Whether the backup metadata and inventory are being loaded.
 * @property failed Whether loading failed.
 * @property config Backup configuration used to access the repository.
 * @property source Captured backend access parameters.
 * @property sourceInfo Backend-neutral display metadata.
 * @property inventory Restorable items available in the selected snapshot.
 * @property selected Selected inventory IDs across all categories: apps, Wi-Fi networks, contacts, call logs, and messages.
 * @property appParts Selected restore parts for each app, keyed by its inventory ID, such as APKs and internal or external data.
 */
data class RestoreState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val config: BackupConfig? = null,
    val source: RestoreSource? = null,
    val sourceInfo: RestoreSourceInfo? = null,
    val inventory: RestoreInventory? = null,
    val selected: Set<String> = emptySet(),
    val appParts: Map<String, Set<BackupSourceCategory>> = emptyMap(),
)
