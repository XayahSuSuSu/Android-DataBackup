package com.xayah.databackup.rootservice

import android.app.ActivityManagerHidden
import android.app.ActivityManagerNative
import android.app.AppOpsManager
import android.app.AppOpsManagerHidden
import android.app.IActivityManagerApi24
import android.app.IActivityManagerApi26
import android.content.AttributionSource
import android.content.ContentResolverHidden
import android.content.Context
import android.content.ContextWrapperHidden
import android.content.IContentProvider
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.provider.Telephony
import androidx.annotation.RequiresApi
import com.xayah.hiddenapi.castTo
import java.io.Closeable
import java.util.ArrayDeque

/**
 * Provides access to content providers from a root process (UID 0).
 * Acquires and releases external provider references through ActivityManager and attributes calls to root.
 */
internal class RootContentResolver(context: Context) : ContentResolverHidden(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) AttributionContext(context) else RootContext(context)
) {
    init {
        check(Process.myUid() == ROOT_UID) { "Root resolver requires UID 0" }
    }

    private val mAppOpsManager = context.getSystemService(AppOpsManager::class.java)
    private val mAppOpsManagerHidden = mAppOpsManager.castTo<AppOpsManagerHidden>()
    private val mActivityManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ActivityManagerHidden.getService() else ActivityManagerNative.getDefault()
    private val mProviderReferences = mutableMapOf<IContentProvider, ArrayDeque<ProviderReference>>()

    /**
     * Temporarily allows the WRITE_SMS app-op for root.
     * Use the returned handle with `use` to restore the previous mode even if writing fails.
     *
     * @return A handle that restores the previous mode when closed, or does nothing if already allowed.
     */
    fun allowTemporaryMessageWrites(): Closeable {
        val op = AppOpsManagerHidden.OP_WRITE_SMS
        val previousMode = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA ->
                mAppOpsManager.checkOpRawNoThrow(AppOpsManagerHidden.OPSTR_WRITE_SMS, ROOT_UID, ROOT_PACKAGE, null)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                mAppOpsManager.unsafeCheckOpRawNoThrow(AppOpsManagerHidden.OPSTR_WRITE_SMS, ROOT_UID, ROOT_PACKAGE)
            }

            else -> mAppOpsManagerHidden.checkOpNoThrow(op, ROOT_UID, ROOT_PACKAGE)
        }
        if (previousMode == AppOpsManager.MODE_ALLOWED) {
            return Closeable { }
        }
        mAppOpsManagerHidden.setMode(op, ROOT_UID, ROOT_PACKAGE, AppOpsManager.MODE_ALLOWED)
        return Closeable { mAppOpsManagerHidden.setMode(op, ROOT_UID, ROOT_PACKAGE, previousMode) }
    }

    @Synchronized
    override fun acquireProvider(context: Context, name: String): IContentProvider? {
        // Telephony providers are singleUser and shared by the device.
        require(name in supportedAuthorities) { "Unsupported message provider" }
        val token = Binder()
        val provider = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val holder = mActivityManager.getContentProviderExternal(name, 0, token, "databackup-restore") ?: return null
                holder.provider
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                val holder = mActivityManager.castTo<IActivityManagerApi26>().getContentProviderExternal(name, 0, token) ?: return null
                holder.provider
            }
            else -> {
                val holder = mActivityManager.castTo<IActivityManagerApi24>().getContentProviderExternal(name, 0, token) ?: return null
                holder.provider
            }
        }
        val reference = ProviderReference(name, token)
        if (provider == null) {
            releaseProviderReference(reference)
            return null
        }
        mProviderReferences.getOrPut(provider) { ArrayDeque() }.addLast(reference)
        return provider
    }

    override fun acquireUnstableProvider(context: Context, name: String): IContentProvider? = acquireProvider(context, name)

    override fun acquireExistingProvider(context: Context, name: String): IContentProvider? = acquireProvider(context, name)

    @Synchronized
    override fun releaseProvider(provider: IContentProvider): Boolean {
        val entries = mProviderReferences[provider] ?: return false
        val reference = entries.pollLast() ?: return false
        if (entries.isEmpty()) mProviderReferences.remove(provider)
        releaseProviderReference(reference)
        return true
    }

    override fun releaseUnstableProvider(provider: IContentProvider): Boolean = releaseProvider(provider)

    override fun unstableProviderDied(provider: IContentProvider) {
        // No cached provider to invalidate. External references are released
        // through releaseProvider or releaseUnstableProvider.
    }

    private fun releaseProviderReference(providerReference: ProviderReference) {
        // This overload derives the user ID from the Binder caller; root resolves to user 0.
        mActivityManager.removeContentProviderExternal(providerReference.authority, providerReference.token)
    }

    private data class ProviderReference(val authority: String, val token: IBinder)

    private open class RootContext(base: Context) : ContextWrapperHidden(base) {
        override fun getOpPackageName(): String = ROOT_PACKAGE
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private class AttributionContext(base: Context) : RootContext(base) {
        override fun getAttributionSource(): AttributionSource = AttributionSource.Builder(Process.myUid()).setPackageName(ROOT_PACKAGE).build()
    }

    private companion object {
        val supportedAuthorities = setOf(
            Telephony.Sms.CONTENT_URI.authority,
            Telephony.Mms.CONTENT_URI.authority,
            Telephony.MmsSms.CONTENT_URI.authority,
        )

        // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:frameworks/base/services/core/java/com/android/server/AppOpsService.java;l=2280
        const val ROOT_UID = 0

        // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:frameworks/base/services/core/java/com/android/server/AppOpsService.java;l=2280
        const val ROOT_PACKAGE = "root"
    }
}
