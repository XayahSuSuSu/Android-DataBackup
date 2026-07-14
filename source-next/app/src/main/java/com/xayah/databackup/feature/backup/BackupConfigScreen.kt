package com.xayah.databackup.feature.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.ui.component.DataBackupDialog
import com.xayah.databackup.ui.component.DialogActionButton
import com.xayah.databackup.ui.component.DialogDestructiveButton
import com.xayah.databackup.ui.component.DialogDismissButton
import com.xayah.databackup.ui.component.DialogIcon
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.PreferenceGroup
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.SelectablePreferenceGroup
import com.xayah.databackup.ui.component.SelectablePreferenceItemInfo
import com.xayah.databackup.ui.component.SmallActionButton
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BackupConfigScreen(
    navigator: Navigator,
    viewModel: BackupConfigViewModel,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val backupConfig by viewModel.backupConfig.collectAsStateWithLifecycle(null)
    val backupBackend by viewModel.backupBackend.collectAsStateWithLifecycle(BackupBackend.Archive())
    val backupBackendSelectedIndex by viewModel.backupBackendSelectedIndex.collectAsStateWithLifecycle(1)
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

    if (openDeleteDialog) {
        DeleteDialog(
            onDismissRequest = {
                openDeleteDialog = false
            },
            onConfirm = {
                viewModel.deleteConfig {
                    withContext(Dispatchers.Main) {
                        openDeleteDialog = false
                        navigator.popBackStackSafely()
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
                    IconButton(onClick = { navigator.popBackStackSafely() }) {
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

                BackendRow(enabled = backupConfig == null, selectedIndex = backupBackendSelectedIndex) {
                    viewModel.selectBackupBackend(it)
                }

                AnimatedVisibility(
                    visible = backupBackend is BackupBackend.Rustic,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    RusticPasswordRow(
                        enabled = backupConfig == null,
                        password = (backupBackend as? BackupBackend.Rustic)?.password ?: BackupBackend.DEFAULT_PASSWORD,
                        onPasswordChanged = viewModel::changeRusticPassword,
                    )
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
    PreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
        Preference(
            icon = ImageVector.vectorResource(R.drawable.ic_id_card),
            title = stringResource(R.string.id),
            subtitle = uuid ?: stringResource(R.string.not_created_yet),
        ) {
        }
    }
}

@Composable
private fun BackendRow(
    enabled: Boolean,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
) {
    val context = LocalContext.current

    SectionHeader(
        modifier = Modifier.padding(16.dp),
        title = stringResource(R.string.backup_backend),
    )

    val items = remember {
        listOf(
            SelectablePreferenceItemInfo(
                icon = ImageVector.vectorResource(null, context.resources, R.drawable.ic_chart_bar_stacked),
                title = context.getString(R.string.rustic),
                subtitle = context.getString(R.string.rustic_backup_backend_desc),
            ),
            SelectablePreferenceItemInfo(
                icon = ImageVector.vectorResource(null, context.resources, R.drawable.ic_archive),
                title = context.getString(R.string.archive),
                subtitle = context.getString(R.string.archive_backup_backend_desc),
            )
        )
    }

    SelectablePreferenceGroup(
        modifier = Modifier.padding(horizontal = 16.dp),
        enabled = enabled,
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChanged = onSelectedIndexChanged
    )
}

@Composable
private fun RusticPasswordRow(
    enabled: Boolean,
    password: String,
    onPasswordChanged: (String) -> Unit,
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        enabled = enabled,
        value = password,
        onValueChange = {
            if (enabled) onPasswordChanged(it)
        },
        label = { Text(text = stringResource(R.string.password)) },
        singleLine = true,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val contentDescription = if (showPassword) {
                stringResource(R.string.hide_password)
            } else {
                stringResource(R.string.show_password)
            }
            IconButton(onClick = { showPassword = showPassword.not() }) {
                Icon(
                    imageVector = if (showPassword) {
                        ImageVector.vectorResource(R.drawable.ic_eye_off)
                    } else {
                        ImageVector.vectorResource(R.drawable.ic_eye)
                    },
                    contentDescription = contentDescription,
                )
            }
        },
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
    DataBackupDialog(
        title = stringResource(R.string.edit_name),
        onDismissRequest = onDismissRequest,
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_square_pen)) },
        content = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                onValueChange = {
                    isError = it.text.isBlank()
                    text = it
                },
                isError = isError,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                label = { Text(text = stringResource(R.string.name)) },
                supportingText = if (isError) {
                    { Text(text = stringResource(R.string.required)) }
                } else {
                    null
                },
            )
        },
        confirmButton = {
            DialogActionButton(
                text = stringResource(R.string.save),
                enabled = isError.not() && text.text.isNotBlank(),
                icon = ImageVector.vectorResource(R.drawable.ic_check),
                onClick = { onConfirm.invoke(text.text) },
            )
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.cancel),
                onClick = onDismissRequest,
            )
        },
    )
}

@Composable
private fun DeleteDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    var isDeleting by remember { mutableStateOf(false) }
    DataBackupDialog(
        title = stringResource(R.string.delete),
        onDismissRequest = onDismissRequest,
        icon = {
            AnimatedVisibility(visible = isDeleting, enter = fadeIn(), exit = fadeOut()) {
                LoadingIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            AnimatedVisibility(visible = isDeleting.not(), enter = fadeIn(), exit = fadeOut()) {
                DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_trash))
            }
        },
        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
        content = { Text(text = stringResource(R.string.confirm_delete)) },
        confirmButton = {
            DialogDestructiveButton(
                text = stringResource(R.string.delete),
                enabled = isDeleting.not(),
                icon = ImageVector.vectorResource(R.drawable.ic_trash),
                onClick = {
                    isDeleting = true
                    onConfirm.invoke()
                },
            )
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.cancel),
                enabled = isDeleting.not(),
                onClick = onDismissRequest,
            )
        },
    )
}
