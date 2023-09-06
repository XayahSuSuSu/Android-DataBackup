package com.xayah.databackup.ui.activity.operation.page.packages.restore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.R
import com.xayah.databackup.data.PackageRestoreEntire
import com.xayah.databackup.ui.activity.operation.page.packages.backup.ListState
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.ListItemPackageRestore
import com.xayah.databackup.ui.component.ListTopBar
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.ModalStringListDropdownMenu
import com.xayah.databackup.ui.component.SearchBar
import com.xayah.databackup.ui.component.TopSpacer
import com.xayah.databackup.ui.component.emphasizedOffset
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.token.AnimationTokens
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.DateUtil
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.ExceptionUtil.tryService
import com.xayah.librootservice.util.withIOContext
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PackageRestoreList() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = hiltViewModel<ListViewModel>()
    val navController = LocalSlotScope.current!!.navController
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uiState by viewModel.uiState
    val packages by uiState.packages.collectAsState(initial = listOf())
    var predicate: (PackageRestoreEntire) -> Boolean by remember { mutableStateOf({ true }) }
    val selectedAPKs by uiState.selectedAPKs.collectAsState(initial = 0)
    val selectedData by uiState.selectedData.collectAsState(initial = 0)
    val selected = selectedAPKs != 0 || selectedData != 0
    var state by remember { mutableStateOf(ListState.Idle) }
    val snackbarHostState = remember { SnackbarHostState() }
    var emphasizedState by remember { mutableStateOf(false) }
    val emphasizedOffset by emphasizedOffset(targetState = emphasizedState)

    LaunchedEffect(null) {
        withIOContext {
            val remoteRootService = RemoteRootService(context)
            tryService(onFailed = { msg ->
                scope.launch {
                    state = state.setState(ListState.Error)
                    snackbarHostState.showSnackbar(
                        message = "$msg\n${context.getString(R.string.remote_service_err_info)}",
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }) {
                remoteRootService.testService()
            }
            viewModel.initializeUiState()
            state = state.setState(ListState.Done)
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ListTopBar(scrollBehavior = scrollBehavior, title = stringResource(id = R.string.restore_list))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(visible = state != ListState.Idle, enter = scaleIn(), exit = scaleOut()) {
                ExtendedFloatingActionButton(
                    modifier = Modifier
                        .padding(CommonTokens.PaddingMedium)
                        .offset(x = emphasizedOffset),
                    onClick = {
                        if (selected.not() || state == ListState.Error) emphasizedState = !emphasizedState
                        else navController.navigate(OperationRoutes.PackageRestoreManifest.route)
                    },
                    expanded = selected || state != ListState.Done,
                    icon = {
                        Icon(
                            imageVector = if (state != ListState.Done) Icons.Rounded.Refresh else if (selected) Icons.Rounded.ArrowForward else Icons.Rounded.Close,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = "$selectedAPKs ${stringResource(id = R.string.apk)}, $selectedData ${stringResource(id = R.string.data)}"
                        )
                    },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Column {
            TopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = state, label = AnimationTokens.CrossFadeLabel) { state ->
                    if (state != ListState.Idle)
                        LazyColumn(
                            modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
                            verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(CommonTokens.PaddingMedium))
                                SearchBar(onTextChange = { text ->
                                    predicate = { packageBackupEntire ->
                                        packageBackupEntire.label.lowercase().contains(text.lowercase())
                                                || packageBackupEntire.packageName.lowercase().contains(text.lowercase())
                                    }
                                })
                            }

                            item {
                                var expanded by remember { mutableStateOf(false) }
                                val dateList = uiState.timestamps.map { timestamp -> DateUtil.formatTimestamp(timestamp) }
                                val selectedIndex = uiState.selectedIndex

                                Box(
                                    modifier = Modifier.wrapContentSize(Alignment.TopStart)
                                ) {
                                    AssistChip(
                                        onClick = { if (dateList.isNotEmpty()) expanded = true },
                                        label = { Text(stringResource(R.string.date)) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_rounded_unfold_more),
                                                contentDescription = null,
                                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                                            )
                                        }
                                    )

                                    ModalStringListDropdownMenu(
                                        expanded = expanded,
                                        selectedIndex = selectedIndex,
                                        list = dateList,
                                        maxDisplay = 6,
                                        onSelected = { index, _ ->
                                            scope.launch {
                                                expanded = false
                                                viewModel.setSelectedIndex(index)
                                            }
                                        },
                                        onDismissRequest = { expanded = false })
                                }
                            }

                            items(items = packages.filter(predicate), key = { it.packageName }) { packageInfo ->
                                ListItemPackageRestore(packageInfo = packageInfo)
                            }
                            item {
                                Spacer(modifier = Modifier.height(CommonTokens.None))
                            }
                        }
                }
            }
        }
    }
}
