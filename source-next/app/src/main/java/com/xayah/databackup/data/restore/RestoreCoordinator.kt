package com.xayah.databackup.data.restore

import com.xayah.databackup.data.rustic.RusticSourceCategory
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal sealed interface RestoreEvent {
    val task: RestoreTask

    data class Started(override val task: RestoreTask) : RestoreEvent
    data class Completed(override val task: RestoreTask, val skipped: Set<String>) : RestoreEvent
    data class Failed(override val task: RestoreTask) : RestoreEvent
    data class RecordFailed(override val task: RestoreTask.Records, val id: String) : RestoreEvent
    data class RecordStarted(override val task: RestoreTask.Records, val id: String) : RestoreEvent
    data class RecordCompleted(override val task: RestoreTask.Records, val id: String, val skipped: Boolean) : RestoreEvent
    data class AppPartStarted(override val task: RestoreTask.App, val parts: Set<RusticSourceCategory>) : RestoreEvent
    data class AppPartCompleted(override val task: RestoreTask.App, val parts: Set<RusticSourceCategory>) : RestoreEvent
}

/**
 * Coordinates restore task execution and reports progress and results.
 */
internal class RestoreCoordinator(private val mGateway: RestoreGateway) {
    companion object {
        private const val TAG = "RestoreCoordinator"
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
    ): Boolean = mMutex.withLock {
        for (task in request.tasks) {
            currentCoroutineContext().ensureActive()
            if (isCanceled()) {
                return@withLock false
            }
            onEvent(RestoreEvent.Started(task))
            try {
                val pending = task.ids.toMutableSet()
                val started = mutableSetOf<String>()
                val completed = mutableMapOf<String, Boolean>()
                val failed = mutableSetOf<String>()
                val pendingParts = (task as? RestoreTask.App)?.source?.paths.orEmpty().map { it.category }.toMutableSet()
                val startedParts = mutableSetOf<RusticSourceCategory>()
                val skipped = mGateway.restore(request, task) { event ->
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
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
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
