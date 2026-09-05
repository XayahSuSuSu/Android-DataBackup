package com.xayah.databackup.feature.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.feature.BackupConfigRoute
import com.xayah.databackup.feature.UpdatesRoute
import com.xayah.databackup.ui.component.LocalFloatingNavigationBarBottomPadding
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.StorageCard
import com.xayah.databackup.ui.component.fadeContentTransitionSpec
import com.xayah.databackup.ui.component.rememberFadingEdgeState
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.ui.theme.primaryContainerLow
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.navigateSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

private sealed interface DashboardBackupsUiState {
    data object Loading : DashboardBackupsUiState
    data object Error : DashboardBackupsUiState
    data object Empty : DashboardBackupsUiState
    data class Content(val backups: List<BackupConfig>) : DashboardBackupsUiState
}

@Composable
fun DashboardScreen(
    navigator: Navigator,
    onShowBackups: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val storageUiState = viewModel.storageUiState.collectAsStateWithLifecycle().value
    val backupConfigs = viewModel.backupConfigs.collectAsStateWithLifecycle().value
    val backupsLoadState = viewModel.backupsLoadState.collectAsStateWithLifecycle().value
    val backupsUiState = when {
        backupsLoadState == DashboardBackupsLoadState.Loading -> DashboardBackupsUiState.Loading
        backupsLoadState == DashboardBackupsLoadState.Error -> DashboardBackupsUiState.Error
        backupConfigs.isEmpty() -> DashboardBackupsUiState.Empty
        else -> DashboardBackupsUiState.Content(backupConfigs)
    }
    val floatingNavigationBarBottomPadding = LocalFloatingNavigationBarBottomPadding.current
    val scrollState = rememberScrollState()
    val fadingEdgeState = rememberFadingEdgeState(scrollState, label = "dashboard")

    LaunchedEffect(context = Dispatchers.IO, null) {
        viewModel.initialize()
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navigator.navigateSafely(UpdatesRoute) }) {
                        BadgedBox(
                            badge = {
                                Badge()
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_badge_info),
                                contentDescription = stringResource(R.string.update)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.surfaceTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalFadingEdges(fadingEdgeState)
                .verticalScroll(scrollState)
                .padding(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + floatingNavigationBarBottomPadding + 16.dp,
                ),
        ) {
            StorageCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                free = storageUiState.free,
                other = storageUiState.other,
                backups = storageUiState.backups,
                freeBytes = storageUiState.freeBytes,
                otherBytes = storageUiState.otherBytes,
                backupsBytes = storageUiState.backupsBytes,
                totalBytes = storageUiState.totalBytes,
                isLoading = storageUiState.isLoading,
                title = stringResource(R.string.internal_storage),
                subtitle = storageUiState.subtitle,
                storage = storageUiState.storage,
                onClick = {},
            )

            DashboardBackupsHeader(
                modifier = Modifier.fillMaxWidth(),
                showViewAll = backupsUiState is DashboardBackupsUiState.Content && backupsUiState.backups.size > 3,
                onShowAll = onShowBackups,
            )

            AnimatedContent(
                targetState = backupsUiState,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = fadeContentTransitionSpec(),
                contentKey = { it::class },
                label = "dashboardBackupsContent",
            ) { state ->
                when (state) {
                    DashboardBackupsUiState.Loading -> DashboardBackupsLoading()
                    DashboardBackupsUiState.Error -> DashboardBackupCard(
                        hasError = true,
                        onRetry = viewModel::retryLoadBackupConfigs,
                    )
                    DashboardBackupsUiState.Empty -> DashboardBackupCard()
                    is DashboardBackupsUiState.Content -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            state.backups.take(3).forEachIndexed { index, backup ->
                                DashboardBackupCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backup = backup,
                                    onClick = { navigator.navigateSafely(BackupConfigRoute(index)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardBackupsHeader(
    modifier: Modifier = Modifier,
    showViewAll: Boolean,
    onShowAll: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeader(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
            title = stringResource(R.string.recent_backups),
        )
        AnimatedVisibility(
            visible = showViewAll,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            TextButton(onClick = onShowAll) {
                Text(text = stringResource(R.string.view_all))
                Icon(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(18.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun DashboardBackupsLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator()
    }
}

@Composable
private fun DashboardBackupCard(
    modifier: Modifier = Modifier,
    backup: BackupConfig? = null,
    hasError: Boolean = false,
    onRetry: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                hasError -> {
                    DashboardBackupIcon(
                        icon = ImageVector.vectorResource(R.drawable.ic_circle_x),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.error),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    DashboardBackupChip(
                        text = stringResource(R.string.retry),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = { onRetry?.invoke() },
                    )
                }

                backup == null -> {
                    DashboardBackupIcon(icon = ImageVector.vectorResource(R.drawable.ic_folder_archive))
                    Text(
                        text = stringResource(R.string.no_backups),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    DashboardBackupIcon(icon = ImageVector.vectorResource(R.drawable.ic_archive))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = backup.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.last_backup_value, backup.displayUpdatedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    DashboardBackupChip(
                        text = stringResource(if (backup.backupBackend is BackupBackend.Rustic) R.string.rustic else R.string.archive),
                        onClick = { onClick?.invoke() },
                    )
                }
            }
        }
    }
    val shape = RoundedCornerShape(20.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            onClick = onClick,
            content = content,
        )
    }
}

@Composable
private fun DashboardBackupChip(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainerLow,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
        ),
        border = null,
    )
}

@Composable
private fun DashboardBackupIcon(
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = icon,
                contentDescription = null,
            )
        }
    }
}
