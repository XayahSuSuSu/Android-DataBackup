package com.xayah.databackup.data

import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.RestoreEvent
import com.xayah.databackup.entity.restore.RestoreRequest
import com.xayah.databackup.entity.restore.RestoreTask
import com.xayah.databackup.service.restore.RestoreHelper
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class RestoreProcessRepository(private val mRestoreHelper: RestoreHelper) {
    companion object {
        private const val TAG = "RestoreProcessRepository"
    }

    private val mMutex = Mutex()

    /**
     * Returns true when all tasks have been attempted, or false when isCanceled requests cancellation before a task starts.
     * Coroutine cancellation propagates as an exception.
     */
    suspend fun restore(
        request: RestoreRequest,
        isCanceled: () -> Boolean,
        onEvent: (RestoreEvent) -> Unit,
    ): Boolean {
        return mMutex.withLock {
            for (task in request.tasks) {
                currentCoroutineContext().ensureActive()
                if (isCanceled()) {
                    return@withLock false
                }
                onEvent(RestoreEvent.Started(task))
                runCatching {
                    val pending = task.ids.toMutableSet()
                    val started = mutableSetOf<String>()
                    val completed = mutableMapOf<String, Boolean>()
                    val failed = mutableSetOf<String>()
                    val pendingParts = (task as? RestoreTask.App)?.source?.paths.orEmpty().map { it.category }.toMutableSet()
                    val startedParts = mutableSetOf<BackupSourceCategory>()
                    val skipped = mRestoreHelper.restore(request, task) { event ->
                        check(event.task == task) { "Backend reported a different restore task" }
                        when (event) {
                            is RestoreEvent.RecordStarted -> {
                                check(pending.remove(event.id)) { "Backend started an unknown or repeated restore record" }
                                started.add(event.id)
                            }

                            is RestoreEvent.RecordCompleted -> {
                                check(started.remove(event.id)) { "Backend completed a record that was not started" }
                                completed[event.id] = event.skipped
                            }

                            is RestoreEvent.RecordFailed -> {
                                check(started.remove(event.id)) { "Backend failed a record that was not started" }
                                failed.add(event.id)
                            }

                            is RestoreEvent.AppPartStarted -> {
                                check(event.parts.isNotEmpty() && pendingParts.containsAll(event.parts)) { "Invalid app parts started" }
                                pendingParts.removeAll(event.parts)
                                startedParts.addAll(event.parts)
                            }

                            is RestoreEvent.AppPartCompleted -> {
                                check(event.parts.isNotEmpty() && startedParts.containsAll(event.parts)) { "Invalid app parts completed" }
                                startedParts.removeAll(event.parts)
                            }

                            else -> error("Unexpected backend restore event")
                        }
                        onEvent(event)
                    }.toSet()
                    check(task.ids.containsAll(skipped)) { "Backend returned unknown restore records" }
                    if (task is RestoreTask.Records) {
                        check((completed.keys + failed) == task.ids.toSet() && completed.filterValues { it }.keys == skipped) {
                            "Backend returned incomplete restore results"
                        }
                    } else {
                        check(pendingParts.isEmpty() && startedParts.isEmpty() && skipped.isEmpty()) { "Incomplete app restore results" }
                    }
                    onEvent(RestoreEvent.Completed(task, skipped))
                }.onFailure { error ->
                    if (error is CancellationException || error !is Exception) throw error
                    val msg = when (task) {
                        is RestoreTask.App -> "App restore failed: id=${task.id}"
                        is RestoreTask.Records -> "Batch restore failed: category=${task.category}, records=${task.ids.size}"
                    }
                    LogHelper.e(TAG, "restore", msg, error)
                    onEvent(RestoreEvent.Failed(task))
                }
            }
            true
        }
    }
}
