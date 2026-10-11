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
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.entity.restore.RestoreProcessState
import com.xayah.databackup.entity.restore.RestoreProcessStatus
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
 * Starts or cancels a restore by task ID. The request data stays in [RestoreProcessRepository],
 * which prevents repeated service starts from executing the same request more than once.
 */
object RestoreService {
    const val EXTRA_TASK_ID = "restore_task_id"
    private const val ACTION_START = "com.xayah.databackup.START_RESTORE"
    private const val ACTION_CANCEL = "com.xayah.databackup.CANCEL_RESTORE"
    private const val NOTIFICATION_ID = 4
    private const val TAG = "RestoreService"

    fun start(context: Context, taskId: String) {
        ContextCompat.startForegroundService(context, Intent(context, RestoreServiceImpl::class.java)
            .setAction(ACTION_START).putExtra(EXTRA_TASK_ID, taskId))
    }

    fun cancel(context: Context, taskId: String) {
        context.startService(Intent(context, RestoreServiceImpl::class.java)
            .setAction(ACTION_CANCEL).putExtra(EXTRA_TASK_ID, taskId))
    }

    /**
     * Runs in the app process. The manifest registers this service as RestoreService$RestoreServiceImpl.
     */
    class RestoreServiceImpl : Service() {
        private val mProcessRepo: RestoreProcessRepository by inject()
        private val mScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
            if (intent.action != ACTION_START) {
                if (mActiveId == null) {
                    stopSelf(startId)
                }
                return START_NOT_STICKY
            }
            // Every foreground start delivery must satisfy Android's deadline, including a
            // duplicate delivered while a previous execution is detaching its notification.
            try {
                NotificationHelper.createChannelIfNecessary(this)
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
                val state = mProcessRepo.getState(mActiveId ?: taskId).value
                ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(state), type)
            } catch (error: Exception) {
                LogHelper.w(TAG, "onStartCommand", "Cannot promote restore service: error=${error.javaClass.simpleName}")
                if (mActiveId == null) {
                    mProcessRepo.failToStart(taskId)
                    stopSelf(startId)
                } else {
                    stopTask(checkNotNull(mActiveId), isInterrupted = true)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            if (mActiveId != null) {
                return START_NOT_STICKY
            }
            if (!mProcessRepo.canExecute(taskId)) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                stopSelf(startId)
                return START_NOT_STICKY
            }
            mActiveId = taskId
            mIsFinishing = false
            try {
                mCanPublishProgress = true
                val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:RestoreService").apply {
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
                            if (mCanPublishProgress && NotificationHelper.checkPermission(this@RestoreServiceImpl)) {
                                NotificationHelper.getNotificationManager(this@RestoreServiceImpl).notify(NOTIFICATION_ID, buildNotification(state))
                            }
                        }
                    }
                }
                mScope.launch {
                    try {
                        mProcessRepo.execute(taskId)
                    } finally {
                        withContext(NonCancellable + Dispatchers.Main.immediate) {
                            mIsFinishing = true
                            releaseResources()
                            val state = mProcessRepo.getState(taskId).value
                            ServiceCompat.stopForeground(this@RestoreServiceImpl, ServiceCompat.STOP_FOREGROUND_DETACH)
                            try {
                                if (NotificationHelper.checkPermission(this@RestoreServiceImpl)) {
                                    NotificationHelper.getNotificationManager(this@RestoreServiceImpl)
                                        .notify(NOTIFICATION_ID, buildNotification(state))
                                }
                            } finally {
                                mActiveId = null
                                stopSelf()
                            }
                        }
                    }
                }
            } catch (error: Exception) {
                LogHelper.w(TAG, "onStartCommand", "Cannot start foreground restore: error=${error.javaClass.simpleName}")
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
                    LogHelper.w(TAG, "stopTask", "Root restore termination failed")
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

        private fun buildNotification(state: RestoreProcessState): android.app.Notification {
            val openIntent = Intent(this, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_TASK_ID, state.id)
            val contentIntent = PendingIntent.getActivity(this, NOTIFICATION_ID, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val title = when (state.status) {
                RestoreProcessStatus.Finished -> R.string.finished
                RestoreProcessStatus.Canceled -> R.string.canceled
                RestoreProcessStatus.Interrupted -> R.string.restore_interrupted
                RestoreProcessStatus.Failed -> R.string.failed
                RestoreProcessStatus.FinishedWithErrors -> R.string.restore_finished_with_errors
                else -> R.string.restoring
            }
            val total = state.items.sumOf { it.totalCount }
            val completed = state.items.sumOf { it.completedCount }
            val percent = if (total > 0) (completed.toLong() * 100 / total).toInt() else 0
            return NotificationHelper.getNotificationBuilder(this)
                .setContentTitle(getString(title))
                .setContentText(state.backupName.ifBlank { getString(R.string.restore) })
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
