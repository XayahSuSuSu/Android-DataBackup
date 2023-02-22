package com.xayah.databackup.ui.activity.list.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.BottomSheet

@ExperimentalMaterial3Api
@Composable
fun ListBottomSheet(
    isOpen: MutableState<Boolean>,
    actions: LazyListScope.() -> Unit,
    content: @Composable() (ColumnScope.() -> Unit)? = null
) {
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val bigPadding = dimensionResource(R.dimen.padding_big)
    BottomSheet(isOpen = isOpen) {
        Column(modifier = Modifier.padding(bigPadding, nonePadding)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(bigPadding)) {
                actions()
            }
            if (content != null) {
                Divider(modifier = Modifier.padding(nonePadding, mediumPadding))
                content()
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bigPadding)
            )
        }
    }
}
