package com.xayah.databackup.ui.activity.operation.page.packages.backup

import android.content.pm.PackageInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.R
import com.xayah.databackup.data.PackageBackupActivate
import com.xayah.databackup.data.PackageBackupEntire
import com.xayah.databackup.data.PackageBackupUpdate
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.ListItemPackageBackup
import com.xayah.databackup.ui.component.ListTopBar
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.SearchBar
import com.xayah.databackup.ui.component.TopSpacer
import com.xayah.databackup.ui.component.emphasizedOffset
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.token.AnimationTokens
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.DateUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.EnvUtil
import com.xayah.databackup.util.readIconSaveTime
import com.xayah.databackup.util.saveIconSaveTime
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.ExceptionUtil.tryService
import com.xayah.librootservice.util.withIOContext
import kotlinx.coroutines.launch

enum class ListState {
    Idle,
    Update,
    Error,
    Done;

    fun setState(state: ListState): ListState = if (this == Error) Error else state
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PackageBackupList() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<ListViewModel>()
    val navController = LocalSlotScope.current!!.navController
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uiState by viewModel.uiState
    val packages by uiState.packages.collectAsState(initial = listOf())
    var predicate: (PackageBackupEntire) -> Boolean by remember { mutableStateOf({ true }) }
    val selectedAPKs by uiState.selectedAPKs.collectAsState(initial = 0)
    val selectedData by uiState.selectedData.collectAsState(initial = 0)
    val selected = selectedAPKs != 0 || selectedData != 0
    val packageManager = context.packageManager
    var progress by remember { mutableFloatStateOf(1f) }
    var state by remember { mutableStateOf(ListState.Idle) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var emphasizedState by remember { mutableStateOf(false) }
    val emphasizedOffset by emphasizedOffset(targetState = emphasizedState)
    var updatingText: String? by remember { mutableStateOf(null) }

    LaunchedEffect(null) {
        withIOContext {
            val remoteRootService = RemoteRootService(context)

            // Inactivate all packages and activate installed only.
            viewModel.inactivatePackages()
            var installedPackages = listOf<PackageInfo>()
            tryService(onFailed = { msg ->
                scope.launch {
                    state = state.setState(ListState.Error)
                    snackbarHostState.showSnackbar(
                        message = "$msg\n${context.getString(R.string.remote_service_err_info)}",
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }) {
                installedPackages = remoteRootService.getInstalledPackagesAsUser(0, 0)
            }
            val activePackages = mutableListOf<PackageBackupActivate>()
            installedPackages.forEach { packageInfo ->
                activePackages.add(PackageBackupActivate(packageName = packageInfo.packageName, active = true))
            }
            viewModel.activatePackages(activePackages)
            val activatePackagesEndIndex = activePackages.size - 1
            state = state.setState(ListState.Update)

            val newPackages = mutableListOf<PackageBackupUpdate>()
            EnvUtil.createIconDirectory(context)
            // Update packages' info.
            val iconSaveTime = context.readIconSaveTime()
            val now = DateUtil.getTimestamp()
            val hasPassedOneDay = DateUtil.getNumberOfDaysPassed(iconSaveTime, now) >= 1
            if (hasPassedOneDay) context.saveIconSaveTime(now)
            installedPackages.forEachIndexed { index, packageInfo ->
                val iconExists = remoteRootService.exists(PathUtil.getIconPath(context, packageInfo.packageName))
                if (iconExists.not() || (iconExists && hasPassedOneDay)) {
                    val icon = packageInfo.applicationInfo.loadIcon(packageManager)
                    EnvUtil.saveIcon(context, packageInfo.packageName, icon)
                }

                newPackages.add(
                    PackageBackupUpdate(
                        packageName = packageInfo.packageName,
                        label = packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                        versionName = packageInfo.versionName,
                        versionCode = packageInfo.longVersionCode,
                        apkSize = 0,
                        userSize = 0,
                        userDeSize = 0,
                        dataSize = 0,
                        obbSize = 0,
                        mediaSize = 0,
                        firstInstallTime = packageInfo.firstInstallTime,
                        active = true
                    )
                )
                progress = index.toFloat() / activatePackagesEndIndex
                updatingText = "${context.getString(R.string.updating)} (${index + 1}/${activatePackagesEndIndex + 1})"
            }
            viewModel.updatePackages(newPackages)
            state = state.setState(ListState.Done)
            updatingText = null
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                ListTopBar(scrollBehavior = scrollBehavior, title = stringResource(id = R.string.backup_list))
                if (progress != 1F) LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = progress
                )
            }
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
                        else navController.navigate(OperationRoutes.PackageBackupManifest.route)
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
                            text = if (updatingText != null) updatingText!!
                            else "$selectedAPKs ${stringResource(id = R.string.apk)}, $selectedData ${stringResource(id = R.string.data)}"
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
                            items(items = packages.filter(predicate), key = { it.packageName }) { packageInfo ->
                                ListItemPackageBackup(packageInfo = packageInfo)
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
