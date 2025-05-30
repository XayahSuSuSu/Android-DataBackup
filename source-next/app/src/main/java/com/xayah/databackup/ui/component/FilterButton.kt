package com.xayah.databackup.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xayah.databackup.ui.theme.DataBackupTheme

@Immutable
data class FilterButtonColors(
    val circleColor: Color,
    val selectedCircleColor: Color,
    val iconColor: Color,
    val selectedIconColor: Color,
    val titleColor: Color,
    val selectedTitleColor: Color,
)

@Composable
fun filterButtonPrimaryColors(
    circleColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    selectedCircleColor: Color = MaterialTheme.colorScheme.primary,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedIconColor: Color = MaterialTheme.colorScheme.onPrimary,
    titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTitleColor: Color = MaterialTheme.colorScheme.primary,
) = FilterButtonColors(
    circleColor = circleColor,
    selectedCircleColor = selectedCircleColor,
    iconColor = iconColor,
    selectedIconColor = selectedIconColor,
    titleColor = titleColor,
    selectedTitleColor = selectedTitleColor,
)

@Composable
fun filterButtonSecondaryColors(
    circleColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    selectedCircleColor: Color = MaterialTheme.colorScheme.secondary,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedIconColor: Color = MaterialTheme.colorScheme.onSecondary,
    titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTitleColor: Color = MaterialTheme.colorScheme.secondary,
) = FilterButtonColors(
    circleColor = circleColor,
    selectedCircleColor = selectedCircleColor,
    iconColor = iconColor,
    selectedIconColor = selectedIconColor,
    titleColor = titleColor,
    selectedTitleColor = selectedTitleColor,
)

@Composable
fun filterButtonGreenColors(
    circleColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    selectedCircleColor: Color = DataBackupTheme.greenColorScheme.primary,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedIconColor: Color = DataBackupTheme.greenColorScheme.onPrimary,
    titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTitleColor: Color = DataBackupTheme.greenColorScheme.primary,
) = FilterButtonColors(
    circleColor = circleColor,
    selectedCircleColor = selectedCircleColor,
    iconColor = iconColor,
    selectedIconColor = selectedIconColor,
    titleColor = titleColor,
    selectedTitleColor = selectedTitleColor,
)

@Composable
fun FilterButton(
    selected: Boolean,
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    colors: FilterButtonColors = filterButtonPrimaryColors(),
    onClick: () -> Unit
) {
    val animatedCircleColor by animateColorAsState(
        targetValue = if (selected) colors.selectedCircleColor else colors.circleColor,
        label = "animatedOnSurfaceColor"
    )
    val animatedTitleColor by animateColorAsState(
        targetValue = if (selected) colors.selectedTitleColor else colors.titleColor,
        label = "animatedOnSurfaceColor"
    )
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        Surface(modifier = Modifier.semantics { role = Role.Button }, onClick = onClick, color = Color.Transparent) {
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = animatedCircleColor) {
                    Box(contentAlignment = Alignment.Center) {
                        icon.invoke()
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedContent(
                    targetState = title,
                    transitionSpec = textTransitionSpec()
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = it,
                        style = MaterialTheme.typography.titleSmall,
                        color = animatedTitleColor,
                        textAlign = TextAlign.Center,
                    )
                }
                if (subtitle != null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = animatedTitleColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun FilterButton(
    selected: Boolean,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    colors: FilterButtonColors = filterButtonPrimaryColors(),
    onClick: () -> Unit
) {
    val animatedIconColor by animateColorAsState(
        targetValue = if (selected) colors.selectedIconColor else colors.iconColor,
        label = "animatedOnSurfaceColor"
    )
    FilterButton(
        selected = selected,
        title = title,
        subtitle = subtitle,
        icon = {
            Icon(
                imageVector = icon,
                tint = animatedIconColor,
                contentDescription = null
            )
        },
        colors = colors,
        onClick = onClick
    )
}

@Composable
fun FilterButton(
    selected: Boolean,
    title: String,
    subtitle: String? = null,
    icon: Painter,
    colors: FilterButtonColors = filterButtonPrimaryColors(),
    onClick: () -> Unit
) {
    val animatedIconColor by animateColorAsState(
        targetValue = if (selected) colors.selectedIconColor else colors.iconColor,
        label = "animatedOnSurfaceColor"
    )
    FilterButton(
        selected = selected,
        title = title,
        subtitle = subtitle,
        icon = {
            Icon(
                painter = icon,
                tint = animatedIconColor,
                contentDescription = null
            )
        },
        colors = colors,
        onClick = onClick
    )
}
