package com.xayah.databackup.data

import com.xayah.databackup.entity.BackupBackendType
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.RestoreEvent
import com.xayah.databackup.entity.restore.RestoreItemStatus
import com.xayah.databackup.entity.restore.RestoreProcessState
import com.xayah.databackup.entity.restore.RestoreProcessStatus
import com.xayah.databackup.entity.restore.RestoreRequest
import com.xayah.databackup.entity.restore.RestoreSource
import com.xayah.databackup.entity.restore.RestoreTask
import com.xayah.databackup.entity.restore.finish
import com.xayah.databackup.entity.restore.reduce
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.restore.RestoreHelper
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import java.io.Closeable

internal class RestoreProcessRepository(
    private val mRusticHelper: RestoreHelper,
    private val mArchiveHelper: RestoreHelper,
    private val mTaskRepo: TaskRepository,
    private val mOpenApkInstallSession: suspend () -> Closeable = { RemoteRootService.openApkInstallSession() },
    private val mStopRootService: suspend () -> Unit = { RemoteRootService.stopService() },
) {
    companion object {
        private const val TAG = "RestoreProcessRepository"
    }

    private class Session(request: RestoreRequest, initialState: RestoreProcessState) {
        var pendingRequest: RestoreRequest? = request
        val state = MutableStateFlow(initialState)
        val task = TaskInfo(request.id, TaskType.Restore, request.backendType)
        var job: Job? = null
        var stopCompletion: CompletableDeferred<Unit>? = null
        var isInterrupted = false
        var hasStopFailure = false
    }

    private val mLock = Any()
    private val mSessions = mutableMapOf<String, Session>()

    fun getStateOrNull(id: String): StateFlow<RestoreProcessState>? = synchronized(mLock) {
        mSessions[id]?.state?.asStateFlow()
    }

    fun getState(id: String): StateFlow<RestoreProcessState> = getStateOrNull(id)
        ?: MutableStateFlow(RestoreProcessState(
            id = id,
            backendType = BackupBackendType.Archive,
            status = RestoreProcessStatus.Interrupted,
            isExpired = true,
        )).asStateFlow()

    fun prepare(request: RestoreRequest, state: RestoreProcessState): RestoreProcessState {
        require(state.id == request.id && state.backendType == request.backendType && state.status == RestoreProcessStatus.Pending) {
            "Restore state does not match the prepared request"
        }
        synchronized(mLock) {
            check(request.id !in mSessions) { "Restore request was already submitted" }
            val session = Session(request, state)
            mTaskRepo.acquire(session.task)
            mSessions[request.id] = session
        }
        return state
    }

    fun canExecute(id: String): Boolean = synchronized(mLock) {
        mSessions[id]?.let { it.pendingRequest != null && it.state.value.status == RestoreProcessStatus.Pending } == true
    }

    fun failToStart(id: String) {
        synchronized(mLock) {
            val session = mSessions[id] ?: return
            if (!canExecute(id) || session.job != null) {
                return
            }
            session.pendingRequest = null
            session.state.value = session.state.value.finish(RestoreProcessStatus.Failed)
            mTaskRepo.release(session.task)
        }
    }

    suspend fun execute(id: String) {
        val executionJob = currentCoroutineContext().job
        val request = synchronized(mLock) {
            val session = mSessions[id] ?: return
            if (!canExecute(id)) {
                return
            }
            val pendingRequest = checkNotNull(session.pendingRequest)
            session.pendingRequest = null
            session.job = executionJob
            session.state.value = session.state.value.copy(status = RestoreProcessStatus.Processing)
            executionJob.invokeOnCompletion {
                synchronized(mLock) {
                    session.job = null
                    if (!session.hasStopFailure) {
                        mTaskRepo.release(session.task)
                    }
                }
            }
            pendingRequest
        }
        val onEvent: (RestoreEvent) -> Unit = { event ->
            synchronized(mLock) {
                val session = checkNotNull(mSessions[id])
                if (executionJob.isActive && session.state.value.status == RestoreProcessStatus.Processing) {
                    session.state.value = session.state.value.reduce(event)
                }
            }
        }
        var terminalStatus = RestoreProcessStatus.Failed
        var apkInstallSession: Closeable? = null
        try {
            if (request.tasks.any { task ->
                task is RestoreTask.App && task.source.paths.any { it.category == BackupSourceCategory.Apk }
            }) {
                // Retain the handle even if cancellation arrives while the remote session is opening.
                withContext(NonCancellable) {
                    apkInstallSession = mOpenApkInstallSession()
                }
                currentCoroutineContext().ensureActive()
            }
            val helper: RestoreHelper = when (request.source) {
                is RestoreSource.Archive -> mArchiveHelper
                is RestoreSource.Rustic -> mRusticHelper
            }
            for (task in request.tasks) {
                currentCoroutineContext().ensureActive()
                onEvent(RestoreEvent.Started(task))
                runCatching {
                    val pendingIds = task.ids.toMutableSet()
                    val startedIds = mutableSetOf<String>()
                    val skippedStatusByRecordId = mutableMapOf<String, Boolean>()
                    val failedIds = mutableSetOf<String>()
                    val pendingParts = (task as? RestoreTask.App)?.source?.paths.orEmpty().map { it.category }.toMutableSet()
                    val startedParts = mutableSetOf<BackupSourceCategory>()
                    val recordEventLock = Any()
                    var isAcceptingEvents = true
                    val skippedIds = try {
                        helper.restore(request, task) { event ->
                            synchronized(recordEventLock) {
                                if (!isAcceptingEvents || !executionJob.isActive) {
                                    return@synchronized
                                }
                                check(event.task == task) { "Backend reported a different restore task" }
                                when (event) {
                                    is RestoreEvent.RecordStarted -> {
                                        check(pendingIds.remove(event.id)) { "Backend started an unknown or repeated restore record" }
                                        startedIds.add(event.id)
                                    }

                                    is RestoreEvent.RecordCompleted -> {
                                        check(startedIds.remove(event.id)) { "Backend completed a record that was not started" }
                                        skippedStatusByRecordId[event.id] = event.isSkipped
                                    }

                                    is RestoreEvent.RecordFailed -> {
                                        check(startedIds.remove(event.id)) { "Backend failed a record that was not started" }
                                        failedIds.add(event.id)
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
                            }
                        }.toSet()
                    } finally {
                        synchronized(recordEventLock) { isAcceptingEvents = false }
                    }
                    currentCoroutineContext().ensureActive()
                    check(task.ids.containsAll(skippedIds)) { "Backend returned unknown restore records" }
                    if (task is RestoreTask.Records) {
                        check((skippedStatusByRecordId.keys + failedIds) == task.ids.toSet() && skippedStatusByRecordId.filterValues { it }.keys == skippedIds) {
                            "Backend returned incomplete restore results"
                        }
                    } else {
                        check(pendingParts.isEmpty() && startedParts.isEmpty() && skippedIds.isEmpty()) { "Incomplete app restore results" }
                    }
                    onEvent(RestoreEvent.Completed(task, skippedIds))
                }.onFailure { error ->
                    currentCoroutineContext().ensureActive()
                    if (error is CancellationException || error !is Exception) {
                        throw error
                    }
                    LogHelper.w(TAG, "execute", "Restore task failed: category=${task.category}, error=${error.javaClass.simpleName}")
                    onEvent(RestoreEvent.Failed(task))
                }
            }
            currentCoroutineContext().ensureActive()
            terminalStatus = if (getState(id).value.items.any { it.status == RestoreItemStatus.Failed }) {
                RestoreProcessStatus.FinishedWithErrors
            } else {
                RestoreProcessStatus.Finished
            }
        } catch (error: CancellationException) {
            terminalStatus = RestoreProcessStatus.Canceled
            // Unexpected scope cancellation must also stop any blocking Root operation.
            withContext(NonCancellable) {
                val hasStopRequest = synchronized(mLock) { mSessions[id]?.stopCompletion != null }
                if (!hasStopRequest) {
                    runCatching { cancel(id, isInterrupted = true) }.onFailure {
                        LogHelper.w(TAG, "execute", "Cannot confirm Root termination")
                    }
                }
            }
            throw error
        } catch (error: Exception) {
            LogHelper.w(TAG, "execute", "Restore failed: error=${error.javaClass.simpleName}")
        } finally {
            withContext(NonCancellable) {
                val stopCompletion = synchronized(mLock) { mSessions[id]?.stopCompletion }
                stopCompletion?.await()
                withContext(Dispatchers.IO) {
                    runCatching { apkInstallSession?.close() }.onFailure {
                        terminalStatus = RestoreProcessStatus.Failed
                        LogHelper.w(TAG, "execute", "Cannot close APK install session")
                        runCatching { mStopRootService() }.onFailure {
                            synchronized(mLock) { mSessions[id]?.hasStopFailure = true }
                        }
                    }
                }
                synchronized(mLock) {
                    val session = checkNotNull(mSessions[id])
                    val status = when {
                        session.isInterrupted || session.hasStopFailure -> RestoreProcessStatus.Interrupted
                        session.stopCompletion != null -> RestoreProcessStatus.Canceled
                        else -> terminalStatus
                    }
                    session.state.value = session.state.value.finish(status)
                }
            }
        }
    }

    suspend fun cancel(id: String, isInterrupted: Boolean = false) {
        val job: Job?
        val stopCompletion: CompletableDeferred<Unit>
        val session: Session
        synchronized(mLock) {
            session = mSessions[id] ?: return
            if (!session.state.value.isActive || (session.task.backendType == BackupBackendType.Rustic && !isInterrupted)) {
                return
            }
            session.isInterrupted = session.isInterrupted || isInterrupted
            if (session.stopCompletion != null) {
                return
            }
            stopCompletion = CompletableDeferred()
            session.stopCompletion = stopCompletion
            session.pendingRequest = null
            job = session.job
            session.state.value = session.state.value.copy(status = RestoreProcessStatus.Canceling)
            job?.cancel(CancellationException("Restore stop requested"))
        }
        withContext(NonCancellable + Dispatchers.IO) {
            try {
                if (job != null) {
                    mStopRootService()
                } else {
                    synchronized(mLock) {
                        val status = if (session.isInterrupted) RestoreProcessStatus.Interrupted else RestoreProcessStatus.Canceled
                        session.state.value = session.state.value.finish(status)
                        mTaskRepo.release(session.task)
                    }
                }
            } catch (error: Exception) {
                synchronized(mLock) { session.hasStopFailure = true }
                throw error
            } finally {
                stopCompletion.complete(Unit)
            }
        }
    }
}
