package com.xayah.databackup.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.CustomSUFileDialog
import com.xayah.databackup.ui.component.LocalFloatingNavigationBarBottomPadding
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.PreferenceGroup
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.ShellHelper
import kotlinx.coroutines.Dispatchers

@Composable
fun SettingsScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val floatingNavigationBarBottomPadding = LocalFloatingNavigationBarBottomPadding.current
    var openCustomSUFileDialog by remember { mutableStateOf(false) }
    val unknown = stringResource(R.string.unknown)
    var rootSummary by remember(unknown) { mutableStateOf(unknown) }
    var rootSummaryLoading by remember { mutableStateOf(true) }

    LaunchedEffect(context = Dispatchers.IO, unknown) {
        rootSummaryLoading = true
        rootSummary = ShellHelper.getSuVersion() ?: unknown
        rootSummaryLoading = false
    }

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
                colors = TopAppBarDefaults.defaultLargeTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            SettingsOverviewCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                rootSummary = rootSummary,
                rootSummaryLoading = rootSummaryLoading,
                onRootClick = { openCustomSUFileDialog = true },
            )

            SectionHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = stringResource(R.string.application),
            )

            SettingsApplicationCard(modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding() + floatingNavigationBarBottomPadding))
        }
    }
}

@Composable
private fun SettingsOverviewCard(
    modifier: Modifier = Modifier,
    rootSummary: String,
    rootSummaryLoading: Boolean,
    onRootClick: () -> Unit,
) {
    PreferenceGroup(modifier = modifier) {
        Preference(
            icon = ImageVector.vectorResource(R.drawable.ic_smartphone),
            title = stringResource(R.string.model),
            subtitle = Build.MODEL,
        )
        Preference(
            icon = ImageVector.vectorResource(R.drawable.ic_cpu),
            title = stringResource(R.string.abi),
            subtitle = Build.SUPPORTED_ABIS.firstOrNull() ?: stringResource(R.string.unknown),
        )
        Preference(
            icon = ImageVector.vectorResource(R.drawable.ic_hash),
            title = stringResource(R.string.root),
            subtitle = rootSummary,
            subtitleShimmer = rootSummaryLoading,
            onClick = onRootClick,
            slot = {
                IconButton(onClick = onRootClick) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.custom_su_file),
                    )
                }
            },
        )
    }
}

@Composable
private fun SettingsApplicationCard(modifier: Modifier = Modifier) {
    PreferenceGroup(modifier = modifier) {
        SettingsEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_palette),
            title = stringResource(R.string.appearance),
            subtitle = stringResource(R.string.app_theme_settings),
        )
        SettingsEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_archive),
            title = stringResource(R.string.backup),
            subtitle = stringResource(R.string.backup_settings),
        )
        SettingsEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_archive_restore),
            title = stringResource(R.string.restore),
            subtitle = stringResource(R.string.restore_settings),
        )
        SettingsEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_wrench),
            title = stringResource(R.string.advanced),
            subtitle = stringResource(R.string.advanced_settings),
        )
        SettingsEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_layout_grid),
            title = stringResource(R.string.about),
            subtitle = BuildConfig.VERSION_NAME,
        )
    }
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
