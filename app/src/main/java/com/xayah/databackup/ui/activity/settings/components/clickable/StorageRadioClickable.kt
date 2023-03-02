package com.xayah.databackup.ui.activity.settings.components.clickable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.settings.components.StorageRadioDialogItem
import com.xayah.databackup.ui.activity.settings.components.dialog.StorageRadioDialog
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun StorageRadioClickable(
    subtitle: MutableState<String>,
    onPrepare: suspend () -> Pair<List<StorageRadioDialogItem>, StorageRadioDialogItem>,
    onConfirm: (value: StorageRadioDialogItem) -> Unit,
    onEdit: () -> Unit,
) {
    val icon = ImageVector.vectorResource(id = R.drawable.ic_round_folder_open)
    val scope = rememberCoroutineScope()
    val isDialogOpen = remember {
        mutableStateOf(false)
    }
    val items = remember {
        mutableListOf(
            StorageRadioDialogItem(
                title = "",
                progress = 0f,
                path = "",
                display = "",
            )
        )
    }
    val selected = remember {
        mutableStateOf(items[0])
    }
    StorageRadioDialog(
        isOpen = isDialogOpen,
        icon = icon,
        items = items,
        selected = selected,
        onConfirm = {
            subtitle.value = items[it].display
            onConfirm(items[it])
        })
    IconButtonClickable(
        title = stringResource(id = R.string.backup_dir),
        subtitle = subtitle.value,
        icon = icon,
        iconButton = Icons.Rounded.Edit,
        onClick = {
            scope.launch {
                val (list, value) = onPrepare()
                items.apply {
                    clear()
                    addAll(list)
                }
                selected.value = value
                isDialogOpen.value = true
            }
        },
        onIconButtonClick = onEdit
    )
}
