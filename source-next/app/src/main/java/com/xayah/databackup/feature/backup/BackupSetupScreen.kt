package com.xayah.databackup.feature.backup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.feature.BackupApps
import com.xayah.databackup.feature.BackupCallLogs
import com.xayah.databackup.feature.BackupContacts
import com.xayah.databackup.feature.BackupMessages
import com.xayah.databackup.feature.BackupNetworks
import com.xayah.databackup.feature.BackupProcess
import com.xayah.databackup.ui.component.ActionButtonState
import com.xayah.databackup.ui.component.AutoScreenOffSwitch
import com.xayah.databackup.ui.component.IncrementalBackupAndCleanBackupSwitches
import com.xayah.databackup.ui.component.ResetBackupListSwitch
import com.xayah.databackup.ui.component.SelectableCardButton
import com.xayah.databackup.ui.component.SmallCheckActionButton
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.ui.component.horizontalFadingEdges
import com.xayah.databackup.ui.component.rememberCallLogPermissionsState
import com.xayah.databackup.ui.component.rememberContactPermissionsState
import com.xayah.databackup.ui.component.rememberMessagePermissionsState
import com.xayah.databackup.ui.component.selectableCardButtonSecondaryColors
import com.xayah.databackup.ui.component.selectableCardButtonTertiaryColors
import com.xayah.databackup.ui.component.shimmer
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.AppsOptionSelectedBackup
import com.xayah.databackup.util.CallLogsOptionSelectedBackup
import com.xayah.databackup.util.ContactsOptionSelectedBackup
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.MessagesOptionSelectedBackup
import com.xayah.databackup.util.NetworksOptionSelectedBackup
import com.xayah.databackup.util.items
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.popBackStackSafely
import com.xayah.databackup.util.saveBoolean
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupSetupScreen(
    navController: NavHostController,
    viewModel: BackupSetupViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle(null)
    val nextBtnEnabled by viewModel.nextBtnEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(context = Dispatchers.IO, null) {
        viewModel.initialize()
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.backup),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            modifier = Modifier.shimmer(selectedItems == null),
                            text = selectedItems?.let { stringResource(R.string.items_selected, it.first, it.second) } ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                colors = TopAppBarDefaults.defaultLargeTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState),
            ) {
                TargetRow(navController = navController, viewModel = viewModel)

                StorageRow(viewModel = viewModel)

                BackupRow(uiState = uiState, viewModel = viewModel)

                Settings()

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
                    enabled = nextBtnEnabled,
                    onClick = {
                        navController.navigateSafely(BackupProcess)
                    }
                ) {
                    Text(text = stringResource(R.string.next))
                }
            }

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun TargetRow(
    navController: NavHostController,
    viewModel: BackupSetupViewModel,
) {
    val appsItem by viewModel.appsItem.collectAsStateWithLifecycle(null)
    val filesItem by viewModel.filesItem.collectAsStateWithLifecycle(null)
    val networksItem by viewModel.networksItem.collectAsStateWithLifecycle(null)
    val contactsItem by viewModel.contactsItem.collectAsStateWithLifecycle(null)
    val callLogsItem by viewModel.callLogsItem.collectAsStateWithLifecycle(null)
    val messagesItem by viewModel.messagesItem.collectAsStateWithLifecycle(null)

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        text = stringResource(R.string.target),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SmallCheckActionButton(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize(),
                checked = appsItem?.selected ?: false,
                icon = ImageVector.vectorResource(R.drawable.ic_layout_grid),
                title = stringResource(R.string.apps),
                titleShimmer = appsItem == null,
                subtitle = stringResource(R.string.items_selected, appsItem?.selections?.first ?: 0, appsItem?.selections?.second ?: 0),
                subtitleShimmer = appsItem == null,
                onCheckedChange = {
                    viewModel.withLock(Dispatchers.Default) {
                        App.application.saveBoolean(AppsOptionSelectedBackup.first, it)
                    }
                }
            ) {
                navController.navigateSafely(BackupApps)
            }

            SmallCheckActionButton(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize(),
                checked = filesItem?.selected ?: false,
                icon = ImageVector.vectorResource(R.drawable.ic_folder),
                title = stringResource(R.string.files),
                titleShimmer = filesItem == null,
                subtitle = stringResource(R.string.items_selected, filesItem?.selections?.first ?: 0, filesItem?.selections?.second ?: 0),
                subtitleShimmer = filesItem == null,
                onCheckedChange = {}
            ) {}
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SmallCheckActionButton(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize(),
                checked = networksItem?.selected ?: false,
                icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                title = stringResource(R.string.networks),
                titleShimmer = networksItem == null,
                subtitle = stringResource(R.string.items_selected, networksItem?.selections?.first ?: 0, networksItem?.selections?.second ?: 0),
                subtitleShimmer = networksItem == null,
                onCheckedChange = {
                    viewModel.withLock(Dispatchers.Default) {
                        App.application.saveBoolean(NetworksOptionSelectedBackup.first, it)
                    }
                }
            ) {
                navController.navigateSafely(BackupNetworks)
            }

            val contactsPermissionState = rememberContactPermissionsState()
            SmallCheckActionButton(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize(),
                state = if (contactsPermissionState.allPermissionsGranted) ActionButtonState.NORMAL else ActionButtonState.ERROR,
                checked = contactsItem?.selected ?: false,
                checkBoxVisible = contactsPermissionState.allPermissionsGranted,
                icon = ImageVector.vectorResource(R.drawable.ic_user_round),
                title = stringResource(R.string.contacts),
                titleShimmer = contactsItem == null,
                subtitle = if (contactsPermissionState.allPermissionsGranted)
                    stringResource(R.string.items_selected, contactsItem?.selections?.first ?: 0, contactsItem?.selections?.second ?: 0)
                else
                    stringResource(R.string.no_permissions),
                subtitleShimmer = contactsItem == null,
                onCheckedChange = {
                    viewModel.withLock(Dispatchers.Default) {
                        App.application.saveBoolean(ContactsOptionSelectedBackup.first, it)
                    }
                }
            ) {
                if (contactsPermissionState.allPermissionsGranted) {
                    navController.navigateSafely(BackupContacts)
                } else {
                    contactsPermissionState.launchMultiplePermissionRequest()
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val callLogsPermissionState = rememberCallLogPermissionsState()
            SmallCheckActionButton(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize(),
                state = if (callLogsPermissionState.allPermissionsGranted) ActionButtonState.NORMAL else ActionButtonState.ERROR,
                checked = callLogsItem?.selected ?: false,
                checkBoxVisible = callLogsPermissionState.allPermissionsGranted,
                icon = ImageVector.vectorResource(R.drawable.ic_phone),
                title = stringResource(R.string.call_logs),
                titleShimmer = callLogsItem == null,
                subtitle = if (callLogsPermissionState.allPermissionsGranted)
                    stringResource(R.string.items_selected, callLogsItem?.selections?.first ?: 0, callLogsItem?.selections?.second ?: 0)
                else
                    stringResource(R.string.no_permissions),
                subtitleShimmer = callLogsItem == null,
                onCheckedChange = {
                    viewModel.withLock(Dispatchers.Default) {
                        App.application.saveBoolean(CallLogsOptionSelectedBackup.first, it)
                    }
                }
            ) {
                if (callLogsPermissionState.allPermissionsGranted) {
                    navController.navigateSafely(BackupCallLogs)
                } else {
                    callLogsPermissionState.launchMultiplePermissionRequest()
                }
            }

            val messagesPermissionState = rememberMessagePermissionsState()
            SmallCheckActionButton(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize(),
                state = if (messagesPermissionState.allPermissionsGranted) ActionButtonState.NORMAL else ActionButtonState.ERROR,
                checked = messagesItem?.selected ?: false,
                checkBoxVisible = messagesPermissionState.allPermissionsGranted,
                icon = ImageVector.vectorResource(R.drawable.ic_message_circle),
                title = stringResource(R.string.messages),
                titleShimmer = messagesItem == null,
                subtitle = if (messagesPermissionState.allPermissionsGranted)
                    stringResource(R.string.items_selected, messagesItem?.selections?.first ?: 0, messagesItem?.selections?.second ?: 0)
                else
                    stringResource(R.string.no_permissions),
                subtitleShimmer = messagesItem == null,
                onCheckedChange = {
                    viewModel.withLock(Dispatchers.Default) {
                        App.application.saveBoolean(MessagesOptionSelectedBackup.first, it)
                    }
                }
            ) {
                if (messagesPermissionState.allPermissionsGranted) {
                    navController.navigateSafely(BackupMessages)
                } else {
                    messagesPermissionState.launchMultiplePermissionRequest()
                }
            }
        }
    }
}

@Composable
private fun StorageRow(
    viewModel: BackupSetupViewModel,
) {
    val locationScrollState = rememberScrollState()

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        text = stringResource(R.string.storage),
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.labelLarge
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(locationScrollState)
            .horizontalFadingEdges(locationScrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.size(0.dp, 148.dp))

        var localStorage: String by remember { mutableStateOf("") }
        LaunchedEffect(context = Dispatchers.IO, null) {
            localStorage = viewModel.getLocalStorage()
        }
        SelectableCardButton(
            modifier = Modifier.size(148.dp),
            selected = true,
            title = stringResource(R.string.local),
            subtitle = localStorage,
            subtitleShimmer = localStorage.isEmpty(),
            icon = ImageVector.vectorResource(R.drawable.ic_smartphone),
            colors = selectableCardButtonSecondaryColors(),
        ) {
        }

        /**
         * SelectableCardButton(
         *     modifier = Modifier.size(148.dp),
         *     selected = false,
         *     title = stringResource(R.string.cloud),
         *     subtitle = stringResource(R.string.not_set_up),
         *     subtitleShimmer = false,
         *     icon = ImageVector.vectorResource(R.drawable.ic_cloud_upload),
         *     iconButton = ImageVector.vectorResource(R.drawable.ic_settings),
         *     onIconButtonClick = {}
         * ) {
         * }
         */

        Spacer(modifier = Modifier.size(0.dp, 148.dp))
    }
}

@Composable
private fun BackupRow(
    uiState: BackupSetupUiState,
    viewModel: BackupSetupViewModel,
) {
    val selectedConfigIndex by viewModel.selectedConfigIndex.collectAsStateWithLifecycle()
    val backupConfigs by viewModel.backupConfigs.collectAsStateWithLifecycle()

    Text(
        modifier = Modifier.padding(16.dp),
        text = stringResource(R.string.backup),
        color = MaterialTheme.colorScheme.tertiary,
        style = MaterialTheme.typography.labelLarge
    )

    val lazyListState = rememberLazyListState()
    var showStartEdge by remember { mutableStateOf(false) }
    var showEndEdge by remember { mutableStateOf(false) }
    val startEdgeRange: Float by animateFloatAsState(if (showStartEdge) 1f else 0f, label = "alpha")
    val endEdgeRange: Float by animateFloatAsState(if (showEndEdge) 1f else 0f, label = "alpha")
    LaunchedEffect(context = Dispatchers.Default, lazyListState.canScrollBackward) {
        showStartEdge = lazyListState.canScrollBackward
    }
    LaunchedEffect(context = Dispatchers.Default, lazyListState.canScrollForward) {
        showEndEdge = lazyListState.canScrollForward
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalFadingEdges(startEdgeRange, endEdgeRange),
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.size(0.dp, 148.dp))
        }

        item(key = "-1") {
            SelectableCardButton(
                modifier = Modifier
                    .size(148.dp)
                    .animateItem(),
                selected = selectedConfigIndex == -1,
                title = stringResource(R.string.new_backup),
                titleShimmer = uiState.isLoadingConfigs,
                colors = selectableCardButtonTertiaryColors(),
                icon = ImageVector.vectorResource(R.drawable.ic_plus),
                iconShimmer = uiState.isLoadingConfigs,
            ) {
                viewModel.selectBackup(-1)
            }
        }

        items(items = backupConfigs, key = { _, item -> item.uuid }) { index, item ->
            var backupStorage: String by remember { mutableStateOf("") }
            LaunchedEffect(context = Dispatchers.IO, null) {
                backupStorage = viewModel.getBackupStorage(item.path)
            }
            SelectableCardButton(
                modifier = Modifier
                    .size(148.dp)
                    .animateItem(),
                selected = selectedConfigIndex == index,
                title = item.displayTitle,
                subtitle = backupStorage,
                subtitleShimmer = backupStorage.isEmpty(),
                colors = selectableCardButtonTertiaryColors(),
                icon = ImageVector.vectorResource(R.drawable.ic_archive),
                iconButton = ImageVector.vectorResource(R.drawable.ic_trash),
                onIconButtonClick = {},
            ) {
                viewModel.selectBackup(index)
            }
        }

        item {
            Spacer(modifier = Modifier.size(0.dp, 148.dp))
        }
    }
}

@Composable
private fun Settings() {
    Text(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        text = stringResource(R.string.settings),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge
    )

    IncrementalBackupAndCleanBackupSwitches()

    AutoScreenOffSwitch()

    ResetBackupListSwitch()
}
