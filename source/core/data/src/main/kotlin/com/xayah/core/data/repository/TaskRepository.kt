package com.xayah.core.data.repository

import android.content.Context
import androidx.annotation.StringRes
import com.xayah.core.database.dao.PackageBackupEntireDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.database.model.TaskEntity
import com.xayah.core.datastore.readBackupSaveParentPath
import com.xayah.core.datastore.readBackupSavePath
import com.xayah.core.util.DateUtil
import com.xayah.librootservice.service.RemoteRootService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class TaskRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val taskDao: TaskDao,
    private val packageBackupDao: PackageBackupEntireDao,
) {
    fun getString(@StringRes resId: Int) = context.getString(resId)

    private suspend fun getTargetParentPath() = context.readBackupSaveParentPath().first()
    suspend fun getTargetPath() = context.readBackupSavePath().first()

    suspend fun getRawBytes(): Double = run {
        var total = 0.0
        val bothPackages = packageBackupDao.queryActiveBothPackages().first()
        val apkOnlyPackages = packageBackupDao.queryActiveAPKOnlyPackages().first()
        val dataOnlyPackages = packageBackupDao.queryActiveDataOnlyPackages().first()
        bothPackages.forEach { total += it.storageStats.appBytes + it.storageStats.dataBytes }
        apkOnlyPackages.forEach { total += it.storageStats.appBytes }
        dataOnlyPackages.forEach { total += it.storageStats.dataBytes }
        total
    }

    suspend fun getAvailableBytes(): Double = rootService.readStatFs(getTargetParentPath()).availableBytes.toDouble()
    suspend fun getTotalBytes(): Double = rootService.readStatFs(getTargetParentPath()).totalBytes.toDouble()

    fun getShortRelativeTimeSpanString(time1: Long, time2: Long) =
        DateUtil.getShortRelativeTimeSpanString(context = context, time1 = time1, time2 = time2)

    fun getTaskOrNull(timestamp: Long): Flow<TaskEntity?> = taskDao.queryFlow(timestamp).distinctUntilChanged()
    suspend fun upsertTask(task: TaskEntity) = taskDao.upsert(task)
    suspend fun updateEndTimestamp(timestamp: Long, endTimestamp: Long) = taskDao.updateEndTimestamp(timestamp = timestamp, endTimestamp = endTimestamp)
    suspend fun updateStartTimestamp(timestamp: Long, startTimestamp: Long) =
        taskDao.updateStartTimestamp(timestamp = timestamp, startTimestamp = startTimestamp)
}
