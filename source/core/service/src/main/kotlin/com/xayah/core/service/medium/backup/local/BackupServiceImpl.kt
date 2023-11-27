package com.xayah.core.service.medium.backup.local

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.xayah.core.data.repository.MediaRestoreRepository
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.model.MediaBackupOperationEntity
import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.datastore.readBackupItself
import com.xayah.core.datastore.readResetBackupList
import com.xayah.core.model.OperationState
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.service.util.MediumBackupUtil
import com.xayah.core.service.util.upsertBackupOpData
import com.xayah.core.util.DateUtil
import com.xayah.core.util.NotificationUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import javax.inject.Inject
import com.xayah.core.util.LogUtil.log as KLog

@AndroidEntryPoint
internal class BackupServiceImpl : Service() {
    companion object {
        private const val TAG = "MediumBackupServiceImpl"
    }

    private val binder = OperationLocalBinder()

    override fun onBind(intent: Intent): IBinder {
        startForeground(1, NotificationUtil.getForegroundNotification(applicationContext))
        return binder
    }

    inner class OperationLocalBinder : Binder() {
        fun getService(): BackupServiceImpl = this@BackupServiceImpl
    }

    private val mutex = Mutex()
    private val context by lazy { applicationContext }
    private fun log(msg: () -> String) = KLog { TAG to msg() }

    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var pathUtil: PathUtil

    @Inject
    lateinit var mediumBackupUtil: MediumBackupUtil

    @Inject
    lateinit var mediaDao: MediaDao

    @Inject
    lateinit var mediaRestoreRepository: MediaRestoreRepository

    @ExperimentalSerializationApi
    suspend fun processing(timestamp: Long) = withIOContext {
        mutex.withLock {
            log { "Processing is starting." }

            rootService.mkdirs(pathUtil.getLocalBackupArchivesMediumDir())
            rootService.mkdirs(pathUtil.getLocalBackupConfigsDir())

            val medium = mediaDao.queryBackupSelected()
            medium.forEach { currentMedia ->
                log { "Current media: ${currentMedia.name}, src: ${currentMedia.path}." }

                val mediaBackupOperationEntity = MediaBackupOperationEntity(
                    timestamp = timestamp,
                    startTimestamp = DateUtil.getTimestamp(),
                    endTimestamp = 0,
                    path = currentMedia.path,
                    name = currentMedia.name,
                    mediaState = OperationState.PROCESSING,
                ).also { entity -> entity.id = mediaDao.upsertBackupOp(entity) }

                val dstDir = "${pathUtil.getLocalBackupArchivesMediumDir()}/${currentMedia.name}/${timestamp}"
                rootService.mkdirs(dstDir)

                if (currentMedia.selected) {
                    mediaDao.upsertBackupOpData(
                        op = mediaBackupOperationEntity,
                        opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(mediaBackupOperationEntity.path)
                    )
                    mediumBackupUtil.backupData(src = currentMedia.path, dstDir = dstDir)
                        .also { result ->
                            mediaDao.upsertBackupOpData(
                                op = mediaBackupOperationEntity,
                                opState = if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                }

                // Update package state and end time.
                mediaBackupOperationEntity.mediaState = if (mediaBackupOperationEntity.isSucceed) OperationState.DONE else OperationState.ERROR
                mediaBackupOperationEntity.endTimestamp = DateUtil.getTimestamp()
                mediaDao.upsertBackupOp(mediaBackupOperationEntity)

                // Insert restore config into database.
                if (mediaBackupOperationEntity.isSucceed) {
                    log { "Succeed." }

                    val restoreEntire = MediaRestoreEntity(
                        timestamp = timestamp,
                        path = currentMedia.path,
                        name = currentMedia.name,
                        sizeBytes = 0,
                        selected = false,
                        savePath = context.localBackupSaveDir(),
                    )
                    mediaDao.upsertRestore(restoreEntire)

                    // Save config
                    rootService.writeProtoBuf(data = restoreEntire, dst = PathUtil.getMediaRestoreConfigDst(dstDir = dstDir))

                    // Reset selected items if enabled.
                    if (context.readResetBackupList().first()) {
                        currentMedia.selected = false
                        mediaDao.upsertBackup(currentMedia)
                    }
                } else {
                    log { "Failed." }
                }
            }
        }
    }

    @ExperimentalSerializationApi
    suspend fun postProcessing(timestamp: Long) = withIOContext {
        mutex.withLock {
            log { "PostProcessing is starting." }

            val dstDir = context.localBackupSaveDir()
            // Backup itself if enabled.
            if (context.readBackupItself().first()) {
                log { "Backup itself enabled." }
                mediumBackupUtil.backupItself(dstDir = dstDir)
            }

            log { "Save configs." }
            val configsDst = PathUtil.getMediaRestoreConfigDst(dstDir = pathUtil.getConfigsDir(dstDir))
            mediaRestoreRepository.writeMediaProtoBuf(configsDst) { storedList ->
                storedList.apply { addAll(mediaDao.queryRestoreMedium(timestamp)) }.toList()
            }
        }
    }
}
