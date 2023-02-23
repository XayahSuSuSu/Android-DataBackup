package com.xayah.databackup.ui.activity.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Composable
fun <T> RadioButtonGroup(
    items: List<T>,
    selected: MutableState<T>,
    itemVerticalArrangement: Arrangement.Vertical = Arrangement.Top,
    onItemClick: (item: T, selected: MutableState<T>) -> Unit = { i, s ->
        s.value = i
    },
    onItemEnabled: (item: T) -> Boolean = { true },
    content: @Composable (item: T) -> Unit
) {
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val settingsTitlePadding = dimensionResource(R.dimen.padding_settings_title)
    Column(
        Modifier.selectableGroup(),
        verticalArrangement = itemVerticalArrangement
    ) {
        items.forEach { item ->
            Row(
                Modifier
                    .defaultMinSize(nonePadding, settingsTitlePadding)
                    .fillMaxWidth()
                    .selectable(
                        selected = (item == selected.value),
                        onClick = {
                            if (onItemEnabled(item))
                                onItemClick(item, selected)
                        },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = mediumPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(mediumPadding),
            ) {
                RadioButton(
                    selected = (item == selected.value),
                    enabled = onItemEnabled(item),
                    onClick = null
                )
                content(item)
            }
        }
    }
}
