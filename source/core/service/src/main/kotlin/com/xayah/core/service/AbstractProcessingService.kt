package com.xayah.core.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.view.SurfaceControlHidden
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.ConstantUtil.DEFAULT_IDLE_TIMEOUT
import com.xayah.core.datastore.readAutoScreenOff
import com.xayah.core.datastore.readScreenOffTimeout
import com.xayah.core.datastore.saveScreenOffCountDown
import com.xayah.core.datastore.saveScreenOffTimeout
import com.xayah.core.model.OperationState
import com.xayah.core.model.database.ProcessingInfoEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.model.util.set
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.util.DateUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.NotificationUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.withLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class AbstractProcessingService : Service() {
    override fun onBind(intent: Intent): IBinder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, NotificationUtil.getForegroundNotification(applicationContext), FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, NotificationUtil.getForegroundNotification(applicationContext))
        }
        return mBinder
    }

    inner class OperationLocalBinder : Binder() {
        fun getService(): AbstractProcessingService = this@AbstractProcessingService
    }

    protected fun log(onMsg: () -> String): String = run {
        val msg = onMsg()
        LogUtil.log { mTAG to msg }
        msg
    }

    protected suspend fun runCatchingOnService(block: suspend () -> Unit): Boolean = runCatching { block() }.onFailure { mRootService.onFailure(it) }.withLog().isSuccess

    protected suspend fun ProcessingInfoEntity.update(
        bytes: Long? = null,
        log: String? = null,
        title: String? = null,
        content: String? = null,
        progress: Float? = null,
        state: OperationState? = null,
    ) {
        set(bytes, log, title, content, progress, state)
        mTaskDao.upsert(this)
    }

    protected suspend fun TaskEntity.update(
        startTimestamp: Long? = null,
        endTimestamp: Long? = null,
        rawBytes: Double? = null,
        availableBytes: Double? = null,
        totalBytes: Double? = null,
        totalCount: Int? = null,
        successCount: Int? = null,
        failureCount: Int? = null,
        preprocessingIndex: Int? = null,
        processingIndex: Int? = null,
        postProcessingIndex: Int? = null,
        isProcessing: Boolean? = null,
        cloud: String? = null,
        backupDir: String? = null,
    ) {
        set(startTimestamp, endTimestamp, rawBytes, availableBytes, totalBytes, totalCount, successCount, failureCount, preprocessingIndex, processingIndex, postProcessingIndex, isProcessing, cloud, backupDir)
        mTaskDao.upsert(this)
    }

    /**
     * To wait for animation.
     */
    protected suspend fun executeAtLeast(minTime: Int = 1000, block: suspend () -> Unit) {
        val startTimestamp = DateUtil.getTimestamp()
        block()
        val endTimestamp = DateUtil.getTimestamp()
        val diff = endTimestamp - startTimestamp
        if (diff < minTime) {
            delay(minTime - diff)
        }
    }

    abstract suspend fun onInitializingPreprocessingEntities(entities: MutableList<ProcessingInfoEntity>)
    abstract suspend fun onInitializingPostProcessingEntities(entities: MutableList<ProcessingInfoEntity>)
    abstract suspend fun onInitializing()
    protected open suspend fun beforePreprocessing() {}
    abstract suspend fun onPreprocessing(entity: ProcessingInfoEntity)
    protected open suspend fun afterPreprocessing() {}
    protected open suspend fun beforePostProcessing() {}
    abstract suspend fun onPostProcessing(entity: ProcessingInfoEntity)
    protected open suspend fun afterPostProcessing() {}
    abstract suspend fun onProcessing()

    private val mBinder = OperationLocalBinder()
    private val mMutex = Mutex()
    protected val mContext: Context by lazy { applicationContext }
    protected abstract val mTAG: String
    protected abstract val mRootService: RemoteRootService
    protected abstract val mPathUtil: PathUtil
    protected abstract val mCommonBackupUtil: CommonBackupUtil
    protected abstract val mTaskDao: TaskDao
    protected abstract val mTaskRepo: TaskRepository
    protected abstract val mTaskEntity: TaskEntity

    protected val mNotificationBuilder by lazy { NotificationUtil.getProgressNotificationBuilder(mContext) }
    private val mPreprocessingEntities: MutableList<ProcessingInfoEntity> = mutableListOf()
    private val mPostProcessingEntities: MutableList<ProcessingInfoEntity> = mutableListOf()
    private var mIsInitialized: Boolean = false
    protected var mStartTimestamp: Long = 0
    protected var mEndTimestamp: Long = 0

    @SuppressLint("StringFormatInvalid")
    suspend fun initialize(): Long {
        mMutex.withLock {
            if (mIsInitialized.not()) {
                mTaskDao.upsert(mTaskEntity).apply {
                    // Initialize task id.
                    mTaskEntity.id = this
                }

                onInitializingPreprocessingEntities(mPreprocessingEntities)
                onInitializingPostProcessingEntities(mPostProcessingEntities)
                onInitializing()

                mIsInitialized = true
            }
            return mTaskEntity.id
        }
    }

    suspend fun preprocessing(): Unit = withIOContext {
        mMutex.withLock {
            mStartTimestamp = DateUtil.getTimestamp()
            mTaskEntity.update(startTimestamp = mStartTimestamp)
            log { "Preprocessing is starting." }
            beforePreprocessing()

            if (mContext.readAutoScreenOff().first()) {
                mContext.saveScreenOffTimeout(mRootService.getScreenOffTimeout())
                mContext.saveScreenOffCountDown(3)
            }

            mPreprocessingEntities.forEach {
                it.update(state = OperationState.PROCESSING)
                executeAtLeast {
                    onPreprocessing(it)
                }
                mTaskEntity.update(preprocessingIndex = mTaskEntity.preprocessingIndex + 1)
            }

            afterPreprocessing()
            mTaskEntity.update(processingIndex = mTaskEntity.processingIndex + 1)
        }
    }


    suspend fun processing() = withIOContext {
        mMutex.withLock {
            log { "Processing is starting." }
            onProcessing()
        }
    }

    suspend fun postProcessing() = withIOContext {
        mMutex.withLock {
            log { "PostProcessing is starting." }
            beforePostProcessing()

            mPostProcessingEntities.forEach {
                it.update(state = OperationState.PROCESSING)
                executeAtLeast {
                    onPostProcessing(it)
                }
                mTaskEntity.update(postProcessingIndex = mTaskEntity.postProcessingIndex + 1)
            }

            mRootService.setScreenOffTimeout(mContext.readScreenOffTimeout().first())
            mContext.saveScreenOffTimeout(DEFAULT_IDLE_TIMEOUT)
            mRootService.setDisplayPowerMode(SurfaceControlHidden.POWER_MODE_NORMAL)

            mEndTimestamp = DateUtil.getTimestamp()
            afterPostProcessing()
            mTaskEntity.update(endTimestamp = mEndTimestamp, isProcessing = false, processingIndex = mTaskEntity.processingIndex + 1)
        }
    }
}