package com.xayah.databackup.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.xayah.core.rootservice.util.withMainContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class BackupPreparation(
    val keyboard: String,
    val services: String,
)

class OperationLocalService(private val context: Context, private val cloudMode: Boolean) {
    private var mBinder: IBinder? = null
    private var mService: OperationLocalServiceImpl? = null
    private var mConnection: ServiceConnection? = null

    private suspend fun bindService(): OperationLocalServiceImpl = suspendCoroutine { continuation ->
        if (mService == null) {
            mConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    mBinder = service
                    mService = (mBinder as OperationLocalServiceImpl.OperationLocalBinder).getService()
                    continuation.resume(mService!!)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    mBinder = null
                    mService = null
                    mConnection = null
                    val msg = "Service disconnected."
                    continuation.resumeWithException(RemoteException(msg))
                }

                override fun onBindingDied(name: ComponentName) {
                    mBinder = null
                    mService = null
                    mConnection = null
                    val msg = "Binding died."
                    continuation.resumeWithException(RemoteException(msg))
                }

                override fun onNullBinding(name: ComponentName) {
                    mBinder = null
                    mService = null
                    mConnection = null
                    val msg = "Null binding."
                    continuation.resumeWithException(RemoteException(msg))
                }
            }
            val intent = Intent(context, OperationLocalServiceImpl::class.java)
            context.bindService(intent, mConnection!!, Context.BIND_AUTO_CREATE)
        } else {
            mService
        }
    }

    /**
     * Destroy the service.
     */
    fun destroyService() {
        if (mConnection != null)
            context.unbindService(mConnection!!)
        mBinder = null
        mService = null
        mConnection = null
    }

    private suspend fun getService(): OperationLocalServiceImpl = withMainContext {
        if (mService == null) {
            bindService()
        } else if (mBinder!!.isBinderAlive.not()) {
            destroyService()
            bindService()
        } else {
            mService!!
        }
    }

    suspend fun backupPackagesPreparation(): BackupPreparation = getService().backupPackagesPreparation()
    suspend fun backupPackages(timestamp: Long) = getService().backupPackages(timestamp, cloudMode)
    suspend fun backupPackagesAfterwards(preparation: BackupPreparation) = getService().backupPackagesAfterwards(preparation, cloudMode)

    suspend fun restorePackagesPreparation() = getService().restorePackagesPreparation()
    suspend fun restorePackages(timestamp: Long) = getService().restorePackages(timestamp)
    suspend fun backupMedium(timestamp: Long) = getService().backupMedium(timestamp, cloudMode)
    suspend fun backupMediumAfterwards() = getService().backupMediumAfterwards(cloudMode)
    suspend fun restoreMedium(timestamp: Long) = getService().restoreMedium(timestamp)
}
