package com.xayah.feature.main.task.medium.local.backup

sealed class TaskMediumBackupRoutes(val route: String) {
    object List : TaskMediumBackupRoutes(route = "task_medium_backup_list")
    object Processing : TaskMediumBackupRoutes(route = "task_medium_backup_processing")
}
