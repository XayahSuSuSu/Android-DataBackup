package com.xayah.databackup.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.ui.theme.tone

@Immutable
data class SelectableCardButtonColors(
    val circleColor: Color,
    val selectedCircleColor: Color,
    val iconColor: Color,
    val selectedIconColor: Color,
    val iconButtonColor: Color,
    val selectedIconButtonColor: Color,
    val titleColor: Color,
    val selectedTitleColor: Color,
    val subtitleColor: Color,
    val selectedSubtitleColor: Color,
    val borderColor: Color,
    val selectedBorderColor: Color,
    val backgroundColor: Color,
    val selectedBackgroundColor: Color,
)

@Composable
fun selectableCardButtonPrimaryColors(
    circleColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    selectedCircleColor: Color = MaterialTheme.colorScheme.primary,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedIconColor: Color = MaterialTheme.colorScheme.onPrimary,
    iconButtonColor: Color = MaterialTheme.colorScheme.outline,
    selectedIconButtonColor: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTitleColor: Color = MaterialTheme.colorScheme.primary,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedSubtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    selectedBorderColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow.tone(if (isSystemInDarkTheme()) 8 else 98),
    selectedBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer.tone(if (isSystemInDarkTheme()) 12 else 96),
) = SelectableCardButtonColors(
    circleColor = circleColor,
    selectedCircleColor = selectedCircleColor,
    iconColor = iconColor,
    selectedIconColor = selectedIconColor,
    iconButtonColor = iconButtonColor,
    selectedIconButtonColor = selectedIconButtonColor,
    titleColor = titleColor,
    selectedTitleColor = selectedTitleColor,
    subtitleColor = subtitleColor,
    selectedSubtitleColor = selectedSubtitleColor,
    borderColor = borderColor,
    selectedBorderColor = selectedBorderColor,
    backgroundColor = backgroundColor,
    selectedBackgroundColor = selectedBackgroundColor,
)

@Composable
fun selectableCardButtonSecondaryColors(
    selectedCircleColor: Color = MaterialTheme.colorScheme.secondary,
    selectedIconColor: Color = MaterialTheme.colorScheme.onSecondary,
    selectedIconButtonColor: Color = MaterialTheme.colorScheme.secondary,
    selectedTitleColor: Color = MaterialTheme.colorScheme.secondary,
    selectedBorderColor: Color = MaterialTheme.colorScheme.secondary,
) = selectableCardButtonPrimaryColors(
    selectedCircleColor = selectedCircleColor,
    selectedIconColor = selectedIconColor,
    selectedIconButtonColor = selectedIconButtonColor,
    selectedTitleColor = selectedTitleColor,
    selectedBorderColor = selectedBorderColor,
)

@Composable
fun selectableCardButtonTertiaryColors(
    selectedCircleColor: Color = MaterialTheme.colorScheme.tertiary,
    selectedIconColor: Color = MaterialTheme.colorScheme.onTertiary,
    selectedIconButtonColor: Color = MaterialTheme.colorScheme.tertiary,
    selectedTitleColor: Color = MaterialTheme.colorScheme.tertiary,
    selectedBorderColor: Color = MaterialTheme.colorScheme.tertiary,
) = selectableCardButtonPrimaryColors(
    selectedCircleColor = selectedCircleColor,
    selectedIconColor = selectedIconColor,
    selectedIconButtonColor = selectedIconButtonColor,
    selectedTitleColor = selectedTitleColor,
    selectedBorderColor = selectedBorderColor,
)

@Composable
fun selectableCardButtonGreenColors(
    selectedCircleColor: Color = DataBackupTheme.greenColorScheme.primary,
    selectedIconColor: Color = DataBackupTheme.greenColorScheme.onPrimary,
    selectedIconButtonColor: Color = DataBackupTheme.greenColorScheme.primary,
    selectedTitleColor: Color = DataBackupTheme.greenColorScheme.primary,
    selectedBorderColor: Color = DataBackupTheme.greenColorScheme.primary,
    selectedBackgroundColor: Color = DataBackupTheme.greenColorScheme.surfaceContainer.tone(if (isSystemInDarkTheme()) 12 else 98),
) = selectableCardButtonPrimaryColors(
    selectedCircleColor = selectedCircleColor,
    selectedIconColor = selectedIconColor,
    selectedIconButtonColor = selectedIconButtonColor,
    selectedTitleColor = selectedTitleColor,
    selectedBorderColor = selectedBorderColor,
    selectedBackgroundColor = selectedBackgroundColor,
)

@Composable
fun SelectableCardButton(
    modifier: Modifier,
    selected: Boolean,
    title: String,
    titleShimmer: Boolean = false,
    subtitle: String? = null,
    subtitleShimmer: Boolean = false,
    icon: @Composable () -> Unit,
    iconShimmer: Boolean = false,
    iconButton: (@Composable () -> Unit)? = null,
    colors: SelectableCardButtonColors = selectableCardButtonPrimaryColors(),
    onClick: () -> Unit,
) {
    val animatedCircleColor by animateColorAsState(
        targetValue = if (selected) colors.selectedCircleColor else colors.circleColor,
        label = "animatedColor"
    )
    val animatedTitleColor by animateColorAsState(
        targetValue = if (selected) colors.selectedTitleColor else colors.titleColor,
        label = "animatedColor"
    )
    val animatedSubtitleColor by animateColorAsState(
        targetValue = if (selected) colors.selectedSubtitleColor else colors.subtitleColor,
        label = "animatedColor"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (selected) colors.selectedBorderColor else colors.borderColor,
        label = "animatedColor"
    )
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (selected) colors.selectedBackgroundColor else colors.backgroundColor,
        label = "animatedColor"
    )

    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, animatedBorderColor),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            if (iconButton != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    iconButton.invoke()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .shimmer(iconShimmer),
                    shape = CircleShape,
                    color = animatedCircleColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (iconShimmer.not()) {
                            icon.invoke()
                        }
                    }
                }
                AnimatedContent(
                    targetState = title,
                    transitionSpec = textTransitionSpec()
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                            .shimmer(titleShimmer),
                        text = it,
                        style = MaterialTheme.typography.titleSmall,
                        color = animatedTitleColor,
                        textAlign = TextAlign.Center,
                    )
                }
                if (subtitle != null) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                            .shimmer(subtitleShimmer),
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = animatedSubtitleColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun SelectableCardButton(
    modifier: Modifier,
    selected: Boolean,
    title: String,
    titleShimmer: Boolean = false,
    subtitle: String? = null,
    subtitleShimmer: Boolean = false,
    icon: ImageVector,
    iconShimmer: Boolean = false,
    iconButton: ImageVector? = null,
    onIconButtonClick: (() -> Unit)? = null,
    colors: SelectableCardButtonColors = selectableCardButtonPrimaryColors(),
    onClick: () -> Unit
) {
    val animatedIconColor by animateColorAsState(
        targetValue = if (selected) colors.selectedIconColor else colors.iconColor,
        label = "animatedColor"
    )
    val animatedIconButtonColor by animateColorAsState(
        targetValue = if (selected) colors.selectedIconButtonColor else colors.iconButtonColor,
        label = "animatedColor"
    )
    SelectableCardButton(
        modifier = modifier,
        selected = selected,
        title = title,
        titleShimmer = titleShimmer,
        subtitle = subtitle,
        subtitleShimmer = subtitleShimmer,
        icon = {
            Icon(
                imageVector = icon,
                tint = animatedIconColor,
                contentDescription = null
            )
        },
        iconShimmer = iconShimmer,
        iconButton = if (iconButton != null) {
            {
                IconButton(onClick = { onIconButtonClick?.invoke() }) {
                    Icon(
                        imageVector = iconButton,
                        tint = animatedIconButtonColor,
                        contentDescription = null
                    )
                }
            }
        } else {
            null
        },
        colors = colors,
        onClick = onClick
    )
}
