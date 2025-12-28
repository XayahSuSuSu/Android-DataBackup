package com.xayah.databackup.feature.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.SelectablePreferenceGroup
import com.xayah.databackup.ui.component.SelectablePreferenceItemInfo
import com.xayah.databackup.ui.component.SmallActionButton
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupConfigScreen(
    navController: NavHostController,
    viewModel: BackupConfigViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val backupConfig by viewModel.backupConfig.collectAsStateWithLifecycle(null)
    val appsBackupStrategySelectedIndex by viewModel.appsBackupStrategySelectedIndex.collectAsStateWithLifecycle(0)
    var openEditNameDialog by remember { mutableStateOf(false) }
    var openDeleteDialog by remember { mutableStateOf(false) }

    if (backupConfig != null && openEditNameDialog) {
        EditNameDialog(
            name = backupConfig?.name ?: "",
            onDismissRequest = {
                openEditNameDialog = false
            },
            onConfirm = {
                viewModel.changeName(it)
                openEditNameDialog = false
            }
        )
    }

    if (backupConfig != null && openDeleteDialog) {
        DeleteDialog(
            onDismissRequest = {
                openDeleteDialog = false
            },
            onConfirm = {
                viewModel.deleteConfig {
                    withContext(Dispatchers.Main) {
                        openDeleteDialog = false
                        navController.popBackStackSafely()
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    val name = remember(backupConfig?.name) { backupConfig?.displayName }
                    Text(
                        text = name ?: stringResource(R.string.new_backup),
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
                actions = {
                    if (backupConfig != null) {
                        val editNameDesc = stringResource(R.string.edit_name)
                        TooltipBox(
                            positionProvider =
                                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                            tooltip = { PlainTooltip { Text(editNameDesc) } },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = { openEditNameDialog = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_square_pen),
                                    contentDescription = editNameDesc
                                )
                            }
                        }

                        val deleteDesc = stringResource(R.string.delete)
                        TooltipBox(
                            positionProvider =
                                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                            tooltip = { PlainTooltip { Text(deleteDesc) } },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = { openDeleteDialog = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_trash),
                                    contentDescription = deleteDesc
                                )
                            }
                        }
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
                InfoRow(backupConfig = backupConfig)

                StrategyRow(selectedIndex = appsBackupStrategySelectedIndex, backupConfig = backupConfig) {
                    viewModel.selectAppsBackupStrategy(it)
                }

                Spacer(modifier = Modifier.height(0.dp))
            }

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun InfoRow(
    backupConfig: BackupConfig?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val createdAt = remember(backupConfig?.createdAt) { backupConfig?.displayCreatedAt }
        SmallActionButton(
            modifier = Modifier
                .weight(1f)
                .wrapContentSize(),
            icon = ImageVector.vectorResource(R.drawable.ic_clock_plus),
            title = stringResource(R.string.created_at),
            subtitle = createdAt ?: stringResource(R.string.not_created_yet)
        ) {
        }

        val updatedAt = remember(backupConfig?.updatedAt) { backupConfig?.displayUpdatedAt }
        SmallActionButton(
            modifier = Modifier
                .weight(1f)
                .wrapContentSize(),
            icon = ImageVector.vectorResource(R.drawable.ic_clock_arrow_up),
            title = stringResource(R.string.updated_at),
            subtitle = updatedAt ?: stringResource(R.string.not_created_yet)
        ) {
        }
    }

    val uuid = remember(backupConfig?.uuid) { backupConfig?.uuidString }
    Preference(
        icon = ImageVector.vectorResource(R.drawable.ic_id_card),
        title = stringResource(R.string.id),
        subtitle = uuid ?: stringResource(R.string.not_created_yet)
    ) {
    }
}

@Composable
private fun StrategyRow(
    backupConfig: BackupConfig?,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
) {
    val context = LocalContext.current

    Text(
        modifier = Modifier.padding(16.dp),
        text = stringResource(R.string.apps_backup_strategy),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge
    )

    val items = remember {
        listOf(
            SelectablePreferenceItemInfo(
                icon = ImageVector.vectorResource(null, context.resources, R.drawable.ic_chart_bar_stacked),
                title = context.getString(R.string.incremental_backup),
                subtitle = context.getString(R.string.incremental_backup_desc),
            ),
            SelectablePreferenceItemInfo(
                icon = ImageVector.vectorResource(null, context.resources, R.drawable.ic_brush_cleaning),
                title = context.getString(R.string.clean_backup),
                subtitle = context.getString(R.string.clean_backup_desc),
            )
        )
    }

    SelectablePreferenceGroup(
        enabled = backupConfig == null,
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChanged = onSelectedIndexChanged
    )
}

@Composable
private fun EditNameDialog(
    name: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = name))
    }
    var isError by rememberSaveable { mutableStateOf(name.isBlank()) }
    AlertDialog(
        title = { Text(text = stringResource(R.string.edit_name)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    isError = it.text.isBlank()
                    text = it
                },
                isError = isError,
                label = { Text(text = stringResource(R.string.name)) },
            )
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                enabled = isError.not(),
                onClick = { onConfirm.invoke(text.text) }
            ) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(R.string.dismiss))
            }
        }
    )
}

@Composable
private fun DeleteDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    var isDeleting by remember { mutableStateOf(false) }
    AlertDialog(
        icon = {
            Box(contentAlignment = Alignment.Center) {
                AnimatedVisibility(visible = isDeleting, enter = fadeIn(), exit = fadeOut()) {
                    LoadingIndicator()
                }
                AnimatedVisibility(visible = isDeleting.not(), enter = fadeIn(), exit = fadeOut()) {
                    Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_badge_info), contentDescription = stringResource(R.string.delete))
                }
            }
        },
        title = { Text(text = stringResource(R.string.delete)) },
        text = {
            Text(text = stringResource(R.string.confirm_delete))
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                enabled = true,
                onClick = {
                    isDeleting = true
                    onConfirm.invoke()
                }
            ) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(R.string.dismiss))
            }
        }
    )
}
