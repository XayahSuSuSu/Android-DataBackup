package com.xayah.databackup.ui.activity.list.common.components.menu.item

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.xayah.databackup.ui.activity.list.common.components.SelectionItem

@ExperimentalMaterial3Api
@Composable
fun SelectionItem(
    title: String,
    list: List<SelectionItem>,
    onClick: (Boolean) -> Unit,
) {
    Item(title = title) {
        items(items = list) {
            val isSelected = remember {
                mutableStateOf(it.selected)
            }
            FilterChip(
                selected = isSelected.value,
                onClick = {
                    it.selected = it.selected.not()
                    isSelected.value = it.selected
                    onClick(isSelected.value)
                },
                label = { Text(text = it.text) },
                leadingIcon = if (isSelected.value) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}
