package com.xayah.core.ui.route

sealed class MainRoutes(val route: String) {
    companion object {
        const val ArgPackageName = "pkgName"
        const val ArgMediaName = "mediaName"
        const val ArgUserId = "userId"
        const val ArgTaskId = "taskId"
        const val ArgAccountName = "accountName"
    }

    data object Index : MainRoutes(route = "main_index")
    data object Settings : MainRoutes(route = "main_settings")
    data object BackupSettings : MainRoutes(route = "main_backup_settings")
    data object RestoreSettings : MainRoutes(route = "main_restore_settings")
    data object Packages : MainRoutes(route = "main_packages")
    data object PackageDetail : MainRoutes(route = "main_package_detail/{$ArgPackageName}/{$ArgUserId}") {
        fun getRoute(packageName: String, userId: Int) = "main_package_detail/${packageName}/${userId}"
    }

    data object Medium : MainRoutes(route = "main_medium")
    data object MediumDetail : MainRoutes(route = "main_medium_detail/{$ArgMediaName}") {
        fun getRoute(name: String) = "main_medium_detail/${name}"
    }

    data object CloudAccount : MainRoutes(route = "main_cloud_account/{$ArgAccountName}") {
        fun getRoute(name: String) = "main_cloud_account/$name"
    }

    data object TaskList : MainRoutes(route = "main_taskList")
    data object TaskPackageDetail : MainRoutes(route = "main_task_package_detail/{$ArgTaskId}") {
        fun getRoute(taskId: Long) = "main_task_package_detail/${taskId}"
    }

    data object TaskMediaDetail : MainRoutes(route = "main_task_media_detail/{$ArgTaskId}") {
        fun getRoute(taskId: Long) = "main_task_media_detail/${taskId}"
    }

    data object Log : MainRoutes(route = "main_log")
    data object Tree : MainRoutes(route = "main_tree")
    data object Reload : MainRoutes(route = "main_reload")
    data object Directory : MainRoutes(route = "main_directory")
}
