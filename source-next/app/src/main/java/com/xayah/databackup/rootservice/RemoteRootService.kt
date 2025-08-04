@file:Suppress("DEPRECATION")

package com.xayah.databackup.rootservice

import android.app.ActivityThread
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.content.pm.UserInfo
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiConfigurationHidden
import android.net.wifi.WifiManagerHidden
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.os.UserManagerHidden
import com.topjohnwu.superuser.ipc.RootService
import com.xayah.databackup.App
import com.xayah.databackup.database.entity.AppInfo
import com.xayah.databackup.database.entity.AppStorage
import com.xayah.databackup.database.entity.Info
import com.xayah.databackup.database.entity.Storage
import com.xayah.databackup.parcelables.BytesParcelable
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.ParcelableHelper.marshall
import com.xayah.databackup.util.ParcelableHelper.unmarshall
import com.xayah.databackup.util.PathHelper
import com.xayah.hiddenapi.castTo
import com.xayah.libnative.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object RemoteRootService {
    private const val TAG = "RemoteRootService"
    private const val TIMEOUT = 10000L // 10s

    private fun writeToParcel(context: Context, block: (Parcel) -> Unit): ParcelFileDescriptor {
        val parcel = Parcel.obtain()
        parcel.setDataPosition(0)
        block(parcel)
        val tmpFile = File.createTempFile("databackup-parcel-", ".tmp", context.cacheDir)
        tmpFile.delete()
        tmpFile.createNewFile()
        tmpFile.writeBytes(parcel.marshall())
        val pfd = ParcelFileDescriptor.open(tmpFile, ParcelFileDescriptor.MODE_READ_WRITE)
        tmpFile.delete()
        parcel.recycle()
        return pfd
    }

    private fun readFromParcel(pfd: ParcelFileDescriptor, block: (Parcel) -> Unit) = run {
        val stream = ParcelFileDescriptor.AutoCloseInputStream(pfd)
        val bytes = stream.readBytes()
        val parcel = Parcel.obtain()
        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0)
        block(parcel)
        parcel.recycle()
    }

    private var mMutex = Mutex()
    private var mService: IRemoteRootService? = null
    private var mConnection: ServiceConnection? = null
    private val mServiceIntent by lazy { Intent().apply { component = ComponentName(App.application.packageName, Service::class.java.name) } }
    private var mRetries = 0
    private var mOnErrorEvent: (() -> Unit)? = null

    class Service : RootService() {
        init {
            System.loadLibrary("nativelib")
        }

        override fun onBind(intent: Intent): IBinder = Impl(applicationContext).apply { onBind() }
    }

    class Impl(private val context: Context) : IRemoteRootService.Stub() {
        private lateinit var mSystemContext: Context
        private lateinit var mPackageManager: PackageManager
        private lateinit var mPackageManagerHidden: PackageManagerHidden
        private lateinit var mUserManager: UserManagerHidden
        private lateinit var mWifiManager: WifiManagerHidden

        fun onBind() {
            mSystemContext = ActivityThread.systemMain().systemContext
            mPackageManager = mSystemContext.packageManager
            mPackageManagerHidden = mPackageManager.castTo()
            mUserManager = UserManagerHidden.get(mSystemContext).castTo()
            mWifiManager = mSystemContext.getSystemService(Context.WIFI_SERVICE).castTo()
        }

        override fun getInstalledAppInfos(): ParcelFileDescriptor {
            return writeToParcel(context) { parcel ->
                val infos = mutableListOf<AppInfo>()
                val users = mUserManager.users
                users.forEach { user ->
                    infos.addAll(mPackageManagerHidden.getInstalledPackagesAsUser(0, user.id).map {
                        AppInfo(
                            packageName = it.packageName,
                            userId = user.id,
                            info = Info(
                                uid = it.applicationInfo?.uid ?: 0,
                                label = it.applicationInfo?.loadLabel(mPackageManager).toString(),
                                versionName = it.versionName ?: "",
                                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    it.longVersionCode
                                } else {
                                    it.versionCode.toLong()
                                },
                                flags = it.applicationInfo?.flags ?: 0,
                                firstInstallTime = it.firstInstallTime,
                                lastUpdateTime = it.lastUpdateTime
                            )
                        )
                    })
                }
                parcel.writeTypedList(infos)
            }
        }

        override fun getInstalledAppStorages(): ParcelFileDescriptor {
            return writeToParcel(context) { parcel ->
                val storages = mutableListOf<AppStorage>()
                val users = mUserManager.users
                users.forEach { user ->
                    storages.addAll(mPackageManagerHidden.getInstalledPackagesAsUser(0, user.id).map {
                        val apkBytes = runCatching {
                            it.applicationInfo?.sourceDir?.let { path -> File(path).parent }?.let { path -> NativeLib.calculateTreeSize(path) }
                        }.getOrNull() ?: 0
                        val userBytes = NativeLib.calculateTreeSize(PathHelper.getAppUserDir(user.id, it.packageName))
                        val userDeBytes = NativeLib.calculateTreeSize(PathHelper.getAppUserDeDir(user.id, it.packageName))
                        val dataBytes = NativeLib.calculateTreeSize(PathHelper.getAppDataDir(user.id, it.packageName))
                        val obbBytes = NativeLib.calculateTreeSize(PathHelper.getAppObbDir(user.id, it.packageName))
                        val mediaBytes = NativeLib.calculateTreeSize(PathHelper.getAppMediaDir(user.id, it.packageName))
                        AppStorage(
                            packageName = it.packageName,
                            userId = user.id,
                            storage = Storage(
                                apkBytes = apkBytes,
                                internalDataBytes = userBytes + userDeBytes,
                                externalDataBytes = dataBytes,
                                obbAndMediaBytes = obbBytes + mediaBytes,
                            )
                        )
                    })
                }
                parcel.writeTypedList(storages)
            }
        }

        override fun getUsers(): List<UserInfo> {
            return mUserManager.users
        }

        override fun getPrivilegedConfiguredNetworks(): List<BytesParcelable> {
            val networks = mutableListOf<BytesParcelable>()
            mWifiManager.privilegedConfiguredNetworks.forEach {
                val bytes = it.marshall()
                networks.add(BytesParcelable(bytes.size, bytes))
            }
            return networks
        }

        override fun addNetworks(configs: List<BytesParcelable>): IntArray {
            val networkIds = mutableListOf<Int>()
            configs.forEach {
                it.bytes.unmarshall { parcel ->
                    val config = WifiConfigurationHidden.CREATOR.createFromParcel(parcel)
                    val id = mWifiManager.addNetwork(config)
                    networkIds.add(id)
                }
            }
            return networkIds.toIntArray()
        }
    }

    private fun destroyService() {
        if (mConnection != null || mService != null) {
            LogHelper.i(TAG, "Destroy the root service.")
            mConnection = null
            mService = null
        }
    }

    private suspend fun bindService(): IRemoteRootService {
        return withTimeout(TIMEOUT) {
            suspendCancellableCoroutine { continuation ->
                if (mService == null) {
                    mRetries++
                    destroyService()
                    LogHelper.i(TAG, "Bind the root service, retries: $mRetries.")
                    val connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, service: IBinder) {
                            val ipc = IRemoteRootService.Stub.asInterface(service)
                            mService = ipc
                            val msg = "Service connected."
                            LogHelper.i(TAG, msg)
                            if (continuation.context.isActive) continuation.resume(ipc)
                        }

                        override fun onServiceDisconnected(name: ComponentName) {
                            val msg = "Service disconnected."
                            LogHelper.w(TAG, msg)
                            destroyService()
                            if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                        }

                        override fun onBindingDied(name: ComponentName) {
                            val msg = "Binding died."
                            LogHelper.e(TAG, msg)
                            destroyService()
                            if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                        }

                        override fun onNullBinding(name: ComponentName) {
                            val msg = "Null binding."
                            LogHelper.e(TAG, msg)
                            destroyService()
                            if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                        }
                    }
                    RootService.bind(mServiceIntent, connection)
                    mConnection = connection
                } else {
                    mRetries = 0
                    mService
                }
            }
        }
    }

    private suspend fun getService(): IRemoteRootService? {
        mMutex.withLock {
            var rootService: IRemoteRootService? = mService
            while (rootService == null && mRetries < 3) {
                withContext(Dispatchers.Main) {
                    rootService = runCatching { bindService() }.getOrNull()
                }
                delay(1000)
            }
            if (rootService == null) {
                val msg = "Failed to bind the root service."
                LogHelper.e(TAG, msg)
                mOnErrorEvent?.invoke()
            }
            return rootService
        }
    }

    fun setOnErrorEvent(block: () -> Unit) {
        mOnErrorEvent = block
    }

    suspend fun checkService(): Boolean {
        mRetries = 0
        return getService() != null
    }

    suspend fun getInstalledAppInfos(): List<AppInfo> {
        val infos = mutableListOf<AppInfo>()
        getService()?.installedAppInfos?.also { pfd ->
            readFromParcel(pfd) {
                it.readTypedList(infos, AppInfo.CREATOR)
            }
        }
        return infos
    }

    suspend fun getInstalledAppStorages(): List<AppStorage> {
        val storages = mutableListOf<AppStorage>()
        getService()?.installedAppStorages?.also { pfd ->
            readFromParcel(pfd) {
                it.readTypedList(storages, AppStorage.CREATOR)
            }
        }
        return storages
    }

    suspend fun getUsers(): List<UserInfo> {
        return getService()?.users ?: listOf()
    }

    suspend fun getPrivilegedConfiguredNetworks(): List<WifiConfiguration> {
        val networks = mutableListOf<WifiConfiguration>()
        getService()?.getPrivilegedConfiguredNetworks()?.forEach {
            it.bytes.unmarshall { parcel ->
                networks.add(WifiConfigurationHidden.CREATOR.createFromParcel(parcel))
            }
        }
        return networks
    }

    suspend fun addNetworks(configs: List<WifiConfiguration>): IntArray {
        val networks = mutableListOf<BytesParcelable>()
        configs.forEach {
            val bytes = it.marshall()
            networks.add(BytesParcelable(bytes.size, bytes))
        }
        return getService()?.addNetworks(networks) ?: intArrayOf()
    }
}
