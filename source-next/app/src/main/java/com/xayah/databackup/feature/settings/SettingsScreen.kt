package com.xayah.databackup.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.CustomSUFileDialog
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.SmallActionButton
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.ShellHelper
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers

@Composable
fun SettingsScreen(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var openCustomSUFileDialog by remember { mutableStateOf(false) }
    if (openCustomSUFileDialog) {
        CustomSUFileDialog {
            openCustomSUFileDialog = false
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = ImageVector.vectorResource(R.drawable.ic_archive),
                    title = stringResource(R.string.model),
                    subtitle = Build.MODEL
                )

                SettingsInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = ImageVector.vectorResource(R.drawable.ic_archive_restore),
                    title = stringResource(R.string.abi),
                    subtitle = Build.SUPPORTED_ABIS.firstOrNull() ?: stringResource(R.string.unknown)
                )
            }

            val context = LocalContext.current
            var rootSummary by remember { mutableStateOf(context.getString(R.string.unknown)) }
            var rootSummaryLoading by remember { mutableStateOf(true) }

            LaunchedEffect(context = Dispatchers.IO, context) {
                rootSummaryLoading = true
                rootSummary = ShellHelper.getSuVersion() ?: context.getString(R.string.unknown)
                rootSummaryLoading = false
            }
            Preference(
                icon = ImageVector.vectorResource(R.drawable.ic_hash),
                title = stringResource(R.string.root),
                subtitle = rootSummary,
                subtitleShimmer = rootSummaryLoading,
                onClick = { openCustomSUFileDialog = true },
                slot = {
                    IconButton(onClick = { openCustomSUFileDialog = true }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.custom_su_file)
                        )
                    }
                }
            )

            SectionHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = stringResource(R.string.info),
            )

            SettingsEntry(
                icon = ImageVector.vectorResource(R.drawable.ic_palette),
                title = stringResource(R.string.appearance),
                subtitle = stringResource(R.string.app_theme_settings)
            )

            SettingsEntry(
                icon = ImageVector.vectorResource(R.drawable.ic_archive),
                title = stringResource(R.string.backup),
                subtitle = stringResource(R.string.backup_settings)
            )

            SettingsEntry(
                icon = ImageVector.vectorResource(R.drawable.ic_archive_restore),
                title = stringResource(R.string.restore),
                subtitle = stringResource(R.string.restore_settings)
            )

            SettingsEntry(
                icon = ImageVector.vectorResource(R.drawable.ic_wrench),
                title = stringResource(R.string.advanced),
                subtitle = stringResource(R.string.advanced_settings)
            )

            SettingsEntry(
                icon = ImageVector.vectorResource(R.drawable.ic_layout_grid),
                title = stringResource(R.string.about),
                subtitle = BuildConfig.VERSION_NAME
            )

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun SettingsInfoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    SmallActionButton(
        modifier = modifier,
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = {}
    )
}

@Composable
private fun SettingsEntry(icon: ImageVector, title: String, subtitle: String) {
    Preference(
        icon = icon,
        title = title,
        subtitle = subtitle,
        slot = {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right),
                contentDescription = null
            )
        },
        onClick = {}
    )
}
