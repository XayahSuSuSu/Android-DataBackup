package com.xayah.databackup.compose.ui.activity.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import com.xayah.databackup.R

data class DescItem(
    val title: String,
    val subtitle: String
)

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
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
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
                itemVerticalArrangement = Arrangement.spacedBy(smallPadding)
            ) {
                Column {
                    Text(
                        text = it.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = it.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        onConfirm = onConfirm,
    )
}
