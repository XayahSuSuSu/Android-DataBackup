package com.xayah.databackup.util

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

inline fun <T> LazyListScope.items(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
) =
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(index, items[index]) } else null,
        contentType = { index: Int -> contentType(index, items[index]) },
    ) {
        itemContent(it, items[it])
    }
