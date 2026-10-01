package com.xayah.databackup.ui.component

import androidx.annotation.FloatRange
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ProcessItemHolder(
    modifier: Modifier,
    @FloatRange(0.0, 1.0) process: () -> Float,
    showProgress: Boolean = true,
    item: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item()
        AnimatedVisibility(
            visible = showProgress,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val animatedProgress: Float by animateFloatAsState(
                targetValue = process.invoke(),
                label = "progress",
                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessVeryLow)
            )
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = { animatedProgress })
        }
    }
}

@Composable
fun ProcessItemCard(
    icon: ImageVector,
    title: String,
    label: String,
    subtitle: String,
    subtitleShimmer: Boolean,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    ProcessItemCardContainer(onClick = onClick) {
        ProcessItemCardContent(
            icon = icon,
            title = title,
            label = label,
            subtitle = subtitle,
            subtitleShimmer = subtitleShimmer,
            trailingContent = trailingContent,
        )
    }
}

@Composable
fun ProcessItemCardContainer(onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        content()
    }
}

@Composable
fun ProcessItemCardContent(
    icon: ImageVector,
    title: String,
    label: String,
    subtitle: String,
    subtitleShimmer: Boolean,
    trailingContent: (@Composable () -> Unit)? = null,
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
                    modifier = Modifier.shimmer(subtitleShimmer),
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
                            modifier = Modifier.shimmer(subtitleShimmer),
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
        trailingContent?.invoke()
    }
}
