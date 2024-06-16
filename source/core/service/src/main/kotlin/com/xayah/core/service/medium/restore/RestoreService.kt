package com.xayah.core.service.medium.restore

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.view.SurfaceControlHidden
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.readAutoScreenOff
import com.xayah.core.datastore.readResetRestoreList
import com.xayah.core.datastore.readScreenOffTimeout
import com.xayah.core.datastore.readSelectionType
import com.xayah.core.datastore.saveLastRestoreTime
import com.xayah.core.datastore.saveScreenOffCountDown
import com.xayah.core.datastore.saveScreenOffTimeout
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.ProcessingType
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.Info
import com.xayah.core.model.database.ProcessingInfoEntity
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.service.R
import com.xayah.core.service.util.MediumRestoreUtil
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
internal abstract class RestoreService : Service() {
    companion object {
        private const val TAG = "MediumRestoreServiceImpl"
    }

    private val binder = OperationLocalBinder()

    override fun onBind(intent: Intent): IBinder {
        startForeground(1, NotificationUtil.getForegroundNotification(applicationContext))
        return binder
    }

    inner class OperationLocalBinder : Binder() {
        fun getService(): RestoreService = this@RestoreService
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
    abstract val mediaDao: MediaDao
    abstract val mediumRestoreUtil: MediumRestoreUtil
    abstract val taskRepository: TaskRepository
    abstract val mediaRepository: MediaRepository

    private val notificationBuilder by lazy { NotificationUtil.getProgressNotificationBuilder(context) }
    internal var startTimestamp: Long = 0
    internal var endTimestamp: Long = 0
    abstract val taskEntity: TaskEntity

    private lateinit var prePreparationsEntity: ProcessingInfoEntity
    private lateinit var postDataProcessingEntity: ProcessingInfoEntity
    private val mediaEntities: MutableList<TaskDetailMediaEntity> = mutableListOf()

    private var isInitialized: Boolean = false

    @SuppressLint("StringFormatInvalid")
    suspend fun initialize(cloudName: String, cloudRemote: String): Long {
        mutex.withLock {
            if (rootService.getScreenOffTimeout() != Int.MAX_VALUE) {
                context.saveScreenOffTimeout(rootService.getScreenOffTimeout())
            }
            if (isInitialized.not()) {
                taskEntity.also {
                    it.id = taskDao.upsert(it)
                }
                prePreparationsEntity = ProcessingInfoEntity(
                    id = 0,
                    taskId = taskEntity.id,
                    title = context.getString(R.string.necessary_preparations),
                    type = ProcessingType.PREPROCESSING,
                ).apply {
                    id = taskDao.upsert(this)
                }
                postDataProcessingEntity = ProcessingInfoEntity(
                    id = 0,
                    taskId = taskEntity.id,
                    title = context.getString(R.string.necessary_remaining_data_processing),
                    type = ProcessingType.POST_PROCESSING,
                ).apply {
                    id = taskDao.upsert(this)
                }

                val medium = if (cloudName.isEmpty())
                    mediaRepository.queryActivated(OpType.RESTORE, "", context.localBackupSaveDir())
                else
                    mediaRepository.queryActivated(OpType.RESTORE, cloudName, cloudRemote)

                medium.forEach { media ->
                    mediaEntities.add(TaskDetailMediaEntity(
                        id = 0,
                        taskId = taskEntity.id,
                        mediaEntity = media,
                        mediaInfo = Info(title = context.getString(R.string.args_restore, DataType.PACKAGE_MEDIA.type.uppercase())),
                    ).apply {
                        id = taskDao.upsert(this)
                    })
                }
                isInitialized = true
            }
            return taskEntity.id
        }
    }

    suspend fun preprocessing() = withIOContext {
        mutex.withLock {
            prePreparationsEntity.also {
                it.state = OperationState.PROCESSING
                taskDao.upsert(it)
            }

            if (context.readAutoScreenOff().first()) {
                context.saveScreenOffCountDown(3)
            }

            startTimestamp = DateUtil.getTimestamp()

            NotificationUtil.notify(context, notificationBuilder, context.getString(R.string.restoring), context.getString(R.string.preprocessing))
            log { "Preprocessing is starting." }

            runCatchingOnService { createTargetDirs() }

            prePreparationsEntity.also {
                it.state = OperationState.DONE
                taskDao.upsert(it)
            }

            taskEntity.also {
                it.processingIndex++
                taskDao.upsert(it)
            }
        }
    }

    abstract suspend fun createTargetDirs()
    abstract suspend fun restoreMedia(t: TaskDetailMediaEntity)
    abstract suspend fun clear()

    private suspend fun runCatchingOnService(block: suspend () -> Unit) = runCatching { block() }.onFailure {
        log { it.message.toString() }
        rootService.onFailure(it)
    }

    @ExperimentalSerializationApi
    suspend fun processing() = withIOContext {
        mutex.withLock {
            log { "Processing is starting." }
            val selectionType = context.readSelectionType().first()
            log { "Selection: $selectionType." }

            // createTargetDirs() before readStatFs().
            taskEntity.also {
                it.startTimestamp = startTimestamp
                it.rawBytes = taskRepository.getRawBytes(TaskType.MEDIA)
                it.availableBytes = taskRepository.getAvailableBytes(OpType.RESTORE)
                it.totalBytes = taskRepository.getTotalBytes(OpType.RESTORE)
            }

            log { "Task count: ${mediaEntities.size}." }
            taskEntity.also {
                it.totalCount = mediaEntities.size
                taskDao.upsert(it)
            }

            mediaEntities.forEachIndexed { index, media ->
                NotificationUtil.notify(
                    context,
                    notificationBuilder,
                    context.getString(R.string.restoring),
                    media.mediaEntity.name,
                    mediaEntities.size,
                    index
                )
                log { "Current media: ${media.mediaEntity}" }

                runCatchingOnService { restoreMedia(media) }

                taskEntity.also {
                    it.processingIndex++
                    taskDao.upsert(it)
                }
            }
        }
    }

    @ExperimentalSerializationApi
    suspend fun postProcessing() = withIOContext {
        mutex.withLock {
            NotificationUtil.notify(
                context,
                notificationBuilder,
                context.getString(R.string.restoring),
                context.getString(R.string.wait_for_remaining_data_processing)
            )
            log { "PostProcessing is starting." }

            postDataProcessingEntity.also {
                it.state = OperationState.PROCESSING
                taskDao.upsert(it)
            }

            runCatchingOnService { clear() }

            postDataProcessingEntity.also {
                it.state = OperationState.DONE
                taskDao.upsert(it)
            }

            if (context.readResetRestoreList().first()) mediaDao.clearActivated()
            endTimestamp = DateUtil.getTimestamp()
            taskEntity.also {
                it.endTimestamp = endTimestamp
                it.isProcessing = false
                taskDao.upsert(it)
            }
            val time = DateUtil.getShortRelativeTimeSpanString(context = context, time1 = startTimestamp, time2 = endTimestamp)
            context.saveLastRestoreTime(endTimestamp)
            NotificationUtil.notify(
                context,
                notificationBuilder,
                context.getString(R.string.restore_completed),
                "${time}, ${taskEntity.successCount} ${context.getString(R.string.succeed)}, ${taskEntity.failureCount} ${context.getString(R.string.failed)}",
                ongoing = false
            )

            taskEntity.also {
                it.processingIndex++
                taskDao.upsert(it)
            }

            rootService.setScreenOffTimeout(context.readScreenOffTimeout().first())
            rootService.setDisplayPowerMode(SurfaceControlHidden.POWER_MODE_NORMAL)
        }
    }
}
