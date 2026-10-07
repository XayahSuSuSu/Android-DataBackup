package com.xayah.databackup.service.restore.archive

import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.util.PathHelper

internal object ArchiveRestorePaths {
    fun validatePackageName(packageName: String) {
        require(Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*").matches(packageName)) { "Invalid archive package name" }
    }

    fun getAppArchivePaths(parent: String, packageName: String): Map<String, BackupSourceCategory> {
        validatePackageName(packageName)
        return linkedMapOf(
            PathHelper.getBackupAppsApkFilePath(parent, packageName) to BackupSourceCategory.Apk,
            PathHelper.getBackupAppsUserFilePath(parent, packageName) to BackupSourceCategory.InternalData,
            PathHelper.getBackupAppsUserDeFilePath(parent, packageName) to BackupSourceCategory.InternalData,
            PathHelper.getBackupAppsDataFilePath(parent, packageName) to BackupSourceCategory.ExternalData,
            PathHelper.getBackupAppsObbFilePath(parent, packageName) to BackupSourceCategory.AdditionalData,
            PathHelper.getBackupAppsMediaFilePath(parent, packageName) to BackupSourceCategory.AdditionalData,
        )
    }

    val structuredFileRelativePaths: List<String> = listOf(
        PathHelper.getBackupNetworksConfigFileRelativePath(),
        PathHelper.getBackupContactsConfigFileRelativePath(),
        PathHelper.getBackupCallLogsConfigFileRelativePath(),
        PathHelper.getBackupMessagesSmsConfigFileRelativePath(),
        PathHelper.getBackupMessagesMmsConfigFileRelativePath(),
    )
}
