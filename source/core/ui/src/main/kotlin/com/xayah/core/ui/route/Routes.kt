package com.xayah.core.ui.route

sealed class MainRoutes(val route: String) {
    companion object {
        const val ArgPackageName = "pkgName"
        const val ArgMediaName = "mediaName"
        const val ArgUserId = "userId"
        const val ArgTaskId = "taskId"
        const val ArgAccountName = "accountName"
        const val ArgAccountRemote = "accountRemote"
    }

    data object Index : MainRoutes(route = "main_index")
    data object Dashboard : MainRoutes(route = "main_dashboard")
    data object Cloud : MainRoutes(route = "main_cloud")
    data object CloudAddAccount : MainRoutes(route = "main_cloud_add_account")
    data object FTPSetup : MainRoutes(route = "main_ftp_setup/{$ArgAccountName}") {
        fun getRoute(name: String) = "main_ftp_setup/$name"
    }
    data object WebDAVSetup : MainRoutes(route = "main_webdav_setup/{$ArgAccountName}") {
        fun getRoute(name: String) = "main_webdav_setup/$name"
    }
    data object SMBSetup : MainRoutes(route = "main_smb_setup/{$ArgAccountName}") {
        fun getRoute(name: String) = "main_smb_setup/$name"
    }
    data object Settings : MainRoutes(route = "main_settings")
    data object Restore : MainRoutes(route = "main_restore")
    data object BackupSettings : MainRoutes(route = "main_backup_settings")
    data object RestoreSettings : MainRoutes(route = "main_restore_settings")
    data object Packages : MainRoutes(route = "main_packages")

    data object PackagesBackupList : MainRoutes(route = "main_packages_backup_list")
    data object PackagesBackupDetail : MainRoutes(route = "main_packages_backup_detail/{$ArgPackageName}/{$ArgUserId}") {
        fun getRoute(packageName: String, userId: Int) = "main_packages_backup_detail/${packageName}/${userId}"
    }
    data object PackagesBackupProcessingGraph : MainRoutes(route = "main_packages_backup_processing_graph")
    data object PackagesBackupProcessing : MainRoutes(route = "main_packages_backup_processing")
    data object PackagesBackupProcessingSetup : MainRoutes(route = "main_packages_backup_processing_setup")

    data object PackagesRestoreList : MainRoutes(route = "main_packages_restore_list/{$ArgAccountName}/{$ArgAccountRemote}") {
        fun getRoute(name: String, remote: String) = "main_packages_restore_list/${name}/${remote}"
    }
    data object PackagesRestoreDetail : MainRoutes(route = "main_packages_restore_detail/{$ArgPackageName}/{$ArgUserId}") {
        fun getRoute(packageName: String, userId: Int) = "main_packages_restore_detail/${packageName}/${userId}"
    }
    data object PackagesRestoreProcessingGraph : MainRoutes(route = "main_packages_restore_processing_graph/{$ArgAccountName}/{$ArgAccountRemote}") {
        fun getRoute(name: String, remote: String) = "main_packages_restore_processing_graph/${name}/${remote}"
    }
    data object PackagesRestoreProcessing : MainRoutes(route = "main_packages_restore_processing")
    data object PackagesRestoreProcessingSetup : MainRoutes(route = "main_packages_restore_processing_setup")

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
