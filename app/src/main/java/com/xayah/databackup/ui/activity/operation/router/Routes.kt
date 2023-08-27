package com.xayah.databackup.ui.activity.operation.router

sealed class OperationRoutes(val route: String) {
    object PackageBackup : OperationRoutes(route = "operation_package_backup")
    object PackageBackupList : OperationRoutes(route = "operation_package_backup_list")
    object PackageBackupManifest : OperationRoutes(route = "operation_package_backup_manifest")
    object PackageBackupProcessing : OperationRoutes(route = "operation_package_backup_processing")
    object PackageBackupCompletion : OperationRoutes(route = "operation_package_backup_completion")
}
