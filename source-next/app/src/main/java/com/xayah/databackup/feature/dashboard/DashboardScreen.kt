package com.xayah.databackup.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.feature.Backup
import com.xayah.databackup.ui.component.ActionButton
import com.xayah.databackup.ui.component.SmallActionButton
import com.xayah.databackup.ui.component.StorageCard
import com.xayah.databackup.util.navigateSafely

@Composable
fun DashboardScreen(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* do something */ }) {
                        BadgedBox(
                            badge = {
                                Badge()
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_badge_info),
                                contentDescription = "Localized description"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
                            contentDescription = "Localized description"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StorageCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    free = 0.25f,
                    other = 0.5f,
                    backups = 0.25f,
                    title = "Internal storage",
                    subtitle = "/data/media/0/DataBackup",
                    progress = "28%",
                    storage = "52 GB",
                ) {}

                Text(stringResource(R.string.actions), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SmallActionButton(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentSize(),
                        icon = ImageVector.vectorResource(R.drawable.ic_archive),
                        title = stringResource(R.string.backup),
                        subtitle = stringResource(R.string.backup_your_data)
                    ) {
                        navController.navigateSafely(Backup)
                    }

                    SmallActionButton(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentSize(),
                        icon = ImageVector.vectorResource(R.drawable.ic_archive_restore),
                        title = stringResource(R.string.restore),
                        subtitle = stringResource(R.string.restore_your_data)
                    ) {}
                }

                ActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_clock),
                    title = stringResource(R.string.history),
                    subtitle = stringResource(R.string.see_your_previous_backups)
                ) {}

                ActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_cloud_upload),
                    title = stringResource(R.string.cloud),
                    subtitle = stringResource(R.string.set_up_cloud_storage)
                ) {}

                ActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_calendar_check),
                    title = stringResource(R.string.schedule),
                    subtitle = stringResource(R.string.configure_automatic_backups)
                ) {}
            }

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}
