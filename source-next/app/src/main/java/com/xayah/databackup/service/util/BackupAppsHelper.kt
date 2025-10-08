package com.xayah.databackup.service.util

import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.ZstdHelper

class BackupAppsHelper(private val mBackupProcessRepo: BackupProcessRepository) {
    companion object {
        private const val TAG = "BackupAppsHelper"
    }

    private suspend fun packageAndCompressApk(app: App) {
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        val apkPath = PathHelper.getBackupAppsApkFilePath(backupConfig.path, app.packageName)
        val apkParentPath = PathHelper.getParentPath(apkPath)
        val apkList = RemoteRootService.getPackageSourceDir(app.packageName, app.userId)

        if (apkList.isEmpty()) {
            LogHelper.e(TAG, "packageAndCompressApk", "Failed to get apk sources.")
        }

        if (RemoteRootService.mkdirs(apkParentPath).not()) {
            LogHelper.e(TAG, "packageAndCompressApk", "Failed to mkdirs: $apkParentPath.")
        }

        val inputArgs = mutableListOf<String>()
        apkList.forEach {
            inputArgs.add("-C")
            inputArgs.add(PathHelper.getParentPath(it))
            inputArgs.add(PathHelper.getChildPath(it))
        }
        val (status, info) = ZstdHelper.packageAndCompress(apkPath, *inputArgs.toTypedArray())
    }

    private suspend fun packageAndCompress(inputDir: String, outputPath: String) {
        if (RemoteRootService.exists(inputDir).not()) {
            LogHelper.i(TAG, "packageAndCompress", "Input path not exists: $inputDir.")
            return
        }

        val outputParentPath = PathHelper.getParentPath(outputPath)
        if (RemoteRootService.mkdirs(outputParentPath).not()) {
            LogHelper.e(TAG, "packageAndCompress", "Failed to mkdirs: $outputParentPath.")
        }

        val inputArgs = mutableListOf<String>()
        inputArgs.add("-C")
        inputArgs.add(PathHelper.getParentPath(inputDir))
        inputArgs.add(PathHelper.getChildPath(inputDir))
        val (status, info) = ZstdHelper.packageAndCompress(outputPath, *inputArgs.toTypedArray())
    }

    private suspend fun packageAndCompressUser(app: App) {
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        packageAndCompress(
            inputDir = PathHelper.getAppUserDir(app.userId, app.packageName),
            outputPath = PathHelper.getBackupAppsUserFilePath(backupConfig.path, app.packageName)
        )
    }

    private suspend fun packageAndCompressUserDe(app: App) {
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        packageAndCompress(
            inputDir = PathHelper.getAppUserDeDir(app.userId, app.packageName),
            outputPath = PathHelper.getBackupAppsUserDeFilePath(backupConfig.path, app.packageName)
        )
    }

    private suspend fun packageAndCompressData(app: App) {
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        packageAndCompress(
            inputDir = PathHelper.getAppDataDir(app.userId, app.packageName),
            outputPath = PathHelper.getBackupAppsDataFilePath(backupConfig.path, app.packageName)
        )
    }

    private suspend fun packageAndCompressObb(app: App) {
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        packageAndCompress(
            inputDir = PathHelper.getAppObbDir(app.userId, app.packageName),
            outputPath = PathHelper.getBackupAppsObbFilePath(backupConfig.path, app.packageName)
        )
    }

    private suspend fun packageAndCompressMedia(app: App) {
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        packageAndCompress(
            inputDir = PathHelper.getAppMediaDir(app.userId, app.packageName),
            outputPath = PathHelper.getBackupAppsMediaFilePath(backupConfig.path, app.packageName)
        )
    }

    private suspend fun packageAndCompressIntData(app: App) {
        packageAndCompressUser(app)
        packageAndCompressUserDe(app)
    }

    private suspend fun packageAndCompressExtData(app: App) {
        packageAndCompressData(app)
    }

    private suspend fun packageAndCompressAddlData(app: App) {
        packageAndCompressObb(app)
        packageAndCompressMedia(app)
    }

    suspend fun start() {
        val apps = mBackupProcessRepo.getApps()
        apps.forEachIndexed { index, app ->
            mBackupProcessRepo.updateAppsItem(index, app.info.label)
            packageAndCompressApk(app)
            packageAndCompressIntData(app)
            packageAndCompressExtData(app)
            packageAndCompressAddlData(app)
        }

        mBackupProcessRepo.updateAppsItem(apps.size, "done")
    }
}
