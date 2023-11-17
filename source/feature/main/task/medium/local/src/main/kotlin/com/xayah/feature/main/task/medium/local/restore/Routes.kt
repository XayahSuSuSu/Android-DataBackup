package com.xayah.feature.main.task.medium.local.restore

sealed class TaskMediumRestoreRoutes(val route: String) {
    object List : TaskMediumRestoreRoutes(route = "task_medium_restore_list")
    object Processing : TaskMediumRestoreRoutes(route = "task_medium_restore_processing")
}
