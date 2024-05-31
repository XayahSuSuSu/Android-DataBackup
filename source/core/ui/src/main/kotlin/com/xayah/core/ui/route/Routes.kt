package com.xayah.core.ui.route

sealed class MainRoutes(val route: String) {
    companion object {
        const val ARG_PACKAGE_NAME = "pkgName"
        const val ARG_USER_ID = "userId"
        const val ARG_PRESERVE_ID = "preserveId"
        const val ARG_ACCOUNT_NAME = "accountName"
        const val ARG_ACCOUNT_REMOTE = "accountRemote"
    }

    data object Dashboard : MainRoutes(route = "main_dashboard")
    data object Cloud : MainRoutes(route = "main_cloud")
    data object CloudAddAccount : MainRoutes(route = "main_cloud_add_account")
    data object FTPSetup : MainRoutes(route = "main_ftp_setup/{$ARG_ACCOUNT_NAME}") {
        fun getRoute(name: String) = "main_ftp_setup/$name"
    }

    data object WebDAVSetup : MainRoutes(route = "main_webdav_setup/{$ARG_ACCOUNT_NAME}") {
        fun getRoute(name: String) = "main_webdav_setup/$name"
    }

    data object SMBSetup : MainRoutes(route = "main_smb_setup/{$ARG_ACCOUNT_NAME}") {
        fun getRoute(name: String) = "main_smb_setup/$name"
    }
    data object Settings : MainRoutes(route = "main_settings")
    data object Restore : MainRoutes(route = "main_restore")
    data object Reload : MainRoutes(route = "main_reload/{$ARG_ACCOUNT_NAME}/{$ARG_ACCOUNT_REMOTE}") {
        fun getRoute(name: String, remote: String) = "main_reload/${name}/${remote}"
    }
    data object BackupSettings : MainRoutes(route = "main_backup_settings")
    data object RestoreSettings : MainRoutes(route = "main_restore_settings")

    data object PackagesBackupList : MainRoutes(route = "main_packages_backup_list")
    data object PackagesBackupDetail : MainRoutes(route = "main_packages_backup_detail/{$ARG_PACKAGE_NAME}/{$ARG_USER_ID}") {
        fun getRoute(packageName: String, userId: Int) = "main_packages_backup_detail/${packageName}/${userId}"
    }
    data object PackagesBackupProcessingGraph : MainRoutes(route = "main_packages_backup_processing_graph")
    data object PackagesBackupProcessing : MainRoutes(route = "main_packages_backup_processing")
    data object PackagesBackupProcessingSetup : MainRoutes(route = "main_packages_backup_processing_setup")

    data object PackagesRestoreList : MainRoutes(route = "main_packages_restore_list/{$ARG_ACCOUNT_NAME}/{$ARG_ACCOUNT_REMOTE}") {
        fun getRoute(name: String, remote: String) = "main_packages_restore_list/${name}/${remote}"
    }

    data object PackagesRestoreDetail : MainRoutes(route = "main_packages_restore_detail/{$ARG_PACKAGE_NAME}/{$ARG_USER_ID}/{$ARG_PRESERVE_ID}") {
        fun getRoute(packageName: String, userId: Int, preserveId: Long) = "main_packages_restore_detail/${packageName}/${userId}/${preserveId}"
    }

    data object PackagesRestoreProcessingGraph : MainRoutes(route = "main_packages_restore_processing_graph/{$ARG_ACCOUNT_NAME}/{$ARG_ACCOUNT_REMOTE}") {
        fun getRoute(name: String, remote: String) = "main_packages_restore_processing_graph/${name}/${remote}"
    }
    data object PackagesRestoreProcessing : MainRoutes(route = "main_packages_restore_processing")
    data object PackagesRestoreProcessingSetup : MainRoutes(route = "main_packages_restore_processing_setup")
    data object Directory : MainRoutes(route = "main_directory")
}
