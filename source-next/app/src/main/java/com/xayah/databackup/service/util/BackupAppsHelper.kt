package com.xayah.databackup.service.util

import arrow.optics.copy
import arrow.optics.dsl.index
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.data.ProcessAppDataDetailItem
import com.xayah.databackup.data.ProcessAppDataItem
import com.xayah.databackup.data.ProcessAppItem
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.data.STATUS_CANCEL
import com.xayah.databackup.data.STATUS_ERROR
import com.xayah.databackup.data.STATUS_SKIP
import com.xayah.databackup.data.STATUS_SUCCESS
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
import kotlinx.coroutines.CancellationException

class BackupAppsHelper(private val mBackupProcessRepo: BackupProcessRepository) {
    companion object {
        private const val TAG = "BackupAppsHelper"
    }

    private fun getMsgByStatus(status: Int): String {
        return when (status) {
            STATUS_SUCCESS -> application.getString(R.string.succeed)
            STATUS_SKIP -> application.getString(R.string.skip)
            STATUS_CANCEL -> application.getString(R.string.cancel)
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

    private fun ensureNotCanceled() {
        if (mBackupProcessRepo.mIsCanceled) {
            throw CancellationException("Backup apps canceled.")
        }
    }

    private suspend fun packageAndCompressApk(app: App, onProgress: (bytesWritten: Long, speed: Long) -> Unit): Pair<Int, String> {
        var status = STATUS_SUCCESS
        var info = ""
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        val apkPath = PathHelper.getBackupAppsApkFilePath(backupConfig.path, app.packageName)
        val apkParentPath = PathHelper.getParentPath(apkPath)
        val apkList = RemoteRootService.getPackageSourceDir(app.packageName, app.userId)

        ensureNotCanceled()

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

        ensureNotCanceled()

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

    private fun getCanceledProcessAppItem(app: App): ProcessAppItem {
        fun applyState(item: ProcessAppDataItem, selected: Boolean): ProcessAppDataItem {
            return if (selected) {
                item.copy {
                    ProcessAppDataItem.enabled set getEnabledByStatus(STATUS_CANCEL)
                    ProcessAppDataItem.subtitle set getSubtitleByStatus(STATUS_CANCEL, item.subtitle)
                    ProcessAppDataItem.msg set getMsgByStatus(STATUS_CANCEL)
                    item.details.indices.forEach { i ->
                        inside(ProcessAppDataItem.details.index(i)) {
                            ProcessAppDataDetailItem.status set STATUS_CANCEL
                            ProcessAppDataDetailItem.info set application.getString(R.string.cancel)
                        }
                    }
                }
            } else {
                item.copy {
                    ProcessAppDataItem.enabled set false
                    ProcessAppDataItem.subtitle set application.getString(R.string.not_selected)
                    ProcessAppDataItem.msg set application.getString(R.string.skip)
                }
            }
        }

        val base = ProcessAppItem(
            label = app.info.label,
            packageName = app.packageName,
            userId = app.userId,
        )
        return base.copy {
            ProcessAppItem.apkItem set applyState(base.apkItem, app.option.apk)
            ProcessAppItem.intDataItem set applyState(base.intDataItem, app.option.internalData)
            ProcessAppItem.extDataItem set applyState(base.extDataItem, app.option.externalData)
            ProcessAppItem.addlDataItem set applyState(base.addlDataItem, app.option.additionalData)
            ProcessAppItem.progress set 0f
        }
    }

    private data class AppStepState(
        val totalStepCount: Int,
        var completedStepCount: Int = 0,
        var apkHandled: Boolean = false,
        var intDataHandled: Boolean = false,
        var extDataHandled: Boolean = false,
        var addlDataHandled: Boolean = false,
    ) {

        fun completeStep() {
            completedStepCount += 1
        }

        fun currentProgress(): Float {
            return if (totalStepCount == 0) 1f else completedStepCount.toFloat() / totalStepCount.toFloat()
        }
    }

    private fun updateCurrentProcessApp(onUpdate: ProcessAppItem.() -> ProcessAppItem) {
        mBackupProcessRepo.updateProcessAppItem { onUpdate() }
    }

    private suspend fun processApkStep(app: App, state: AppStepState) {
        if (app.option.apk) {
            packageAndCompressApk(app) { bytesWritten, speed ->
                updateCurrentProcessApp {
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
                state.completeStep()
                updateCurrentProcessApp {
                    copy {
                        ProcessAppItem.apkItem.enabled set getEnabledByStatus(status)
                        ProcessAppItem.apkItem.subtitle set getSubtitleByStatus(status, apkItem.subtitle)
                        ProcessAppItem.apkItem.msg set getMsgByStatus(status)
                        ProcessAppItem.progress set state.currentProgress()
                        inside(ProcessAppItem.apkItem.details.index(0)) {
                            ProcessAppDataDetailItem.status set status
                            ProcessAppDataDetailItem.info set info
                        }
                    }
                }
            }
        } else {
            state.completeStep()
            updateCurrentProcessApp {
                copy {
                    ProcessAppItem.apkItem.enabled set false
                    ProcessAppItem.apkItem.subtitle set application.getString(R.string.not_selected)
                    ProcessAppItem.apkItem.msg set application.getString(R.string.skip)
                    ProcessAppItem.progress set state.currentProgress()
                }
            }
        }
    }

    private suspend fun processIntDataStep(app: App, state: AppStepState) {
        if (app.option.internalData) {
            packageAndCompressIntData(app) { index, bytesWritten, speed ->
                updateCurrentProcessApp {
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
                state.completeStep()
                updateCurrentProcessApp {
                    copy {
                        ProcessAppItem.intDataItem.enabled set getEnabledByStatus(finalStatus)
                        ProcessAppItem.intDataItem.subtitle set getSubtitleByStatus(finalStatus, intDataItem.subtitle)
                        ProcessAppItem.intDataItem.msg set getMsgByStatus(finalStatus)
                        ProcessAppItem.progress set state.currentProgress()
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
            state.completeStep()
            updateCurrentProcessApp {
                copy {
                    ProcessAppItem.intDataItem.enabled set false
                    ProcessAppItem.intDataItem.subtitle set application.getString(R.string.not_selected)
                    ProcessAppItem.intDataItem.msg set application.getString(R.string.skip)
                    ProcessAppItem.progress set state.currentProgress()
                }
            }
        }
    }

    private suspend fun processExtDataStep(app: App, state: AppStepState) {
        if (app.option.externalData) {
            packageAndCompressExtData(app) { index, bytesWritten, speed ->
                updateCurrentProcessApp {
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
                state.completeStep()
                updateCurrentProcessApp {
                    copy {
                        ProcessAppItem.extDataItem.enabled set getEnabledByStatus(finalStatus)
                        ProcessAppItem.extDataItem.subtitle set getSubtitleByStatus(finalStatus, extDataItem.subtitle)
                        ProcessAppItem.extDataItem.msg set getMsgByStatus(finalStatus)
                        ProcessAppItem.progress set state.currentProgress()
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
            state.completeStep()
            updateCurrentProcessApp {
                copy {
                    ProcessAppItem.extDataItem.enabled set false
                    ProcessAppItem.extDataItem.subtitle set application.getString(R.string.not_selected)
                    ProcessAppItem.extDataItem.msg set application.getString(R.string.skip)
                    ProcessAppItem.progress set state.currentProgress()
                }
            }
        }
    }

    private suspend fun processAddlDataStep(app: App, state: AppStepState) {
        if (app.option.additionalData) {
            packageAndCompressAddlData(app) { index, bytesWritten, speed ->
                updateCurrentProcessApp {
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
                state.completeStep()
                updateCurrentProcessApp {
                    copy {
                        ProcessAppItem.addlDataItem.enabled set getEnabledByStatus(finalStatus)
                        ProcessAppItem.addlDataItem.subtitle set getSubtitleByStatus(finalStatus, addlDataItem.subtitle)
                        ProcessAppItem.addlDataItem.msg set getMsgByStatus(finalStatus)
                        ProcessAppItem.progress set state.currentProgress()
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
            state.completeStep()
            updateCurrentProcessApp {
                copy {
                    ProcessAppItem.addlDataItem.enabled set false
                    ProcessAppItem.addlDataItem.subtitle set application.getString(R.string.not_selected)
                    ProcessAppItem.addlDataItem.msg set application.getString(R.string.skip)
                    ProcessAppItem.progress set state.currentProgress()
                }
            }
        }
    }

    private fun markCurrentAppRemainingStepsCanceled(app: App, state: AppStepState) {
        if (app.option.apk && state.apkHandled.not()) {
            updateCurrentProcessApp {
                copy {
                    ProcessAppItem.apkItem.enabled set getEnabledByStatus(STATUS_CANCEL)
                    ProcessAppItem.apkItem.subtitle set getSubtitleByStatus(STATUS_CANCEL, apkItem.subtitle)
                    ProcessAppItem.apkItem.msg set getMsgByStatus(STATUS_CANCEL)
                    inside(ProcessAppItem.apkItem.details.index(0)) {
                        ProcessAppDataDetailItem.status set STATUS_CANCEL
                        ProcessAppDataDetailItem.info set application.getString(R.string.cancel)
                    }
                }
            }
        }

        if (app.option.internalData && state.intDataHandled.not()) {
            updateCurrentProcessApp {
                copy {
                    ProcessAppItem.intDataItem.enabled set getEnabledByStatus(STATUS_CANCEL)
                    ProcessAppItem.intDataItem.subtitle set getSubtitleByStatus(STATUS_CANCEL, intDataItem.subtitle)
                    ProcessAppItem.intDataItem.msg set getMsgByStatus(STATUS_CANCEL)
                    inside(ProcessAppItem.intDataItem.details.index(0)) {
                        ProcessAppDataDetailItem.status set STATUS_CANCEL
                        ProcessAppDataDetailItem.info set application.getString(R.string.cancel)
                    }
                    inside(ProcessAppItem.intDataItem.details.index(1)) {
                        ProcessAppDataDetailItem.status set STATUS_CANCEL
                        ProcessAppDataDetailItem.info set application.getString(R.string.cancel)
                    }
                }
            }
        }

        if (app.option.externalData && state.extDataHandled.not()) {
            updateCurrentProcessApp {
                copy {
                    ProcessAppItem.extDataItem.enabled set getEnabledByStatus(STATUS_CANCEL)
                    ProcessAppItem.extDataItem.subtitle set getSubtitleByStatus(STATUS_CANCEL, extDataItem.subtitle)
                    ProcessAppItem.extDataItem.msg set getMsgByStatus(STATUS_CANCEL)
                    inside(ProcessAppItem.extDataItem.details.index(0)) {
                        ProcessAppDataDetailItem.status set STATUS_CANCEL
                        ProcessAppDataDetailItem.info set application.getString(R.string.cancel)
                    }
                }
            }
        }

        if (app.option.additionalData && state.addlDataHandled.not()) {
            updateCurrentProcessApp {
                copy {
                    ProcessAppItem.addlDataItem.enabled set getEnabledByStatus(STATUS_CANCEL)
                    ProcessAppItem.addlDataItem.subtitle set getSubtitleByStatus(STATUS_CANCEL, addlDataItem.subtitle)
                    ProcessAppItem.addlDataItem.msg set getMsgByStatus(STATUS_CANCEL)
                    inside(ProcessAppItem.addlDataItem.details.index(0)) {
                        ProcessAppDataDetailItem.status set STATUS_CANCEL
                        ProcessAppDataDetailItem.info set application.getString(R.string.cancel)
                    }
                    inside(ProcessAppItem.addlDataItem.details.index(1)) {
                        ProcessAppDataDetailItem.status set STATUS_CANCEL
                        ProcessAppDataDetailItem.info set application.getString(R.string.cancel)
                    }
                }
            }
        }
    }

    suspend fun start() {
        val apps = mBackupProcessRepo.getApps()
        apps.forEachIndexed { index, app ->
            ensureNotCanceled()
            mBackupProcessRepo.updateAppsItem {
                copy {
                    ProcessItem.currentIndex set index
                    ProcessItem.msg set app.info.label
                    ProcessItem.progress set index.toFloat() / apps.size
                }
            }
            mBackupProcessRepo.addProcessAppItem(ProcessAppItem(label = app.info.label, packageName = app.packageName, userId = app.userId))
            val selectedStepCount = listOf(
                app.option.apk,
                app.option.internalData,
                app.option.externalData,
                app.option.additionalData,
            ).count { it }
            val state = AppStepState(totalStepCount = selectedStepCount)

            try {
                processApkStep(app, state)
                state.apkHandled = true

                processIntDataStep(app, state)
                state.intDataHandled = true

                processExtDataStep(app, state)
                state.extDataHandled = true

                processAddlDataStep(app, state)
                state.addlDataHandled = true
            } catch (e: CancellationException) {
                markCurrentAppRemainingStepsCanceled(app, state)
                apps.drop(index + 1).forEach { pendingApp ->
                    mBackupProcessRepo.addProcessAppItem(getCanceledProcessAppItem(pendingApp))
                }
                throw e
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
