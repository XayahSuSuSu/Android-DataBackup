package com.xayah.feature.main.dashboard

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.common.util.BuildConfigUtil
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.MainIndexSubScaffold
import com.xayah.core.ui.component.Section
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.SegmentProgress
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageDashboard() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val viewModel = hiltViewModel<IndexViewModel>()
    val navController = LocalNavController.current!!
    val lastBackupTime by viewModel.lastBackupTimeState.collectAsStateWithLifecycle()
    val directoryState by viewModel.directoryState.collectAsStateWithLifecycle()
    val nullBackupDir by remember(directoryState) { mutableStateOf(directoryState == null) }

    MainIndexSubScaffold(
        scrollBehavior = scrollBehavior,
        title = StringResourceToken.fromStringArgs(
            StringResourceToken.fromStringId(R.string.app_name),
            StringResourceToken.fromString(" ${BuildConfigUtil.VERSION_NAME}"),
        ),
        actions = {
            IconButton(
                icon = ImageVectorToken.fromVector(Icons.Outlined.Settings),
                onClick = {
                    navController.navigate(MainRoutes.Settings.route)
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
            Section(title = StringResourceToken.fromStringId(R.string.overlook)) {
                OverviewLastBackupCard(nullBackupDir = nullBackupDir, lastBackupTime = lastBackupTime) {
                    if (nullBackupDir)
                        navController.navigate(MainRoutes.Directory.route)
                }

                if (directoryState != null) {
                    OverviewStorageCard(
                        StringResourceToken.fromString(directoryState!!.title),
                        SegmentProgress(used = directoryState!!.usedBytes, total = directoryState!!.totalBytes),
                        SegmentProgress(used = directoryState!!.childUsedBytes, total = directoryState!!.totalBytes),
                    ) {
                        navController.navigate(MainRoutes.Directory.route)
                    }
                }
            }

            Section(title = StringResourceToken.fromStringId(R.string.quick_actions)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                    maxItemsInEachRow = 2,
                ) {
                    QuickActionsButton(
                        modifier = Modifier.weight(1f),
                        enabled = nullBackupDir.not(),
                        title = StringResourceToken.fromStringId(R.string.backup_apps),
                        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_acute),
                        colorContainer = ColorSchemeKeyTokens.RedPrimaryContainer,
                        colorL80D20 = ColorSchemeKeyTokens.RedL80D20,
                        onColorContainer = ColorSchemeKeyTokens.RedOnPrimaryContainer
                    )
                    QuickActionsButton(
                        modifier = Modifier.weight(1f),
                        enabled = nullBackupDir.not(),
                        title = StringResourceToken.fromStringId(R.string.backup_files),
                        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_acute),
                        colorContainer = ColorSchemeKeyTokens.YellowPrimaryContainer,
                        colorL80D20 = ColorSchemeKeyTokens.YellowL80D20,
                        onColorContainer = ColorSchemeKeyTokens.YellowOnPrimaryContainer
                    )
                    QuickActionsButton(
                        modifier = Modifier.weight(1f),
                        enabled = nullBackupDir.not(),
                        title = StringResourceToken.fromStringId(R.string.backup_messages),
                        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_acute),
                        colorContainer = ColorSchemeKeyTokens.BluePrimaryContainer,
                        colorL80D20 = ColorSchemeKeyTokens.BlueL80D20,
                        onColorContainer = ColorSchemeKeyTokens.BlueOnPrimaryContainer
                    )
                    QuickActionsButton(
                        modifier = Modifier.weight(1f),
                        enabled = nullBackupDir.not(),
                        title = StringResourceToken.fromStringId(R.string.backup_contacts),
                        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_acute),
                        colorContainer = ColorSchemeKeyTokens.GreenPrimaryContainer,
                        colorL80D20 = ColorSchemeKeyTokens.GreenL80D20,
                        onColorContainer = ColorSchemeKeyTokens.GreenOnPrimaryContainer
                    )
                    QuickActionsButton(
                        modifier = Modifier.weight(1f),
                        enabled = nullBackupDir.not(),
                        title = StringResourceToken.fromStringId(R.string.sync_to_cloud),
                        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_acute),
                        colorContainer = ColorSchemeKeyTokens.PrimaryContainer,
                        colorL80D20 = ColorSchemeKeyTokens.PrimaryL80D20,
                        onColorContainer = ColorSchemeKeyTokens.OnPrimaryContainer,
                        actionIcon = ImageVectorToken.fromVector(Icons.Rounded.KeyboardArrowRight)
                    )
                    QuickActionsButton(
                        modifier = Modifier.weight(1f),
                        enabled = nullBackupDir.not(),
                        title = StringResourceToken.fromStringId(R.string.restore),
                        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_history),
                        colorContainer = ColorSchemeKeyTokens.SecondaryContainer,
                        colorL80D20 = ColorSchemeKeyTokens.SecondaryL80D20,
                        onColorContainer = ColorSchemeKeyTokens.OnSecondaryContainer,
                        actionIcon = ImageVectorToken.fromVector(Icons.Rounded.KeyboardArrowRight)
                    )
                }
            }
        }
    }
}
