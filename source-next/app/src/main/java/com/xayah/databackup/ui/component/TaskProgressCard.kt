package com.xayah.databackup.ui.component

import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Display values only; backup and restore retain ownership of their outcome and progress semantics. */
internal data class TaskProgressCardState(
    val icon: ImageVector,
    val title: String,
    val label: String,
    val subtitle: String,
    val progress: Float = 0f,
    val isProcessing: Boolean = false,
)

@Composable
internal fun TaskProgressCard(
    category: TaskProgressCardState,
    isTerminal: Boolean,
    currentApp: TaskProgressCardState? = null,
) {
    val canShowCurrentApp = !isTerminal && currentApp != null
    var isCurrentAppVisible by rememberSaveable(isTerminal, canShowCurrentApp) { mutableStateOf(false) }
    val displayedItem = currentApp.takeIf { isCurrentAppVisible && canShowCurrentApp } ?: category
    TaskProgressCardLayout(
        modifier = Modifier.fillMaxWidth(),
        progress = { displayedItem.progress.coerceIn(0f, 1f) },
        isProgressVisible = !isTerminal && displayedItem.isProcessing,
    ) {
        TaskProgressCardContainer(
            onClick = if (canShowCurrentApp) {
                { isCurrentAppVisible = !isCurrentAppVisible }
            } else {
                null
            },
        ) {
            AnimatedContent(
                targetState = isCurrentAppVisible && canShowCurrentApp,
                transitionSpec = fadeSlideContentTransitionSpec(),
                label = "currentAppTransition",
            ) { isShowingApp ->
                val item = currentApp.takeIf { isShowingApp } ?: category
                TaskProgressCardContent(
                    icon = item.icon,
                    title = item.title,
                    label = item.label,
                    subtitle = item.subtitle,
                )
            }
        }
    }
}

@Composable
private fun TaskProgressCardLayout(
    modifier: Modifier,
    @FloatRange(0.0, 1.0) progress: () -> Float,
    isProgressVisible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        content()
        AnimatedVisibility(
            visible = isProgressVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val animatedProgress: Float by animateFloatAsState(
                targetValue = progress(),
                label = "progress",
                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessVeryLow)
            )
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = { animatedProgress })
        }
    }
}

@Composable
private fun TaskProgressCardContainer(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val shape = RoundedCornerShape(16.dp)
    if (onClick == null) {
        Card(modifier = Modifier.fillMaxWidth(), colors = colors, shape = shape, content = content)
    } else {
        Card(modifier = Modifier.fillMaxWidth(), colors = colors, shape = shape, onClick = onClick, content = content)
    }
}

@Composable
private fun TaskProgressCardContent(
    icon: ImageVector,
    title: String,
    label: String,
    subtitle: String,
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
            contentDescription = null
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FadeVisibility(visible = subtitle.isNotEmpty()) {
                    Row {
                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
