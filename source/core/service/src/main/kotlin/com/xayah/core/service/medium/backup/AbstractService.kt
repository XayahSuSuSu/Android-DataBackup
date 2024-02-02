package com.xayah.core.service.medium.backup

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.readBackupItself
import com.xayah.core.model.OpType
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.service.R
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.service.util.MediumBackupUtil
import com.xayah.core.util.DateUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.NotificationUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi

@AndroidEntryPoint
internal abstract class AbstractService : Service() {
    companion object {
        private const val TAG = "MediumBackupServiceImpl"
    }

    private val binder = OperationLocalBinder()

    override fun onBind(intent: Intent): IBinder {
        startForeground(1, NotificationUtil.getForegroundNotification(applicationContext))
        return binder
    }

    inner class OperationLocalBinder : Binder() {
        fun getService(): AbstractService = this@AbstractService
    }

    private val mutex = Mutex()
    private val context by lazy { applicationContext }

    internal fun log(onMsg: () -> String): String = run {
        val msg = onMsg()
        LogUtil.log { TAG to msg }
        msg
    }

    abstract val rootService: RemoteRootService
    abstract val pathUtil: PathUtil
    abstract val taskDao: TaskDao
    abstract val mediaDao: MediaDao
    abstract val mediumBackupUtil: MediumBackupUtil
    abstract val taskRepository: TaskRepository
    abstract val commonBackupUtil: CommonBackupUtil
    abstract val mediaRepository: MediaRepository

    private val notificationBuilder by lazy { NotificationUtil.getProgressNotificationBuilder(context) }
    private var startTimestamp: Long = 0
    private var endTimestamp: Long = 0
    internal val taskEntity by lazy {
        TaskEntity(
            id = 0,
            opType = OpType.BACKUP,
            taskType = TaskType.MEDIA,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            backupDir = context.localBackupSaveDir(),
            rawBytes = 0.toDouble(),
            availableBytes = 0.toDouble(),
            totalBytes = 0.toDouble(),
            totalCount = 0,
            successCount = 0,
            failureCount = 0,
            isProcessing = true,
            cloud = "",
        )
    }

    suspend fun preprocessing() = withIOContext {
        mutex.withLock {
            startTimestamp = DateUtil.getTimestamp()

            NotificationUtil.notify(context, notificationBuilder, context.getString(R.string.backing_up), context.getString(R.string.preprocessing))
            log { "Preprocessing is starting." }
        }
    }

    abstract suspend fun createTargetDirs()
    abstract suspend fun backupMedia(m: MediaEntity)

    @ExperimentalSerializationApi
    suspend fun processing() = withIOContext {
        mutex.withLock {
            log { "Processing is starting." }
            createTargetDirs()

            // createTargetDirs() before readStatFs().
            taskEntity.also {
                it.startTimestamp = startTimestamp
                it.rawBytes = taskRepository.getRawBytes(TaskType.MEDIA)
                it.availableBytes = taskRepository.getAvailableBytes(OpType.BACKUP)
                it.totalBytes = taskRepository.getTotalBytes(OpType.BACKUP)
                it.id = taskDao.upsert(it)
            }

            val medium = mediaDao.queryActivated()
            log { "Task count: ${medium.size}." }
            taskEntity.also {
                it.totalCount = medium.size
                taskDao.upsert(it)
            }

            medium.forEachIndexed { index, currentMedia ->
                NotificationUtil.notify(
                    context,
                    notificationBuilder,
                    context.getString(R.string.backing_up),
                    currentMedia.name,
                    medium.size,
                    index
                )
                log { "Current media: $currentMedia" }

                backupMedia(currentMedia)
            }
        }
    }

    @ExperimentalSerializationApi
    suspend fun postProcessing() = withIOContext {
        mutex.withLock {
            NotificationUtil.notify(
                context,
                notificationBuilder,
                context.getString(R.string.backing_up),
                context.getString(R.string.wait_for_remaining_data_processing)
            )
            log { "PostProcessing is starting." }

            val dstDir = context.localBackupSaveDir()
            // Backup itself if enabled.
            if (context.readBackupItself().first()) {
                log { "Backup itself enabled." }
                commonBackupUtil.backupItself(dstDir = dstDir)
            }

            mediaDao.clearActivated()
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
