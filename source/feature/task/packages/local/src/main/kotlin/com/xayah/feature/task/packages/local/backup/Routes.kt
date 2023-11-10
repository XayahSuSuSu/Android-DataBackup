package com.xayah.feature.task.packages.local.backup

sealed class TaskPackagesBackupRoutes(val route: String) {
    object List : TaskPackagesBackupRoutes(route = "task_packages_backup_list")
    object Processing : TaskPackagesBackupRoutes(route = "task_packages_backup_processing")
}
