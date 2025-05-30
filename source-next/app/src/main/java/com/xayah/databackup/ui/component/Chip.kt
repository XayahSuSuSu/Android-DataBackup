package com.xayah.databackup.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R

@Composable
fun SelectableChip(selected: Boolean, icon: AnimatedImageVector, text: String, onCheckedChange: (Boolean) -> Unit) {
    val animatedCorner by animateDpAsState(
        targetValue = if (selected) 18.dp else 8.dp,
        label = "animatedCorner"
    )
    val animatedSurfaceColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "animatedSurfaceColor"
    )
    val animatedOnSurfaceColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
        label = "animatedOnSurfaceColor"
    )
    val animatedCheckIcon = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animated_check),
        atEnd = selected
    )
    val animatedIcon = rememberAnimatedVectorPainter(
        animatedImageVector = icon,
        atEnd = selected.not()
    )
    Surface(
        shape = RoundedCornerShape(animatedCorner),
        color = animatedSurfaceColor,
        onClick = { onCheckedChange.invoke(selected) }
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = if (selected) animatedCheckIcon else animatedIcon,
                tint = animatedOnSurfaceColor,
                contentDescription = null
            )

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = animatedOnSurfaceColor
            )
        }
    }
}
