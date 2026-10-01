package com.xayah.databackup.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.RemoteException
import com.xayah.databackup.App
import com.xayah.databackup.data.ArchiveBackupProcessRepository
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.BackupSelectionRepository
import com.xayah.databackup.service.backup.archive.BackupAppsHelper
import com.xayah.databackup.service.backup.archive.BackupCallLogsHelper
import com.xayah.databackup.service.backup.archive.BackupContactsHelper
import com.xayah.databackup.service.backup.archive.BackupMessagesHelper
import com.xayah.databackup.service.backup.archive.BackupNetworksHelper
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.seconds

object BackupService {
    private const val TAG = "BackupService"
    private const val TIMEOUT = 10 // 10s

    private var mMutex = Mutex()
    private var mBinder: IBinder? = null
    private var mService: BackupServiceImpl? = null
    private var mConnection: ServiceConnection? = null

    class BackupServiceImpl : Service() {
        private val mBackupSelectionRepo: BackupSelectionRepository by inject()
        private val mBackupConfigRepo: BackupConfigRepository by inject()
        private val mBackupProcessRepo: ArchiveBackupProcessRepository by inject()
        private val mBackupAppsHelper: BackupAppsHelper by inject()
        private val mBackupNetworksHelper: BackupNetworksHelper by inject()
        private val mBackupContactsHelper: BackupContactsHelper by inject()
        private val mBackupCallLogsHelper: BackupCallLogsHelper by inject()
        private val mBackupMessagesHelper: BackupMessagesHelper by inject()
        private val mBinder: Binder = Service()

        inner class Service : Binder() {
            fun getService(): BackupServiceImpl = this@BackupServiceImpl
        }

        override fun onBind(intent: Intent): IBinder {
            return mBinder
        }

        suspend fun backupApps() {
            mMutex.withLock {
                mBackupAppsHelper.start()
            }
        }

        suspend fun backupNetworks() {
            mMutex.withLock {
                mBackupNetworksHelper.start()
            }
        }

        suspend fun backupContacts() {
            mMutex.withLock {
                mBackupContactsHelper.start()
            }
        }

        suspend fun backupCallLogs() {
            mMutex.withLock {
                mBackupCallLogsHelper.start()
            }
        }

        suspend fun backupMessages() {
            mMutex.withLock {
                mBackupMessagesHelper.start()
            }
        }

        suspend fun setupBackupConfig() {
            mMutex.withLock {
                mBackupConfigRepo.setupBackupConfig()
            }
        }

        private fun ensureNotCanceled(stage: String) {
            if (mBackupProcessRepo.isCanceled) {
                throw CancellationException("Backup canceled before $stage.")
            }
        }

        suspend fun start() {
            try {
                ensureNotCanceled("apps backup")
                if (mBackupSelectionRepo.isBackupAppsSelected.first()) {
                    backupApps()
                }

                ensureNotCanceled("networks backup")
                if (mBackupSelectionRepo.isBackupNetworksSelected.first()) {
                    backupNetworks()
                }

                ensureNotCanceled("contacts backup")
                if (mBackupSelectionRepo.isBackupContactsSelected.first()) {
                    backupContacts()
                }

                ensureNotCanceled("call logs backup")
                if (mBackupSelectionRepo.isBackupCallLogsSelected.first()) {
                    backupCallLogs()
                }

                ensureNotCanceled("messages backup")
                if (mBackupSelectionRepo.isBackupMessagesSelected.first()) {
                    backupMessages()
                }

                setupBackupConfig()
            } catch (e: CancellationException) {
                LogHelper.i(TAG, "start", "Backup pipeline canceled and exited early: ${e.message}")
            }
        }
    }

    private suspend fun bindService(context: Context): BackupServiceImpl? {
        return withTimeoutOrNull(TIMEOUT.seconds) {
            suspendCancellableCoroutine { continuation ->
                if (mService == null) {
                    val connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, service: IBinder) {
                            mBinder = service
                            (service as? BackupServiceImpl.Service)?.getService()?.also {
                                val msg = "Service connected."
                                LogHelper.i(TAG, "bindService", msg)
                                mService = it
                                if (continuation.context.isActive) continuation.resume(it)
                                return
                            }
                            val msg = "Service connected, but failed to get service instance."
                            if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                        }

                        override fun onServiceDisconnected(name: ComponentName) {
                            val msg = "Service disconnected."
                            LogHelper.w(TAG, "bindService", msg)
                            if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                        }

                        override fun onBindingDied(name: ComponentName) {
                            val msg = "Binding died."
                            LogHelper.e(TAG, "bindService", msg)
                            if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                        }

                        override fun onNullBinding(name: ComponentName) {
                            val msg = "Null binding."
                            LogHelper.e(TAG, "bindService", msg)
                            if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                        }
                    }
                    context.bindService(Intent(context, BackupServiceImpl::class.java), connection, Context.BIND_AUTO_CREATE)
                    mConnection = connection
                } else {
                    mService
                }
            }
        }
    }

    fun destroyService(context: Context) {
        mConnection?.also { context.unbindService(it) }
        mService?.stopSelf()
        mBinder = null
        mService = null
        mConnection = null
    }

    private suspend fun getService(): BackupServiceImpl? {
        return if (mService == null) {
            runCatching { bindService(App.application) }.getOrElse { error ->
                if (error is CancellationException) throw error
                null
            }
        } else if (mBinder?.isBinderAlive == false) {
            destroyService(App.application)
            runCatching { bindService(App.application) }.getOrElse { error ->
                if (error is CancellationException) throw error
                null
            }
        } else {
            mService
        }
    }

    suspend fun start() {
        getService()?.start()
    }
}
