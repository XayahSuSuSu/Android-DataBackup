package com.xayah.databackup.feature.restore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.RestoreCategory
import com.xayah.databackup.entity.restore.RestoreItemStatus
import com.xayah.databackup.entity.restore.RestoreProcessItem
import com.xayah.databackup.entity.restore.RestoreProcessStatus
import com.xayah.databackup.entity.restore.RestoreRecordResult
import com.xayah.databackup.entity.restore.RestoreRecordStatus
import com.xayah.databackup.ui.component.ProcessItemCard
import com.xayah.databackup.ui.component.ProcessItemCardState
import com.xayah.databackup.ui.component.ProcessScaffold
import com.xayah.databackup.ui.theme.DataBackupTheme

@Composable
internal fun RestoreProcessContent(
    uiState: RestoreProcessUiState,
    overallProgress: String,
    onRequestCancel: () -> Unit,
    onFinish: () -> Unit,
    onRequestLeave: () -> Unit = onFinish,
) {
    val onBack: () -> Unit = {
        when {
            uiState.isTerminal -> onFinish()
            uiState.canCancel -> onRequestCancel()
            uiState.canLeave -> onRequestLeave()
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
    ProcessScaffold(
        overallProgress = overallProgress,
        statusLabel = statusLabel,
        isProcessing = uiState.isProcessing,
        isCanceling = uiState.isCanceling,
        isTerminal = uiState.isTerminal,
        isBackEnabled = uiState.isTerminal || uiState.canCancel || uiState.canLeave,
        notice = if (uiState.isFailed) uiState.errorMessage.ifEmpty { stringResource(R.string.failed) } else "",
        onBack = onBack,
        onFinish = onFinish,
    ) {
        uiState.items.filter { it.totalCount > 0 }.forEach { item ->
            key(item.category) {
                RestoreProcessItemCard(item = item, isTerminal = uiState.isTerminal)
            }
        }
    }
}

@Composable
private fun RestoreProcessItemCard(item: RestoreProcessItem, isTerminal: Boolean) {
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
    ProcessItemCard(
        category = ProcessItemCardState(
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
            ProcessItemCardState(
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

@Preview(name = "Phone", widthDp = 400, heightDp = 800)
@Preview(name = "Foldable", widthDp = 673, heightDp = 841)
@Preview(name = "Tablet", widthDp = 840, heightDp = 900)
@Preview(name = "Desktop", widthDp = 1200, heightDp = 800)
@Preview(name = "Large text", widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
private fun RestoreProcessPreview() = RestoreProcessPreviewContent(RestoreProcessStatus.Processing)

@Preview(name = "Canceling", widthDp = 400, heightDp = 800)
@Composable
private fun RestoreCancelingPreview() = RestoreProcessPreviewContent(RestoreProcessStatus.Canceling)

@Preview(name = "Canceled", widthDp = 400, heightDp = 800)
@Composable
private fun RestoreCanceledPreview() = RestoreProcessPreviewContent(RestoreProcessStatus.Canceled)

@Preview(name = "Finished", widthDp = 400, heightDp = 800)
@Composable
private fun RestoreFinishedPreview() = RestoreProcessPreviewContent(RestoreProcessStatus.Finished)

@Preview(name = "Failed", widthDp = 400, heightDp = 800)
@Composable
private fun RestoreFailedPreview() = RestoreProcessPreviewContent(RestoreProcessStatus.Failed)

@Composable
private fun RestoreProcessPreviewContent(status: RestoreProcessStatus) {
    val itemStatus = when (status) {
        RestoreProcessStatus.Pending, RestoreProcessStatus.Processing, RestoreProcessStatus.Canceling -> RestoreItemStatus.Processing
        RestoreProcessStatus.Canceled -> RestoreItemStatus.Canceled
        RestoreProcessStatus.Finished -> RestoreItemStatus.Finished
        RestoreProcessStatus.Interrupted, RestoreProcessStatus.Failed, RestoreProcessStatus.FinishedWithErrors -> RestoreItemStatus.Failed
    }
    DataBackupTheme(dynamicColor = false) {
        RestoreProcessContent(
            uiState = RestoreProcessUiState(
                status = status,
                items = RestoreCategory.entries.mapIndexed { index, category ->
                    val finished = status == RestoreProcessStatus.Finished || index == 0
                    RestoreProcessItem(
                        category = category,
                        totalCount = 10,
                        completedCount = if (finished) 10 else if (index == 1) 5 else 0,
                        status = when {
                            finished -> RestoreItemStatus.Finished
                            index == 1 -> itemStatus
                            status == RestoreProcessStatus.Canceled -> RestoreItemStatus.Canceled
                            else -> RestoreItemStatus.Pending
                        },
                        progress = if (finished) 1f else if (index == 1) 0.5f else 0f,
                    )
                },
            ),
            overallProgress = if (status == RestoreProcessStatus.Finished) "100" else "30",
            onRequestCancel = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Apps card", widthDp = 400, showBackground = true)
@Preview(name = "Apps card large text", widthDp = 360, fontScale = 1.5f, showBackground = true)
@Composable
private fun RestoreAppsCardPreview() {
    DataBackupTheme(dynamicColor = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            RestoreProcessItemCard(
                item = RestoreProcessItem(
                    category = RestoreCategory.Apps,
                    totalCount = 10,
                    completedCount = 3,
                    status = RestoreItemStatus.Processing,
                    progress = 0.3f,
                    records = listOf(
                        RestoreRecordResult(
                            id = "calendar",
                            title = "Calendar",
                            subtitle = "com.example.calendar · 0",
                            status = RestoreRecordStatus.Processing,
                            parts = mapOf(
                                BackupSourceCategory.Apk to RestoreRecordStatus.Restored,
                                BackupSourceCategory.InternalData to RestoreRecordStatus.Processing,
                                BackupSourceCategory.ExternalData to RestoreRecordStatus.Pending,
                                BackupSourceCategory.AdditionalData to RestoreRecordStatus.Pending,
                            ),
                        ),
                    ),
                ),
                isTerminal = false,
            )
        }
    }
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
