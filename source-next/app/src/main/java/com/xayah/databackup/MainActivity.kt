package com.xayah.databackup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xayah.databackup.ui.component.ActionButton
import com.xayah.databackup.ui.component.SmallActionButton
import com.xayah.databackup.ui.component.StorageCard
import com.xayah.databackup.ui.theme.DataBackupTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DataBackupTheme {
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    "DataBackup",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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

                            Text("Actions", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                SmallActionButton(
                                    modifier = Modifier
                                        .weight(1f)
                                        .wrapContentSize(),
                                    icon = ImageVector.vectorResource(R.drawable.ic_archive),
                                    title = "Backup",
                                    subtitle = "Backup your data"
                                ) {}

                                SmallActionButton(
                                    modifier = Modifier
                                        .weight(1f)
                                        .wrapContentSize(),
                                    icon = ImageVector.vectorResource(R.drawable.ic_archive_restore),
                                    title = "Restore",
                                    subtitle = "Restore your data"
                                ) {}
                            }

                            ActionButton(
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                                    .wrapContentSize(),
                                icon = ImageVector.vectorResource(R.drawable.ic_clock),
                                title = "History",
                                subtitle = "See your previous backups"
                            ) {}

                            ActionButton(
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                                    .wrapContentSize(),
                                icon = ImageVector.vectorResource(R.drawable.ic_cloud_upload),
                                title = "Cloud",
                                subtitle = "Set-up cloud storage"
                            ) {}

                            ActionButton(
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                                    .wrapContentSize(),
                                icon = ImageVector.vectorResource(R.drawable.ic_calendar_check),
                                title = "Schedule",
                                subtitle = "Configure automatic backups"
                            ) {}
                        }

                        Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
                    }
                }
            }
        }
    }
}
