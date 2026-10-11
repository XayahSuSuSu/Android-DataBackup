package com.xayah.databackup.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.xayah.databackup.R
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.entity.BackupBackendType
import com.xayah.databackup.entity.backup.BackupState
import com.xayah.databackup.entity.backup.BackupStatus
import com.xayah.databackup.feature.MainActivity
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * Starts or cancels a backup by task ID. The request data stays in [BackupProcessRepository],
 * which prevents repeated service starts from executing the same request more than once.
 */
object BackupService {
    const val EXTRA_TASK_ID = "backup_task_id"
    private const val ACTION_START = "com.xayah.databackup.START_BACKUP"
    private const val ACTION_CANCEL = "com.xayah.databackup.CANCEL_BACKUP"
    private const val NOTIFICATION_ID = 3
    private const val TAG = "BackupService"

    fun start(context: Context, taskId: String) {
        ContextCompat.startForegroundService(context, Intent(context, BackupServiceImpl::class.java)
            .setAction(ACTION_START).putExtra(EXTRA_TASK_ID, taskId))
    }

    fun cancel(context: Context, taskId: String) {
        context.startService(Intent(context, BackupServiceImpl::class.java)
            .setAction(ACTION_CANCEL).putExtra(EXTRA_TASK_ID, taskId))
    }

    /**
     * Runs in the app process. The manifest registers this service as BackupService$BackupServiceImpl.
     */
    class BackupServiceImpl : Service() {
        private val mProcessRepo: BackupProcessRepository by inject()
        private val mScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var mExecutionJob: Job? = null
        private var mNotificationJob: Job? = null
        private var mWakeJob: Job? = null
        private var mWakeLock: PowerManager.WakeLock? = null
        private val mResourceLock = Any()
        private var mCanPublishProgress = false
        private var mActiveId: String? = null
        private var mIsFinishing = false

        override fun onBind(intent: Intent): IBinder? = null

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            val taskId = intent?.getStringExtra(EXTRA_TASK_ID)
            if (taskId.isNullOrBlank()) {
                if (mActiveId == null) {
                    stopSelf(startId)
                }
                return START_NOT_STICKY
            }
            if (intent.action == ACTION_CANCEL) {
                if (taskId == mActiveId) {
                    stopTask(taskId, isInterrupted = false)
                } else if (mActiveId == null) {
                    stopSelf(startId)
                }
                return START_NOT_STICKY
            }
            if (mActiveId != null) {
                // Duplicate start delivery only reconnects observers; it must never start another writer.
                return START_NOT_STICKY
            }
            if (intent.action != ACTION_START || !mProcessRepo.canExecute(taskId)) {
                stopSelf(startId)
                return START_NOT_STICKY
            }
            mActiveId = taskId
            mIsFinishing = false
            try {
                NotificationHelper.createChannelIfNecessary(this)
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
                ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(mProcessRepo.getState(taskId).value), type)
                mCanPublishProgress = true
                val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:BackupService").apply {
                    setReferenceCounted(false)
                    acquire(10 * 60 * 1000L)
                }
                mWakeJob = mScope.launch {
                    while (isActive) {
                        delay(5 * 60 * 1000L)
                        synchronized(mResourceLock) {
                            mWakeLock?.acquire(10 * 60 * 1000L)
                        }
                    }
                }
                mNotificationJob = mScope.launch {
                    mProcessRepo.getState(taskId).sample(500).collect { state ->
                        synchronized(mResourceLock) {
                            if (mCanPublishProgress && NotificationHelper.checkPermission(this@BackupServiceImpl)) {
                                NotificationHelper.getNotificationManager(this@BackupServiceImpl).notify(NOTIFICATION_ID, buildNotification(state))
                            }
                        }
                    }
                }
                mExecutionJob = mScope.launch {
                    try {
                        mProcessRepo.execute(taskId)
                    } finally {
                        withContext(NonCancellable + Dispatchers.Main.immediate) {
                            mIsFinishing = true
                            releaseResources()
                            val state = mProcessRepo.getState(taskId).value
                            ServiceCompat.stopForeground(this@BackupServiceImpl, ServiceCompat.STOP_FOREGROUND_DETACH)
                            try {
                                if (NotificationHelper.checkPermission(this@BackupServiceImpl)) {
                                    NotificationHelper.getNotificationManager(this@BackupServiceImpl).notify(NOTIFICATION_ID, buildNotification(state))
                                }
                            } finally {
                                mActiveId = null
                                stopSelf()
                            }
                        }
                    }
                }
            } catch (error: Exception) {
                LogHelper.w(TAG, "onStartCommand", "Cannot start foreground backup: error=${error.javaClass.simpleName}")
                mProcessRepo.failToStart(taskId)
                mIsFinishing = true
                releaseResources()
                mActiveId = null
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        override fun onTimeout(startId: Int, fgsType: Int) {
            mActiveId?.let { stopTask(it, isInterrupted = true) }
            releaseResources()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            // Do not wait for blocking Binder/root cleanup on the main thread or exceed Android's timeout grace period.
            stopSelf()
        }

        override fun onDestroy() {
            if (!mIsFinishing) {
                mActiveId?.let { stopTask(it, isInterrupted = true) }
            }
            releaseResources()
            mScope.cancel()
            super.onDestroy()
        }

        private fun stopTask(taskId: String, isInterrupted: Boolean) {
            // Start undispatched to register cancellation before onDestroy cancels the execution scope.
            CoroutineScope(Dispatchers.IO).launch(start = CoroutineStart.UNDISPATCHED) {
                runCatching { mProcessRepo.cancel(taskId, isInterrupted) }.onFailure { error ->
                    if (error is CancellationException) {
                        throw error
                    }
                    LogHelper.w(TAG, "stopTask", "Root backup termination failed")
                }
            }
        }

        private fun releaseResources() {
            mNotificationJob?.cancel()
            mWakeJob?.cancel()
            synchronized(mResourceLock) {
                mCanPublishProgress = false
                mWakeLock?.let {
                    if (it.isHeld) {
                        it.release()
                    }
                }
                mWakeLock = null
            }
        }

        private fun buildNotification(state: BackupState): android.app.Notification {
            val openIntent = Intent(this, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_TASK_ID, state.id)
            val contentIntent = PendingIntent.getActivity(this, NOTIFICATION_ID, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val title = when (state.status) {
                BackupStatus.Completed -> R.string.finished
                BackupStatus.Canceled -> R.string.canceled
                BackupStatus.Interrupted -> R.string.backup_interrupted
                BackupStatus.Failed, BackupStatus.PartialFailure -> R.string.failed
                else -> R.string.backing_up
            }
            val total = state.categoryProgress.sumOf { it.totalCount }
            val completed = state.categoryProgress.sumOf { it.completedCount }
            val percent = when (state.backendType) {
                BackupBackendType.Archive -> if (total > 0) {
                    completed * 100 / total
                } else {
                    0
                }
                BackupBackendType.Rustic -> (state.progress * 100).toInt()
            }
            return NotificationHelper.getNotificationBuilder(this)
                .setContentTitle(getString(title))
                .setContentText(state.backupName.ifBlank { getString(R.string.backup) })
                .setContentIntent(contentIntent)
                .setOngoing(state.isActive)
                .setAutoCancel(!state.isActive)
                .setProgress(
                    if (state.isActive) {
                        100
                    } else {
                        0
                    },
                    if (state.isActive) {
                        percent
                    } else {
                        0
                    },
                    state.isActive && percent == 0,
                )
                .build()
        }
    }
}
