package com.xayah.databackup.data

import com.xayah.databackup.entity.BackupBackendType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TaskType { Backup, Restore }

data class TaskInfo(val id: String, val type: TaskType, val backendType: BackupBackendType)

/**
 * Allows only one backup or restore task at a time.
 * Callers acquire the task before execution and release it only after execution and cleanup have finished.
 */
class TaskRepository {
    private val mLock = Any()
    private val _activeTask = MutableStateFlow<TaskInfo?>(null)
    val activeTask: StateFlow<TaskInfo?> = _activeTask.asStateFlow()

    fun acquire(task: TaskInfo) {
        synchronized(mLock) {
            check(_activeTask.value == null || _activeTask.value == task) { "A backup or restore is already running" }
            _activeTask.value = task
        }
    }

    fun release(task: TaskInfo) {
        synchronized(mLock) {
            if (_activeTask.value == task) {
                _activeTask.value = null
            }
        }
    }
}
