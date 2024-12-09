package com.xayah.core.data.repository

import android.content.Context
import com.xayah.core.database.dao.MessageDao
import com.xayah.core.model.CompressionType
import com.xayah.core.model.OpType
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.MessageEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class MessagesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val cloudRepository: CloudRepository,
    private val messageDao: MessageDao,
    private val pathUtil: PathUtil,
) {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    private fun log(onMsg: () -> String): String = run {
        val msg = onMsg()
        LogUtil.log { TAG to msg }
        msg
    }

    fun queryFlow(opType: OpType) = messageDao.queryFlow(opType).distinctUntilChanged()
    suspend fun query(opType: OpType) = messageDao.query(opType)
    suspend fun upsert(item: MessageEntity) = messageDao.upsert(item)
    suspend fun upsert(items: List<MessageEntity>) = messageDao.upsert(items)
    suspend fun delete(id: Long) = messageDao.delete(id)
    suspend fun queryActivated(opType: OpType) = messageDao.queryActivated(opType)
    suspend fun queryActivated(opType: OpType, cloud: String, backupDir: String) = messageDao.queryActivated(opType, cloud, backupDir)
    suspend fun query(opType: OpType, preserveId: Long, name: String, cloud: String, backupDir: String) = messageDao.query(opType, preserveId, name, cloud, backupDir)
    suspend fun query(opType: OpType, preserveId: Long, name: String, ct: CompressionType, cloud: String, backupDir: String) = messageDao.query(opType, preserveId, name, ct, cloud, backupDir)
    suspend fun query(opType: OpType, name: String, cloud: String, backupDir: String) = messageDao.query(opType, name, cloud, backupDir)
    suspend fun query(opType: OpType, preserveId: Long, cloud: String, backupDir: String) = messageDao.query(opType, preserveId, cloud, backupDir)
    suspend fun query(opType: OpType, cloud: String, backupDir: String) = messageDao.query(opType, cloud, backupDir)
    suspend fun query(name: String, opType: OpType) = messageDao.query(name, opType)

    private val localBackupSaveDir get() = context.localBackupSaveDir()
    val backupMessagesDir get() = pathUtil.getLocalMessagesDir()
}