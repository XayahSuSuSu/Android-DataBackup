package com.xayah.databackup.ui.activity.settings.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.settings.components.DescItem
import com.xayah.databackup.ui.components.BodySmallText
import com.xayah.databackup.ui.components.TitleMediumText

@ExperimentalMaterial3Api
@Composable
fun RadioButtonDescDialog(
    isOpen: MutableState<Boolean>,
    icon: ImageVector,
    title: String,
    items: List<DescItem>,
    selected: MutableState<DescItem>,
    onConfirm: (index: Int) -> Unit,
) {
    val smallPadding = dimensionResource(R.dimen.padding_small)
    RadioButtonDialog(
        isOpen = isOpen,
        icon = icon,
        title = title,
        items = items,
        selected = selected,
        content = {
            RadioButtonGroup(
                items = items,
                selected = selected,
                itemVerticalArrangement = Arrangement.spacedBy(smallPadding),
                onItemEnabled = { it.enabled }
            ) {
                Column {
                    TitleMediumText(text = it.title)
                    BodySmallText(text = it.subtitle)
                }
            }
        },
        onConfirm = onConfirm,
    )
}
