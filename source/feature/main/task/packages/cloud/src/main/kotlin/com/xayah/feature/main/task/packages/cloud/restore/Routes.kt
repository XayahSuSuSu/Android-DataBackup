package com.xayah.feature.main.task.packages.cloud.restore

sealed class TaskPackagesRestoreRoutes(val route: String) {
    object List : TaskPackagesRestoreRoutes(route = "task_packages_restore_list")
    object Processing : TaskPackagesRestoreRoutes(route = "task_packages_restore_processing")
}
