package com.xayah.databackup.ui.activity.settings.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Composable
fun <T> RadioButtonDialog(
    isOpen: MutableState<Boolean>,
    icon: ImageVector,
    title: String,
    items: List<T>,
    selected: MutableState<T>,
    content: @Composable () -> Unit,
    onConfirm: (index: Int) -> Unit,
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
                LazyColumn() {
                    item {
                        content()
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(items.indexOf(selected.value))
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
