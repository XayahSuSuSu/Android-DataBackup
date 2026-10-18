package com.xayah.databackup.rootservice.install

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import androidx.core.content.ContextCompat
import com.xayah.databackup.rootservice.IApkInstaller
import com.xayah.databackup.rootservice.IApkInstallerConnection
import com.xayah.databackup.rootservice.IRemoteRootService
import com.xayah.databackup.util.LogHelper
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * Opens a task-scoped APK install session and relays worker Binders, tokens, and target user IDs from broadcasts to the root service.
 * Closing the connection closes the remote session and unregisters the receiver.
 */
internal object ApkInstallConnection {
    private const val TAG = "ApkInstallConnection"

    fun getActionPrefix(packageName: String): String = "$packageName.INSTALLER."

    fun open(context: Context, service: IRemoteRootService): Closeable {
        val action = "${getActionPrefix(context.packageName)}${UUID.randomUUID()}"
        val connection = AtomicReference<IApkInstallerConnection?>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != action || intent.`package` != context.packageName) {
                    return
                }
                runCatching {
                    val extras = intent.extras ?: return
                    val token = extras.getString(ApkInstallerMain.EXTRA_TOKEN) ?: return
                    val userId = extras.getInt(ApkInstallerMain.EXTRA_USER_ID, -1)
                    val binder = extras.getBinder(ApkInstallerMain.EXTRA_BINDER) ?: return
                    if (userId >= 0) {
                        connection.get()?.connect(token, userId, IApkInstaller.Stub.asInterface(binder))
                    }
                }.onFailure {
                    LogHelper.w(TAG, "onReceive", "Cannot connect APK installer: ${it.javaClass.simpleName}")
                }
            }
        }
        // Require a privileged sender permission, including on older Android versions without receiver export flag enforcement.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(action),
            Manifest.permission.BROADCAST_PACKAGE_REMOVED,
            null,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        try {
            connection.set(service.openApkInstallSession(action, Binder()))
        } catch (error: Exception) {
            context.unregisterReceiver(receiver)
            throw error
        }
        return Closeable {
            try {
                if (service.asBinder().isBinderAlive) {
                    service.closeApkInstallSession(action)
                }
            } finally {
                context.unregisterReceiver(receiver)
            }
        }
    }
}
