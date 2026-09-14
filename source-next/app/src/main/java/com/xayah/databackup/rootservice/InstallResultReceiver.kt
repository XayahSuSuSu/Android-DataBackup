package com.xayah.databackup.rootservice

import android.content.Intent
import android.content.IIntentReceiver
import android.content.IIntentSender
import android.content.IntentSender
import android.content.IntentSenderHidden
import android.os.Bundle
import android.os.IBinder
import com.xayah.hiddenapi.castTo
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Receives a PackageInstaller session result through a Binder callback.
 *
 * Pass [sender] to the session's commit method, then call [await] off the main thread.
 * The callback enqueues the result without blocking; [await] blocks until a result arrives
 * and throws if none arrives within five minutes.
 *
 * The framework Stub handles Parcel decoding, so no broadcast receiver is needed.
 */
internal class InstallResultReceiver : IIntentSender.Stub() {
    private val mResults = ArrayBlockingQueue<Intent>(1)
    val sender: IntentSender = IntentSenderHidden(this).castTo()

    override fun send(
        code: Int,
        intent: Intent?,
        resolvedType: String?,
        finishedReceiver: IIntentReceiver?,
        requiredPermission: String?,
        options: Bundle?
    ) {
        intent?.let { mResults.offer(it) }
    }

    override fun send(
        code: Int,
        intent: Intent?,
        resolvedType: String?,
        whitelistToken: IBinder?,
        finishedReceiver: IIntentReceiver?,
        requiredPermission: String?,
        options: Bundle?
    ) {
        send(code, intent, resolvedType, finishedReceiver, requiredPermission, options)
    }

    fun await(): Intent = checkNotNull(mResults.poll(5, TimeUnit.MINUTES)) { "Timed out waiting for APK installation result" }
}
