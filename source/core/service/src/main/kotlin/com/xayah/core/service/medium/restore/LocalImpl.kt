package com.xayah.core.service.medium.restore

import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.service.util.MediumRestoreUtil
import com.xayah.core.util.PathUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class LocalImpl @Inject constructor() : AbstractService() {
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

    private val archivesMediumDir by lazy { pathUtil.getLocalBackupArchivesMediumDir() }

    override suspend fun restoreMedia(m: MediaEntity) {
        val srcDir = "${archivesMediumDir}/${m.archivesPreserveRelativeDir}"

        val t = TaskDetailMediaEntity(
            id = 0,
            taskId = taskEntity.id,
            mediaEntity = m,
        ).apply {
            id = taskDao.upsert(this)
        }

        mediumRestoreUtil.restoreData(m = m, t = t, srcDir = srcDir)

        t.apply {
            mediaEntity = m
            taskDao.upsert(this)
        }
        taskEntity.also {
            if (t.isSuccess) it.successCount++ else it.failureCount++
            taskDao.upsert(it)
        }
    }
}
