package com.xayah.feature.main.dashboard

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.common.util.BuildConfigUtil
import com.xayah.core.model.OpType
import com.xayah.core.model.Target
import com.xayah.core.ui.component.DismissState
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.MainIndexSubScaffold
import com.xayah.core.ui.component.Section
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.model.SegmentProgress
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.navigateSingle
import kotlinx.coroutines.launch

@SuppressLint("StringFormatInvalid")
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageDashboard() {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = LocalNavController.current!!
    val lastBackupTime by viewModel.lastBackupTimeState.collectAsStateWithLifecycle()
    val directoryState by viewModel.directoryState.collectAsStateWithLifecycle()
    val nullBackupDir by remember(directoryState) { mutableStateOf(directoryState == null) }
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val scope = rememberCoroutineScope()

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(IndexUiIntent.Update)
    }

    MainIndexSubScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = stringResource(id = R.string.app_name),
        updateAvailable = uiState.latestRelease != null,
        onVersionChipClick = {
            scope.launch {
                val state = dialogState.open(
                    initialState = false,
                    title = context.getString(R.string.update_available),
                    icon = null,
                    dismissText = context.getString(R.string.changelog),
                    confirmText = context.getString(R.string.download),
                    block = { _ -> Text(text = context.getString(R.string.args_update_from, BuildConfigUtil.VERSION_NAME, uiState.latestRelease?.name)) }
                ).first
                when (state) {
                    DismissState.CONFIRM -> {
                        uiState.latestRelease?.assets?.firstOrNull { it.url.contains(BuildConfigUtil.FLAVOR_feature) && it.url.contains(BuildConfigUtil.FLAVOR_abi) }?.apply {
                            viewModel.emitIntent(IndexUiIntent.ToBrowser(context = context, url = this.url))
                        }
                    }

                    DismissState.CANCEL -> {
                        uiState.latestRelease?.url?.apply {
                            viewModel.emitIntent(IndexUiIntent.ToBrowser(context = context, url = this))
                        }
                    }

                    DismissState.DISMISS -> {}
                }
            }
        },
        actions = {
            IconButton(
                enabled = nullBackupDir.not(),
                icon = Icons.Outlined.Settings,
                onClick = {
                    navController.navigateSingle(MainRoutes.Settings.route)
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .paddingTop(SizeTokens.Level8)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
        ) {
            Section(title = stringResource(id = R.string.overlook)) {
                OverviewLastBackupCard(nullBackupDir = nullBackupDir, lastBackupTime = lastBackupTime) {
                    if (nullBackupDir)
                        navController.navigateSingle(MainRoutes.Directory.route)
                }

                if (directoryState != null) {
                    OverviewStorageCard(
                        stringResource(id = directoryState!!.titleResId),
                        SegmentProgress(used = directoryState!!.usedBytes, total = directoryState!!.totalBytes),
                        SegmentProgress(used = directoryState!!.childUsedBytes, total = directoryState!!.totalBytes),
                    ) {
                        navController.navigateSingle(MainRoutes.Directory.route)
                    }
                }
            }

            Section(title = stringResource(id = R.string.quick_actions)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                    maxItemsInEachRow = 2,
                ) {
                    QuickActionsButton(
                        modifier = Modifier.weight(1f),
                        enabled = nullBackupDir.not(),
                        title = stringResource(id = R.string.backup_apps),
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_acute),
                        colorContainer = ThemedColorSchemeKeyTokens.RedPrimaryContainer,
                        colorL80D20 = ThemedColorSchemeKeyTokens.RedL80D20,
                        onColorContainer = ThemedColorSchemeKeyTokens.RedOnPrimaryContainer
                    ) {
                        navController.navigateSingle(MainRoutes.List.getRoute(target = Target.Apps, opType = OpType.BACKUP))
                    }
                    QuickActionsButton(
                        modifier = Modifier.weight(1f),
                        enabled = nullBackupDir.not(),
                        title = stringResource(id = R.string.backup_files),
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_acute),
                        colorContainer = ThemedColorSchemeKeyTokens.YellowPrimaryContainer,
                        colorL80D20 = ThemedColorSchemeKeyTokens.YellowL80D20,
                        onColorContainer = ThemedColorSchemeKeyTokens.YellowOnPrimaryContainer
                    ) {
                        navController.navigateSingle(MainRoutes.List.getRoute(target = Target.Files, opType = OpType.BACKUP))
                    }
                    // TODO MMS/SMS, Contacts backup/restore
//                    QuickActionsButton(
//                        modifier = Modifier.weight(1f),
//                        enabled = false,
//                        title = stringResource(id = R.string.backup_messages),
//                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_acute),
//                        colorContainer = ThemedColorSchemeKeyTokens.BluePrimaryContainer,
//                        colorL80D20 = ThemedColorSchemeKeyTokens.BlueL80D20,
//                        onColorContainer = ThemedColorSchemeKeyTokens.BlueOnPrimaryContainer
//                    )
//                    QuickActionsButton(
//                        modifier = Modifier.weight(1f),
//                        enabled = false,
//                        title = stringResource(id = R.string.backup_contacts),
//                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_acute),
//                        colorContainer = ThemedColorSchemeKeyTokens.GreenPrimaryContainer,
//                        colorL80D20 = ThemedColorSchemeKeyTokens.GreenL80D20,
//                        onColorContainer = ThemedColorSchemeKeyTokens.GreenOnPrimaryContainer
//                    )
                    QuickActionsButton(
                        modifier = Modifier.weight(1f),
                        enabled = nullBackupDir.not(),
                        title = stringResource(id = R.string.cloud),
                        icon = Icons.Outlined.Cloud,
                        colorContainer = ThemedColorSchemeKeyTokens.PurplePrimaryContainer,
                        colorL80D20 = ThemedColorSchemeKeyTokens.PurpleL80D20,
                        onColorContainer = ThemedColorSchemeKeyTokens.PurpleOnPrimaryContainer,
                        actionIcon = Icons.Rounded.KeyboardArrowRight
                    ) {
                        navController.navigateSingle(MainRoutes.Cloud.route)
                    }
                    QuickActionsButton(
                        modifier = Modifier.weight(1f),
                        enabled = nullBackupDir.not(),
                        title = stringResource(id = R.string.restore),
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_history),
                        colorContainer = ThemedColorSchemeKeyTokens.OrangePrimaryContainer,
                        colorL80D20 = ThemedColorSchemeKeyTokens.OrangeL80D20,
                        onColorContainer = ThemedColorSchemeKeyTokens.OrangeOnPrimaryContainer,
                        actionIcon = Icons.Rounded.KeyboardArrowRight
                    ) {
                        navController.navigateSingle(MainRoutes.Restore.route)
                    }
                    QuickActionsButton(
                        modifier = Modifier.weight(1f),
                        enabled = nullBackupDir.not(),
                        title = stringResource(R.string.history),
                        icon = Icons.Rounded.ListAlt,
                        colorContainer = ThemedColorSchemeKeyTokens.PinkPrimaryContainer,
                        colorL80D20 = ThemedColorSchemeKeyTokens.PinkL80D20,
                        onColorContainer = ThemedColorSchemeKeyTokens.PinkOnPrimaryContainer,
                        actionIcon = Icons.Rounded.KeyboardArrowRight
                    ) {
                        navController.navigateSingle(MainRoutes.History.route)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
