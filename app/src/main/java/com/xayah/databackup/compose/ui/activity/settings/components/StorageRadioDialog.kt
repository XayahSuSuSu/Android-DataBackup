package com.xayah.databackup.compose.ui.activity.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.xayah.databackup.R

data class StorageRadioDialogItem(
    var title: String,
    var progress: Float,
    var path: String,
    var display: String,
    var enabled: Boolean = true,
)

@ExperimentalMaterial3Api
@Composable
fun StorageRadioDialog(
    isOpen: MutableState<Boolean>,
    icon: ImageVector,
    items: List<StorageRadioDialogItem>,
    selected: MutableState<StorageRadioDialogItem>,
    onConfirm: (index: Int) -> Unit,
) {
    if (isOpen.value) {
        val tinyPadding = dimensionResource(R.dimen.padding_tiny)
        val smallPadding = dimensionResource(R.dimen.padding_small)
        val mediumPadding = dimensionResource(R.dimen.padding_medium)
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
                Text(text = stringResource(id = R.string.backup_dir))
            },
            text = {
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(smallPadding)
                ) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize()
                                .selectable(
                                    selected = (item.display == selected.value.display),
                                    onClick = {
                                        if (item.enabled)
                                            selected.value = item
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = mediumPadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(mediumPadding)
                        ) {
                            RadioButton(
                                selected = (item.display == selected.value.display),
                                enabled = item.enabled,
                                onClick = null
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(tinyPadding)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                LinearProgressIndicator(
                                    modifier = Modifier.clip(CircleShape),
                                    progress = item.progress
                                )
                                Text(
                                    text = item.display,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
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
