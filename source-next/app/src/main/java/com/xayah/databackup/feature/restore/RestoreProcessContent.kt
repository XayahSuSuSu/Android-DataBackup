package com.xayah.databackup.feature.restore

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R
import com.xayah.databackup.data.restore.RestoreCategory
import com.xayah.databackup.data.rustic.RusticSourceCategory
import com.xayah.databackup.ui.component.BackupProgressHeader
import com.xayah.databackup.ui.component.FadeVisibility
import com.xayah.databackup.ui.component.InlineNotice
import com.xayah.databackup.ui.component.ProcessItemCardContainer
import com.xayah.databackup.ui.component.ProcessItemCardContent
import com.xayah.databackup.ui.component.ProcessItemHolder
import com.xayah.databackup.ui.component.fadeSlideContentTransitionSpec
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.ui.theme.DataBackupTheme

@Composable
internal fun RestoreProcessContent(
    uiState: RestoreProcessUiState,
    overallProgress: String,
    onRequestCancel: () -> Unit,
    onFinish: () -> Unit,
) {
    val onBack: () -> Unit = {
        when {
            uiState.isTerminal -> onFinish()
            uiState.isProcessing -> onRequestCancel()
        }
    }
    BackHandler(onBack = onBack)

    val statusLabel = when (uiState.status) {
        RestoreProcessStatus.Canceling -> stringResource(R.string.processing)
        RestoreProcessStatus.Canceled -> stringResource(R.string.canceled)
        RestoreProcessStatus.Processing -> stringResource(R.string.restoring)
        RestoreProcessStatus.Finished -> stringResource(R.string.finished)
        RestoreProcessStatus.FinishedWithErrors -> stringResource(R.string.restore_finished_with_errors)
        RestoreProcessStatus.Failed -> stringResource(R.string.failed)
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.isCanceling) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_arrow_left), stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.surfaceTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            BackupProgressHeader(
                progress = overallProgress,
                statusLabel = statusLabel,
                showLoading = uiState.isProcessing,
            )
            AnimatedVisibility(visible = uiState.isCanceling) {
                InlineNotice(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    text = stringResource(R.string.wait_for_remaining_data_processing),
                    icon = ImageVector.vectorResource(R.drawable.ic_badge_info),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            AnimatedVisibility(visible = uiState.isFailed) {
                InlineNotice(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    text = uiState.errorMessage.ifEmpty { stringResource(R.string.failed) },
                    icon = ImageVector.vectorResource(R.drawable.ic_circle_x),
                )
            }
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                uiState.items.filter { it.totalCount > 0 }.forEach { item ->
                    key(item.category) {
                        RestoreProcessItemCard(item = item, isTerminal = uiState.isTerminal)
                    }
                }
            }
            FadeVisibility(visible = uiState.isTerminal) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onFinish) {
                        Text(text = stringResource(R.string.finish))
                    }
                }
            }
        }
    }
}

@Composable
private fun RestoreProcessItemCard(
    item: RestoreProcessItem,
    isTerminal: Boolean,
) {
    var showAppDetails by rememberSaveable(item.category, isTerminal) { mutableStateOf(false) }
    val currentApp = item.currentApp
    val canShowAppDetails = !isTerminal && currentApp != null
    val displayedApp = currentApp.takeIf { showAppDetails && canShowAppDetails }
    val progress = displayedApp?.progress ?: item.progress
    val toggleDetails: () -> Unit = { showAppDetails = !showAppDetails }
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
    ProcessItemHolder(
        modifier = Modifier.fillMaxWidth(),
        process = { progress.coerceIn(0f, 1f) },
        showProgress = !isTerminal && if (displayedApp != null) {
            displayedApp.status == RestoreRecordStatus.Processing
        } else {
            item.status == RestoreItemStatus.Processing
        },
    ) {
        ProcessItemCardContainer(onClick = { if (canShowAppDetails) toggleDetails() }) {
            AnimatedContent(
                targetState = showAppDetails && canShowAppDetails,
                transitionSpec = fadeSlideContentTransitionSpec(),
                label = "restoreAppDetails",
            ) { showDetails ->
                val app = currentApp.takeIf { showDetails }
                ProcessItemCardContent(
                    icon = ImageVector.vectorResource(if (app != null) R.drawable.ic_resource_package else iconRes),
                    title = app?.title?.ifBlank { stringResource(R.string.unknown) } ?: stringResource(titleRes),
                    subtitle = if (app != null) {
                        stringResource(app.status.titleRes)
                    } else if (item.failedCount > 0) {
                        stringResource(R.string.restore_result_counts, statusLabel, item.failedCount, item.skippedCount)
                    } else if (item.skippedCount > 0) {
                        stringResource(R.string.restore_skipped_count, statusLabel, item.skippedCount)
                    } else item.message.ifEmpty { statusLabel },
                    subtitleShimmer = false,
                    label = if (app != null) {
                        app.currentPart?.let { stringResource(it.restoreTitleRes) } ?: stringResource(R.string.apps)
                    } else {
                        "${item.completedCount}/${item.totalCount}"
                    },
                )
            }
        }
    }
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
        RestoreProcessStatus.Processing, RestoreProcessStatus.Canceling -> RestoreItemStatus.Processing
        RestoreProcessStatus.Canceled -> RestoreItemStatus.Canceled
        RestoreProcessStatus.Finished -> RestoreItemStatus.Finished
        RestoreProcessStatus.Failed, RestoreProcessStatus.FinishedWithErrors -> RestoreItemStatus.Failed
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
                                RusticSourceCategory.Apk to RestoreRecordStatus.Restored,
                                RusticSourceCategory.InternalData to RestoreRecordStatus.Processing,
                                RusticSourceCategory.ExternalData to RestoreRecordStatus.Pending,
                                RusticSourceCategory.AdditionalData to RestoreRecordStatus.Pending,
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
        RestoreRecordStatus.NotProcessed -> R.string.restore_result_not_processed
    }

private val RusticSourceCategory.restoreTitleRes: Int
    get() = when (this) {
        RusticSourceCategory.Apk -> R.string.apk
        RusticSourceCategory.InternalData -> R.string.internal_data
        RusticSourceCategory.ExternalData -> R.string.external_data
        RusticSourceCategory.AdditionalData -> R.string.additional_data
        else -> R.string.unknown
    }
