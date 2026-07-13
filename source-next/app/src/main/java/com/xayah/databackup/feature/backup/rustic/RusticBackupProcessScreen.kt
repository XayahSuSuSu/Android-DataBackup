package com.xayah.databackup.feature.backup.rustic

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.DataBackupDialog
import com.xayah.databackup.ui.component.DialogActionButton
import com.xayah.databackup.ui.component.DialogIcon
import com.xayah.databackup.ui.component.StorageDistributionBar
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.formatToStorageSize
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun RusticBackupProcessScreen(
    navController: NavHostController,
    viewModel: RusticBackupProcessViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overallProgress by viewModel.overallProgress.collectAsStateWithLifecycle()
    val sourceItems by viewModel.sourceItems.collectAsStateWithLifecycle()
    var showRepositoryInfo by rememberSaveable { mutableStateOf(false) }
    var showProcessingNotice by rememberSaveable { mutableStateOf(false) }

    val leave = {
        if (uiState.isProcessing) showProcessingNotice = true else navController.popBackStackSafely()
    }
    BackHandler(onBack = leave)

    LaunchedEffect(context = Dispatchers.IO, null) {
        viewModel.loadProcessItems()
    }
    LaunchedEffect(context = Dispatchers.Default, showRepositoryInfo, uiState.status) {
        if (showRepositoryInfo) viewModel.refreshRepositoryStorage()
    }

    val statusLabel = when (uiState.status) {
        RusticBackupProcessStatus.Processing -> stringResource(R.string.backing_up)
        RusticBackupProcessStatus.Finished -> stringResource(R.string.finished)
        RusticBackupProcessStatus.Failed -> stringResource(R.string.failed)
    }

    if (showRepositoryInfo) {
        RusticRepositoryInfoDialog(
            uiState = uiState,
            onDismissRequest = { showRepositoryInfo = false },
        )
    }

    if (showProcessingNotice) {
        DataBackupDialog(
            title = stringResource(R.string.backup_in_progress),
            onDismissRequest = { showProcessingNotice = false },
            icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_clock)) },
            content = { Text(stringResource(R.string.rustic_backup_leave_blocked)) },
            confirmButton = {
                DialogActionButton(text = stringResource(R.string.confirm), onClick = { showProcessingNotice = false })
            },
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
                    IconButton(onClick = leave) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showRepositoryInfo = true }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_badge_info),
                            contentDescription = stringResource(R.string.repository),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.defaultLargeTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            RusticBackupProcessContent(
                modifier = Modifier.weight(1f),
                uiState = uiState,
                sources = sourceItems,
                overallProgress = overallProgress,
                statusLabel = statusLabel,
                onFinish = { navController.popBackStackSafely() },
            )

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
internal fun RusticRepositoryInfoDialog(
    uiState: RusticBackupProcessUiState,
    onDismissRequest: () -> Unit,
) {
    val backupName = uiState.backupName.ifBlank { stringResource(R.string.rustic_backup) }
    val repositoryPath = uiState.repositoryPath.ifBlank { stringResource(R.string.not_set_up) }
    val scrollState = rememberScrollState()
    val protection = stringResource(if (uiState.isPasswordProtected) R.string.password_protected else R.string.anonymous_repository)

    DataBackupDialog(
        title = stringResource(R.string.repository),
        onDismissRequest = onDismissRequest,
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_folder_archive)) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RepositoryStorageOverview(storage = uiState.repositoryStorage)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column {
                        RepositoryInfoItem(stringResource(R.string.name), backupName)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        RepositoryInfoItem(stringResource(R.string.repository_path), repositoryPath)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        RepositoryInfoItem(stringResource(R.string.password), protection)
                    }
                }
            }
        },
        confirmButton = {
            DialogActionButton(text = stringResource(R.string.confirm), onClick = onDismissRequest)
        },
    )
}

@Composable
private fun RepositoryStorageOverview(storage: RusticRepositoryStorageUiState) {
    val repositorySize = when {
        storage.isLoading -> stringResource(R.string.calculating)
        storage.isAvailable -> storage.repositoryBytes.formatToStorageSize
        else -> stringResource(R.string.unknown)
    }
    val availableSpace = when {
        storage.isLoading -> stringResource(R.string.calculating)
        storage.isAvailable -> storage.freeBytes.formatToStorageSize
        else -> stringResource(R.string.unknown)
    }
    val animatedRepositoryRatio by animateFloatAsState(
        targetValue = storage.repositoryRatio,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "repository-storage-ratio",
    )
    val animatedOtherRatio by animateFloatAsState(
        targetValue = storage.otherRatio,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "other-storage-ratio",
    )
    val animatedFreeRatio by animateFloatAsState(
        targetValue = storage.freeRatio,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "free-storage-ratio",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RepositoryStorageMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.data_size),
                    value = repositorySize,
                )
                RepositoryStorageMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.available_space),
                    value = availableSpace,
                )
            }

            Crossfade(
                targetState = storage.isLoading,
                animationSpec = tween(durationMillis = 250),
                label = "repository-storage-loading",
            ) { isLoading ->
                when {
                    isLoading -> LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        strokeCap = StrokeCap.Round,
                    )

                    storage.isAvailable -> StorageDistributionBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        backups = animatedRepositoryRatio,
                        other = animatedOtherRatio,
                        free = animatedFreeRatio,
                    )

                    else -> Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {}
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RepositoryStorageLegend(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.repository),
                )
                RepositoryStorageLegend(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary,
                    label = stringResource(R.string.other),
                )
                RepositoryStorageLegend(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline,
                    label = stringResource(R.string.free),
                )
            }
        }
    }
}

@Composable
private fun RepositoryStorageMetric(
    modifier: Modifier,
    label: String,
    value: String,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Crossfade(
            targetState = value,
            animationSpec = tween(durationMillis = 250),
            label = "repository-storage-value",
        ) { animatedValue ->
            Text(
                text = animatedValue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RepositoryStorageLegend(
    modifier: Modifier,
    color: Color,
    label: String,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        Surface(
            modifier = Modifier.size(6.dp),
            shape = CircleShape,
            color = color,
        ) {}
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RepositoryInfoItem(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
