package com.xayah.core.service.medium.restore

import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.MediumRestoreUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class RestoreServiceLocalImpl @Inject constructor() : RestoreService() {
    @Inject
    override lateinit var rootService: RemoteRootService

    @Inject
    override lateinit var pathUtil: PathUtil

    @Inject
    override lateinit var taskDao: TaskDao

    @Inject
    override lateinit var mediaDao: MediaDao

    @Inject
    override lateinit var mediumRestoreUtil: MediumRestoreUtil

    @Inject
    override lateinit var taskRepository: TaskRepository

    @Inject
    override lateinit var mediaRepository: MediaRepository

    override val taskEntity by lazy {
        TaskEntity(
            id = 0,
            opType = OpType.RESTORE,
            taskType = TaskType.PACKAGE,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            backupDir = context.localBackupSaveDir(),
            isProcessing = true,
        )
    }

    override suspend fun createTargetDirs() {}

    private val filesDir by lazy { pathUtil.getLocalBackupFilesDir() }

    override suspend fun restoreMedia(t: TaskDetailMediaEntity) {
        t.apply {
            state = OperationState.PROCESSING
            taskDao.upsert(this)
        }

        val m = t.mediaEntity
        val srcDir = "${filesDir}/${m.archivesRelativeDir}"

        mediumRestoreUtil.restoreMedia(m = m, t = t, srcDir = srcDir)

        t.apply {
            t.apply {
                state = if (isSuccess) OperationState.DONE else OperationState.ERROR
                taskDao.upsert(this)
            }
            mediaEntity = m
            taskDao.upsert(this)
        }
        taskEntity.also {
            if (t.isSuccess) it.successCount++ else it.failureCount++
            taskDao.upsert(it)
        }
    }

    override suspend fun clear() {}
}
