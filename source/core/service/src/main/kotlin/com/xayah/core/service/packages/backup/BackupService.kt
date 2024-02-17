package com.xayah.core.service.packages.backup

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.model.OpType
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.service.R
import com.xayah.core.service.model.BackupPreprocessing
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.service.util.PackagesBackupUtil
import com.xayah.core.util.DateUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.NotificationUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.command.PreparationUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi

@AndroidEntryPoint
internal abstract class BackupService : Service() {
    companion object {
        private const val TAG = "PackagesBackupServiceImpl"
    }

    private val binder = OperationLocalBinder()

    override fun onBind(intent: Intent): IBinder {
        startForeground(1, NotificationUtil.getForegroundNotification(applicationContext))
        return binder
    }

    inner class OperationLocalBinder : Binder() {
        fun getService(): BackupService = this@BackupService
    }

    private val mutex = Mutex()
    internal val context by lazy { applicationContext }

    internal fun log(onMsg: () -> String): String = run {
        val msg = onMsg()
        LogUtil.log { TAG to msg }
        msg
    }

    abstract val rootService: RemoteRootService
    abstract val pathUtil: PathUtil
    abstract val taskDao: TaskDao
    abstract val packageDao: PackageDao
    abstract val packagesBackupUtil: PackagesBackupUtil
    abstract val taskRepository: TaskRepository
    abstract val commonBackupUtil: CommonBackupUtil
    abstract val packageRepository: PackageRepository

    private val notificationBuilder by lazy { NotificationUtil.getProgressNotificationBuilder(context) }
    internal var startTimestamp: Long = 0
    internal var endTimestamp: Long = 0
    abstract val taskEntity: TaskEntity

    suspend fun preprocessing(): BackupPreprocessing = withIOContext {
        mutex.withLock {
            startTimestamp = DateUtil.getTimestamp()

            NotificationUtil.notify(context, notificationBuilder, context.getString(R.string.backing_up), context.getString(R.string.preprocessing))
            log { "Preprocessing is starting." }

            /**
             * Somehow the input methods and accessibility services
             * will be changed after backing up on some devices,
             * so we restore them manually.
             */
            val backupPreprocessing = BackupPreprocessing(inputMethods = "", accessibilityServices = "")

            PreparationUtil.getInputMethods().also { result ->
                backupPreprocessing.inputMethods = result.outString.trim()
            }
            PreparationUtil.getAccessibilityServices().also { result ->
                backupPreprocessing.accessibilityServices = result.outString.trim()
            }
            log { "InputMethods: ${backupPreprocessing.inputMethods}." }
            log { "AccessibilityServices: ${backupPreprocessing.accessibilityServices}." }
            backupPreprocessing
        }
    }

    abstract suspend fun createTargetDirs()
    abstract suspend fun backupPackage(p: PackageEntity)
    abstract suspend fun backupItself()
    abstract suspend fun backupIcons()
    abstract suspend fun clear()

    private suspend fun runCatchingOnService(block: suspend () -> Unit) = runCatching { block() }.onFailure {
        log { it.message.toString() }
        rootService.onFailure(it)
    }

    @ExperimentalSerializationApi
    suspend fun processing() = withIOContext {
        mutex.withLock {
            log { "Processing is starting." }
            runCatchingOnService { createTargetDirs() }

            // createTargetDirs() before readStatFs().
            taskEntity.also {
                it.startTimestamp = startTimestamp
                it.rawBytes = taskRepository.getRawBytes(TaskType.PACKAGE)
                it.availableBytes = taskRepository.getAvailableBytes(OpType.BACKUP)
                it.totalBytes = taskRepository.getTotalBytes(OpType.BACKUP)
                it.id = taskDao.upsert(it)
            }

            val packages = packageDao.queryActivated()
            log { "Task count: ${packages.size}." }
            taskEntity.also {
                it.totalCount = packages.size
                taskDao.upsert(it)
            }

            packages.forEachIndexed { index, currentPackage ->
                NotificationUtil.notify(
                    context,
                    notificationBuilder,
                    context.getString(R.string.backing_up),
                    currentPackage.packageInfo.label,
                    packages.size,
                    index
                )
                log { "Current package: $currentPackage" }

                // Kill the package.
                log { "Trying to kill ${currentPackage.packageName}." }
                BaseUtil.killPackage(userId = currentPackage.userId, packageName = currentPackage.packageName)

                runCatchingOnService { backupPackage(currentPackage) }
            }
        }
    }

    @ExperimentalSerializationApi
    suspend fun postProcessing(backupPreprocessing: BackupPreprocessing) = withIOContext {
        mutex.withLock {
            NotificationUtil.notify(
                context,
                notificationBuilder,
                context.getString(R.string.backing_up),
                context.getString(R.string.wait_for_remaining_data_processing)
            )
            log { "PostProcessing is starting." }

            // Restore keyboard and services.
            if (backupPreprocessing.inputMethods.isNotEmpty()) {
                PreparationUtil.setInputMethods(inputMethods = backupPreprocessing.inputMethods)
                log { "InputMethods restored: ${backupPreprocessing.inputMethods}." }
            } else {
                log { "InputMethods is empty, skip restoring." }
            }
            if (backupPreprocessing.accessibilityServices.isNotEmpty()) {
                PreparationUtil.setAccessibilityServices(accessibilityServices = backupPreprocessing.accessibilityServices)
                log { "AccessibilityServices restored: ${backupPreprocessing.accessibilityServices}." }
            } else {
                log { "AccessibilityServices is empty, skip restoring." }
            }

            runCatchingOnService { backupItself() }
            runCatchingOnService { backupIcons() }
            runCatchingOnService { clear() }

            packageDao.clearActivated()
            endTimestamp = DateUtil.getTimestamp()
            taskEntity.also {
                it.endTimestamp = endTimestamp
                it.isProcessing = false
                taskDao.upsert(it)
            }
            val time = DateUtil.getShortRelativeTimeSpanString(context = context, time1 = startTimestamp, time2 = endTimestamp)
            NotificationUtil.notify(
                context,
                notificationBuilder,
                context.getString(R.string.backup_completed),
                "${time}, ${taskEntity.successCount} ${context.getString(R.string.succeed)}, ${taskEntity.failureCount} ${context.getString(R.string.failed)}",
                ongoing = false
            )
        }
    }
}
