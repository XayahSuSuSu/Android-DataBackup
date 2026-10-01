package com.xayah.databackup.data.restore

import com.xayah.databackup.data.rustic.RusticRestoreInventory
import com.xayah.databackup.data.rustic.RusticSnapshot
import com.xayah.databackup.data.rustic.RusticSourceCategory
import com.xayah.databackup.entity.BackupConfig

/**
 * Restore setup state held by [RestoreSession].
 *
 * Converted into a [RestoreRequest] before execution. Restore progress and results are tracked separately.
 *
 * @property loading Whether the backup metadata and inventory are being loaded.
 * @property failed Whether loading failed.
 * @property config Backup configuration used to access the repository.
 * @property snapshot Snapshot selected as the restore source.
 * @property inventory Restorable items available in the selected snapshot.
 * @property selected Selected inventory IDs across all categories: apps, Wi-Fi networks, contacts, call logs, and messages.
 * @property appParts Selected restore parts for each app, keyed by its inventory ID, such as APKs and internal or external data.
 */
data class RestoreSessionState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val config: BackupConfig? = null,
    val snapshot: RusticSnapshot? = null,
    val inventory: RusticRestoreInventory? = null,
    val selected: Set<String> = emptySet(),
    val appParts: Map<String, Set<RusticSourceCategory>> = emptyMap(),
)
