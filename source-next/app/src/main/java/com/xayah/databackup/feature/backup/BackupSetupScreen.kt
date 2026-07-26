package com.xayah.databackup.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.feature.BackupAppsRoute
import com.xayah.databackup.feature.BackupCallLogsRoute
import com.xayah.databackup.feature.BackupContactsRoute
import com.xayah.databackup.feature.BackupMessagesRoute
import com.xayah.databackup.feature.BackupNetworksRoute
import com.xayah.databackup.feature.BackupProcessRoute
import com.xayah.databackup.feature.RusticBackupProcessRoute
import com.xayah.databackup.ui.component.ActionButtonState
import com.xayah.databackup.ui.component.AutoScreenOffSwitch
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.PreferenceGroup
import com.xayah.databackup.ui.component.ResetBackupListSwitch
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.SmallCheckActionButton
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.ui.component.rememberCallLogPermissionsState
import com.xayah.databackup.ui.component.rememberContactPermissionsState
import com.xayah.databackup.ui.component.rememberMessagePermissionsState
import com.xayah.databackup.ui.component.shimmer
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.AppsOptionSelectedBackup
import com.xayah.databackup.util.CallLogsOptionSelectedBackup
import com.xayah.databackup.util.ContactsOptionSelectedBackup
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.MessagesOptionSelectedBackup
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.NetworksOptionSelectedBackup
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.formatToStorageSize
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.popBackStackSafely
import com.xayah.databackup.util.saveBoolean
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupSetupScreen(
    navigator: Navigator,
    viewModel: BackupSetupViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val selectedBackup by viewModel.selectedBackup.collectAsStateWithLifecycle()
    val selectedBackupSize by viewModel.selectedBackupSize.collectAsStateWithLifecycle()
    val isLoadingConfigs by viewModel.isLoadingConfigs.collectAsStateWithLifecycle()
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
                    IconButton(onClick = { navigator.popBackStackSafely() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.surfaceTopAppBarColors(),
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
                SelectedBackupInfo(
                    backup = selectedBackup,
                    backupSize = selectedBackupSize,
                    isLoading = isLoadingConfigs,
                )

                TargetRow(navigator = navigator, viewModel = viewModel)

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
                        viewModel.resetProcessRepo()
                        if (viewModel.isCurrentBackupRustic()) {
                            navigator.navigateSafely(RusticBackupProcessRoute)
                        } else {
                            navigator.navigateSafely(BackupProcessRoute)
                        }
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
    navigator: Navigator,
    viewModel: BackupSetupViewModel,
) {
    val appsItem by viewModel.appsItem.collectAsStateWithLifecycle(null)
    val filesItem by viewModel.filesItem.collectAsStateWithLifecycle(null)
    val networksItem by viewModel.networksItem.collectAsStateWithLifecycle(null)
    val contactsItem by viewModel.contactsItem.collectAsStateWithLifecycle(null)
    val callLogsItem by viewModel.callLogsItem.collectAsStateWithLifecycle(null)
    val messagesItem by viewModel.messagesItem.collectAsStateWithLifecycle(null)

    SectionHeader(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        title = stringResource(R.string.target),
        color = MaterialTheme.colorScheme.primary,
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
                navigator.navigateSafely(BackupAppsRoute)
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
                navigator.navigateSafely(BackupNetworksRoute)
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
                    navigator.navigateSafely(BackupContactsRoute)
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
                    navigator.navigateSafely(BackupCallLogsRoute)
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
                    navigator.navigateSafely(BackupMessagesRoute)
                } else {
                    messagesPermissionState.launchMultiplePermissionRequest()
                }
            }
        }
    }
}

@Composable
private fun SelectedBackupInfo(
    backup: BackupConfig?,
    backupSize: Long?,
    isLoading: Boolean,
) {
    backup?.let { selectedBackup ->
        val rusticBackend = selectedBackup.backupBackend is BackupBackend.Rustic
        val relativePath = remember(selectedBackup.path) {
            PathHelper.getChildPath(selectedBackup.path).ifEmpty { selectedBackup.path }
        }
        PreferenceGroup(
            modifier = Modifier
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            Preference(
                icon = ImageVector.vectorResource(
                    if (rusticBackend) R.drawable.ic_database_backup else R.drawable.ic_archive
                ),
                title = selectedBackup.displayName,
                subtitle = stringResource(if (rusticBackend) R.string.rustic else R.string.archive),
            )
            Preference(
                icon = ImageVector.vectorResource(R.drawable.ic_map_pin),
                title = stringResource(R.string.backup_dir),
                subtitle = relativePath,
                subtitleIcon = ImageVector.vectorResource(R.drawable.ic_folder),
            )
            Preference(
                icon = ImageVector.vectorResource(R.drawable.ic_database),
                title = stringResource(R.string.storage),
                subtitle = backupSize?.formatToStorageSize.orEmpty(),
                subtitleShimmer = backupSize == null,
            )
        }
    } ?: Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            LoadingIndicator()
        } else {
            Text(
                text = stringResource(R.string.no_item_selected),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Settings() {
    SectionHeader(
        modifier = Modifier.padding(16.dp),
        title = stringResource(R.string.settings),
        color = MaterialTheme.colorScheme.primary,
    )

    PreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
        AutoScreenOffSwitch()
        ResetBackupListSwitch()
    }
}
