package com.xayah.databackup.feature.restore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R
import com.xayah.databackup.entity.BackupBackendType
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.RestoreCategory
import com.xayah.databackup.entity.restore.RestoreItemStatus
import com.xayah.databackup.entity.restore.RestoreProcessStatus
import com.xayah.databackup.entity.restore.RestoreRecordStatus
import com.xayah.databackup.ui.component.DataBackupDialog
import com.xayah.databackup.ui.component.DialogActionButton
import com.xayah.databackup.ui.component.DialogDestructiveButton
import com.xayah.databackup.ui.component.DialogDismissButton
import com.xayah.databackup.ui.component.DialogIcon
import com.xayah.databackup.ui.component.TaskProgressCard
import com.xayah.databackup.ui.component.TaskProgressCardState
import com.xayah.databackup.ui.component.TaskProgressScaffold
import com.xayah.databackup.ui.theme.DataBackupTheme

@Composable
internal fun RestoreProcessContent(
    uiState: RestoreProcessUiState,
    onCancel: () -> Unit,
    onFinish: () -> Unit,
) {
    var isCancelDialogVisible by rememberSaveable(uiState.canCancel) { mutableStateOf(false) }
    var isLeaveDialogVisible by rememberSaveable(uiState.canLeave) { mutableStateOf(false) }
    val onBack: () -> Unit = {
        when {
            uiState.isTerminal -> onFinish()
            uiState.canCancel -> isCancelDialogVisible = true
            uiState.canLeave -> isLeaveDialogVisible = true
        }
    }
    val statusLabel = when (uiState.status) {
        RestoreProcessStatus.Pending -> stringResource(R.string.waiting_to_start)
        RestoreProcessStatus.Interrupted -> stringResource(R.string.restore_interrupted)
        RestoreProcessStatus.Canceling -> stringResource(R.string.processing)
        RestoreProcessStatus.Canceled -> stringResource(R.string.canceled)
        RestoreProcessStatus.Processing -> stringResource(R.string.restoring)
        RestoreProcessStatus.Finished -> stringResource(R.string.finished)
        RestoreProcessStatus.FinishedWithErrors -> stringResource(R.string.restore_finished_with_errors)
        RestoreProcessStatus.Failed -> stringResource(R.string.failed)
    }
    TaskProgressScaffold(
        overallProgress = uiState.overallProgress,
        statusLabel = statusLabel,
        isProcessing = uiState.isProcessing,
        isCanceling = uiState.isCanceling,
        isTerminal = uiState.isTerminal,
        isBackEnabled = uiState.isBackEnabled,
        notice = when {
            uiState.isExpired -> stringResource(R.string.restore_session_expired)
            uiState.isFailed -> stringResource(R.string.restore_process_failed)
            else -> ""
        },
        onBack = onBack,
        onFinish = onFinish,
    ) {
        uiState.items.forEach { item ->
            key(item.category) {
                RestoreProcessItemCard(item = item, isTerminal = uiState.isTerminal)
            }
        }
    }
    if (isCancelDialogVisible && uiState.canCancel) {
        DataBackupDialog(
            title = stringResource(R.string.prompt),
            onDismissRequest = { isCancelDialogVisible = false },
            icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_badge_info)) },
            content = { Text(stringResource(R.string.restore_cancel_description)) },
            confirmButton = {
                DialogDestructiveButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        isCancelDialogVisible = false
                        onCancel()
                    },
                )
            },
            dismissButton = {
                DialogDismissButton(text = stringResource(R.string.cancel), onClick = { isCancelDialogVisible = false })
            },
        )
    }
    if (isLeaveDialogVisible && uiState.canLeave) {
        DataBackupDialog(
            title = stringResource(R.string.restoring),
            onDismissRequest = { isLeaveDialogVisible = false },
            icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_clock)) },
            content = { Text(stringResource(R.string.restore_background_description)) },
            confirmButton = {
                DialogActionButton(text = stringResource(R.string.confirm), onClick = {
                    isLeaveDialogVisible = false
                    onFinish()
                })
            },
            dismissButton = {
                DialogDismissButton(text = stringResource(R.string.cancel), onClick = { isLeaveDialogVisible = false })
            },
        )
    }
}

@Composable
private fun RestoreProcessItemCard(item: RestoreProcessItemUiState, isTerminal: Boolean) {
    val (titleRes, iconRes) = when (item.category) {
        RestoreCategory.Apps -> R.string.apps to R.drawable.ic_layout_grid
        RestoreCategory.Networks -> R.string.networks to R.drawable.ic_wifi
        RestoreCategory.Contacts -> R.string.contacts to R.drawable.ic_user_round
        RestoreCategory.CallLogs -> R.string.call_logs to R.drawable.ic_phone
        RestoreCategory.Messages -> R.string.messages to R.drawable.ic_message_circle
    }
    val statusLabel = stringResource(
        when (item.status) {
            RestoreItemStatus.Pending -> R.string.waiting_to_start
            RestoreItemStatus.Processing -> R.string.restoring
            RestoreItemStatus.Finished -> R.string.finished
            RestoreItemStatus.Failed -> R.string.failed
            RestoreItemStatus.Canceled -> R.string.canceled
        }
    )
    TaskProgressCard(
        category = TaskProgressCardState(
            icon = ImageVector.vectorResource(iconRes),
            title = stringResource(titleRes),
            label = "${item.completedCount}/${item.totalCount}",
            subtitle = if (item.failedCount > 0) {
                stringResource(R.string.restore_result_counts, statusLabel, item.failedCount, item.skippedCount)
            } else if (item.skippedCount > 0) {
                stringResource(R.string.restore_skipped_count, statusLabel, item.skippedCount)
            } else {
                statusLabel
            },
            progress = item.progress,
            isProcessing = item.status == RestoreItemStatus.Processing,
        ),
        isTerminal = isTerminal,
        currentApp = item.currentApp?.let { app ->
            TaskProgressCardState(
                icon = ImageVector.vectorResource(R.drawable.ic_resource_package),
                title = app.title.ifBlank { stringResource(R.string.unknown) },
                label = app.currentPart?.let { stringResource(it.restoreTitleRes) } ?: stringResource(R.string.apps),
                subtitle = stringResource(app.status.titleRes),
                progress = app.progress,
                isProcessing = app.status == RestoreRecordStatus.Processing,
            )
        },
    )
}

private val RestoreRecordStatus.titleRes: Int
    get() = when (this) {
        RestoreRecordStatus.Pending -> R.string.waiting_to_start
        RestoreRecordStatus.Processing -> R.string.restoring
        RestoreRecordStatus.Restored -> R.string.restore_result_restored
        RestoreRecordStatus.Skipped -> R.string.restore_result_skipped
        RestoreRecordStatus.Failed -> R.string.failed
        RestoreRecordStatus.Interrupted -> R.string.restore_interrupted
        RestoreRecordStatus.NotProcessed -> R.string.restore_result_not_processed
    }

private val BackupSourceCategory.restoreTitleRes: Int
    get() = when (this) {
        BackupSourceCategory.Apk -> R.string.apk
        BackupSourceCategory.InternalData -> R.string.internal_data
        BackupSourceCategory.ExternalData -> R.string.external_data
        BackupSourceCategory.AdditionalData -> R.string.additional_data
        else -> R.string.unknown
    }

@Preview(name = "Phone", widthDp = 400, heightDp = 800)
@Preview(name = "Foldable", widthDp = 673, heightDp = 841)
@Preview(name = "Tablet", widthDp = 840, heightDp = 900)
@Preview(name = "Desktop", widthDp = 1200, heightDp = 800)
@Preview(name = "Large text", widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
private fun RestoreProcessPreview() = RestoreProcessPreviewContent(
    createRestoreProcessPreviewState(BackupBackendType.Archive, RestoreProcessStatus.Processing),
)

@Preview(name = "Rustic phone", widthDp = 400, heightDp = 800)
@Preview(name = "Rustic foldable", widthDp = 673, heightDp = 841)
@Preview(name = "Rustic tablet", widthDp = 840, heightDp = 900)
@Preview(name = "Rustic desktop", widthDp = 1200, heightDp = 800)
@Preview(name = "Rustic large text", widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
private fun RusticRestoreProcessPreview() = RestoreProcessPreviewContent(
    createRestoreProcessPreviewState(BackupBackendType.Rustic, RestoreProcessStatus.Processing),
)

@Preview(name = "Restore states", widthDp = 400, heightDp = 800)
@Composable
private fun RestoreProcessStatesPreview(
    @PreviewParameter(RestoreProcessPreviewProvider::class) uiState: RestoreProcessUiState,
) = RestoreProcessPreviewContent(uiState)

internal class RestoreProcessPreviewProvider : PreviewParameterProvider<RestoreProcessUiState> {
    override val values: Sequence<RestoreProcessUiState> = BackupBackendType.entries.asSequence().flatMap { backendType ->
        RestoreProcessStatus.entries.map { status -> createRestoreProcessPreviewState(backendType, status) } +
            createRestoreProcessPreviewState(backendType, RestoreProcessStatus.Interrupted).copy(isExpired = true)
    }
}

@Composable
private fun RestoreProcessPreviewContent(uiState: RestoreProcessUiState) {
    DataBackupTheme(dynamicColor = false) {
        RestoreProcessContent(uiState = uiState, onCancel = {}, onFinish = {})
    }
}

private fun createRestoreProcessPreviewState(backendType: BackupBackendType, status: RestoreProcessStatus): RestoreProcessUiState {
    val itemStatus = when (status) {
        RestoreProcessStatus.Pending -> RestoreItemStatus.Pending
        RestoreProcessStatus.Processing, RestoreProcessStatus.Canceling -> RestoreItemStatus.Processing
        RestoreProcessStatus.Canceled -> RestoreItemStatus.Canceled
        RestoreProcessStatus.Finished -> RestoreItemStatus.Finished
        RestoreProcessStatus.Interrupted, RestoreProcessStatus.Failed, RestoreProcessStatus.FinishedWithErrors -> RestoreItemStatus.Failed
    }
    return RestoreProcessUiState(
        backendType = backendType,
        status = status,
        isExpired = false,
        overallProgress = when (status) {
            RestoreProcessStatus.Pending -> "0"
            RestoreProcessStatus.Finished -> "100"
            else -> "30"
        },
        items = RestoreCategory.entries.mapIndexed { index, category ->
            val isFinished = status == RestoreProcessStatus.Finished || (index == 0 && status != RestoreProcessStatus.Pending)
            val isStarted = index == 1 && status != RestoreProcessStatus.Pending
            RestoreProcessItemUiState(
                category = category,
                totalCount = 10,
                completedCount = when {
                    isFinished -> 10
                    isStarted -> 5
                    else -> 0
                },
                skippedCount = if (isStarted && status == RestoreProcessStatus.FinishedWithErrors) 1 else 0,
                failedCount = if (isStarted && status == RestoreProcessStatus.FinishedWithErrors) 2 else 0,
                status = when {
                    isFinished -> RestoreItemStatus.Finished
                    isStarted -> itemStatus
                    status == RestoreProcessStatus.Canceled -> RestoreItemStatus.Canceled
                    else -> RestoreItemStatus.Pending
                },
                progress = when {
                    isFinished -> 1f
                    isStarted -> 0.5f
                    else -> 0f
                },
                currentApp = null,
            )
        },
    )
}

@Preview(name = "Apps card", widthDp = 400, showBackground = true)
@Preview(name = "Apps card large text", widthDp = 360, fontScale = 1.5f, showBackground = true)
@Composable
private fun RestoreAppsCardPreview() {
    DataBackupTheme(dynamicColor = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            RestoreProcessItemCard(
                item = RestoreProcessItemUiState(
                    category = RestoreCategory.Apps,
                    totalCount = 10,
                    completedCount = 3,
                    skippedCount = 0,
                    failedCount = 0,
                    status = RestoreItemStatus.Processing,
                    progress = 0.3f,
                    currentApp = RestoreCurrentApp("Calendar", RestoreRecordStatus.Processing, BackupSourceCategory.InternalData, 0.25f),
                ),
                isTerminal = false,
            )
        }
    }
}
