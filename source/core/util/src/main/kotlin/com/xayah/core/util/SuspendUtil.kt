package com.xayah.core.util

import android.content.Context
import com.xayah.core.util.command.BaseUtil
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Tracks the packages that were paused (force-stopped and suspended) by the running task.
 *
 * A suspended package stays unusable until it is unsuspended, so the list is persisted to the app's
 * private storage: a task that is killed before it can resume its packages leaves a record behind and
 * [resumeAll] lifts every leftover suspension the next time a task starts.
 *
 * Every entry point is non-cancellable and never throws: pausing an app is a best-effort helper of the
 * task, a failure in here must not change the outcome of the backup or of the restore it belongs to.
 */
object SuspendUtil {
    private const val PendingFileName = "suspended_packages.txt"

    private fun pendingFile(context: Context): File = File(context.filesDir(), PendingFileName)

    private fun entryOf(userId: Int, packageName: String): String = "$userId:$packageName"

    private fun readEntries(context: Context): List<String> = runCatching {
        pendingFile(context).takeIf { it.isFile }?.readLines().orEmpty()
    }.getOrDefault(emptyList()).map { it.trim() }.filter { it.isNotEmpty() }

    private fun writeEntries(context: Context, entries: List<String>) {
        runCatching {
            val file = pendingFile(context)
            if (entries.isEmpty()) {
                file.delete()
            } else {
                file.writeText(entries.distinct().joinToString(separator = "\n"))
            }
        }
    }

    private fun parseEntry(entry: String): Pair<Int, String>? {
        val separator = entry.indexOf(':')
        if (separator <= 0 || separator == entry.lastIndex) {
            return null
        }
        return entry.substring(0, separator).toIntOrNull()?.let { userId -> userId to entry.substring(separator + 1) }
    }

    private suspend fun <T> nonCancellable(default: T, block: suspend () -> T): T =
        withContext(Dispatchers.IO + NonCancellable) {
            runCatching { block() }.getOrElse { default }
        }

    /**
     * Force-stops and suspends [packageName], and remembers it until [resumePackage] or [resumeAll] runs.
     *
     * Safe to call repeatedly: an already suspended package is force-stopped and suspended again, which is
     * exactly what is needed when the user (or the system) managed to start it in the meantime.
     *
     * @return false when the package was skipped or the suspension failed.
     */
    suspend fun suspendPackage(context: Context, userId: Int, packageName: String): Boolean = nonCancellable(false) {
        if (packageName == context.packageName || packageName == "android") {
            // Suspending the running task or the framework itself could not be undone in time.
            false
        } else {
            // Recorded before suspending: an interruption in between only causes a harmless unsuspend.
            writeEntries(context, readEntries(context) + entryOf(userId, packageName))
            BaseUtil.suspendPackage(context = context, userId = userId, packageName = packageName)
        }
    }

    /**
     * Lifts the suspension of [packageName] and drops its record.
     */
    suspend fun resumePackage(context: Context, userId: Int, packageName: String) = nonCancellable(Unit) {
        val entry = entryOf(userId, packageName)
        // Always unsuspend: the record may be missing after an update while the package is still paused.
        BaseUtil.unsuspendPackage(context = context, userId = userId, packageName = packageName)
        writeEntries(context, readEntries(context).filter { it != entry })
    }

    /**
     * Lifts every suspension recorded by a previous task.
     */
    suspend fun resumeAll(context: Context) = nonCancellable(Unit) {
        readEntries(context).mapNotNull { parseEntry(it) }.forEach { (userId, packageName) ->
            BaseUtil.unsuspendPackage(context = context, userId = userId, packageName = packageName)
        }
        writeEntries(context, emptyList())
    }
}
