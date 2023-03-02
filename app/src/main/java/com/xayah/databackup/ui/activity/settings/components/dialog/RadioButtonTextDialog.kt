package com.xayah.databackup.ui.activity.settings.components.dialog

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun RadioButtonTextDialogPreview() {
    val isOpen = remember {
        mutableStateOf(true)
    }
    val selected = remember {
        mutableStateOf("0")
    }
    RadioButtonTextDialog(
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
fun RadioButtonTextDialog(
    isOpen: MutableState<Boolean>,
    icon: ImageVector,
    title: String,
    items: List<String>,
    selected: MutableState<String>,
    onConfirm: (index: Int) -> Unit,
) {
    RadioButtonDialog(
        isOpen = isOpen,
        icon = icon,
        title = title,
        items = items,
        selected = selected,
        content = {
            RadioButtonGroup(
                items = items,
                selected = selected
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        onConfirm = onConfirm,
    )
}
