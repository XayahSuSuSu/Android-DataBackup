package com.xayah.databackup.data

import com.xayah.databackup.entity.BackupBackendType
import com.xayah.databackup.entity.backup.BackupEvent
import com.xayah.databackup.entity.backup.BackupRequest
import com.xayah.databackup.entity.backup.BackupSelection
import com.xayah.databackup.entity.backup.BackupStage
import com.xayah.databackup.entity.backup.BackupState
import com.xayah.databackup.entity.backup.BackupStatus
import com.xayah.databackup.entity.backup.finish
import com.xayah.databackup.entity.backup.reduce
import com.xayah.databackup.entity.backup.toInitialState
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.backup.BackupHelper
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

class BackupProcessRepository(
    private val mArchiveHelper: BackupHelper,
    private val mRusticHelper: BackupHelper,
    private val mConfigRepo: BackupConfigRepository,
    private val mTaskRepo: TaskRepository,
    private val mStopRootService: suspend () -> Unit = { RemoteRootService.stopService() },
) {
    companion object {
        private const val TAG = "BackupProcessRepository"
    }

    private class Session(request: BackupRequest, state: BackupState) {
        var pendingRequest: BackupRequest? = request
        val state = MutableStateFlow(state)
        var job: Job? = null
        var stopCompletion: CompletableDeferred<Unit>? = null
        var isInterrupted: Boolean = false
        var hasStopFailure: Boolean = false
        val task = TaskInfo(request.id, TaskType.Backup, request.backendType)
    }

    private val mLock = Any()
    private val mSessions = mutableMapOf<String, Session>()
    private val _latestState = MutableStateFlow<BackupState?>(null)
    val latestState: StateFlow<BackupState?> = _latestState.asStateFlow()

    fun getStateOrNull(id: String): StateFlow<BackupState>? = synchronized(mLock) {
        mSessions[id]?.state?.asStateFlow()
    }

    /**
     * Navigation may be restored after process death, while in-memory tasks are lost.
     * Return a display-only interrupted state without creating a session.
     */
    fun getState(id: String): StateFlow<BackupState> = getStateOrNull(id)
        ?: MutableStateFlow(BackupState(id, backendType = BackupBackendType.Archive, status = BackupStatus.Interrupted)).asStateFlow()

    fun prepare(selection: BackupSelection): BackupState {
        val request = BackupRequest(selection)
        check(request.getConfig().path.isNotBlank()) { "Backup destination is empty" }
        val state = request.toInitialState()
        synchronized(mLock) {
            check(mSessions.values.none { it.state.value.isActive || it.job != null }) { "A backup is already running" }
            val session = Session(request, state)
            mTaskRepo.acquire(session.task)
            mSessions[request.id] = session
            _latestState.value = state
        }
        return state
    }

    fun canExecute(id: String): Boolean = synchronized(mLock) {
        mSessions[id]?.let { it.pendingRequest != null && it.state.value.status == BackupStatus.Pending } == true
    }

    fun failToStart(id: String) {
        synchronized(mLock) {
            val session = mSessions[id] ?: return
            if (session.job != null || session.state.value.status != BackupStatus.Pending) {
                return
            }
            session.pendingRequest = null
            updateState(id) { it.finish(BackupStatus.Failed) }
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
            val pending = checkNotNull(session.pendingRequest)
            session.pendingRequest = null
            session.job = executionJob
            executionJob.invokeOnCompletion {
                synchronized(mLock) {
                    session.job = null
                    if (!session.hasStopFailure) {
                        mTaskRepo.release(session.task)
                    }
                }
            }
            updateState(id) { it.copy(status = BackupStatus.Running) }
            pending
        }
        var terminalStatus = BackupStatus.Failed
        try {
            val helper = when (request.backendType) {
                BackupBackendType.Archive -> mArchiveHelper
                BackupBackendType.Rustic -> mRusticHelper
            }
            val result = helper.backup(request) { event ->
                if (executionJob.isActive) {
                    updateState(id) { it.reduce(event) }
                }
            }
            currentCoroutineContext().ensureActive()
            updateState(id) { it.copy(result = result).reduce(BackupEvent.StageChanged(BackupStage.FinalizeBackup)) }
            mConfigRepo.setupBackupConfig(request.getConfig())
            currentCoroutineContext().ensureActive()
            terminalStatus = if (result.hasFailures) BackupStatus.PartialFailure else BackupStatus.Completed
        } catch (error: CancellationException) {
            terminalStatus = BackupStatus.Canceled
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
            LogHelper.w(TAG, "execute", "Backup failed: error=${error.javaClass.simpleName}")
        } finally {
            withContext(NonCancellable) {
                val stopCompletion = synchronized(mLock) { mSessions[id]?.stopCompletion }
                stopCompletion?.await()
                synchronized(mLock) {
                    val session = checkNotNull(mSessions[id])
                    val status = when {
                        session.isInterrupted || session.hasStopFailure -> BackupStatus.Interrupted
                        session.stopCompletion != null -> BackupStatus.Canceled
                        else -> terminalStatus
                    }
                    updateState(id) { it.finish(status) }
                }
            }
        }
    }

    /** Stop is scoped to this request. Completion waits for root termination and backend cleanup before accepting another request. */
    suspend fun cancel(id: String, isInterrupted: Boolean = false) {
        val job: Job?
        val stopCompletion: CompletableDeferred<Unit>
        synchronized(mLock) {
            val session = mSessions[id] ?: return
            if (!session.state.value.isActive || (session.state.value.backendType == BackupBackendType.Rustic && !isInterrupted)) {
                return
            }
            if (session.stopCompletion != null) {
                session.isInterrupted = session.isInterrupted || isInterrupted
                return
            }
            session.isInterrupted = isInterrupted
            stopCompletion = CompletableDeferred()
            session.stopCompletion = stopCompletion
            session.pendingRequest = null
            job = session.job
            updateState(id) { it.copy(status = BackupStatus.Canceling) }
            job?.cancel(CancellationException("Backup stop requested"))
        }
        withContext(NonCancellable + Dispatchers.IO) {
            try {
                if (job != null) {
                    mStopRootService()
                } else {
                    synchronized(mLock) {
                        val session = checkNotNull(mSessions[id])
                        updateState(id) { it.finish(if (session.isInterrupted) BackupStatus.Interrupted else BackupStatus.Canceled) }
                        mTaskRepo.release(session.task)
                    }
                }
            } catch (error: Exception) {
                synchronized(mLock) { mSessions[id]?.hasStopFailure = true }
                throw error
            } finally {
                stopCompletion.complete(Unit)
            }
        }
    }

    private fun updateState(id: String, transform: (BackupState) -> BackupState) {
        synchronized(mLock) {
            val session = mSessions[id] ?: return
            session.state.value = transform(session.state.value)
            if (_latestState.value?.id == id) {
                _latestState.value = session.state.value
            }
        }
    }
}
