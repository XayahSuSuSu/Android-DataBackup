package com.xayah.databackup.feature.backup

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.xayah.databackup.R
import com.xayah.databackup.entity.BackupBackendType
import com.xayah.databackup.entity.backup.BackupCategory
import com.xayah.databackup.entity.backup.BackupOutcome
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.backup.BackupStatus
import com.xayah.databackup.ui.component.DataBackupDialog
import com.xayah.databackup.ui.component.DialogActionButton
import com.xayah.databackup.ui.component.DialogDestructiveButton
import com.xayah.databackup.ui.component.DialogDismissButton
import com.xayah.databackup.ui.component.DialogIcon
import com.xayah.databackup.ui.component.ProcessItemCard
import com.xayah.databackup.ui.component.ProcessItemCardState
import com.xayah.databackup.ui.component.ProcessScaffold
import com.xayah.databackup.ui.theme.DataBackupTheme

@Composable
internal fun BackupProcessContent(
    uiState: BackupProcessUiState,
    onCancel: () -> Unit,
    onFinish: () -> Unit,
) {
    var isCancelDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isProcessingNoticeVisible by rememberSaveable { mutableStateOf(false) }
    val statusLabel = stringResource(
        when (uiState.status) {
            BackupStatus.Pending -> R.string.waiting_to_start
            BackupStatus.Running -> R.string.backing_up
            BackupStatus.Canceling -> R.string.processing
            BackupStatus.Completed -> R.string.finished
            BackupStatus.PartialFailure -> R.string.backup_partial_failure
            BackupStatus.Failed -> R.string.failed
            BackupStatus.Canceled -> R.string.canceled
            BackupStatus.Interrupted -> R.string.backup_interrupted
        }
    )
    val notice = when (uiState.status) {
        BackupStatus.Failed, BackupStatus.PartialFailure, BackupStatus.Interrupted -> statusLabel
        else -> ""
    }
    ProcessScaffold(
        overallProgress = uiState.overallProgress,
        statusLabel = statusLabel,
        isProcessing = uiState.isProcessing,
        isCanceling = uiState.isCanceling,
        isTerminal = uiState.isTerminal,
        isBackEnabled = uiState.isBackEnabled,
        notice = notice,
        onBack = {
            when {
                uiState.isTerminal -> onFinish()
                uiState.canCancel -> isCancelDialogVisible = true
                uiState.backendType == BackupBackendType.Rustic && uiState.isProcessing -> isProcessingNoticeVisible = true
            }
        },
        onFinish = onFinish,
    ) {
        uiState.items.forEach { item ->
            key(item.category) {
                BackupProcessItemCard(item, uiState.backendType, uiState.isTerminal)
            }
        }
    }
    if (isCancelDialogVisible && uiState.canCancel) {
        DataBackupDialog(
            title = stringResource(R.string.prompt),
            onDismissRequest = { isCancelDialogVisible = false },
            icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_badge_info)) },
            content = { Text(stringResource(R.string.prompt_cancel_operation)) },
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
    if (isProcessingNoticeVisible && uiState.backendType == BackupBackendType.Rustic && uiState.isProcessing) {
        DataBackupDialog(
            title = stringResource(R.string.backup_in_progress),
            onDismissRequest = { isProcessingNoticeVisible = false },
            icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_clock)) },
            content = { Text(stringResource(R.string.rustic_backup_leave_blocked)) },
            confirmButton = {
                DialogActionButton(text = stringResource(R.string.confirm), onClick = { isProcessingNoticeVisible = false })
            },
        )
    }
}

@Composable
private fun BackupProcessItemCard(item: BackupProcessItem, backendType: BackupBackendType, isTerminal: Boolean) {
    val isArchive = backendType == BackupBackendType.Archive
    val (titleRes, iconRes) = when (item.category) {
        BackupCategory.Apps -> R.string.apps to R.drawable.ic_layout_grid
        BackupCategory.Files -> R.string.files to R.drawable.ic_folder
        BackupCategory.Networks -> R.string.networks to R.drawable.ic_wifi
        BackupCategory.Contacts -> R.string.contacts to R.drawable.ic_user_round
        BackupCategory.CallLogs -> R.string.call_logs to R.drawable.ic_phone
        BackupCategory.Messages -> R.string.messages to R.drawable.ic_message_circle
    }
    ProcessItemCard(
        category = ProcessItemCardState(
            icon = ImageVector.vectorResource(iconRes),
            title = stringResource(titleRes),
            label = if (isArchive) {
                "${item.completedCount}/${item.totalCount}"
            } else {
                stringResource(R.string.items_selected, item.totalCount, item.availableCount)
            },
            subtitle = if (isArchive) stringResource(item.outcome.titleRes) else "",
            progress = item.progress,
            isProcessing = isArchive && item.outcome == BackupOutcome.Running,
        ),
        isTerminal = isTerminal,
        currentApp = item.currentApp?.takeIf { isArchive }?.let { app ->
            ProcessItemCardState(
                icon = ImageVector.vectorResource(R.drawable.ic_resource_package),
                title = app.label.ifBlank { stringResource(R.string.unknown) },
                label = stringResource(app.currentPart.titleRes),
                subtitle = stringResource(R.string.backing_up),
                progress = app.progress,
                isProcessing = true,
            )
        },
    )
}

private val BackupOutcome.titleRes: Int
    get() = when (this) {
        BackupOutcome.Pending -> R.string.waiting_to_start
        BackupOutcome.Running -> R.string.backing_up
        BackupOutcome.Completed -> R.string.succeed
        BackupOutcome.Skipped -> R.string.skip
        BackupOutcome.Failed -> R.string.failed
        BackupOutcome.Canceled -> R.string.canceled
        BackupOutcome.Unprocessed -> R.string.restore_result_not_processed
    }

private val BackupSourceCategory?.titleRes: Int
    get() = when (this) {
        BackupSourceCategory.Apk -> R.string.apk
        BackupSourceCategory.InternalData -> R.string.internal_data
        BackupSourceCategory.ExternalData -> R.string.external_data
        BackupSourceCategory.AdditionalData -> R.string.additional_data
        else -> R.string.apps
    }

@Preview(name = "Phone", widthDp = 400, heightDp = 800)
@Preview(name = "Foldable", widthDp = 673, heightDp = 841)
@Preview(name = "Tablet", widthDp = 840, heightDp = 900)
@Preview(name = "Desktop", widthDp = 1200, heightDp = 800)
@Preview(name = "Large text", widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
private fun ArchiveBackupProcessPreview() = BackupProcessPreviewContent(BackupBackendType.Archive, BackupStatus.Running)

@Preview(name = "Rustic phone", widthDp = 400, heightDp = 800)
@Preview(name = "Rustic foldable", widthDp = 673, heightDp = 841)
@Preview(name = "Rustic tablet", widthDp = 840, heightDp = 900)
@Preview(name = "Rustic desktop", widthDp = 1200, heightDp = 800)
@Preview(name = "Rustic large text", widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
private fun RusticBackupProcessPreview() = BackupProcessPreviewContent(BackupBackendType.Rustic, BackupStatus.Running)

@Preview(name = "Waiting", widthDp = 400, heightDp = 800)
@Composable
private fun BackupPendingPreview() = BackupProcessPreviewContent(BackupBackendType.Archive, BackupStatus.Pending)

@Preview(name = "Canceling", widthDp = 400, heightDp = 800)
@Composable
private fun BackupCancelingPreview() = BackupProcessPreviewContent(BackupBackendType.Archive, BackupStatus.Canceling)

@Preview(name = "Canceled", widthDp = 400, heightDp = 800)
@Composable
private fun BackupCanceledPreview() = BackupProcessPreviewContent(BackupBackendType.Archive, BackupStatus.Canceled)

@Preview(name = "Finished", widthDp = 400, heightDp = 800)
@Composable
private fun BackupFinishedPreview() = BackupProcessPreviewContent(BackupBackendType.Archive, BackupStatus.Completed)

@Preview(name = "Partial failure", widthDp = 400, heightDp = 800)
@Composable
private fun BackupPartialFailurePreview() = BackupProcessPreviewContent(BackupBackendType.Archive, BackupStatus.PartialFailure)

@Preview(name = "Failed", widthDp = 400, heightDp = 800)
@Composable
private fun BackupFailedPreview() = BackupProcessPreviewContent(BackupBackendType.Rustic, BackupStatus.Failed)

@Preview(name = "Interrupted", widthDp = 400, heightDp = 800)
@Composable
private fun BackupInterruptedPreview() = BackupProcessPreviewContent(BackupBackendType.Rustic, BackupStatus.Interrupted)

@Composable
private fun BackupProcessPreviewContent(backendType: BackupBackendType, status: BackupStatus) {
    val outcome = when (status) {
        BackupStatus.Pending -> BackupOutcome.Pending
        BackupStatus.Running, BackupStatus.Canceling -> BackupOutcome.Running
        BackupStatus.Completed -> BackupOutcome.Completed
        BackupStatus.PartialFailure, BackupStatus.Failed -> BackupOutcome.Failed
        BackupStatus.Canceled -> BackupOutcome.Canceled
        BackupStatus.Interrupted -> BackupOutcome.Unprocessed
    }
    DataBackupTheme(dynamicColor = false) {
        BackupProcessContent(
            uiState = BackupProcessUiState(
                backendType = backendType,
                status = status,
                overallProgress = if (status == BackupStatus.Completed) "100" else "30",
                items = BackupCategory.entries.map { category ->
                    BackupProcessItem(
                        category = category,
                        totalCount = 10,
                        availableCount = 20,
                        completedCount = if (status == BackupStatus.Completed) 10 else 3,
                        outcome = outcome,
                        progress = if (status == BackupStatus.Completed) 1f else 0.3f,
                        currentApp = if (category == BackupCategory.Apps && status == BackupStatus.Running) {
                            BackupCurrentApp("com.example.calendar", 0, "Calendar", BackupSourceCategory.InternalData, 0.25f)
                        } else {
                            null
                        },
                    )
                },
            ),
            onCancel = {},
            onFinish = {},
        )
    }
}
