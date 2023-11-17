package com.xayah.core.service.medium.restore.local

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.model.MediaRestoreOperationEntity
import com.xayah.core.datastore.readResetBackupList
import com.xayah.core.model.OperationState
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.service.util.MediumRestoreUtil
import com.xayah.core.service.util.upsertRestoreOpData
import com.xayah.core.util.DateUtil
import com.xayah.core.util.NotificationUtil
import com.xayah.core.util.PathUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import javax.inject.Inject
import com.xayah.core.util.LogUtil.log as KLog

@AndroidEntryPoint
internal class RestoreServiceImpl : Service() {
    companion object {
        private const val TAG = "MediumRestoreServiceImpl"
    }

    private val binder = OperationLocalBinder()

    override fun onBind(intent: Intent): IBinder {
        startForeground(1, NotificationUtil.getForegroundNotification(applicationContext))
        return binder
    }

    inner class OperationLocalBinder : Binder() {
        fun getService(): RestoreServiceImpl = this@RestoreServiceImpl
    }

    private val mutex = Mutex()
    private val context by lazy { applicationContext }
    private fun log(msg: () -> String) = KLog { TAG to msg() }

    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var pathUtil: PathUtil

    @Inject
    lateinit var mediumRestoreUtil: MediumRestoreUtil

    @Inject
    lateinit var mediaDao: MediaDao

    @ExperimentalSerializationApi
    suspend fun processing(timestamp: Long) = withIOContext {
        mutex.withLock {
            log { "Processing is starting." }

            val medium = mediaDao.queryRestoreSelected()
            medium.forEach { currentMedia ->
                log { "Current media: ${currentMedia.name}, src: ${currentMedia.path}." }

                val mediaRestoreOperationEntity = MediaRestoreOperationEntity(
                    entityId = currentMedia.id,
                    timestamp = timestamp,
                    startTimestamp = DateUtil.getTimestamp(),
                    endTimestamp = 0,
                    path = currentMedia.path,
                    name = currentMedia.name,
                    mediaState = OperationState.PROCESSING,
                ).also { entity -> entity.id = mediaDao.upsertRestoreOp(entity) }

                val srcDir = "${pathUtil.getLocalRestoreArchivesMediumDir()}/${currentMedia.name}/${currentMedia.timestamp}"

                if (currentMedia.selected) {
                    mediaDao.upsertRestoreOpData(
                        op = mediaRestoreOperationEntity,
                        opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(mediumRestoreUtil.getDataSrc(srcDir))
                    )
                    mediumRestoreUtil.restoreData(path = currentMedia.path, srcDir = srcDir)
                        .also { result ->
                            mediaDao.upsertRestoreOpData(
                                op = mediaRestoreOperationEntity,
                                opState = if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                }

                // Update package state and end time.
                mediaRestoreOperationEntity.mediaState = if (mediaRestoreOperationEntity.isSucceed) OperationState.DONE else OperationState.ERROR
                mediaRestoreOperationEntity.endTimestamp = DateUtil.getTimestamp()
                mediaDao.upsertRestoreOp(mediaRestoreOperationEntity)

                // Insert restore config into database.
                if (mediaRestoreOperationEntity.isSucceed) {
                    log { "Succeed." }

                    // Reset selected items if enabled.
                    if (context.readResetBackupList().first()) {
                        currentMedia.selected = false
                        mediaDao.upsertRestore(currentMedia)
                    }
                } else {
                    log { "Failed." }
                }
            }
        }
    }
}
