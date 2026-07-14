package com.xayah.databackup.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R

private val FloatingNavigationItemMaxWidth = 76.dp
private val FloatingNavigationItemMinHeight = 56.dp
private val FloatingNavigationBarPadding = 4.dp
private val FloatingNavigationBarHorizontalInsets = FloatingNavigationBarPadding * 2
private val FloatingNavigationLabelHorizontalPadding = FloatingNavigationBarPadding * 2
private val FloatingNavigationShadowRadius = 16.dp
private val FloatingNavigationFocusBorderWidth = 2.dp
private val FloatingNavigationItemSpacing = 1.dp

private const val DARK_BACKGROUND_LUMINANCE_THRESHOLD = 0.5f
private const val DARK_SHADOW_ALPHA = 0.14f
private const val LIGHT_SHADOW_ALPHA = 0.07f
private const val SELECTED_INDICATOR_ALPHA = 0.15f
private const val HOVER_INDICATOR_ALPHA = 0.08f
private const val INDICATOR_SPRING_STIFFNESS = 1_000f

private data class FloatingNavigationItem(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
)

private val FloatingNavigationItems = listOf(
    FloatingNavigationItem(R.string.home, R.drawable.ic_layout_grid),
    FloatingNavigationItem(R.string.backup, R.drawable.ic_archive),
    FloatingNavigationItem(R.string.schedule, R.drawable.ic_calendar_check),
    FloatingNavigationItem(R.string.settings, R.drawable.ic_settings),
)
private val FloatingNavigationBarMaxWidth = FloatingNavigationItemMaxWidth * FloatingNavigationItems.size + FloatingNavigationBarHorizontalInsets
private val FloatingNavigationPressIndication = ripple(
    bounded = false,
    radius = FloatingNavigationBarMaxWidth,
    enableFocusIndication = false,
    enableHoverIndication = false,
    enableDragIndication = false,
)

@Composable
fun FloatingNavigationBar(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(selectedIndex in FloatingNavigationItems.indices) {
        "selectedIndex must be in ${FloatingNavigationItems.indices}, but was $selectedIndex"
    }
    val isDark = MaterialTheme.colorScheme.background.luminance() < DARK_BACKGROUND_LUMINANCE_THRESHOLD

    Surface(
        modifier = modifier
            .widthIn(max = FloatingNavigationBarMaxWidth)
            .fillMaxWidth()
            .dropShadow(
                shape = CircleShape,
                shadow = Shadow(
                    radius = FloatingNavigationShadowRadius,
                    color = Color.Black,
                    alpha = if (isDark) DARK_SHADOW_ALPHA else LIGHT_SHADOW_ALPHA,
                ),
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val itemWidth = (maxWidth - FloatingNavigationBarHorizontalInsets).coerceAtLeast(0.dp) / FloatingNavigationItems.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = INDICATOR_SPRING_STIFFNESS,
                ),
                label = "floatingNavigationIndicator",
            )

            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = (indicatorOffset + FloatingNavigationBarPadding).roundToPx(), y = 0) }
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(vertical = FloatingNavigationBarPadding)
                        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_INDICATOR_ALPHA), shape = CircleShape),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FloatingNavigationBarPadding)
                    .selectableGroup(),
            ) {
                FloatingNavigationItems.forEachIndexed { index, item ->
                    FloatingNavigationBarItem(
                        item = item,
                        selected = selectedIndex == index,
                        onClick = { onSelected(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.FloatingNavigationBarItem(
    item: FloatingNavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = FloatingNavigationItemMinHeight)
            .then(
                if (isHovered) {
                    Modifier.background(color = MaterialTheme.colorScheme.onSurface.copy(alpha = HOVER_INDICATOR_ALPHA), shape = CircleShape)
                } else {
                    Modifier
                }
            )
            .then(
                if (isFocused) {
                    Modifier.border(
                        width = FloatingNavigationFocusBorderWidth,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
                } else {
                    Modifier
                }
            )
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = FloatingNavigationPressIndication,
                role = Role.Tab,
                onClick = { if (!selected) onClick() },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = FloatingNavigationItemSpacing,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(item.iconRes),
            contentDescription = null,
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FloatingNavigationLabelHorizontalPadding),
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}
