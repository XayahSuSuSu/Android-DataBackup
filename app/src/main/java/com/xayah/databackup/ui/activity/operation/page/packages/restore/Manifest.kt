package com.xayah.databackup.ui.activity.operation.page.packages.restore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.GridItemPackage
import com.xayah.databackup.ui.component.ListItemManifest
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.ManifestTopBar
import com.xayah.databackup.ui.component.TopSpacer
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingVertical
import com.xayah.databackup.ui.token.AnimationTokens
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.GridItemTokens

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PackageRestoreManifest() {
    val viewModel = hiltViewModel<ManifestViewModel>()
    val navController = LocalSlotScope.current!!.navController
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val selectedTabIndex = remember { mutableIntStateOf(0) }
    val titles =
        listOf(stringResource(id = R.string.overlook), stringResource(R.string.both), stringResource(id = R.string.apk), stringResource(id = R.string.data))
    val uiState = viewModel.uiState.value
    val selectedAPKs by uiState.selectedAPKs.collectAsState(initial = 0)
    val selectedData by uiState.selectedData.collectAsState(initial = 0)
    val bothPackages by uiState.bothPackages.collectAsState(initial = listOf())
    val apkOnlyPackages by uiState.apkOnlyPackages.collectAsState(initial = listOf())
    val dataOnlyPackages by uiState.dataOnlyPackages.collectAsState(initial = listOf())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ManifestTopBar(scrollBehavior = scrollBehavior, title = stringResource(R.string.manifest), selectedTabIndex = selectedTabIndex, titles = titles)
        },
        floatingActionButton = {
            AnimatedVisibility(visible = true, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(
                    modifier = Modifier.padding(CommonTokens.PaddingMedium),
                    onClick = {
                        navController.navigate(OperationRoutes.PackageRestoreProcessing.route)
                    },
                    content = {
                        Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null)
                    }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Column {
            TopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium)) {
                    Crossfade(targetState = selectedTabIndex.intValue, label = AnimationTokens.CrossFadeLabel) { index ->
                        when (index) {
                            // Overlook
                            0 -> {
                                Column(
                                    modifier = Modifier.paddingVertical(CommonTokens.PaddingMedium),
                                    verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
                                ) {
                                    ListItemManifest(
                                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_apps),
                                        title = stringResource(R.string.selected_apks),
                                        content = selectedAPKs.toString()
                                    ) {
                                        selectedTabIndex.intValue = 2
                                    }
                                    ListItemManifest(
                                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_database),
                                        title = stringResource(R.string.selected_data),
                                        content = selectedData.toString()
                                    ) {
                                        selectedTabIndex.intValue = 3
                                    }
                                }
                            }

                            // APK + Data
                            1 -> {
                                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = GridItemTokens.ItemWidth)) {
                                    items(items = bothPackages) { item ->
                                        GridItemPackage(item.packageName, item.label)
                                    }
                                }
                            }

                            // APK only
                            2 -> {
                                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = GridItemTokens.ItemWidth)) {
                                    items(items = apkOnlyPackages) { item ->
                                        GridItemPackage(item.packageName, item.label)
                                    }
                                }
                            }

                            // Data only
                            3 -> {
                                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = GridItemTokens.ItemWidth)) {
                                    items(items = dataOnlyPackages) { item ->
                                        GridItemPackage(item.packageName, item.label)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
