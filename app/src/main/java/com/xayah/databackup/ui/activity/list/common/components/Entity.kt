package com.xayah.databackup.ui.activity.list.common.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.xayah.databackup.data.AppListSort

data class ManifestDescItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconId: Int,
    val icon: ImageVector? = null,
)

data class SortItem(
    val text: String,
    val type: AppListSort,
)

data class FilterItem<T>(
    val text: String,
    val type: T,
)

data class SelectionItem(
    val text: String,
    var selected: Boolean,
)
