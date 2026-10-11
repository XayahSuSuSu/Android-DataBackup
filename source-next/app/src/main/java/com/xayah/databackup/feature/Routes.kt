package com.xayah.databackup.feature

import androidx.navigation3.runtime.NavKey
import com.xayah.databackup.data.TaskInfo
import com.xayah.databackup.data.TaskType
import com.xayah.databackup.entity.BackupBackendType
import kotlinx.serialization.Serializable

@Serializable
data object MainNavigationRoute : NavKey

@Serializable
data object UpdatesRoute : NavKey

@Serializable
data object AboutRoute : NavKey

@Serializable
data object TranslatorsRoute : NavKey

@Serializable
data object BackupSetupRoute : NavKey

@Serializable
data class BackupProcessRoute(val taskId: String = "") : NavKey

@Serializable
data class RusticBackupProcessRoute(val taskId: String = "") : NavKey

@Serializable
data class BackupConfigRoute(val index: Int) : NavKey {
    init {
        require(index >= 0) { "BackupConfigRoute only accepts an existing backup index." }
    }
}

@Serializable
data object BackupAppsRoute : NavKey

@Serializable
data object BackupNetworksRoute : NavKey

@Serializable
data object BackupContactsRoute : NavKey

@Serializable
data object BackupCallLogsRoute : NavKey

@Serializable
data object BackupMessagesRoute : NavKey

@Serializable
data class RestoreRoute(val configUuid: String, val snapshotId: String) : NavKey

@Serializable
data object RestoreSetupRoute : NavKey

@Serializable
data class RestoreProcessRoute(val taskId: String = "") : NavKey

@Serializable
data object RestoreAppsRoute : NavKey

@Serializable
data object RestoreNetworksRoute : NavKey

@Serializable
data object RestoreContactsRoute : NavKey

@Serializable
data object RestoreCallLogsRoute : NavKey

@Serializable
data object RestoreMessagesRoute : NavKey

internal fun TaskInfo.toProcessRoute(): NavKey = when (type) {
    TaskType.Restore -> RestoreProcessRoute(id)
    TaskType.Backup -> when (backendType) {
        BackupBackendType.Archive -> BackupProcessRoute(id)
        BackupBackendType.Rustic -> RusticBackupProcessRoute(id)
    }
}
