package com.xayah.databackup.ui.activity.list.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Composable
fun <T> MenuTopBatchDeleteButton(
    isOpen: MutableState<Boolean>,
    selectedItems: List<T>,
    itemText: (Int) -> String,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val maxHeight = dimensionResource(R.dimen.dialog_delete_content_max_height)

    ConfirmDialog(
        isOpen = isOpen,
        icon = Icons.Rounded.Info,
        title = "${stringResource(id = R.string.delete)}(${selectedItems.size})",
        content = {
            Column {
                Divider(
                    modifier = Modifier.padding(
                        nonePadding,
                        smallPadding,
                        nonePadding,
                        nonePadding
                    )
                )
                LazyColumn(modifier = Modifier.sizeIn(maxHeight = maxHeight)) {
                    item {
                        Spacer(modifier = Modifier.height(smallPadding))
                    }
                    items(selectedItems.size) {
                        Text(text = itemText(it))
                    }
                    item {
                        Spacer(modifier = Modifier.height(smallPadding))
                    }
                }
                Divider(
                    modifier = Modifier.padding(
                        nonePadding,
                        nonePadding,
                        nonePadding,
                        smallPadding
                    )
                )
                Text(
                    text = stringResource(id = R.string.delete_confirm) +
                            stringResource(id = R.string.symbol_question),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }, onConfirm = onDelete
    )

    MenuTopActionButton(
        icon = Icons.Outlined.Delete,
        title = stringResource(R.string.delete_selected)
    ) {
        if (selectedItems.isNotEmpty()) {
            isOpen.value = true
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.selection_is_empty),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
