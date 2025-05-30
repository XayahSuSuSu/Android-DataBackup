package com.xayah.databackup.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
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
import com.xayah.databackup.R
import com.xayah.databackup.feature.BackupApps
import com.xayah.databackup.ui.component.SelectableActionButton
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.popBackStackSafely

@Composable
fun BackupPreviewScreen(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.backup),
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
        Column(modifier = Modifier) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(modifier = Modifier.height(0.dp))

                SelectableActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_layout_grid),
                    title = "Apps",
                    subtitle = "13/79 items selected"
                ) {
                    navController.navigateSafely(BackupApps)
                }

                SelectableActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_folder),
                    title = "Files",
                    subtitle = "13/79 items selected"
                ) {}

                SelectableActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                    title = "Networks",
                    subtitle = "No items selected"
                ) {}

                SelectableActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_user_round),
                    title = "Contacts",
                    subtitle = "No items selected"
                ) {}

                SelectableActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_message_circle),
                    title = "Messages",
                    subtitle = "No items selected"
                ) {}

                SelectableActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_phone),
                    title = "Call logs",
                    subtitle = "No items selected"
                ) {}

                Spacer(modifier = Modifier.height(0.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Button(
                    modifier = Modifier.wrapContentSize(),
                    onClick = { }
                ) {
                    Text(text = stringResource(R.string.next))
                }
            }

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}
