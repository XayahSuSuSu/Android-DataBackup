package com.xayah.databackup.ui.activity.operation.page.packages.restore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.directory.router.DirectoryRoutes
import com.xayah.databackup.ui.activity.main.router.navigateAndPopAllStack
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.GridItemPackage
import com.xayah.databackup.ui.component.ListItemManifestHorizontal
import com.xayah.databackup.ui.component.ListItemManifestVertical
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.ManifestTopBar
import com.xayah.databackup.ui.component.TopSpacer
import com.xayah.databackup.ui.component.material3.SegmentedButton
import com.xayah.databackup.ui.component.material3.SegmentedButtonDefaults
import com.xayah.databackup.ui.component.material3.SingleChoiceSegmentedButtonRow
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.token.AnimationTokens
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.GridItemTokens
import com.xayah.databackup.ui.token.ListItemTokens
import com.xayah.databackup.util.IntentUtil
import com.xayah.databackup.util.readRestoreSavePath

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PackageRestoreManifest() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<ManifestViewModel>()
    val navController = LocalSlotScope.current!!.navController
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val titles = remember {
        listOf(context.getString(R.string.overlook), context.getString(R.string.both), context.getString(R.string.apk), context.getString(R.string.data))
    }
    val uiState by viewModel.uiState
    val selectedBoth by uiState.selectedBoth.collectAsState(initial = 0)
    val selectedAPKs by uiState.selectedAPKs.collectAsState(initial = 0)
    val selectedData by uiState.selectedData.collectAsState(initial = 0)
    val bothPackages by uiState.bothPackages.collectAsState(initial = listOf())
    val apkOnlyPackages by uiState.apkOnlyPackages.collectAsState(initial = listOf())
    val dataOnlyPackages by uiState.dataOnlyPackages.collectAsState(initial = listOf())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ManifestTopBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(R.string.manifest),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = true, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(
                    modifier = Modifier.padding(CommonTokens.PaddingMedium),
                    onClick = {
                        navController.navigateAndPopAllStack(OperationRoutes.PackageRestoreProcessing.route)
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
                    Crossfade(targetState = selectedTabIndex, label = AnimationTokens.CrossFadeLabel) { index ->
                        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = GridItemTokens.ItemWidth)) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    titles.forEachIndexed { index, label ->
                                        SegmentedButton(
                                            shape = SegmentedButtonDefaults.itemShape(index = index, count = titles.size),
                                            onClick = {
                                                selectedTabIndex = index
                                            },
                                            selected = index == selectedTabIndex
                                        ) {
                                            Text(label)
                                        }
                                    }
                                }
                            }

                            when (index) {
                                // Overlook
                                0 -> {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Spacer(modifier = Modifier.height(ListItemTokens.ManifestItemPadding))
                                    }

                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        ListItemManifestHorizontal(
                                            icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_checklist),
                                            title = stringResource(R.string.selected_both),
                                            content = selectedBoth.toString()
                                        ) {
                                            selectedTabIndex = 1
                                        }
                                    }

                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        ListItemManifestHorizontal(
                                            icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_apps),
                                            title = stringResource(R.string.selected_apks),
                                            content = (selectedAPKs - selectedBoth).toString()
                                        ) {
                                            selectedTabIndex = 2
                                        }
                                    }

                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        ListItemManifestHorizontal(
                                            icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_database),
                                            title = stringResource(R.string.selected_data),
                                            content = (selectedData - selectedBoth).toString()
                                        ) {
                                            selectedTabIndex = 3
                                        }
                                    }

                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        val restoreSavePath by context.readRestoreSavePath().collectAsState(initial = "")
                                        ListItemManifestVertical(
                                            icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
                                            title = stringResource(R.string.restore_dir),
                                            content = restoreSavePath
                                        ) {
                                            IntentUtil.toDirectoryActivity(context = context, route = DirectoryRoutes.DirectoryRestore)
                                        }
                                    }
                                }

                                // APK + Data
                                1 -> {
                                    items(items = bothPackages) { item ->
                                        GridItemPackage(item.packageName, item.label)
                                    }
                                }

                                // APK only
                                2 -> {
                                    items(items = apkOnlyPackages) { item ->
                                        GridItemPackage(item.packageName, item.label)
                                    }
                                }

                                // Data only
                                3 -> {
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
