package com.xayah.databackup.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
        dismissButton = if (showDismissBtn) null else {
            { TextButton(text = dismiss, onClick = onDismissClick) }
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        )
    )
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
