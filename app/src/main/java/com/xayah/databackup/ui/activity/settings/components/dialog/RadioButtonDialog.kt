package com.xayah.databackup.ui.activity.settings.components.dialog

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.vector.ImageVector
import com.xayah.databackup.ui.components.ConfirmDialog

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
    ConfirmDialog(
        isOpen = isOpen,
        icon = icon,
        title = title,
        content = {
            LazyColumn() {
                item {
                    content()
                }
            }
        }) {
        onConfirm(items.indexOf(selected.value))
    }
}
