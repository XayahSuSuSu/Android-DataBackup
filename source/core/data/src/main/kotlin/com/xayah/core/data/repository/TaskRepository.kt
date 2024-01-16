package com.xayah.core.data.repository

import android.content.Context
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.model.OpType
import com.xayah.core.model.TaskType
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.DateUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val packageDao: PackageDao,
    private val taskDao: TaskDao,
) {
    val tasks = taskDao.queryFlow().distinctUntilChanged()

    fun queryTaskFlow(id: Long) = taskDao.queryTaskFlow(id)
    fun queryProcessingFlow(taskId: Long) = taskDao.queryFlow(taskId).map { tasks -> tasks.filter { it.isFinished.not() } }.distinctUntilChanged()
    fun querySuccessFlow(taskId: Long) = taskDao.queryFlow(taskId).map { tasks -> tasks.filter { it.isFinished && it.isSuccess } }
    fun queryFailureFlow(taskId: Long) = taskDao.queryFlow(taskId).map { tasks -> tasks.filter { it.isFinished && it.isSuccess.not() } }

    fun getShortRelativeTimeSpanString(time1: Long, time2: Long) = DateUtil.getShortRelativeTimeSpanString(context = context, time1 = time1, time2 = time2)

    suspend fun getRawBytes(taskType: TaskType): Double = run {
        var total = 0.0
        when (taskType) {
            TaskType.PACKAGE -> {
                val packages = packageDao.queryActivated()
                packages.forEach {
                    if (it.apkSelected) total += it.dataStats.apkBytes
                    if (it.userSelected) total += it.dataStats.userBytes
                    if (it.userDeSelected) total += it.dataStats.userDeBytes
                    if (it.dataSelected) total += it.dataStats.dataBytes
                    if (it.obbSelected) total += it.dataStats.obbBytes
                    if (it.mediaSelected) total += it.dataStats.mediaBytes
                }
            }

            TaskType.MEDIA -> {}
        }
        total
    }

    suspend fun getAvailableBytes(opType: OpType): Double = run {
        var total = 0.0
        total += when (opType) {
            OpType.BACKUP -> {
                rootService.readStatFs(context.localBackupSaveDir()).availableBytes.toDouble()
            }

            OpType.RESTORE -> {
                rootService.readStatFs(ConstantUtil.DefaultPathParent).availableBytes.toDouble()
            }
        }
        total
    }

    suspend fun getTotalBytes(opType: OpType): Double = run {
        var total = 0.0
        total += when (opType) {
            OpType.BACKUP -> {
                rootService.readStatFs(context.localBackupSaveDir()).totalBytes.toDouble()
            }

            OpType.RESTORE -> {
                rootService.readStatFs(ConstantUtil.DefaultPathParent).totalBytes.toDouble()
            }
        }
        total
    }
}
