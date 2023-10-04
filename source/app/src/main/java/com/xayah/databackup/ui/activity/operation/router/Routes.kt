package com.xayah.databackup.ui.activity.operation.router

sealed class OperationRoutes(val route: String) {
    object PackageBackup : OperationRoutes(route = "operation_package_backup")
    object PackageBackupList : OperationRoutes(route = "operation_package_backup_list")
    object PackageBackupManifest : OperationRoutes(route = "operation_package_backup_manifest")
    object PackageBackupProcessing : OperationRoutes(route = "operation_package_backup_processing")
    object PackageBackupCompletion : OperationRoutes(route = "operation_package_backup_completion")

    object PackageRestore : OperationRoutes(route = "operation_package_restore")
    object PackageRestoreList : OperationRoutes(route = "operation_package_restore_list")
    object PackageRestoreManifest : OperationRoutes(route = "operation_package_restore_manifest")
    object PackageRestoreProcessing : OperationRoutes(route = "operation_package_restore_processing")
    object PackageRestoreCompletion : OperationRoutes(route = "operation_package_restore_completion")

    object MediaBackup : OperationRoutes(route = "operation_media_backup")
    object MediaRestore : OperationRoutes(route = "operation_media_restore")
}
