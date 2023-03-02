package com.xayah.databackup.ui.activity.list.components.menu.item

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.TitleMediumText

@Composable
fun Item(
    title: String,
    rowContent: LazyListScope.() -> Unit
) {
    val smallPadding = dimensionResource(R.dimen.padding_small)

    TitleMediumText(text = title)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(smallPadding),
        content = rowContent
    )
}
