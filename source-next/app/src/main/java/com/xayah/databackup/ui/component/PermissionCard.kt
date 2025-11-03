package com.xayah.databackup.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R
import com.xayah.databackup.ui.theme.DataBackupTheme

sealed class CardState {
    data object Idle : CardState()
    data object Waiting : CardState()
    data object Success : CardState()
    data object Error : CardState()
}

@Composable
fun PermissionCard(
    state: CardState = CardState.Idle,
    icon: ImageVector,
    title: String,
    content: String,
    onClick: () -> Unit,
) {
    PermissionCard(state, icon, title, content, onClick, null, null, null)
}

@Composable
fun PermissionCard(
    state: CardState = CardState.Idle,
    icon: ImageVector,
    title: String,
    content: String,
    onClick: () -> Unit,
    actionIcon: ImageVector?,
    actionIconDescription: String?,
    onActionButtonClick: (() -> Unit)?
) {
    val animatedColor by animateColorAsState(
        targetValue = when (state) {
            is CardState.Idle, CardState.Waiting -> MaterialTheme.colorScheme.surfaceContainer
            is CardState.Success -> DataBackupTheme.greenColorScheme.surfaceContainer
            is CardState.Error -> MaterialTheme.colorScheme.errorContainer
        },
        label = "animatedColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = animatedColor),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            FadeVisibility(state == CardState.Waiting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = StrokeCap.Round,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = icon,
                        contentDescription = null
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                AnimatedContent(
                    targetState = content,
                    label = "Animated content"
                ) { targetContent ->
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = targetContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (actionIcon != null) {
                IconButton(modifier = Modifier.align(Alignment.TopEnd), onClick = { onActionButtonClick?.invoke() }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
                        contentDescription = actionIconDescription
                    )
                }
            }
        }
    }
}
