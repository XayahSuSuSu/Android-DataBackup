package com.xayah.databackup.feature.backup

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.animateColorAsState
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
import com.xayah.databackup.ui.component.FadeVisibility
import com.xayah.databackup.ui.component.ProcessItemCard
import com.xayah.databackup.ui.component.ProcessItemHolder
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.SymbolHelper
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

                    Text(
                        text = if (uiState.isProcessing) stringResource(R.string.backing_up) else stringResource(R.string.finished),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                FadeVisibility(visible = uiState.isProcessing) {
                    ContainedLoadingIndicator(modifier = Modifier.size(64.dp))
                }
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

                ProcessAppsItem(viewModel = viewModel)

                ProcessFilesItem(filesItem = filesItem)

                ProcessNetworksItem(networksItem = networksItem)

                ProcessContactsItem(contactsItem = contactsItem)

                ProcessCallLogsItem(callLogsItem = callLogsItem)

                ProcessMessagesItem(messagesItem = messagesItem)

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
                    enabled = true,
                    onClick = {
                        onBack.invoke()
                    }
                ) {
                    Text(text = if (uiState.isProcessing) stringResource(R.string.cancel) else stringResource(R.string.finish))
                }
            }

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
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
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState)
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
private fun ProcessAppsItem(viewModel: BackupProcessViewModel) {
    val appsItem by viewModel.appsItem.collectAsStateWithLifecycle()
    val processingAppItem by viewModel.processingAppItem.collectAsStateWithLifecycle()

    var openAppItemDialog by remember { mutableStateOf(false) }
    if (openAppItemDialog) {
        processingAppItem?.also {
            AppItemDialog(appItem = it) {
                openAppItemDialog = false
            }
        }
    }

    FadeVisibility(visible = appsItem.isSelected) {
        ProcessItemHolder(
            modifier = Modifier.fillMaxWidth(),
            process = { appsItem.progress },
        ) {
            ProcessItemCard(
                icon = ImageVector.vectorResource(R.drawable.ic_layout_grid),
                title = stringResource(R.string.apps),
                currentIndex = appsItem.currentIndex,
                totalCount = appsItem.totalCount,
                subtitle = appsItem.msg,
                subtitleShimmer = appsItem.isLoading,
                onIconBtnClick = { openAppItemDialog = true },
                onClick = { }
            )
        }
    }
}

@Composable
private fun ProcessFilesItem(filesItem: ProcessItem) {
    FadeVisibility(visible = filesItem.isSelected) {
        ProcessItemHolder(
            modifier = Modifier.fillMaxWidth(),
            process = { filesItem.progress },
        ) {
            ProcessItemCard(
                icon = ImageVector.vectorResource(R.drawable.ic_folder),
                title = stringResource(R.string.files),
                currentIndex = filesItem.currentIndex,
                totalCount = filesItem.totalCount,
                subtitle = filesItem.msg,
                subtitleShimmer = filesItem.isLoading,
                onIconBtnClick = {},
                onClick = {}
            )
        }
    }
}

@Composable
private fun ProcessNetworksItem(networksItem: ProcessItem) {
    FadeVisibility(visible = networksItem.isSelected) {
        ProcessItemHolder(
            modifier = Modifier.fillMaxWidth(),
            process = { networksItem.progress },
        ) {
            ProcessItemCard(
                icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                title = stringResource(R.string.network),
                currentIndex = networksItem.currentIndex,
                totalCount = networksItem.totalCount,
                subtitle = networksItem.msg,
                subtitleShimmer = networksItem.isLoading,
                onIconBtnClick = {},
                onClick = {}
            )
        }
    }
}

@Composable
private fun ProcessContactsItem(contactsItem: ProcessItem) {
    FadeVisibility(visible = contactsItem.isSelected) {
        ProcessItemHolder(
            modifier = Modifier.fillMaxWidth(),
            process = { contactsItem.progress },
        ) {
            ProcessItemCard(
                icon = ImageVector.vectorResource(R.drawable.ic_user_round),
                title = stringResource(R.string.contacts),
                currentIndex = contactsItem.currentIndex,
                totalCount = contactsItem.totalCount,
                subtitle = contactsItem.msg,
                subtitleShimmer = contactsItem.isLoading,
                onIconBtnClick = {},
                onClick = {}
            )
        }
    }
}

@Composable
private fun ProcessCallLogsItem(callLogsItem: ProcessItem) {
    FadeVisibility(visible = callLogsItem.isSelected) {
        ProcessItemHolder(
            modifier = Modifier.fillMaxWidth(),
            process = { callLogsItem.progress },
        ) {
            ProcessItemCard(
                icon = ImageVector.vectorResource(R.drawable.ic_phone),
                title = stringResource(R.string.call_logs),
                currentIndex = callLogsItem.currentIndex,
                totalCount = callLogsItem.totalCount,
                subtitle = callLogsItem.msg,
                subtitleShimmer = callLogsItem.isLoading,
                onIconBtnClick = {},
                onClick = {}
            )
        }
    }
}

@Composable
private fun ProcessMessagesItem(messagesItem: ProcessItem) {
    FadeVisibility(visible = messagesItem.isSelected) {
        ProcessItemHolder(
            modifier = Modifier.fillMaxWidth(),
            process = { messagesItem.progress },
        ) {
            ProcessItemCard(
                icon = ImageVector.vectorResource(R.drawable.ic_message_circle),
                title = stringResource(R.string.messages),
                currentIndex = messagesItem.currentIndex,
                totalCount = messagesItem.totalCount,
                subtitle = messagesItem.msg,
                subtitleShimmer = messagesItem.isLoading,
                onIconBtnClick = {},
                onClick = {}
            )
        }
    }
}

private const val DisabledOpacity = 0.38f
