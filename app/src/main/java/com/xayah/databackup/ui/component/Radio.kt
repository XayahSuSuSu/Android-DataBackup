package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.xayah.databackup.ui.token.RadioTokens

@ExperimentalMaterial3Api
@Composable
fun <T> RadioButtonGroup(
    items: List<T>,
    defSelected: T,
    itemVerticalArrangement: Arrangement.Vertical = Arrangement.Top,
    onItemClick: (item: T) -> Unit,
    onItemEnabled: (item: T) -> Boolean,
    content: @Composable (item: T) -> Unit
) {
    var selected by remember { mutableStateOf(defSelected) }
    LaunchedEffect(null) {
        onItemClick(defSelected)
    }
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = itemVerticalArrangement) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadioTokens.ItemRoundedCorner))
                    .selectable(
                        selected = item == selected,
                        onClick = {
                            if (onItemEnabled(item)) {
                                selected = item
                                onItemClick(item)
                            }
                        },
                        role = Role.RadioButton
                    )
                    .paddingHorizontal(RadioTokens.ItemHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadioTokens.ItemHorizontalPadding),
            ) {
                RadioButton(
                    selected = item == selected,
                    enabled = onItemEnabled(item),
                    onClick = null
                )
                content(item)
            }
        }
    }
}
