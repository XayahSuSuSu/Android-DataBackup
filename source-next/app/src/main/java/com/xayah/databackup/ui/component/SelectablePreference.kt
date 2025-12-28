package com.xayah.databackup.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
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
    val density = LocalDensity.current
    val itemHeightList = remember { mutableStateMapOf<Int, Int>() }
    val itemYList = remember { mutableStateMapOf<Int, Float>() }

    val animatedOffsetY by animateDpAsState(
        targetValue = with(density) { (itemYList[selectedIndex] ?: 0f).toDp() },
        label = "offsetY"
    )
    val animatedHeight by animateDpAsState(
        targetValue = with(density) { (itemHeightList[selectedIndex] ?: 0).toDp() },
        label = "height"
    )

    Box(modifier = modifier) {
        if (selectedIndex in items.indices) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(animatedHeight)
                    .offset(y = animatedOffsetY),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
            ) {}
        }

        Column {
            items.forEachIndexed { index, item ->
                SelectablePreferenceItem(
                    modifier = Modifier
                        .onGloballyPositioned {
                            itemHeightList[index] = it.size.height
                            itemYList[index] = it.positionInParent().y
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = enabled
                        ) { onSelectedIndexChanged(index) },
                    enabled = enabled,
                    icon = item.icon,
                    title = item.title,
                    subtitle = item.subtitle
                )
            }
        }
    }
}

@Composable
private fun SelectablePreferenceItem(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
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

    Row(
        modifier = modifier
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
