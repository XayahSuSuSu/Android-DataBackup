package com.xayah.databackup.service.restore.archive

import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.PathHelper.APP_USER_PREFIX
import com.xayah.databackup.util.PathHelper.APP_USER_SEPARATOR

internal object ArchiveRestorePaths {
    private val mPackageNamePattern = Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*")

    fun validatePackageName(packageName: String) {
        require(mPackageNamePattern.matches(packageName)) { "Invalid archive package name" }
    }

    fun getAppArchivePaths(parent: String, packageName: String, userId: Int): Map<String, BackupSourceCategory> {
        validatePackageName(packageName)
        return linkedMapOf(
            PathHelper.getBackupAppsApkFilePath(parent, packageName, userId) to BackupSourceCategory.Apk,
            PathHelper.getBackupAppsUserFilePath(parent, packageName, userId) to BackupSourceCategory.InternalData,
            PathHelper.getBackupAppsUserDeFilePath(parent, packageName, userId) to BackupSourceCategory.InternalData,
            PathHelper.getBackupAppsDataFilePath(parent, packageName, userId) to BackupSourceCategory.ExternalData,
            PathHelper.getBackupAppsObbFilePath(parent, packageName, userId) to BackupSourceCategory.AdditionalData,
            PathHelper.getBackupAppsMediaFilePath(parent, packageName, userId) to BackupSourceCategory.AdditionalData,
        )
    }

    internal data class AppDirectory(val packageName: String, val userId: Int)

    fun parseAppDirectory(directoryName: String): AppDirectory? {
        val packageName = directoryName.substringBefore(APP_USER_SEPARATOR)
        if (!mPackageNamePattern.matches(packageName)) {
            return null
        }
        if (APP_USER_SEPARATOR !in directoryName) {
            return null
        }
        val suffix = directoryName.substringAfter(APP_USER_SEPARATOR)
        if (!suffix.startsWith(APP_USER_PREFIX)) {
            return null
        }
        val userIdText = suffix.substring(APP_USER_PREFIX.length)
        val userId = userIdText.toIntOrNull() ?: return null
        // Reject "01" and "+1" so directory names match generated paths.
        if (userId < 0 || userIdText != userId.toString()) {
            return null
        }
        return AppDirectory(packageName, userId)
    }

    fun validateAppSelection(parent: String, packageName: String, userId: Int, paths: List<String>) {
        require(userId >= 0 && paths.isNotEmpty() && paths.distinct().size == paths.size) { "Invalid app archive selection" }
        require(getAppArchivePaths(parent, packageName, userId).keys.containsAll(paths)) {
            "Archive does not belong to selected user"
        }
    }

}
