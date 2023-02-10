package com.xayah.databackup.compose.ui.activity.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun RadioButtonDialogPreview() {
    val isOpen = remember {
        mutableStateOf(true)
    }
    val selected = remember {
        mutableStateOf("0")
    }
    RadioButtonDialog(
        isOpen = isOpen,
        icon = ImageVector.vectorResource(id = R.drawable.ic_round_person),
        title = stringResource(id = R.string.backup_user),
        items = listOf("0", "999"),
        selected = selected,
        onConfirm = {}
    )
}

@ExperimentalMaterial3Api
@Composable
fun RadioButtonDialog(
    isOpen: MutableState<Boolean>,
    icon: ImageVector,
    title: String,
    items: List<String>,
    selected: MutableState<String>,
    onConfirm: (index: Int) -> Unit,
) {
    if (isOpen.value) {
        val mediumPadding = dimensionResource(R.dimen.padding_medium)
        val settingsTitlePadding = dimensionResource(R.dimen.padding_settings_title)
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
                Column(Modifier.selectableGroup()) {
                    items.forEach { text ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(settingsTitlePadding)
                                .selectable(
                                    selected = (text == selected.value),
                                    onClick = { selected.value = text },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = mediumPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (text == selected.value),
                                onClick = null
                            )
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = mediumPadding)
                            )
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
