package com.xayah.databackup.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class ActionButtonState {
    NORMAL,
    ERROR
}

@Composable
fun SmallActionButton(
    modifier: Modifier,
    state: ActionButtonState = ActionButtonState.NORMAL,
    icon: ImageVector,
    title: String,
    titleShimmer: Boolean = false,
    subtitle: String,
    subtitleShimmer: Boolean = false,
    action: (@Composable BoxScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    val animatedIconColor by animateColorAsState(
        targetValue = when (state) {
            ActionButtonState.NORMAL -> MaterialTheme.colorScheme.primary
            ActionButtonState.ERROR -> MaterialTheme.colorScheme.primary
        },
        label = "animatedColor"
    )
    val animatedTitleColor by animateColorAsState(
        targetValue = when (state) {
            ActionButtonState.NORMAL -> MaterialTheme.colorScheme.onSurface
            ActionButtonState.ERROR -> MaterialTheme.colorScheme.error
        },
        label = "animatedColor"
    )
    val animatedSubtitleColor by animateColorAsState(
        targetValue = when (state) {
            ActionButtonState.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
            ActionButtonState.ERROR -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "animatedColor"
    )
    val animatedContainerColor by animateColorAsState(
        targetValue = when (state) {
            ActionButtonState.NORMAL -> MaterialTheme.colorScheme.surfaceContainer
            ActionButtonState.ERROR -> MaterialTheme.colorScheme.errorContainer
        },
        label = "animatedColor"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = animatedContainerColor),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp)
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    tint = animatedIconColor,
                    imageVector = icon,
                    contentDescription = "Localized description"
                )
                Text(
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 4.dp)
                        .shimmer(titleShimmer),
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = animatedTitleColor
                )
                Text(
                    modifier = Modifier.shimmer(subtitleShimmer),
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = animatedSubtitleColor
                )
            }

            action?.invoke(this)
        }
    }
}

@Composable
fun SmallCheckActionButton(
    modifier: Modifier,
    state: ActionButtonState = ActionButtonState.NORMAL,
    checked: Boolean,
    checkBoxVisible: Boolean = true,
    icon: ImageVector,
    title: String,
    titleShimmer: Boolean = false,
    subtitle: String,
    subtitleShimmer: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit
) {
    SmallActionButton(
        modifier = modifier,
        state = state,
        icon = icon,
        title = title,
        titleShimmer = titleShimmer,
        subtitle = subtitle,
        subtitleShimmer = subtitleShimmer,
        action = {
            FadeVisibility(
                modifier = Modifier.align(Alignment.TopEnd),
                visible = checkBoxVisible,
            ) {
                Checkbox(
                    modifier = Modifier.size(48.dp),
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
        },
        onClick = onClick
    )
}

@Composable
fun ActionButton(modifier: Modifier, icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
                imageVector = icon,
                contentDescription = "Localized description"
            )
            Column(modifier = Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
