package com.xayah.databackup.feature.backup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.feature.BackupConfigRoute
import com.xayah.databackup.ui.component.LocalFloatingNavigationBarBottomPadding
import com.xayah.databackup.ui.component.SearchTextField
import com.xayah.databackup.ui.component.rememberFadingEdgeState
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.navigateSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupLibraryScreen(
    navigator: Navigator,
    viewModel: BackupLibraryViewModel = koinViewModel(),
    newBackupViewModel: NewBackupViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val searchScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val content = uiState as? BackupLibraryUiState.Content
    val floatingNavigationBarBottomPadding = LocalFloatingNavigationBarBottomPadding.current
    var onSearch by remember { mutableStateOf(false) }
    var showNewBackupDialog by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val onCreateBackup = { showNewBackupDialog = true }
    val fadingEdgeState = rememberFadingEdgeState(gridState, label = "backupLibrary")

    LaunchedEffect(context = Dispatchers.IO, null) {
        viewModel.initialize()
    }

    LaunchedEffect(onSearch) {
        if (onSearch) {
            focusRequester.requestFocus()
        }
    }

    if (showNewBackupDialog) {
        NewBackupDialog(
            viewModel = newBackupViewModel,
            onDismissRequest = { showNewBackupDialog = false },
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(if (onSearch) searchScrollBehavior.nestedScrollConnection else scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedContent(onSearch) { target ->
                if (target) {
                    TopAppBar(
                        title = {
                            SearchTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp)
                                    .focusRequester(focusRequester),
                                value = content?.searchQuery.orEmpty(),
                                onClose = {
                                    onSearch = false
                                    viewModel.updateSearchQuery("")
                                },
                                onValueChange = viewModel::updateSearchQuery,
                            )
                        },
                        scrollBehavior = searchScrollBehavior,
                    )
                } else {
                    LargeTopAppBar(
                        title = { Text(stringResource(R.string.backup)) },
                        actions = {
                            AnimatedVisibility(visible = uiState is BackupLibraryUiState.Content, enter = fadeIn(), exit = fadeOut()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onSearch = true }) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                                            contentDescription = stringResource(R.string.search),
                                        )
                                    }
                                    IconButton(onClick = onCreateBackup) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_plus),
                                            contentDescription = stringResource(R.string.new_backup),
                                        )
                                    }
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.surfaceTopAppBarColors(),
                    )
                }
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it::class },
            label = "backupLibraryContent",
        ) { targetState ->
            when (targetState) {
                BackupLibraryUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator()
                    }
                }

                BackupLibraryUiState.Empty -> {
                    BackupLibraryEmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding() + floatingNavigationBarBottomPadding),
                        onCreateBackup = onCreateBackup,
                    )
                }

                is BackupLibraryUiState.Content -> {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalFadingEdges(fadingEdgeState),
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 320.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 8.dp,
                            end = 16.dp,
                            bottom = innerPadding.calculateBottomPadding() + floatingNavigationBarBottomPadding + 16.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item(
                            key = "filters",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            BackupLibraryFilters(
                                modifier = Modifier.animateItem(),
                                selectedFilter = targetState.filter,
                                onFilterChanged = viewModel::updateFilter,
                            )
                        }

                        if (targetState.filteredBackups.isEmpty()) {
                            item(
                                key = "no_matching_backups",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                NoMatchingBackupsState(
                                    modifier = Modifier
                                        .animateItem()
                                        .fillMaxWidth()
                                        .padding(vertical = 64.dp),
                                    onClearFilters = viewModel::clearFilters,
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = targetState.filteredBackups,
                                key = { _, indexedBackup -> indexedBackup.value.uuidString },
                            ) { _, indexedBackup ->
                                BackupConfigCard(
                                    modifier = Modifier.animateItem(),
                                    backup = indexedBackup.value,
                                    onClick = { navigator.navigateSafely(BackupConfigRoute(indexedBackup.index)) },
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
private fun BackupLibraryFilters(
    modifier: Modifier = Modifier,
    selectedFilter: BackupLibraryFilter,
    onFilterChanged: (BackupLibraryFilter) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BackupLibraryFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChanged(filter) },
                label = {
                    Text(
                        when (filter) {
                            BackupLibraryFilter.All -> stringResource(R.string.all)
                            BackupLibraryFilter.Rustic -> stringResource(R.string.rustic)
                            BackupLibraryFilter.Archive -> stringResource(R.string.archive)
                        }
                    )
                },
            )
        }
    }
}

@Composable
private fun BackupConfigCard(
    modifier: Modifier = Modifier,
    backup: BackupConfig,
    onClick: () -> Unit,
) {
    val relativeBackupPath = remember(backup.path) {
        PathHelper.getChildPath(backup.path).ifEmpty { backup.path }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = ImageVector.vectorResource(
                        if (backup.backupBackend is BackupBackend.Rustic) {
                            R.drawable.ic_database_backup
                        } else {
                            R.drawable.ic_archive
                        }
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = backup.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    text = if (backup.backupBackend is BackupBackend.Rustic) {
                        stringResource(R.string.rustic)
                    } else {
                        stringResource(R.string.archive)
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_folder),
                    contentDescription = stringResource(R.string.backup_dir),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = relativeBackupPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.last_backup_value, backup.displayUpdatedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BackupLibraryEmptyState(
    modifier: Modifier = Modifier,
    onCreateBackup: () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                modifier = Modifier.size(56.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_folder_archive),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.no_backups),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.no_backups_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onCreateBackup) {
                Text(stringResource(R.string.new_backup))
            }
        }
    }
}

@Composable
private fun NoMatchingBackupsState(
    modifier: Modifier = Modifier,
    onClearFilters: () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                modifier = Modifier.size(48.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.no_matching_backups),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.no_matching_backups_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onClearFilters) {
                Text(stringResource(R.string.clear_filters))
            }
        }
    }
}
