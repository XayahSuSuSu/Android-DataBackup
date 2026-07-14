package com.xayah.databackup.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val DisabledOpacity = 0.38f

data class SelectablePreferenceItemInfo(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

@Composable
fun SelectablePreferenceGroup(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedIndex: Int,
    items: List<SelectablePreferenceItemInfo>,
    onSelectedIndexChanged: (Int) -> Unit,
) {
    PreferenceGroup(modifier = modifier) {
        items.forEachIndexed { index, item ->
            SelectablePreferenceItem(
                selected = index == selectedIndex,
                enabled = enabled,
                icon = item.icon,
                title = item.title,
                subtitle = item.subtitle,
                onClick = { onSelectedIndexChanged(index) },
            )
        }
    }
}

@Composable
private fun SelectablePreferenceItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    enabled: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val animatedContainerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "animatedContainerColor"
    )
    val animatedIconColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = DisabledOpacity),
        label = "animatedIconColor"
    )
    val animatedTitleColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledOpacity),
        label = "animatedTitleColor"
    )
    val animatedSubtitleColor by animateColorAsState(
        targetValue = if (enabled)
            MaterialTheme.colorScheme.onSurfaceVariant
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(DisabledOpacity),
        label = "animatedSubtitleColor"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        color = animatedContainerColor,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                tint = animatedIconColor,
                imageVector = icon,
                contentDescription = null
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = animatedTitleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = animatedSubtitleColor
                )
            }
        }
    }
}
