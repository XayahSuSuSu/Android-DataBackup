package com.xayah.databackup.ui.activity.list.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Composable
fun ConfirmDialog(
    isOpen: MutableState<Boolean>,
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
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
                    onClick = {
                        onConfirm()
                        isOpen.value = false
                    }
                ) {
                    Text(stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isOpen.value = false
                    }
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}
