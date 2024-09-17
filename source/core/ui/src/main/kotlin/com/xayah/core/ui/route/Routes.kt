package com.xayah.core.ui.route

import com.xayah.core.model.OpType
import com.xayah.core.model.Target
import com.xayah.core.util.encodedURLWithSpace

sealed class MainRoutes(val route: String) {
    companion object {
        const val ARG_PACKAGE_NAME = "pkgName"
        const val ARG_MEDIA_NAME = "mediaName"
        const val ARG_USER_ID = "userId"
        const val ARG_PRESERVE_ID = "preserveId"
        const val ARG_ACCOUNT_NAME = "accountName"
        const val ARG_ACCOUNT_REMOTE = "accountRemote"
        const val ARG_TARGET = "target"
        const val ARG_OP_TYPE = "opType"
        const val ARG_ID = "id"
    }

    data object Dashboard : MainRoutes(route = "main_dashboard")
    data object Cloud : MainRoutes(route = "main_cloud")
    data object CloudAddAccount : MainRoutes(route = "main_cloud_add_account")
    data object FTPSetup : MainRoutes(route = "main_ftp_setup/{$ARG_ACCOUNT_NAME}") {
        fun getRoute(name: String) = "main_ftp_setup/$name"
    }

    data object SFTPSetup : MainRoutes(route = "main_sftp_setup/{$ARG_ACCOUNT_NAME}") {
        fun getRoute(name: String) = "main_sftp_setup/$name"
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
    data object LanguageSettings : MainRoutes(route = "main_language_settings")
    data object BlackList : MainRoutes(route = "main_blacklist")
    data object Configurations : MainRoutes(route = "main_configurations")
    data object About : MainRoutes(route = "main_about")
    data object Translators : MainRoutes(route = "main_translators")

    data object List : MainRoutes(route = "main_list/{$ARG_TARGET}/{$ARG_OP_TYPE}/{$ARG_ACCOUNT_NAME}/{$ARG_ACCOUNT_REMOTE}") {
        fun getRoute(target: Target, opType: OpType, cloudName: String = encodedURLWithSpace, backupDir: String = encodedURLWithSpace) =
            "main_list/${target}/${opType}/${cloudName}/${backupDir}"
    }

    data object Details : MainRoutes(route = "main_details/{$ARG_TARGET}/{$ARG_OP_TYPE}/{$ARG_ID}") {
        fun getRoute(target: Target, opType: OpType, id: Long) = "main_details/${target}/${opType}/${id}"
    }

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
        fun getRoute(cloudName: String = encodedURLWithSpace, backupDir: String = encodedURLWithSpace) = "main_packages_restore_processing_graph/${cloudName}/${backupDir}"
    }
    data object PackagesRestoreProcessing : MainRoutes(route = "main_packages_restore_processing")
    data object PackagesRestoreProcessingSetup : MainRoutes(route = "main_packages_restore_processing_setup")

    data object MediumBackupList : MainRoutes(route = "main_medium_backup_list")
    data object MediumBackupDetail : MainRoutes(route = "main_medium_backup_detail/{$ARG_MEDIA_NAME}") {
        fun getRoute(name: String) = "main_medium_backup_detail/${name}"
    }
    data object MediumBackupProcessingGraph : MainRoutes(route = "main_medium_backup_processing_graph")
    data object MediumBackupProcessing : MainRoutes(route = "main_medium_backup_processing")
    data object MediumBackupProcessingSetup : MainRoutes(route = "main_medium_backup_processing_setup")

    data object MediumRestoreList : MainRoutes(route = "main_medium_restore_list/{$ARG_ACCOUNT_NAME}/{$ARG_ACCOUNT_REMOTE}") {
        fun getRoute(name: String, remote: String) = "main_medium_restore_list/${name}/${remote}"
    }

    data object MediumRestoreDetail : MainRoutes(route = "main_medium_restore_detail/{$ARG_MEDIA_NAME}/{$ARG_PRESERVE_ID}") {
        fun getRoute(name: String, preserveId: Long) = "main_medium_restore_detail/${name}/${preserveId}"
    }

    data object MediumRestoreProcessingGraph : MainRoutes(route = "main_medium_restore_processing_graph/{$ARG_ACCOUNT_NAME}/{$ARG_ACCOUNT_REMOTE}") {
        fun getRoute(cloudName: String = encodedURLWithSpace, backupDir: String = encodedURLWithSpace) = "main_medium_restore_processing_graph/${cloudName}/${backupDir}"
    }

    data object MediumRestoreProcessing : MainRoutes(route = "main_medium_restore_processing")
    data object MediumRestoreProcessingSetup : MainRoutes(route = "main_medium_restore_processing_setup")

    data object Directory : MainRoutes(route = "main_directory")
}
