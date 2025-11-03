package com.xayah.databackup.service.util

import arrow.optics.copy
import arrow.optics.dsl.index
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.data.ProcessAppDataDetailItem
import com.xayah.databackup.data.ProcessAppItem
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.data.addlDataItem
import com.xayah.databackup.data.apkItem
import com.xayah.databackup.data.bytes
import com.xayah.databackup.data.currentIndex
import com.xayah.databackup.data.details
import com.xayah.databackup.data.enabled
import com.xayah.databackup.data.extDataItem
import com.xayah.databackup.data.info
import com.xayah.databackup.data.intDataItem
import com.xayah.databackup.data.msg
import com.xayah.databackup.data.progress
import com.xayah.databackup.data.speed
import com.xayah.databackup.data.status
import com.xayah.databackup.data.subtitle
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.rootservice.ICallback
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.ZstdHelper
import com.xayah.databackup.util.formatToStorageSize
import com.xayah.databackup.util.formatToStorageSizePerSecond

class BackupAppsHelper(private val mBackupProcessRepo: BackupProcessRepository) {
    companion object {
        private const val TAG = "BackupAppsHelper"
        private const val STATUS_SUCCESS = 0
        private const val STATUS_ERROR = -1
        private const val STATUS_SKIP = -100
        private const val STATUS_CANCEL = -101
    }

    private fun getMsgByStatus(status: Int): String {
        return when (status) {
            STATUS_SUCCESS -> application.getString(R.string.succeed)
            STATUS_SKIP, STATUS_CANCEL -> application.getString(R.string.skip)
            else -> application.getString(R.string.error)
        }
    }

    private fun getSubtitleByStatus(status: Int, subtitle: String): String {
        return when (status) {
            STATUS_SUCCESS -> subtitle
            STATUS_SKIP -> application.getString(R.string.not_exist)
            STATUS_CANCEL -> application.getString(R.string.cancel)
            else -> subtitle
        }
    }

    private fun getEnabledByStatus(status: Int): Boolean {
        return when (status) {
            STATUS_SUCCESS -> true
            STATUS_SKIP, STATUS_CANCEL -> false
            else -> true
        }
    }

    private fun getFinalStatusByResult(result: List<Pair<Int, String>>): Int {
        return when {
            result.all { it.first == STATUS_SUCCESS } -> STATUS_SUCCESS
            result.all { it.first == STATUS_SKIP } -> STATUS_SKIP
            result.all { it.first == STATUS_CANCEL } -> STATUS_CANCEL
            else -> STATUS_ERROR
        }
    }

    private suspend fun packageAndCompressApk(app: App, onProgress: (bytesWritten: Long, speed: Long) -> Unit): Pair<Int, String> {
        var status = STATUS_SUCCESS
        var info = ""
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        val apkPath = PathHelper.getBackupAppsApkFilePath(backupConfig.path, app.packageName)
        val apkParentPath = PathHelper.getParentPath(apkPath)
        val apkList = RemoteRootService.getPackageSourceDir(app.packageName, app.userId)

        if (mBackupProcessRepo.mIsCanceled) {
            status = STATUS_CANCEL
            info = "Process is canceled."
            LogHelper.i(TAG, "packageAndCompress", info)
            return status to info
        }

        if (apkList.isEmpty()) {
            status = STATUS_ERROR
            info = "Failed to get apk sources."
            LogHelper.e(TAG, "packageAndCompressApk", info)
            return status to info
        }

        if (RemoteRootService.mkdirs(apkParentPath).not()) {
            status = STATUS_ERROR
            info = "Failed to mkdirs: $apkParentPath."
            LogHelper.e(TAG, "packageAndCompressApk", info)
            return status to info
        }

        val inputArgs = mutableListOf<String>()
        apkList.forEach {
            inputArgs.add("-C")
            inputArgs.add(PathHelper.getParentPath(it))
            inputArgs.add(PathHelper.getChildPath(it))
        }
        ZstdHelper.packageAndCompress(
            outputPath = apkPath,
            callback = object : ICallback.Stub() {
                override fun onProgress(bytesWritten: Long, speed: Long) {
                    onProgress(bytesWritten, speed)
                }
            },
            inputArgs = inputArgs.toTypedArray()
        ).also {
            status = it.first
            info = it.second
        }
        return status to info
    }

    private suspend fun packageAndCompress(
        inputDir: String,
        outputPath: String,
        onProgress: (bytesWritten: Long, speed: Long) -> Unit
    ): Pair<Int, String> {
        var status = STATUS_SUCCESS
        var info = ""

        if (mBackupProcessRepo.mIsCanceled) {
            status = STATUS_CANCEL
            info = "Process is canceled."
            LogHelper.i(TAG, "packageAndCompress", info)
            return status to info
        }

        if (RemoteRootService.exists(inputDir).not()) {
            status = STATUS_SKIP
            info = "Input path not exists: $inputDir."
            LogHelper.i(TAG, "packageAndCompress", info)
            return status to info
        }

        val outputParentPath = PathHelper.getParentPath(outputPath)
        if (RemoteRootService.mkdirs(outputParentPath).not()) {
            status = STATUS_ERROR
            info = "Failed to mkdirs: $outputParentPath."
            LogHelper.e(TAG, "packageAndCompress", info)
            return status to info
        }

        val inputArgs = mutableListOf<String>()
        inputArgs.add("-C")
        inputArgs.add(PathHelper.getParentPath(inputDir))
        inputArgs.add(PathHelper.getChildPath(inputDir))
        ZstdHelper.packageAndCompress(
            outputPath = outputPath,
            callback = object : ICallback.Stub() {
                override fun onProgress(bytesWritten: Long, speed: Long) {
                    onProgress(bytesWritten, speed)
                }
            },
            inputArgs = inputArgs.toTypedArray()
        ).also {
            status = it.first
            info = it.second
        }
        return status to info
    }

    private suspend fun packageAndCompressUser(app: App, onProgress: (bytesWritten: Long, speed: Long) -> Unit): Pair<Int, String> {
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        return packageAndCompress(
            inputDir = PathHelper.getAppUserDir(app.userId, app.packageName),
            outputPath = PathHelper.getBackupAppsUserFilePath(backupConfig.path, app.packageName),
            onProgress = onProgress,
        )
    }

    private suspend fun packageAndCompressUserDe(app: App, onProgress: (bytesWritten: Long, speed: Long) -> Unit): Pair<Int, String> {
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        return packageAndCompress(
            inputDir = PathHelper.getAppUserDeDir(app.userId, app.packageName),
            outputPath = PathHelper.getBackupAppsUserDeFilePath(backupConfig.path, app.packageName),
            onProgress = onProgress,
        )
    }

    private suspend fun packageAndCompressData(app: App, onProgress: (bytesWritten: Long, speed: Long) -> Unit): Pair<Int, String> {
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        return packageAndCompress(
            inputDir = PathHelper.getAppDataDir(app.userId, app.packageName),
            outputPath = PathHelper.getBackupAppsDataFilePath(backupConfig.path, app.packageName),
            onProgress = onProgress,
        )
    }

    private suspend fun packageAndCompressObb(app: App, onProgress: (bytesWritten: Long, speed: Long) -> Unit): Pair<Int, String> {
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        return packageAndCompress(
            inputDir = PathHelper.getAppObbDir(app.userId, app.packageName),
            outputPath = PathHelper.getBackupAppsObbFilePath(backupConfig.path, app.packageName),
            onProgress = onProgress,
        )
    }

    private suspend fun packageAndCompressMedia(app: App, onProgress: (bytesWritten: Long, speed: Long) -> Unit): Pair<Int, String> {
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        return packageAndCompress(
            inputDir = PathHelper.getAppMediaDir(app.userId, app.packageName),
            outputPath = PathHelper.getBackupAppsMediaFilePath(backupConfig.path, app.packageName),
            onProgress = onProgress,
        )
    }

    private suspend fun packageAndCompressIntData(
        app: App,
        onProgress: (index: Int, bytesWritten: Long, speed: Long) -> Unit
    ): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        result.add(packageAndCompressUser(app) { bytesWritten, speed -> onProgress.invoke(0, bytesWritten, speed) })
        result.add(packageAndCompressUserDe(app) { bytesWritten, speed -> onProgress.invoke(1, bytesWritten, speed) })
        return result
    }

    private suspend fun packageAndCompressExtData(
        app: App,
        onProgress: (index: Int, bytesWritten: Long, speed: Long) -> Unit
    ): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        result.add(packageAndCompressData(app) { bytesWritten, speed -> onProgress.invoke(0, bytesWritten, speed) })
        return result
    }

    private suspend fun packageAndCompressAddlData(
        app: App,
        onProgress: (index: Int, bytesWritten: Long, speed: Long) -> Unit
    ): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        result.add(packageAndCompressObb(app) { bytesWritten, speed -> onProgress.invoke(0, bytesWritten, speed) })
        result.add(packageAndCompressMedia(app) { bytesWritten, speed -> onProgress.invoke(1, bytesWritten, speed) })
        return result
    }

    suspend fun start() {
        val apps = mBackupProcessRepo.getApps()
        apps.forEachIndexed { index, app ->
            mBackupProcessRepo.updateAppsItem {
                copy {
                    ProcessItem.currentIndex set index
                    ProcessItem.msg set app.info.label
                    ProcessItem.progress set index.toFloat() / apps.size
                }
            }
            mBackupProcessRepo.addProcessAppItem(ProcessAppItem(label = app.info.label, packageName = app.packageName, userId = app.userId))

            if (app.option.apk) {
                packageAndCompressApk(app) { bytesWritten, speed ->
                    mBackupProcessRepo.updateProcessAppItem {
                        val bytesWrittenFormatted = bytesWritten.formatToStorageSize
                        val speedFormatted = speed.formatToStorageSizePerSecond
                        copy {
                            ProcessAppItem.apkItem.subtitle set bytesWrittenFormatted
                            ProcessAppItem.apkItem.msg set speedFormatted
                            inside(ProcessAppItem.apkItem.details.index(0)) {
                                ProcessAppDataDetailItem.bytes set bytesWritten
                                ProcessAppDataDetailItem.speed set speed
                            }
                        }
                    }
                }.also { (status, info) ->
                    mBackupProcessRepo.updateProcessAppItem {
                        copy {
                            ProcessAppItem.apkItem.enabled set getEnabledByStatus(status)
                            ProcessAppItem.apkItem.subtitle set getSubtitleByStatus(status, apkItem.subtitle)
                            ProcessAppItem.apkItem.msg set getMsgByStatus(status)
                            inside(ProcessAppItem.apkItem.details.index(0)) {
                                ProcessAppDataDetailItem.status set status
                                ProcessAppDataDetailItem.info set info
                            }
                        }
                    }
                }
            } else {
                mBackupProcessRepo.updateProcessAppItem {
                    copy {
                        ProcessAppItem.apkItem.enabled set false
                        ProcessAppItem.apkItem.subtitle set application.getString(R.string.not_selected)
                        ProcessAppItem.apkItem.msg set application.getString(R.string.skip)
                    }
                }
            }

            if (app.option.internalData) {
                packageAndCompressIntData(app) { index, bytesWritten, speed ->
                    mBackupProcessRepo.updateProcessAppItem {
                        val bytesWrittenFormatted = (intDataItem.details.withIndex().sumOf { (i, item) ->
                            if (i < index) item.bytes else 0
                        } + bytesWritten).formatToStorageSize
                        val speedFormatted = speed.formatToStorageSizePerSecond
                        copy {
                            ProcessAppItem.intDataItem.subtitle set bytesWrittenFormatted
                            ProcessAppItem.intDataItem.msg set speedFormatted
                            inside(ProcessAppItem.intDataItem.details.index(index)) {
                                ProcessAppDataDetailItem.bytes set bytesWritten
                                ProcessAppDataDetailItem.speed set speed
                            }
                        }
                    }
                }.also { result ->
                    val finalStatus = getFinalStatusByResult(result)
                    mBackupProcessRepo.updateProcessAppItem {
                        copy {
                            ProcessAppItem.intDataItem.enabled set getEnabledByStatus(finalStatus)
                            ProcessAppItem.intDataItem.subtitle set getSubtitleByStatus(finalStatus, intDataItem.subtitle)
                            ProcessAppItem.intDataItem.msg set getMsgByStatus(finalStatus)
                            result.forEachIndexed { index, (status, info) ->
                                inside(ProcessAppItem.intDataItem.details.index(index)) {
                                    ProcessAppDataDetailItem.status set status
                                    ProcessAppDataDetailItem.info set info
                                }
                            }
                        }
                    }
                }
            } else {
                mBackupProcessRepo.updateProcessAppItem {
                    copy {
                        ProcessAppItem.intDataItem.enabled set false
                        ProcessAppItem.intDataItem.subtitle set application.getString(R.string.not_selected)
                        ProcessAppItem.intDataItem.msg set application.getString(R.string.skip)
                    }
                }
            }

            if (app.option.externalData) {
                packageAndCompressExtData(app) { index, bytesWritten, speed ->
                    mBackupProcessRepo.updateProcessAppItem {
                        val bytesWrittenFormatted = (extDataItem.details.withIndex().sumOf { (i, item) ->
                            if (i < index) item.bytes else 0
                        } + bytesWritten).formatToStorageSize
                        val speedFormatted = speed.formatToStorageSizePerSecond
                        copy {
                            ProcessAppItem.extDataItem.subtitle set bytesWrittenFormatted
                            ProcessAppItem.extDataItem.msg set speedFormatted
                            inside(ProcessAppItem.extDataItem.details.index(index)) {
                                ProcessAppDataDetailItem.bytes set bytesWritten
                                ProcessAppDataDetailItem.speed set speed
                            }
                        }
                    }
                }.also { result ->
                    val finalStatus = getFinalStatusByResult(result)
                    mBackupProcessRepo.updateProcessAppItem {
                        copy {
                            ProcessAppItem.extDataItem.enabled set getEnabledByStatus(finalStatus)
                            ProcessAppItem.extDataItem.subtitle set getSubtitleByStatus(finalStatus, extDataItem.subtitle)
                            ProcessAppItem.extDataItem.msg set getMsgByStatus(finalStatus)
                            result.forEachIndexed { index, (status, info) ->
                                inside(ProcessAppItem.extDataItem.details.index(index)) {
                                    ProcessAppDataDetailItem.status set status
                                    ProcessAppDataDetailItem.info set info
                                }
                            }
                        }
                    }
                }
            } else {
                mBackupProcessRepo.updateProcessAppItem {
                    copy {
                        ProcessAppItem.extDataItem.enabled set false
                        ProcessAppItem.extDataItem.subtitle set application.getString(R.string.not_selected)
                        ProcessAppItem.extDataItem.msg set application.getString(R.string.skip)
                    }
                }
            }

            if (app.option.additionalData) {
                packageAndCompressAddlData(app) { index, bytesWritten, speed ->
                    mBackupProcessRepo.updateProcessAppItem {
                        val bytesWrittenFormatted = (addlDataItem.details.withIndex().sumOf { (i, item) ->
                            if (i < index) item.bytes else 0
                        } + bytesWritten).formatToStorageSize
                        val speedFormatted = speed.formatToStorageSizePerSecond
                        copy {
                            ProcessAppItem.addlDataItem.subtitle set bytesWrittenFormatted
                            ProcessAppItem.addlDataItem.msg set speedFormatted
                            inside(ProcessAppItem.addlDataItem.details.index(index)) {
                                ProcessAppDataDetailItem.bytes set bytesWritten
                                ProcessAppDataDetailItem.speed set speed
                            }
                        }
                    }
                }.also { result ->
                    val finalStatus = getFinalStatusByResult(result)
                    mBackupProcessRepo.updateProcessAppItem {
                        copy {
                            ProcessAppItem.addlDataItem.enabled set getEnabledByStatus(finalStatus)
                            ProcessAppItem.addlDataItem.subtitle set getSubtitleByStatus(finalStatus, addlDataItem.subtitle)
                            ProcessAppItem.addlDataItem.msg set getMsgByStatus(finalStatus)
                            result.forEachIndexed { index, (status, info) ->
                                inside(ProcessAppItem.addlDataItem.details.index(index)) {
                                    ProcessAppDataDetailItem.status set status
                                    ProcessAppDataDetailItem.info set info
                                }
                            }
                        }
                    }
                }
            } else {
                mBackupProcessRepo.updateProcessAppItem {
                    copy {
                        ProcessAppItem.addlDataItem.enabled set false
                        ProcessAppItem.addlDataItem.subtitle set application.getString(R.string.not_selected)
                        ProcessAppItem.addlDataItem.msg set application.getString(R.string.skip)
                    }
                }
            }
        }

        mBackupProcessRepo.updateAppsItem {
            copy {
                ProcessItem.currentIndex set apps.size
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
