package com.xayah.librootservice.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.widget.Toast
import com.topjohnwu.superuser.ipc.RootService
import com.xayah.librootservice.IRemoteRootService
import com.xayah.librootservice.impl.RemoteRootServiceImpl
import com.xayah.librootservice.parcelables.StatFsParcelable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RemoteRootService(private val context: Context) {
    private var mService: IRemoteRootService? = null
    private var mConnection: ServiceConnection? = null
    private var isFirstConnection = true

    class RemoteRootService : RootService() {
        override fun onBind(intent: Intent): IBinder {
            return RemoteRootServiceImpl()
        }
    }

    private suspend fun bindService(): IRemoteRootService = suspendCoroutine { continuation ->
        if (mService == null) {
            mConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    mService = IRemoteRootService.Stub.asInterface(service)
                    continuation.resume(mService!!)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    mService = null
                    mConnection = null
                    val msg = "Service disconnected."
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    continuation.resumeWithException(RemoteException(msg))
                }

                override fun onBindingDied(name: ComponentName) {
                    mService = null
                    mConnection = null
                    val msg = "Binding died."
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    continuation.resumeWithException(RemoteException(msg))
                }

                override fun onNullBinding(name: ComponentName) {
                    mService = null
                    mConnection = null
                    val msg = "Null binding."
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    continuation.resumeWithException(RemoteException(msg))
                }
            }
            val intent = Intent().apply {
                component = ComponentName(context.packageName, RemoteRootService::class.java.name)
                addCategory(RootService.CATEGORY_DAEMON_MODE)
            }
            RootService.bind(intent, mConnection!!)
        } else {
            mService
        }
    }

    /**
     * Destroy the service.
     */
    fun destroyService(killDaemon: Boolean = false) {
        if (killDaemon)
            if (mConnection != null)
                RootService.unbind(mConnection!!)
        mConnection = null
        mService = null
    }

    private suspend fun getService(): IRemoteRootService {
        return if (mService == null) {
            val msg = "Service is null."
            if (isFirstConnection)
                isFirstConnection = false
            else
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            bindService()
        } else if (mService!!.asBinder().isBinderAlive.not()) {
            mService = null
            val msg = "Service is dead."
            withContext(Dispatchers.Main) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            bindService()
        } else {
            mService!!
        }
    }

    suspend fun readStatFs(path: String): StatFsParcelable {
        return getService().readStatFs(path)
    }

    suspend fun writeText(text: String, path: String, context: Context): Boolean {
        var state = true
        val tmpFilePath = "${context.cacheDir.path}/tmp"
        val tmpFile = File(tmpFilePath)
        tmpFile.writeText(text)
        if (getService().mkdirs(path).not()) state = false
        if (getService().copyTo(tmpFilePath, path, true).not()) state = false
        tmpFile.deleteRecursively()
        return state
    }
}
