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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.feature.BackupApps
import com.xayah.databackup.feature.BackupCallLogs
import com.xayah.databackup.feature.BackupContacts
import com.xayah.databackup.feature.BackupMessages
import com.xayah.databackup.feature.BackupNetworks
import com.xayah.databackup.ui.component.SelectableActionButton
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.AppsOptionSelectedBackup
import com.xayah.databackup.util.CallLogsOptionSelectedBackup
import com.xayah.databackup.util.ContactsOptionSelectedBackup
import com.xayah.databackup.util.KeyAppsOptionSelectedBackup
import com.xayah.databackup.util.KeyCallLogsOptionSelectedBackup
import com.xayah.databackup.util.KeyContactsOptionSelectedBackup
import com.xayah.databackup.util.KeyMessagesOptionSelectedBackup
import com.xayah.databackup.util.KeyNetworksOptionSelectedBackup
import com.xayah.databackup.util.MessagesOptionSelectedBackup
import com.xayah.databackup.util.NetworksOptionSelectedBackup
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.popBackStackSafely
import com.xayah.databackup.util.readBoolean
import com.xayah.databackup.util.saveBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupPreviewScreen(
    navController: NavHostController,
    viewModel: BackupPreviewViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val appsSelected by viewModel.appsSelected.collectAsStateWithLifecycle()
    val networksSelected by viewModel.networksSelected.collectAsStateWithLifecycle()
    val contactsSelected by viewModel.contactsSelected.collectAsStateWithLifecycle()
    val callLogsSelected by viewModel.callLogsSelected.collectAsStateWithLifecycle()
    val messagesSelected by viewModel.messagesSelected.collectAsStateWithLifecycle()
    val appsOptionSelectedBackup by context.readBoolean(AppsOptionSelectedBackup)
        .collectAsStateWithLifecycle(initialValue = AppsOptionSelectedBackup.second)
    val networksOptionSelectedBackup by context.readBoolean(NetworksOptionSelectedBackup)
        .collectAsStateWithLifecycle(initialValue = NetworksOptionSelectedBackup.second)
    val contactsOptionSelectedBackup by context.readBoolean(ContactsOptionSelectedBackup)
        .collectAsStateWithLifecycle(initialValue = ContactsOptionSelectedBackup.second)
    val callLogsOptionSelectedBackup by context.readBoolean(CallLogsOptionSelectedBackup)
        .collectAsStateWithLifecycle(initialValue = CallLogsOptionSelectedBackup.second)
    val messagesOptionSelectedBackup by context.readBoolean(MessagesOptionSelectedBackup)
        .collectAsStateWithLifecycle(initialValue = MessagesOptionSelectedBackup.second)
    var optionsSelected by remember { mutableIntStateOf(0) }
    var optionsTotal by remember { mutableIntStateOf(6) }

    LaunchedEffect(
        appsOptionSelectedBackup,
        networksOptionSelectedBackup,
        contactsOptionSelectedBackup,
        callLogsOptionSelectedBackup,
        messagesOptionSelectedBackup
    ) {
        optionsSelected = 0
        if (appsOptionSelectedBackup) optionsSelected++
        if (networksOptionSelectedBackup) optionsSelected++
        if (contactsOptionSelectedBackup) optionsSelected++
        if (callLogsOptionSelectedBackup) optionsSelected++
        if (messagesOptionSelectedBackup) optionsSelected++
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
                            text = stringResource(R.string.items_selected, optionsSelected, optionsTotal),
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
                    title = stringResource(R.string.apps),
                    subtitle = stringResource(R.string.items_selected, appsSelected.first, appsSelected.second),
                    checked = appsOptionSelectedBackup,
                    onCheckedChange = {
                        scope.launch {
                            withContext(Dispatchers.Default) {
                                context.saveBoolean(KeyAppsOptionSelectedBackup, appsOptionSelectedBackup.not())
                            }
                        }
                    }
                ) {
                    navController.navigateSafely(BackupApps)
                }

                SelectableActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_folder),
                    title = stringResource(R.string.files),
                    subtitle = stringResource(R.string.items_selected, 0, 0),
                    checked = false,
                    onCheckedChange = {}
                ) {}

                SelectableActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                    title = stringResource(R.string.networks),
                    subtitle = stringResource(R.string.items_selected, networksSelected.first, networksSelected.second),
                    checked = networksOptionSelectedBackup,
                    onCheckedChange = {
                        scope.launch {
                            withContext(Dispatchers.Default) {
                                context.saveBoolean(KeyNetworksOptionSelectedBackup, networksOptionSelectedBackup.not())
                            }
                        }
                    }
                ) {
                    navController.navigateSafely(BackupNetworks)
                }

                SelectableActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_user_round),
                    title = stringResource(R.string.contacts),
                    subtitle = stringResource(R.string.items_selected, contactsSelected.first, contactsSelected.second),
                    checked = contactsOptionSelectedBackup,
                    onCheckedChange = {
                        scope.launch {
                            withContext(Dispatchers.Default) {
                                context.saveBoolean(KeyContactsOptionSelectedBackup, contactsOptionSelectedBackup.not())
                            }
                        }
                    }
                ) {
                    navController.navigateSafely(BackupContacts)
                }

                SelectableActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_phone),
                    title = stringResource(R.string.call_logs),
                    subtitle = stringResource(R.string.items_selected, callLogsSelected.first, callLogsSelected.second),
                    checked = callLogsOptionSelectedBackup,
                    onCheckedChange = {
                        scope.launch {
                            withContext(Dispatchers.Default) {
                                context.saveBoolean(KeyCallLogsOptionSelectedBackup, callLogsOptionSelectedBackup.not())
                            }
                        }
                    }
                ) {
                    navController.navigateSafely(BackupCallLogs)
                }

                SelectableActionButton(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentSize(),
                    icon = ImageVector.vectorResource(R.drawable.ic_message_circle),
                    title = stringResource(R.string.messages),
                    subtitle = stringResource(R.string.items_selected, messagesSelected.first, messagesSelected.second),
                    checked = messagesOptionSelectedBackup,
                    onCheckedChange = {
                        scope.launch {
                            withContext(Dispatchers.Default) {
                                context.saveBoolean(KeyMessagesOptionSelectedBackup, messagesOptionSelectedBackup.not())
                            }
                        }
                    }
                ) {
                    navController.navigateSafely(BackupMessages)
                }

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
