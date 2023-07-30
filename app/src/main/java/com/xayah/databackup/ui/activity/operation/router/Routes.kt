package com.xayah.databackup.ui.activity.operation.router

sealed class OperationRoutes(val route: String) {
    object PackageBackup : OperationRoutes(route = "operation_package_backup")
    object PackageBackupList : OperationRoutes(route = "operation_package_backup_list")
}
