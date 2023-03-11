package com.xayah.databackup.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.xayah.databackup.R

@Composable
fun TextDialog(
    isOpen: MutableState<Boolean>,
    icon: ImageVector,
    title: String,
    content: String,
    dismissOnBackPress: Boolean = false,
    dismissOnClickOutside: Boolean = false,
    confirmText: String = "",
    onConfirmClick: () -> Unit,
    showDismissBtn: Boolean = true,
    dismissText: String = "",
    onDismissClick: () -> Unit = {},
) {
    if (isOpen.value) {
        val confirm = confirmText.ifEmpty { stringResource(id = R.string.confirm) }
        val dismiss = dismissText.ifEmpty { stringResource(id = R.string.cancel) }
        AlertDialog(
            onDismissRequest = {
                isOpen.value = false
            },
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            },
            title = { Text(text = title) },
            text = { Text(text = content) },
            confirmButton = {
                TextButton(text = confirm, onClick = onConfirmClick)
            },
            dismissButton = if (showDismissBtn.not()) null else {
                { TextButton(text = dismiss, onClick = onDismissClick) }
            },
            properties = DialogProperties(
                dismissOnBackPress = dismissOnBackPress,
                dismissOnClickOutside = dismissOnClickOutside
            )
        )
    }
}

@ExperimentalMaterial3Api
@Composable
fun ConfirmDialog(
    isOpen: MutableState<Boolean>,
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
    cancelable: Boolean = true,
    onConfirm: () -> Unit,
) {
    if (isOpen.value) {
        AlertDialog(
            onDismissRequest = {
                isOpen.value = false
            },
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            },
            title = {
                Text(text = title)
            },
            text = {
                content()
            },
            confirmButton = {
                TextButton(
                    text = stringResource(id = R.string.confirm),
                    onClick = {
                        onConfirm()
                        isOpen.value = false
                    })
            },
            dismissButton = if (cancelable) {
                {
                    TextButton(text = stringResource(id = R.string.cancel)) {
                        isOpen.value = false
                    }
                }
            } else null,
            properties = DialogProperties(
                dismissOnBackPress = cancelable,
                dismissOnClickOutside = cancelable
            )
        )
    }
}

@ExperimentalMaterial3Api
@Composable
fun LoadingDialog(
    isOpen: MutableState<Boolean>,
) {
    if (isOpen.value) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null
                )
            },
            title = {
                Text(text = stringResource(id = R.string.please_wait))
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }
}
