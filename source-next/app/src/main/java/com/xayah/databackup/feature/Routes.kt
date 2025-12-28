package com.xayah.databackup.feature

import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

@Serializable
data object BackupRoute

@Serializable
data object BackupSetupRoute

@Serializable
data object BackupProcessRoute

@Serializable
data class BackupConfigRoute(
    val index: Int
)

@Serializable
data object BackupAppsRoute

@Serializable
data object BackupNetworksRoute

@Serializable
data object BackupContactsRoute

@Serializable
data object BackupCallLogsRoute

@Serializable
data object BackupMessagesRoute
