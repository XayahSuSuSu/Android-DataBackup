package com.xayah.databackup.service.backup.archive

import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.entity.backup.BackupEvent
import com.xayah.databackup.entity.backup.BackupOutcome
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.backup.STATUS_ERROR
import com.xayah.databackup.rootservice.ICallback
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.backup.BackupAppSourceHelper
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.ShellHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext

class ArchiveBackupAppsHelper(private val mSourceHelper: BackupAppSourceHelper) {
    companion object {
        private const val TAG = "ArchiveBackupAppsHelper"
    }

    suspend fun backup(app: App, backupPath: String, onEvent: (BackupEvent) -> Unit): Boolean {
        currentCoroutineContext().ensureActive()
        onEvent(BackupEvent.AppStarted(app.pkgUserKey))
        var hasFailures = false
        // Keep missing directories in the candidate list so each part can report failure or be skipped when processed.
        val candidates = mSourceHelper.getCandidatePaths(app, emptyList()).groupBy { it.category }
        val parts = buildList {
            if (app.option.apk) {
                // Add APK explicitly because candidates exclude APK paths. Query them when processing this part
                // so lookup failures can be handled without preventing data backup.
                add(BackupSourceCategory.Apk)
            }
            addAll(candidates.keys)
        }
        for (part in parts) {
            val sourcePaths = candidates[part].orEmpty().map { it.path }
            val count = if (part == BackupSourceCategory.Apk) 1 else sourcePaths.size
            for (index in 0 until count) {
                currentCoroutineContext().ensureActive()
                val outputPath = getOutputPath(backupPath, app.packageName, part, index)
                onEvent(BackupEvent.PartChanged(app.pkgUserKey, part, index, BackupOutcome.Running))
                var bytes = 0L
                var hasStartedArchive = false
                val outcome = runCatching {
                    val inputs = if (part == BackupSourceCategory.Apk) {
                        RemoteRootService.getPackageSourceDir(app.packageName, app.userId).also {
                            check(it.isNotEmpty()) { "Failed to get APK sources" }
                        }
                    } else {
                        listOf(sourcePaths[index])
                    }
                    if (part != BackupSourceCategory.Apk && !RemoteRootService.exists(inputs.single())) {
                        check(!app.option.internalData || inputs.single() != PathHelper.getAppUserDir(app.userId, app.packageName)) {
                            "Internal CE data path does not exist for ${app.packageName}: ${inputs.single()}"
                        }
                        onEvent(BackupEvent.PartChanged(app.pkgUserKey, part, index, BackupOutcome.Skipped))
                        return@runCatching false
                    }
                    check(RemoteRootService.mkdirs(PathHelper.getParentPath(outputPath))) { "Cannot create archive directory" }
                    val args = inputs.flatMap { listOf("-C", PathHelper.getParentPath(it), PathHelper.getChildPath(it)) }
                    hasStartedArchive = true
                    val result = RemoteRootService.packageAndCompressArchive(outputPath, args.toTypedArray(), object : ICallback.Stub() {
                        override fun onProgress(bytesWritten: Long, speed: Long, progress: Float) {
                            bytes = bytesWritten
                            onEvent(BackupEvent.PartChanged(app.pkgUserKey, part, index, BackupOutcome.Running, bytesWritten, speed))
                        }
                    })
                    currentCoroutineContext().ensureActive()
                    val diagnostics = removeRootProcessPrefix(result.mDiagnostics)
                    val isFailed = result.mExitCode != 0
                    onEvent(
                        BackupEvent.PartChanged(
                            key = app.pkgUserKey,
                            category = part,
                            index = index,
                            outcome = if (isFailed) {
                                BackupOutcome.Failed
                            } else {
                                BackupOutcome.Completed
                            },
                            bytes = bytes,
                            exitCode = result.mExitCode,
                            diagnostics = diagnostics
                        )
                    )
                    isFailed
                }.getOrElse { error ->
                    // A canceled blocking Binder call often returns a RemoteException rather than CancellationException.
                    val isCanceled = error is CancellationException || !currentCoroutineContext().job.isActive
                    if (hasStartedArchive) {
                        withContext(NonCancellable) {
                            runCatching {
                                if (!isCanceled) {
                                    ShellHelper.killRootService()
                                }
                                RemoteRootService.deleteRecursively(outputPath)
                            }.onFailure { LogHelper.w(TAG, "backup", "Failed to clean incomplete archive") }
                        }
                    }
                    currentCoroutineContext().ensureActive()
                    if (error is CancellationException || error !is Exception) {
                        throw error
                    }
                    LogHelper.w(TAG, "backup", "Archive part failed: category=$part, error=${error.javaClass.simpleName}")
                    RemoteRootService.checkENOSPC(error.message.orEmpty())
                    onEvent(
                        BackupEvent.PartChanged(
                            key = app.pkgUserKey,
                            category = part,
                            index = index,
                            outcome = BackupOutcome.Failed,
                            bytes = bytes,
                            exitCode = STATUS_ERROR,
                            diagnostics = "Archive operation failed"
                        )
                    )
                    true
                }
                hasFailures = hasFailures || outcome
            }
        }
        onEvent(BackupEvent.AppCompleted(app.pkgUserKey))
        return hasFailures
    }

    private fun removeRootProcessPrefix(diagnostics: String): String {
        val prefix = Regex("^${Regex.escape(application.packageName)}:root:\\d+:\\s*")
        return diagnostics.lineSequence().joinToString("\n") { it.replace(prefix, "") }
    }

    private fun getOutputPath(backupPath: String, packageName: String, category: BackupSourceCategory, index: Int): String = when (category) {
        BackupSourceCategory.Apk -> PathHelper.getBackupAppsApkFilePath(backupPath, packageName)
        BackupSourceCategory.InternalData -> if (index == 0) {
            PathHelper.getBackupAppsUserFilePath(backupPath, packageName)
        } else {
            PathHelper.getBackupAppsUserDeFilePath(backupPath, packageName)
        }
        BackupSourceCategory.ExternalData -> PathHelper.getBackupAppsDataFilePath(backupPath, packageName)
        BackupSourceCategory.AdditionalData -> if (index == 0) {
            PathHelper.getBackupAppsObbFilePath(backupPath, packageName)
        } else {
            PathHelper.getBackupAppsMediaFilePath(backupPath, packageName)
        }
        else -> error("Not an app part")
    }
}
