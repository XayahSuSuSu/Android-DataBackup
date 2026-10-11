package com.xayah.databackup.entity.backup

import arrow.optics.copy
import arrow.optics.dsl.index
import arrow.optics.optics
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupBackendType

@optics
data class BackupPartProgress(
    val category: BackupSourceCategory,
    val index: Int,
    val outcome: BackupOutcome = BackupOutcome.Pending,
    val bytes: Long = 0,
    val speed: Long = 0,
    val exitCode: Int = 0,
    val diagnostics: String = "",
) {
    companion object
}

@optics
data class BackupAppProgress(
    val packageName: String,
    val userId: Int,
    val label: String,
    val parts: List<BackupPartProgress>,
    val outcome: BackupOutcome = BackupOutcome.Pending,
) {
    companion object

    val key: String get() = "$packageName-$userId"
}

@optics
data class BackupCategoryProgress(
    val category: BackupCategory,
    val isSelected: Boolean,
    val totalCount: Int,
    val completedCount: Int = 0,
    val outcome: BackupOutcome = BackupOutcome.Pending,
    val currentLabel: String = "",
    val availableCount: Int = totalCount,
) {
    companion object
}

@optics
data class BackupState(
    val id: String,
    val backendType: BackupBackendType,
    val backupName: String = "",
    val backupPath: String = "",
    val isPasswordProtected: Boolean = false,
    val status: BackupStatus = BackupStatus.Pending,
    val stage: BackupStage = BackupStage.PrepareDestination,
    val categoryProgress: List<BackupCategoryProgress> = emptyList(),
    val appsProgress: List<BackupAppProgress> = emptyList(),
    val bytes: Long = 0,
    val speed: Long = 0,
    val progress: Float = 0f,
    val result: BackupResult = BackupResult(),
) {
    companion object

    val isActive: Boolean get() = status in listOf(BackupStatus.Pending, BackupStatus.Running, BackupStatus.Canceling)
}

fun BackupRequest.toInitialState(): BackupState {
    val selection = getSelection()
    val rusticBackend = selection.config.backupBackend as? BackupBackend.Rustic
    return BackupState(
        id = id,
        backendType = backendType,
        backupName = selection.config.name,
        backupPath = selection.config.path,
        isPasswordProtected = rusticBackend != null && rusticBackend.password.isNotBlank() && rusticBackend.password != BackupBackend.DEFAULT_PASSWORD,
        categoryProgress = listOf(
            BackupCategoryProgress(BackupCategory.Apps, selection.apps != null, selection.apps.orEmpty().size),
            BackupCategoryProgress(BackupCategory.Files, selection.files != null, selection.files.orEmpty().size),
            BackupCategoryProgress(BackupCategory.Networks, selection.networks != null, selection.networks.orEmpty().size),
            BackupCategoryProgress(BackupCategory.Contacts, selection.contacts != null, selection.contacts.orEmpty().size),
            BackupCategoryProgress(BackupCategory.CallLogs, selection.callLogs != null, selection.callLogs.orEmpty().size),
            BackupCategoryProgress(
                category = BackupCategory.Messages,
                isSelected = selection.sms != null || selection.mms != null,
                totalCount = selection.sms.orEmpty().size + selection.mms.orEmpty().size,
            ),
        ).map { it.copy(availableCount = selection.totalCounts[it.category] ?: it.totalCount) },
        appsProgress = selection.apps.orEmpty().map { app ->
            BackupAppProgress(
                packageName = app.packageName,
                userId = app.userId,
                label = app.info.label,
                parts = buildList {
                    if (app.option.apk) {
                        add(BackupPartProgress(BackupSourceCategory.Apk, 0))
                    }
                    if (app.option.internalData) {
                        add(BackupPartProgress(BackupSourceCategory.InternalData, 0))
                        add(BackupPartProgress(BackupSourceCategory.InternalData, 1))
                    }
                    if (app.option.externalData) {
                        add(BackupPartProgress(BackupSourceCategory.ExternalData, 0))
                    }
                    if (app.option.additionalData) {
                        add(BackupPartProgress(BackupSourceCategory.AdditionalData, 0))
                        add(BackupPartProgress(BackupSourceCategory.AdditionalData, 1))
                    }
                })
        },
    )
}

fun BackupState.reduce(event: BackupEvent): BackupState {
    if (isActive.not()) {
        return this
    }
    return when (event) {
        is BackupEvent.StageChanged -> copy(stage = event.stage)
        is BackupEvent.Progress -> copy(
            bytes = event.bytes.coerceAtLeast(0),
            speed = event.speed.coerceAtLeast(0),
            progress = if (event.progress.isFinite()) {
                event.progress.coerceIn(0f, 1f)
            } else {
                progress
            }
        )
        is BackupEvent.SnapshotCreated -> copy(result = result.copy(snapshotId = event.id))
        is BackupEvent.CategoryStarted -> copy(categoryProgress = categoryProgress.map {
            if (it.category == event.category) {
                it.copy(outcome = BackupOutcome.Running)
            } else {
                it
            }
        })
        is BackupEvent.CategoryCompleted -> copy(categoryProgress = categoryProgress.map {
            if (it.category == event.category) {
                it.copy(
                    outcome = event.outcome,
                    completedCount = if (event.outcome == BackupOutcome.Completed) {
                        it.totalCount
                    } else {
                        it.completedCount
                    }
                )
            } else {
                it
            }
        })
        is BackupEvent.AppStarted -> copy(
            appsProgress = appsProgress.map {
                if (it.key == event.key) {
                    it.copy(outcome = BackupOutcome.Running)
                } else {
                    it
                }
            },
            categoryProgress = categoryProgress.map {
                if (it.category == BackupCategory.Apps) {
                    it.copy(currentLabel = appsProgress.first { app -> app.key == event.key }.label)
                } else {
                    it
                }
            },
        )
        is BackupEvent.PartChanged -> {
            val appIndex = appsProgress.indexOfFirst { it.key == event.key }
            check(appIndex >= 0) { "Backend reported an unknown app" }
            val app = appsProgress[appIndex]
            val partIndex = app.parts.indexOfFirst { it.category == event.category && it.index == event.index }
            check(partIndex >= 0) { "Backend reported an unselected app part" }
            val part = app.parts[partIndex]
            copy {
                inside(BackupState.appsProgress.index(appIndex).parts.index(partIndex)) {
                    BackupPartProgress.outcome set event.outcome
                    BackupPartProgress.bytes set maxOf(part.bytes, event.bytes)
                    BackupPartProgress.speed set event.speed.coerceAtLeast(0)
                    BackupPartProgress.exitCode set event.exitCode
                    BackupPartProgress.diagnostics set event.diagnostics
                }
            }
        }
        is BackupEvent.AppCompleted -> copy(
            appsProgress = appsProgress.map { app ->
                if (app.key == event.key) {
                    app.copy(outcome = when {
                        app.parts.any { it.outcome == BackupOutcome.Failed } -> BackupOutcome.Failed
                        app.parts.all { it.outcome == BackupOutcome.Skipped } -> BackupOutcome.Skipped
                        app.parts.all { it.outcome in listOf(BackupOutcome.Completed, BackupOutcome.Skipped) } -> BackupOutcome.Completed
                        else -> BackupOutcome.Unprocessed
                    })
                } else {
                    app
                }
            },
            categoryProgress = categoryProgress.map {
                if (it.category == BackupCategory.Apps) {
                    it.copy(completedCount = (it.completedCount + 1).coerceAtMost(it.totalCount))
                } else {
                    it
                }
            },
        )
    }
}

fun BackupState.finish(status: BackupStatus): BackupState = copy(
    status = status,
    progress = if (status == BackupStatus.Completed) {
        1f
    } else {
        progress
    },
    appsProgress = appsProgress.map { app ->
        app.copy(
            outcome = app.outcome.toTerminalOutcome(status),
            parts = app.parts.map { it.copy(outcome = it.outcome.toTerminalOutcome(status)) }
        )
    },
    categoryProgress = categoryProgress.map { it.copy(outcome = it.outcome.toTerminalOutcome(status)) },
)

private fun BackupOutcome.toTerminalOutcome(status: BackupStatus): BackupOutcome = when (this) {
    BackupOutcome.Pending -> BackupOutcome.Unprocessed
    BackupOutcome.Running -> if (status == BackupStatus.Canceled) {
        BackupOutcome.Canceled
    } else {
        BackupOutcome.Failed
    }
    else -> this
}
