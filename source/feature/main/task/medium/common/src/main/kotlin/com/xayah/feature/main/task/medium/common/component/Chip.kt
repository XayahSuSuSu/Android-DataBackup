package com.xayah.feature.main.task.medium.common.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.value

@ExperimentalMaterial3Api
@Composable
fun OpChip(modifier: Modifier = Modifier, enabled: Boolean = true, selected: Boolean, label: StringResourceToken, onClick: () -> Unit) {
    FilterChip(
        modifier = modifier,
        enabled = enabled,
        selected = selected,
        onClick = onClick,
        label = { Text(text = label.value) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Done,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        }
    )
}
