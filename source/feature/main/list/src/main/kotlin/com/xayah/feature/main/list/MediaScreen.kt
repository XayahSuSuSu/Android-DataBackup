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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.io.File
import com.xayah.core.model.MediaKind
import com.xayah.core.model.ScannedMediaFile
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LinearProgressIndicator
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController

@Composable
fun MediaRoute(
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current!!
    val uiState: MediaUiState by viewModel.uiState.collectAsStateWithLifecycle()
    MediaScreen(
        uiState = uiState,
        onToggle = viewModel::toggle,
        onSelectAll = viewModel::selectAll,
        onUnselectAll = viewModel::unselectAll,
        onRescan = viewModel::scan,
        onBackup = { viewModel.backupSelected(navController) },
        onLoadThumbnail = viewModel::loadThumbnail,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun MediaScreen(
    uiState: MediaUiState,
    onToggle: (String) -> Unit,
    onSelectAll: (MediaKind) -> Unit,
    onUnselectAll: (MediaKind) -> Unit,
    onRescan: () -> Unit,
    onBackup: () -> Unit,
    onLoadThumbnail: suspend (String) -> String?,
) {
    val selected = (uiState as? MediaUiState.Success)?.selected.orEmpty()
    val total = (uiState as? MediaUiState.Success)?.total ?: 0
    var tabIndex by remember { mutableIntStateOf(1) }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
                title = stringResource(id = R.string.backup_media),
                subtitle = if (uiState is MediaUiState.Success) "(${selected.size}/${total})" else null,
                actions = {
                    IconButton(icon = Icons.Rounded.Refresh) {
                        onRescan()
                    }
                },
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(visible = selected.isNotEmpty(), enter = scaleIn(), exit = scaleOut()) {
                ExtendedFloatingActionButton(
                    onClick = onBackup,
                    icon = { Icon(Icons.Rounded.ChevronRight, null) },
                    text = { Text(text = stringResource(id = R.string._continue)) },
                )
            }
        }
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            if (uiState is MediaUiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(id = R.string.media_scanning))
                }
            } else if (uiState is MediaUiState.Success) {
                MediaTabs(
                    imagesCount = uiState.images.size,
                    videosCount = uiState.videos.size,
                    audioCount = uiState.audio.size,
                    selectedTab = tabIndex,
                    onTabClick = { tabIndex = it },
                )
                val kind = when (tabIndex) {
                    0 -> MediaKind.Images
                    2 -> MediaKind.Audio
                    else -> MediaKind.Videos
                }
                val items = when (kind) {
                    MediaKind.Images -> uiState.images
                    MediaKind.Videos -> uiState.videos
                    MediaKind.Audio -> uiState.audio
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SizeTokens.Level16),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { onSelectAll(kind) }) {
                        Text(text = stringResource(id = R.string.select_all))
                    }
                    TextButton(onClick = { onUnselectAll(kind) }) {
                        Text(text = stringResource(id = R.string.unselect_all))
                    }
                }
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(id = R.string.media_empty))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = rememberLazyListState(),
                    ) {
                        items(items, key = { it.path }) { item ->
                            var thumbPath by remember(item.path) { mutableStateOf<String?>(null) }
                            LaunchedEffect(item.path) {
                                thumbPath = onLoadThumbnail(item.path)
                            }
                            MediaRow(
                                item = item,
                                checked = selected.contains(item.path),
                                thumbnail = thumbPath,
                                onToggle = { onToggle(item.path) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun MediaTabs(
    imagesCount: Int,
    videosCount: Int,
    audioCount: Int,
    selectedTab: Int,
    onTabClick: (Int) -> Unit,
) {
    val titles = listOf(
        "${stringResource(id = R.string.media_images)} (${imagesCount})",
        "${stringResource(id = R.string.media_videos)} (${videosCount})",
        "${stringResource(id = R.string.media_audio)} (${audioCount})",
    )
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = SizeTokens.Level0,
        indicator = @Composable {
            TabRowDefaults.PrimaryIndicator(
                Modifier.tabIndicatorOffset(selectedTab, matchContentSize = true),
                shape = CircleShape
            )
        },
        divider = {
            Divider(modifier = Modifier.fillMaxWidth())
        }
    ) {
        titles.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabClick(index) },
                text = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
internal fun MediaRow(
    item: ScannedMediaFile,
    checked: Boolean,
    thumbnail: String?,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaThumbnail(kind = item.kind, thumbnail = thumbnail)
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = item.path, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Divider(modifier = Modifier.fillMaxWidth())
}

@Composable
private fun MediaThumbnail(kind: MediaKind, thumbnail: String?) {
    val shape = RoundedCornerShape(12.dp)
    var failed by remember(thumbnail) { mutableStateOf(false) }
    if (thumbnail == null || failed) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (kind) {
                    MediaKind.Images -> Icons.Rounded.Image
                    MediaKind.Videos -> Icons.Rounded.Movie
                    MediaKind.Audio -> Icons.Rounded.MusicNote
                },
                contentDescription = null,
            )
        }
    } else {
        AsyncImage(
            model = File(thumbnail),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(shape),
            contentScale = ContentScale.Crop,
            onError = { failed = true },
        )
    }
}
