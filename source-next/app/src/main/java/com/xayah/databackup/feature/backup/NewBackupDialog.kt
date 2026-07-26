package com.xayah.databackup.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.ui.component.DataBackupDialog
import com.xayah.databackup.ui.component.DialogActionButton
import com.xayah.databackup.ui.component.DialogDismissButton
import com.xayah.databackup.ui.component.DialogIcon
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.SelectablePreferenceGroup
import com.xayah.databackup.ui.component.SelectablePreferenceItemInfo

@Composable
fun NewBackupDialog(
    viewModel: NewBackupViewModel,
    onDismissRequest: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backupBackend by viewModel.backupBackend.collectAsStateWithLifecycle()
    val selectedBackendIndex = when (backupBackend) {
        is BackupBackend.Archive -> 0
        is BackupBackend.Rustic -> 1
    }
    var showEditPasswordDialog by rememberSaveable { mutableStateOf(false) }
    val dismissDialog = {
        viewModel.discardChanges()
        onDismissRequest()
    }

    uiState.saveError?.let { error ->
        SaveNewBackupErrorDialog(
            error = error,
            onDismissRequest = viewModel::dismissSaveError,
        )
    }

    (backupBackend as? BackupBackend.Rustic)?.let { rusticBackend ->
        if (showEditPasswordDialog) {
            EditNewBackupPasswordDialog(
                password = rusticBackend.password,
                onDismissRequest = { showEditPasswordDialog = false },
                onConfirm = { password ->
                    viewModel.changeRusticPassword(password)
                    showEditPasswordDialog = false
                },
            )
        }
    }

    DataBackupDialog(
        title = stringResource(R.string.new_backup),
        onDismissRequest = {
            if (uiState.isSaving.not()) {
                dismissDialog()
            }
        },
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_plus)) },
        content = {
            NewBackupBackendSelector(
                backupBackend = backupBackend,
                enabled = uiState.isSaving.not(),
                selectedIndex = selectedBackendIndex,
                onSelectedIndexChanged = {
                    showEditPasswordDialog = false
                    viewModel.selectBackupBackend(it)
                },
                onEditPassword = { showEditPasswordDialog = true },
            )
        },
        confirmButton = {
            Button(
                enabled = uiState.isSaving.not(),
                onClick = { viewModel.saveNewBackup(onSaved = onDismissRequest) },
            ) {
                if (uiState.isSaving) {
                    LoadingIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                        contentDescription = null,
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.cancel),
                enabled = uiState.isSaving.not(),
                onClick = dismissDialog,
            )
        },
    )
}

@Composable
private fun NewBackupBackendSelector(
    backupBackend: BackupBackend,
    enabled: Boolean,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    onEditPassword: () -> Unit,
) {
    val items = listOf(
        SelectablePreferenceItemInfo(
            icon = ImageVector.vectorResource(R.drawable.ic_archive),
            title = stringResource(R.string.archive),
            subtitle = stringResource(R.string.archive_backup_backend_desc),
        ),
        SelectablePreferenceItemInfo(
            icon = ImageVector.vectorResource(R.drawable.ic_database_backup),
            title = stringResource(R.string.rustic),
            subtitle = stringResource(R.string.rustic_backup_backend_desc),
        ),
    )

    SelectablePreferenceGroup(
        enabled = enabled,
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChanged = onSelectedIndexChanged,
    ) {
        val rusticBackend = backupBackend as? BackupBackend.Rustic
        NewBackupPasswordPreference(
            password = rusticBackend?.password ?: BackupBackend.DEFAULT_PASSWORD,
            enabled = enabled && rusticBackend != null,
            onClick = onEditPassword,
        )
    }
}

@Composable
private fun NewBackupPasswordPreference(
    password: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    var showPassword by rememberSaveable(password) { mutableStateOf(false) }
    val togglePasswordDescription = stringResource(
        if (showPassword) R.string.hide_password else R.string.show_password
    )

    Preference(
        enabled = enabled,
        icon = ImageVector.vectorResource(R.drawable.ic_key_round),
        title = stringResource(R.string.password),
        subtitle = password.takeIf { showPassword } ?: HIDDEN_PASSWORD,
        slot = {
            IconButton(
                enabled = enabled,
                onClick = { showPassword = showPassword.not() },
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        if (showPassword) R.drawable.ic_eye_off else R.drawable.ic_eye
                    ),
                    contentDescription = togglePasswordDescription,
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun EditNewBackupPasswordDialog(
    password: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable(password) { mutableStateOf(password) }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    DataBackupDialog(
        title = stringResource(R.string.password),
        onDismissRequest = onDismissRequest,
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_key_round)) },
        content = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                label = { Text(text = stringResource(R.string.password)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val contentDescription = stringResource(
                        if (showPassword) R.string.hide_password else R.string.show_password
                    )
                    IconButton(onClick = { showPassword = showPassword.not() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (showPassword) R.drawable.ic_eye_off else R.drawable.ic_eye
                            ),
                            contentDescription = contentDescription,
                        )
                    }
                },
            )
        },
        confirmButton = {
            DialogActionButton(
                text = stringResource(R.string.save),
                icon = ImageVector.vectorResource(R.drawable.ic_check),
                onClick = { onConfirm(text) },
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
private fun SaveNewBackupErrorDialog(
    error: String,
    onDismissRequest: () -> Unit,
) {
    DataBackupDialog(
        title = stringResource(R.string.error),
        onDismissRequest = onDismissRequest,
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_circle_x)) },
        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.save_backup_failed))
                if (error.isNotBlank()) {
                    Text(error)
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = stringResource(R.string.confirm),
                onClick = onDismissRequest,
            )
        },
    )
}
