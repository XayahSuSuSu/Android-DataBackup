package com.xayah.feature.main.packages.redesigned.backup

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.ScrollBar
import com.xayah.core.ui.component.SearchBar
import com.xayah.core.ui.material3.pullrefresh.PullRefreshIndicator
import com.xayah.core.ui.material3.pullrefresh.pullRefresh
import com.xayah.core.ui.material3.pullrefresh.rememberPullRefreshState
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.feature.main.packages.R
import com.xayah.feature.main.packages.redesigned.DotLottieView
import com.xayah.feature.main.packages.redesigned.FilterModalBottomSheetContent
import com.xayah.feature.main.packages.redesigned.ListScaffold
import com.xayah.feature.main.packages.redesigned.PackageItem

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageBackupList() {
    val context = LocalContext.current
    val navController = LocalNavController.current!!
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    val packagesState by viewModel.packagesState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollState = rememberLazyListState()
    val srcPackagesEmptyState by viewModel.srcPackagesEmptyState.collectAsStateWithLifecycle()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = { viewModel.emitIntent(IndexUiIntent.OnRefresh) })
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    ListScaffold(
        scrollBehavior = scrollBehavior,
        title = StringResourceToken.fromStringId(R.string.select_apps),
        actions = {
            if (isRefreshing.not() && srcPackagesEmptyState.not()) {
                IconButton(icon = ImageVectorToken.fromVector(Icons.Outlined.FilterAlt)) { showBottomSheet = true }
            }
        }
    ) {
        if (isRefreshing || srcPackagesEmptyState) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DotLottieView(isRefreshing = isRefreshing, refreshState = refreshState)
                    }
                }
                InnerBottomSpacer(innerPadding = it)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState),
                state = scrollState,
            ) {
                item {
                    SearchBar(
                        modifier = Modifier.padding(SizeTokens.Level16),
                        enabled = true,
                        placeholder = StringResourceToken.fromStringId(R.string.search_bar_hint_packages),
                        onTextChange = {
                            viewModel.emitIntent(IndexUiIntent.FilterByKey(key = it))
                        }
                    )
                }

                items(items = packagesState, key = { it.id }) { item ->
                    Row(modifier = Modifier.animateItemPlacement()) {
                        PackageItem(
                            item = item,
                            onCheckedChange = { viewModel.emitIntent(IndexUiIntent.Select(item)) },
                            onClick = {}
                        )
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState, windowInsets = WindowInsets(0, 0, 0, 0)) {
                FilterModalBottomSheetContent(onDismissRequest = { showBottomSheet = false })
                InnerBottomSpacer(innerPadding = it)
                InnerBottomSpacer(innerPadding = it)
                InnerBottomSpacer(innerPadding = it)
                InnerBottomSpacer(innerPadding = it)
            }
        }

        PullRefreshIndicator(refreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter))
        ScrollBar(modifier = Modifier.align(Alignment.TopEnd), state = scrollState, listSize = packagesState.size)
    }
}
