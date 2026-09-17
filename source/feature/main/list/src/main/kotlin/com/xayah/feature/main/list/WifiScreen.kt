package com.xayah.feature.main.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LinearProgressIndicator
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController

@Composable
fun WifiRoute(
    viewModel: WifiViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current!!
    val uiState: WifiUiState by viewModel.uiState.collectAsStateWithLifecycle()
    WifiScreen(
        uiState = uiState,
        titleRes = R.string.backup_wifi,
        loadingRes = R.string.wifi_scanning,
        emptyRes = R.string.wifi_empty,
        onToggle = viewModel::toggle,
        onSelectAll = viewModel::selectAll,
        onUnselectAll = viewModel::unselectAll,
        onRefresh = viewModel::scan,
        onContinue = { viewModel.backupSelected(navController) },
    )
}

@Composable
fun WifiRestoreRoute(
    viewModel: WifiRestoreViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current!!
    val uiState: WifiUiState by viewModel.uiState.collectAsStateWithLifecycle()
    WifiScreen(
        uiState = uiState,
        titleRes = R.string.restore_wifi,
        loadingRes = R.string.wifi_scanning,
        emptyRes = R.string.wifi_empty,
        onToggle = viewModel::toggle,
        onSelectAll = viewModel::selectAll,
        onUnselectAll = viewModel::unselectAll,
        onRefresh = viewModel::reload,
        onContinue = { viewModel.restoreSelected(navController) },
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun WifiScreen(
    uiState: WifiUiState,
    titleRes: Int,
    loadingRes: Int,
    emptyRes: Int,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onUnselectAll: () -> Unit,
    onRefresh: () -> Unit,
    onContinue: () -> Unit,
) {
    val items = (uiState as? WifiUiState.Success)?.items.orEmpty()
    val selected = (uiState as? WifiUiState.Success)?.selected.orEmpty()

    Scaffold(
        topBar = {
            SecondaryTopBar(
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
                title = stringResource(id = titleRes),
                subtitle = if (uiState is WifiUiState.Success) "(${selected.size}/${items.size})" else null,
                actions = {
                    IconButton(icon = Icons.Rounded.Refresh) {
                        onRefresh()
                    }
                },
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(visible = selected.isNotEmpty(), enter = scaleIn(), exit = scaleOut()) {
                ExtendedFloatingActionButton(
                    onClick = onContinue,
                    icon = { Icon(Icons.Rounded.ChevronRight, null) },
                    text = { Text(text = stringResource(id = R.string._continue)) },
                )
            }
        }
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            if (uiState is WifiUiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(id = loadingRes))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SizeTokens.Level16),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onSelectAll) {
                        Text(text = stringResource(id = R.string.select_all))
                    }
                    TextButton(onClick = onUnselectAll) {
                        Text(text = stringResource(id = R.string.unselect_all))
                    }
                }
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(id = emptyRes))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = rememberLazyListState(),
                    ) {
                        items(items, key = { it.path }) { item ->
                            WifiRow(
                                item = item,
                                checked = selected.contains(item.path),
                                onToggle = { onToggle(item.path) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiRow(
    item: WifiItem,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Rounded.Wifi, contentDescription = null)
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = item.path, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Divider(modifier = Modifier.fillMaxWidth())
}
