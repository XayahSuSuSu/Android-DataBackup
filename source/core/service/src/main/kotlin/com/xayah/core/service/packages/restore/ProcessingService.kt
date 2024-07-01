package com.xayah.core.service.packages.restore

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.xayah.core.util.NotificationUtil
import com.xayah.core.util.withMainContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

abstract class ProcessingService {
    private var mBinder: IBinder? = null
    private var mService: RestoreService? = null
    private var mConnection: ServiceConnection? = null
    abstract val context: Context
    abstract val intent: Intent

    private suspend fun bindService(): RestoreService = suspendCoroutine { continuation ->
        if (mService == null) {
            mConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    mBinder = service
                    mService = (mBinder as RestoreService.OperationLocalBinder).getService()
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
            context.bindService(intent, mConnection!!, Context.BIND_AUTO_CREATE)
        } else {
            mService
        }
    }

    /**
     * Destroy the service.
     */
    fun destroyService(clearNotification: Boolean = false) {
        if (clearNotification) NotificationUtil.cancel(context)
        if (mConnection != null)
            context.unbindService(mConnection!!)
        mService?.stopSelf()
        mBinder = null
        mService = null
        mConnection = null
    }

    private suspend fun getService(): RestoreService = withMainContext {
        if (mService == null) {
            bindService()
        } else if (mBinder!!.isBinderAlive.not()) {
            destroyService()
            bindService()
        } else {
            mService!!
        }
    }

    suspend fun initialize(cloudName: String, cloudRemote: String) = getService().initialize(cloudName, cloudRemote)

    suspend fun preprocessing() = getService().preprocessing()

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun processing() = getService().processing()

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun postProcessing() = getService().postProcessing()
}
