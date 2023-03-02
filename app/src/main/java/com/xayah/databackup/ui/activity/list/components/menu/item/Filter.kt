package com.xayah.databackup.ui.activity.list.components.menu.item

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.xayah.databackup.ui.activity.list.components.FilterItem
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalMaterial3Api
@Composable
fun <T> FilterItem(
    title: String,
    list: List<FilterItem<T>>,
    filter: MutableStateFlow<T>,
    onClick: (T) -> Unit,
) {
    val filterState = filter.collectAsState()
    Item(title = title) {
        items(items = list) {
            FilterChip(
                selected = filterState.value == it.type,
                onClick = {
                    if (filterState.value != it.type) {
                        filter.value = it.type
                        onClick(it.type)
                    }
                },
                label = { Text(text = it.text) },
                leadingIcon = if (filterState.value == it.type) {
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
