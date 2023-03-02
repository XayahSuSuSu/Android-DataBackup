package com.xayah.databackup.ui.activity.list.components.menu.item

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.databackup.data.AppListSort
import com.xayah.databackup.ui.activity.list.components.SortItem

@ExperimentalMaterial3Api
@Composable
fun SortItem(
    list: List<SortItem>,
    active: State<AppListSort>,
    ascending: State<Boolean>,
    onClick: (AppListSort) -> Unit,
) {
    Item(title = stringResource(id = R.string.sort)) {
        items(items = list) {
            AssistChip(
                onClick = {
                    onClick(it.type)
                },
                label = { Text(text = it.text) },
                leadingIcon = if (active.value == it.type) {
                    {
                        Icon(
                            imageVector = if (ascending.value) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}
