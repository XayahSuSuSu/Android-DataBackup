package com.xayah.databackup.feature.backup

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.xayah.databackup.R
import com.xayah.databackup.data.ProcessAppItem
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.feature.BackupProcessDetailsRoute
import com.xayah.databackup.ui.component.FadeVisibility
import com.xayah.databackup.ui.component.ProcessItemCard
import com.xayah.databackup.ui.component.ProcessItemHolder
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.SymbolHelper
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupProcessScreen(
    navController: NavHostController,
    viewModel: BackupProcessViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overallProgress by viewModel.overallProgress.collectAsStateWithLifecycle()
    val filesItem by viewModel.filesItem.collectAsStateWithLifecycle()
    val networksItem by viewModel.networksItem.collectAsStateWithLifecycle()
    val contactsItem by viewModel.contactsItem.collectAsStateWithLifecycle()
    val callLogsItem by viewModel.callLogsItem.collectAsStateWithLifecycle()
    val messagesItem by viewModel.messagesItem.collectAsStateWithLifecycle()
    var openConfirmExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(context = Dispatchers.IO, null) {
        viewModel.loadProcessItems()
    }

    val onBack = remember {
        {
            if (uiState.canBeCanceled) {
                openConfirmExitDialog = true
            } else {
                navController.popBackStackSafely()
            }
        }
    }

    BackHandler {
        onBack.invoke()
    }

    val statusLabel = when (uiState.status) {
        BackupProcessStatus.Canceling -> stringResource(R.string.processing)
        BackupProcessStatus.Canceled -> stringResource(R.string.canceled)
        BackupProcessStatus.Processing -> stringResource(R.string.backing_up)
        BackupProcessStatus.Finished -> stringResource(R.string.finished)
    }
    val actionLabel = when (uiState.status) {
        BackupProcessStatus.Canceling -> stringResource(R.string.processing)
        BackupProcessStatus.Processing -> stringResource(R.string.cancel)
        else -> stringResource(R.string.finish)
    }
    val itemStatusOverride = if (uiState.isCanceled) stringResource(R.string.canceled) else null
    val showItemProgress = uiState.isCanceling.not() && uiState.isCanceled.not()

    if (openConfirmExitDialog) {
        ConfirmExitDialog(
            onConfirm = {
                if (uiState.canBeCanceled) {
                    viewModel.cancel()
                }
                openConfirmExitDialog = false
            },
            onDismissRequest = {
                openConfirmExitDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onBack.invoke() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.defaultLargeTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            Row(
                modifier = Modifier.padding(start = 16.dp, top = 40.dp, bottom = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            modifier = Modifier.alignByBaseline(),
                            text = overallProgress,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val percent by remember { mutableStateOf(SymbolHelper.PERCENT.toString()) }
                        Text(
                            modifier = Modifier.alignByBaseline(),
                            text = percent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AnimatedContent(
                        targetState = statusLabel,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "statusLabelAnimation"
                    ) { label ->
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                FadeVisibility(visible = uiState.isProcessing) {
                    ContainedLoadingIndicator(modifier = Modifier.size(64.dp))
                }
            }

            AnimatedVisibility(visible = uiState.isCanceling) {
                CancelingNotice()
            }

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.size(0.dp))

                ProcessAppsItem(
                    navController = navController,
                    viewModel = viewModel,
                    showProgress = showItemProgress,
                    statusOverride = itemStatusOverride,
                    openDetailsPage = uiState.isProcessing.not() && uiState.isCanceling.not()
                )

                ProcessFilesItem(filesItem = filesItem, showProgress = showItemProgress, statusOverride = itemStatusOverride)

                ProcessNetworksItem(networksItem = networksItem, showProgress = showItemProgress, statusOverride = itemStatusOverride)

                ProcessContactsItem(contactsItem = contactsItem, showProgress = showItemProgress, statusOverride = itemStatusOverride)

                ProcessCallLogsItem(callLogsItem = callLogsItem, showProgress = showItemProgress, statusOverride = itemStatusOverride)

                ProcessMessagesItem(messagesItem = messagesItem, showProgress = showItemProgress, statusOverride = itemStatusOverride)

                Spacer(modifier = Modifier.size(16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Button(
                    modifier = Modifier.wrapContentSize(),
                    enabled = uiState.isCanceling.not(),
                    onClick = {
                        onBack.invoke()
                    }
                ) {
                    AnimatedContent(
                        targetState = actionLabel,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "actionLabelAnimation"
                    ) { label ->
                        Text(text = label)
                    }
                }
            }

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun CancelingNotice() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = CANCELING_NOTICE_ALPHA)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_badge_info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.wait_for_remaining_data_processing),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AppItemDialogListItem(enabled: Boolean, icon: ImageVector, name: String, bytes: String, msg: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val animatedSurfaceColor by animateColorAsState(
            targetValue = if (enabled)
                MaterialTheme.colorScheme.surfaceContainerHighest
            else
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = DisabledOpacity),
            label = "animatedColor"
        )
        val animatedIconColor by animateColorAsState(
            targetValue = if (enabled)
                MaterialTheme.colorScheme.onSecondaryContainer
            else
                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = DisabledOpacity),
            label = "animatedColor"
        )
        val animatedTitleColor by animateColorAsState(
            targetValue = if (enabled)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledOpacity),
            label = "animatedColor"
        )
        val animatedSubtitleColor by animateColorAsState(
            targetValue = if (enabled)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DisabledOpacity),
            label = "animatedColor"
        )

        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = animatedSurfaceColor) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = icon,
                    tint = animatedIconColor,
                    contentDescription = null
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = animatedTitleColor
            )
            Text(
                text = bytes,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = animatedSubtitleColor
            )
        }
        Text(
            text = msg,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = animatedSubtitleColor
        )
    }
}

@Composable
private fun ConfirmExitDialog(
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_badge_info), contentDescription = stringResource(R.string.prompt)) },
        title = { Text(text = stringResource(R.string.prompt)) },
        text = { Text(text = stringResource(R.string.prompt_cancel_operation)) },
        onDismissRequest = onDismissRequest,
        confirmButton = { TextButton(onClick = onConfirm) { Text(text = stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(text = stringResource(R.string.dismiss)) } }
    )
}

@Composable
private fun AppItemDialog(
    appItem: ProcessAppItem,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                var icon: Drawable? by remember { mutableStateOf(null) }
                LaunchedEffect(context = Dispatchers.IO, appItem.packageName) {
                    icon = runCatching { context.packageManager.getApplicationIcon(appItem.packageName) }.getOrNull()
                    if (icon == null) {
                        icon = AppCompatResources.getDrawable(context, android.R.drawable.sym_def_app_icon)
                    }
                }
                AsyncImage(
                    modifier = Modifier.size(32.dp),
                    model = ImageRequest.Builder(context)
                        .data(icon)
                        .crossfade(true)
                        .build(),
                    contentDescription = null
                )

                Text(text = appItem.label)
            }
        },
        text = {
            AppItemDialogItem(appItem)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text(text = stringResource(R.string.confirm))
            }
        }
    )
}

@Composable
private fun AppItemDialogItem(
    appItem: ProcessAppItem,
    scrollable: Boolean = true,
) {
    var modifier: Modifier = Modifier
    if (scrollable) {
        val scrollState = rememberScrollState()
        modifier = Modifier
            .verticalScroll(scrollState)
            .verticalFadingEdges(scrollState)
    }

    Column(
        modifier = modifier
    ) {
        AppItemDialogListItem(
            enabled = appItem.apkItem.enabled,
            icon = ImageVector.vectorResource(R.drawable.ic_resource_package),
            name = appItem.apkItem.title,
            bytes = appItem.apkItem.subtitle,
            msg = appItem.apkItem.msg
        )
        HorizontalDivider()
        AppItemDialogListItem(
            enabled = appItem.intDataItem.enabled,
            icon = ImageVector.vectorResource(R.drawable.ic_user),
            name = appItem.intDataItem.title,
            bytes = appItem.intDataItem.subtitle,
            msg = appItem.intDataItem.msg
        )
        HorizontalDivider()
        AppItemDialogListItem(
            enabled = appItem.extDataItem.enabled,
            icon = ImageVector.vectorResource(R.drawable.ic_database),
            name = appItem.extDataItem.title,
            bytes = appItem.extDataItem.subtitle,
            msg = appItem.extDataItem.msg
        )
        HorizontalDivider()
        AppItemDialogListItem(
            enabled = appItem.addlDataItem.enabled,
            icon = ImageVector.vectorResource(R.drawable.ic_gamepad_2),
            name = appItem.addlDataItem.title,
            bytes = appItem.addlDataItem.subtitle,
            msg = appItem.addlDataItem.msg
        )
    }
}

@Composable
private fun ProcessAppsItem(
    navController: NavHostController,
    viewModel: BackupProcessViewModel,
    showProgress: Boolean,
    statusOverride: String?,
    openDetailsPage: Boolean,
) {
    val appsItem by viewModel.appsItem.collectAsStateWithLifecycle()
    val allItems by viewModel.allProcessedAppItems.collectAsStateWithLifecycle()

    var openAppItemDialog by remember { mutableStateOf(false) }
    if (openAppItemDialog) {
        allItems.lastOrNull()?.also {
            AppItemDialog(appItem = it) {
                openAppItemDialog = false
            }
        }
    }

    FadeVisibility(visible = appsItem.isSelected && appsItem.totalCount > 0) {
        ProcessItemHolder(
            modifier = Modifier.fillMaxWidth(),
            process = { appsItem.progress },
            showProgress = showProgress,
        ) {
            ProcessItemCard(
                icon = ImageVector.vectorResource(R.drawable.ic_layout_grid),
                title = stringResource(R.string.apps),
                currentIndex = appsItem.currentIndex,
                totalCount = appsItem.totalCount,
                subtitle = statusOverride ?: appsItem.msg,
                subtitleShimmer = appsItem.isLoading && statusOverride == null,
                onIconBtnClick = {
                    if (openDetailsPage) {
                        navController.navigateSafely(BackupProcessDetailsRoute)
                    } else {
                        openAppItemDialog = true
                    }
                },
                onClick = { }
            )
        }
    }
}

@Composable
private fun ProcessFilesItem(filesItem: ProcessItem, showProgress: Boolean, statusOverride: String?) {
    FadeVisibility(visible = filesItem.isSelected && filesItem.totalCount > 0) {
        ProcessItemHolder(
            modifier = Modifier.fillMaxWidth(),
            process = { filesItem.progress },
            showProgress = showProgress,
        ) {
            ProcessItemCard(
                icon = ImageVector.vectorResource(R.drawable.ic_folder),
                title = stringResource(R.string.files),
                currentIndex = filesItem.currentIndex,
                totalCount = filesItem.totalCount,
                subtitle = statusOverride ?: filesItem.msg,
                subtitleShimmer = filesItem.isLoading && statusOverride == null,
                onIconBtnClick = null,
                onClick = {}
            )
        }
    }
}

@Composable
private fun ProcessNetworksItem(networksItem: ProcessItem, showProgress: Boolean, statusOverride: String?) {
    FadeVisibility(visible = networksItem.isSelected && networksItem.totalCount > 0) {
        ProcessItemHolder(
            modifier = Modifier.fillMaxWidth(),
            process = { networksItem.progress },
            showProgress = showProgress,
        ) {
            ProcessItemCard(
                icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                title = stringResource(R.string.network),
                currentIndex = networksItem.currentIndex,
                totalCount = networksItem.totalCount,
                subtitle = statusOverride ?: networksItem.msg,
                subtitleShimmer = networksItem.isLoading && statusOverride == null,
                onIconBtnClick = null,
                onClick = {}
            )
        }
    }
}

@Composable
private fun ProcessContactsItem(contactsItem: ProcessItem, showProgress: Boolean, statusOverride: String?) {
    FadeVisibility(visible = contactsItem.isSelected && contactsItem.totalCount > 0) {
        ProcessItemHolder(
            modifier = Modifier.fillMaxWidth(),
            process = { contactsItem.progress },
            showProgress = showProgress,
        ) {
            ProcessItemCard(
                icon = ImageVector.vectorResource(R.drawable.ic_user_round),
                title = stringResource(R.string.contacts),
                currentIndex = contactsItem.currentIndex,
                totalCount = contactsItem.totalCount,
                subtitle = statusOverride ?: contactsItem.msg,
                subtitleShimmer = contactsItem.isLoading && statusOverride == null,
                onIconBtnClick = null,
                onClick = {}
            )
        }
    }
}

@Composable
private fun ProcessCallLogsItem(callLogsItem: ProcessItem, showProgress: Boolean, statusOverride: String?) {
    FadeVisibility(visible = callLogsItem.isSelected && callLogsItem.totalCount > 0) {
        ProcessItemHolder(
            modifier = Modifier.fillMaxWidth(),
            process = { callLogsItem.progress },
            showProgress = showProgress,
        ) {
            ProcessItemCard(
                icon = ImageVector.vectorResource(R.drawable.ic_phone),
                title = stringResource(R.string.call_logs),
                currentIndex = callLogsItem.currentIndex,
                totalCount = callLogsItem.totalCount,
                subtitle = statusOverride ?: callLogsItem.msg,
                subtitleShimmer = callLogsItem.isLoading && statusOverride == null,
                onIconBtnClick = null,
                onClick = {}
            )
        }
    }
}

@Composable
private fun ProcessMessagesItem(messagesItem: ProcessItem, showProgress: Boolean, statusOverride: String?) {
    FadeVisibility(visible = messagesItem.isSelected && messagesItem.totalCount > 0) {
        ProcessItemHolder(
            modifier = Modifier.fillMaxWidth(),
            process = { messagesItem.progress },
            showProgress = showProgress,
        ) {
            ProcessItemCard(
                icon = ImageVector.vectorResource(R.drawable.ic_message_circle),
                title = stringResource(R.string.messages),
                currentIndex = messagesItem.currentIndex,
                totalCount = messagesItem.totalCount,
                subtitle = statusOverride ?: messagesItem.msg,
                subtitleShimmer = messagesItem.isLoading && statusOverride == null,
                onIconBtnClick = null,
                onClick = {}
            )
        }
    }
}

private const val CANCELING_NOTICE_ALPHA = 0.4f
private const val DisabledOpacity = 0.38f
