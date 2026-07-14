package com.xayah.databackup.feature

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object UpdatesRoute : NavKey

@Serializable
data object BackupSetupRoute : NavKey

@Serializable
data object BackupProcessRoute : NavKey

@Serializable
data object RusticBackupProcessRoute : NavKey

@Serializable
data object BackupProcessDetailsRoute : NavKey

@Serializable
data class BackupConfigRoute(val index: Int) : NavKey

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
