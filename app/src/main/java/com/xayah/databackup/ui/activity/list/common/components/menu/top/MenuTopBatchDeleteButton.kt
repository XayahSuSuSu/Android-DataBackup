package com.xayah.databackup.ui.activity.list.common.components.menu.top

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
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
import com.xayah.databackup.ui.components.ConfirmDialog
import com.xayah.databackup.ui.components.paddingBottom
import com.xayah.databackup.ui.components.paddingTop
import com.xayah.databackup.util.makeShortToast

@ExperimentalMaterial3Api
@Composable
fun <T> MenuTopBatchDeleteButton(
    isOpen: MutableState<Boolean>,
    selectedItems: List<T>,
    itemText: (Int) -> String,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val maxHeight = dimensionResource(R.dimen.dialog_delete_content_max_height)

    ConfirmDialog(
        isOpen = isOpen,
        icon = Icons.Rounded.Info,
        title = "${stringResource(id = R.string.delete)}(${selectedItems.size})",
        content = {
            Column {
                Divider(
                    modifier = Modifier.paddingTop(smallPadding)
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
                    modifier = Modifier.paddingBottom(smallPadding)
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
            context.makeShortToast(context.getString(R.string.selection_is_empty))
        }
    }
}
