package com.xayah.databackup.feature.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.ProcessItem
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupProcessScreen(
    navController: NavHostController,
    viewModel: BackupProcessViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val appsItem by viewModel.appsItem.collectAsStateWithLifecycle()
    val filesItem by viewModel.filesItem.collectAsStateWithLifecycle()
    val networksItem by viewModel.networksItem.collectAsStateWithLifecycle()
    val contactsItem by viewModel.contactsItem.collectAsStateWithLifecycle()
    val callLogsItem by viewModel.callLogsItem.collectAsStateWithLifecycle()
    val messagesItem by viewModel.messagesItem.collectAsStateWithLifecycle()

    LaunchedEffect(context = Dispatchers.IO, null) {
        viewModel.loadProcessItems()
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStackSafely() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.defaultLargeTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            Row(
                modifier = Modifier.padding(start = 16.dp, top = 40.dp, bottom = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "50",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Backing up",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                ContainedLoadingIndicator(modifier = Modifier.size(64.dp))
            }

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.size(0.dp))

                AnimatedVisibility(visible = appsItem.isSelected) {
                    ProcessItem(
                        modifier = Modifier.fillMaxWidth(),
                        icon = ImageVector.vectorResource(R.drawable.ic_layout_grid),
                        title = stringResource(R.string.apps),
                        currentIndex = appsItem.currentIndex,
                        totalCount = appsItem.totalCount,
                        subtitle = appsItem.msg,
                        subtitleShimmer = appsItem.isLoading,
                        process = { appsItem.progress },
                        onIconBtnClick = {},
                        onClick = {}
                    )
                }

                AnimatedVisibility(visible = filesItem.isSelected) {
                    ProcessItem(
                        modifier = Modifier.fillMaxWidth(),
                        icon = ImageVector.vectorResource(R.drawable.ic_folder),
                        title = stringResource(R.string.files),
                        currentIndex = filesItem.currentIndex,
                        totalCount = filesItem.totalCount,
                        subtitle = filesItem.msg,
                        subtitleShimmer = filesItem.isLoading,
                        process = { filesItem.progress },
                        onIconBtnClick = {},
                        onClick = {}
                    )
                }

                AnimatedVisibility(visible = networksItem.isSelected) {
                    ProcessItem(
                        modifier = Modifier.fillMaxWidth(),
                        icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                        title = stringResource(R.string.networks),
                        currentIndex = networksItem.currentIndex,
                        totalCount = networksItem.totalCount,
                        subtitle = networksItem.msg,
                        subtitleShimmer = networksItem.isLoading,
                        process = { networksItem.progress },
                        onIconBtnClick = {},
                        onClick = {}
                    )
                }

                AnimatedVisibility(visible = contactsItem.isSelected) {
                    ProcessItem(
                        modifier = Modifier.fillMaxWidth(),
                        icon = ImageVector.vectorResource(R.drawable.ic_user_round),
                        title = stringResource(R.string.contacts),
                        currentIndex = contactsItem.currentIndex,
                        totalCount = contactsItem.totalCount,
                        subtitle = contactsItem.msg,
                        subtitleShimmer = contactsItem.isLoading,
                        process = { contactsItem.progress },
                        onIconBtnClick = {},
                        onClick = {}
                    )
                }

                AnimatedVisibility(visible = callLogsItem.isSelected) {
                    ProcessItem(
                        modifier = Modifier.fillMaxWidth(),
                        icon = ImageVector.vectorResource(R.drawable.ic_phone),
                        title = stringResource(R.string.call_logs),
                        currentIndex = callLogsItem.currentIndex,
                        totalCount = callLogsItem.totalCount,
                        subtitle = callLogsItem.msg,
                        subtitleShimmer = callLogsItem.isLoading,
                        process = { callLogsItem.progress },
                        onIconBtnClick = {},
                        onClick = {}
                    )
                }

                AnimatedVisibility(visible = messagesItem.isSelected) {
                    ProcessItem(
                        modifier = Modifier.fillMaxWidth(),
                        icon = ImageVector.vectorResource(R.drawable.ic_message_circle),
                        title = stringResource(R.string.messages),
                        currentIndex = messagesItem.currentIndex,
                        totalCount = messagesItem.totalCount,
                        subtitle = messagesItem.msg,
                        subtitleShimmer = messagesItem.isLoading,
                        process = { messagesItem.progress },
                        onIconBtnClick = {},
                        onClick = {}
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))
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
                    enabled = true,
                    onClick = {
                    }
                ) {
                    Text(text = "Stop")
                }
            }

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}
