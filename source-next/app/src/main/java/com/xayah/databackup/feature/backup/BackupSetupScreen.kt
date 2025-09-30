package com.xayah.databackup.feature.backup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.AutoScreenOffSwitch
import com.xayah.databackup.ui.component.IncrementalBackupAndCleanBackupSwitches
import com.xayah.databackup.ui.component.ResetBackupListSwitch
import com.xayah.databackup.ui.component.SelectableCardButton
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.ui.component.horizontalFadingEdges
import com.xayah.databackup.ui.component.selectableCardButtonSecondaryColors
import com.xayah.databackup.ui.component.selectableCardButtonTertiaryColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.items
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupSetupScreen(
    navController: NavHostController,
    viewModel: BackupSetupViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val targetItems by viewModel.targetItems.collectAsStateWithLifecycle(DefTargetItems)
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle(DefSelectedItems)
    val nextBtnEnabled by viewModel.nextBtnEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(context = Dispatchers.IO, null) {
        viewModel.initialize()
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.backup),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.items_selected, selectedItems.first, selectedItems.second),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStackSafely() }) {
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

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState),
            ) {
                TargetRow(navController = navController, targetItems = targetItems, viewModel = viewModel)

                StorageRow(viewModel = viewModel)

                BackupRow(uiState = uiState, viewModel = viewModel)

                Settings()

                Spacer(modifier = Modifier.height(0.dp))
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
                    enabled = nextBtnEnabled,
                    onClick = { }
                ) {
                    Text(text = stringResource(R.string.next))
                }
            }

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun TargetRow(
    navController: NavHostController,
    targetItems: List<TargetItem>,
    viewModel: BackupSetupViewModel,
) {
    val lazyGridState = rememberLazyGridState()
    var showStartEdge by remember { mutableStateOf(false) }
    var showEndEdge by remember { mutableStateOf(false) }
    val startEdgeRange: Float by animateFloatAsState(if (showStartEdge) 1f else 0f, label = "alpha")
    val endEdgeRange: Float by animateFloatAsState(if (showEndEdge) 1f else 0f, label = "alpha")
    LaunchedEffect(context = Dispatchers.Default, lazyGridState.canScrollBackward) {
        showStartEdge = lazyGridState.canScrollBackward
    }
    LaunchedEffect(context = Dispatchers.Default, lazyGridState.canScrollForward) {
        showEndEdge = lazyGridState.canScrollForward
    }

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        text = stringResource(R.string.target),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge
    )

    LazyHorizontalGrid(
        modifier = Modifier
            .heightIn(max = (148 * 2 + 16).dp)
            .horizontalFadingEdges(startEdgeRange, endEdgeRange),
        state = lazyGridState,
        rows = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(items = targetItems, key = { index, _ -> index }) { index, item ->
            SelectableCardButton(
                modifier = Modifier
                    .size(148.dp)
                    .animateItem(),
                selected = item.selected,
                title = item.title,
                titleShimmer = item.initialized.not(),
                subtitle = item.subtitle,
                subtitleShimmer = item.initialized.not(),
                icon = item.icon,
                iconButton = ImageVector.vectorResource(R.drawable.ic_settings),
                onIconButtonClick = {
                    item.onClickSettings.invoke(navController)
                }
            ) {
                viewModel.withLock(Dispatchers.Default) {
                    item.onSelectedChanged.invoke(item.selected.not())
                }
            }
        }
    }
}

@Composable
private fun StorageRow(
    viewModel: BackupSetupViewModel,
) {
    val locationScrollState = rememberScrollState()

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        text = stringResource(R.string.storage),
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.labelLarge
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(locationScrollState)
            .horizontalFadingEdges(locationScrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.size(0.dp, 148.dp))

        var localStorage: String by remember { mutableStateOf("") }
        LaunchedEffect(context = Dispatchers.IO, null) {
            localStorage = viewModel.getLocalStorage()
        }
        SelectableCardButton(
            modifier = Modifier.size(148.dp),
            selected = true,
            title = stringResource(R.string.local),
            subtitle = localStorage,
            subtitleShimmer = localStorage.isEmpty(),
            icon = ImageVector.vectorResource(R.drawable.ic_smartphone),
            colors = selectableCardButtonSecondaryColors(),
        ) {
        }

        /**
         * SelectableCardButton(
         *     modifier = Modifier.size(148.dp),
         *     selected = false,
         *     title = stringResource(R.string.cloud),
         *     subtitle = stringResource(R.string.not_set_up),
         *     subtitleShimmer = false,
         *     icon = ImageVector.vectorResource(R.drawable.ic_cloud_upload),
         *     iconButton = ImageVector.vectorResource(R.drawable.ic_settings),
         *     onIconButtonClick = {}
         * ) {
         * }
         */

        Spacer(modifier = Modifier.size(0.dp, 148.dp))
    }
}

@Composable
private fun BackupRow(
    uiState: SetupUiState,
    viewModel: BackupSetupViewModel,
) {
    Text(
        modifier = Modifier.padding(16.dp),
        text = stringResource(R.string.backup),
        color = MaterialTheme.colorScheme.tertiary,
        style = MaterialTheme.typography.labelLarge
    )

    val lazyListState = rememberLazyListState()
    var showStartEdge by remember { mutableStateOf(false) }
    var showEndEdge by remember { mutableStateOf(false) }
    val startEdgeRange: Float by animateFloatAsState(if (showStartEdge) 1f else 0f, label = "alpha")
    val endEdgeRange: Float by animateFloatAsState(if (showEndEdge) 1f else 0f, label = "alpha")
    LaunchedEffect(context = Dispatchers.Default, lazyListState.canScrollBackward) {
        showStartEdge = lazyListState.canScrollBackward
    }
    LaunchedEffect(context = Dispatchers.Default, lazyListState.canScrollForward) {
        showEndEdge = lazyListState.canScrollForward
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalFadingEdges(startEdgeRange, endEdgeRange),
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.size(0.dp, 148.dp))
        }

        item(key = "-1") {
            SelectableCardButton(
                modifier = Modifier
                    .size(148.dp)
                    .animateItem(),
                selected = uiState.selectedConfigIndex == -1,
                title = stringResource(R.string.new_backup),
                titleShimmer = uiState.isLoadingConfigs,
                colors = selectableCardButtonTertiaryColors(),
                icon = ImageVector.vectorResource(R.drawable.ic_plus),
                iconShimmer = uiState.isLoadingConfigs,
            ) {
                viewModel.selectBackup(-1)
            }
        }

        items(items = uiState.configs, key = { _, item -> item.uuid }) { index, item ->
            var backupStorage: String by remember { mutableStateOf("") }
            LaunchedEffect(context = Dispatchers.IO, null) {
                backupStorage = viewModel.getBackupStorage(item.path)
            }
            SelectableCardButton(
                modifier = Modifier
                    .size(148.dp)
                    .animateItem(),
                selected = uiState.selectedConfigIndex == index,
                title = item.displayTitle,
                subtitle = backupStorage,
                subtitleShimmer = backupStorage.isEmpty(),
                colors = selectableCardButtonTertiaryColors(),
                icon = ImageVector.vectorResource(R.drawable.ic_archive),
                iconButton = ImageVector.vectorResource(R.drawable.ic_trash),
                onIconButtonClick = {},
            ) {
                viewModel.selectBackup(index)
            }
        }

        item {
            Spacer(modifier = Modifier.size(0.dp, 148.dp))
        }
    }
}

@Composable
private fun Settings() {
    Text(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        text = stringResource(R.string.settings),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge
    )

    IncrementalBackupAndCleanBackupSwitches()

    AutoScreenOffSwitch()

    ResetBackupListSwitch()
}
