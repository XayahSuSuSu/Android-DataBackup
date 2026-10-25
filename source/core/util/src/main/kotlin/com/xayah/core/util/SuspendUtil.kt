package com.xayah.core.util

import android.content.Context
import com.xayah.core.util.command.BaseUtil
import java.io.File

/**
 * Tracks the packages that were paused (force-stopped and suspended) by the running task.
 *
 * A suspended package stays unusable until it is unsuspended, so the list is persisted to the app's
 * private storage: a task that is killed before it can resume its packages leaves a record behind and
 * [resumeAll] lifts every leftover suspension the next time a task starts.
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

    /**
     * Force-stops and suspends [packageName], and remembers it until [resumePackage] or [resumeAll] runs.
     *
     * @return false when the package was skipped.
     */
    suspend fun suspendPackage(context: Context, userId: Int, packageName: String): Boolean = withIOContext {
        if (packageName == context.packageName || packageName == "android") {
            // Suspending the running task or the framework itself could not be undone in time.
            false
        } else {
            // Recorded before suspending: an interruption in between only causes a harmless unsuspend.
            writeEntries(context, readEntries(context) + entryOf(userId, packageName))
            BaseUtil.suspendPackage(context = context, userId = userId, packageName = packageName)
            true
        }
    }

    /**
     * Lifts the suspension of [packageName] and drops its record.
     */
    suspend fun resumePackage(context: Context, userId: Int, packageName: String) = withIOContext {
        val entry = entryOf(userId, packageName)
        val entries = readEntries(context)
        if (entries.contains(entry)) {
            BaseUtil.unsuspendPackage(context = context, userId = userId, packageName = packageName)
            writeEntries(context, entries.filter { it != entry })
        }
    }

    /**
     * Lifts every suspension recorded by a previous task.
     */
    suspend fun resumeAll(context: Context) = withIOContext {
        val entries = readEntries(context)
        entries.mapNotNull { parseEntry(it) }.forEach { (userId, packageName) ->
            BaseUtil.unsuspendPackage(context = context, userId = userId, packageName = packageName)
        }
        writeEntries(context, emptyList())
    }
}
