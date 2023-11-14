package com.xayah.core.service.medium.restore.local

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.xayah.core.util.withMainContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RestoreService @Inject constructor(@ApplicationContext private val context: Context) {
    private var mBinder: IBinder? = null
    private var mService: RestoreServiceImpl? = null
    private var mConnection: ServiceConnection? = null

    private suspend fun bindService(): RestoreServiceImpl = suspendCoroutine { continuation ->
        if (mService == null) {
            mConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    mBinder = service
                    mService = (mBinder as RestoreServiceImpl.OperationLocalBinder).getService()
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
            val intent = Intent(context, RestoreServiceImpl::class.java)
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

    private suspend fun getService(): RestoreServiceImpl = withMainContext {
        if (mService == null) {
            bindService()
        } else if (mBinder!!.isBinderAlive.not()) {
            destroyService()
            bindService()
        } else {
            mService!!
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun processing(timestamp: Long) = getService().processing(timestamp = timestamp)
}
