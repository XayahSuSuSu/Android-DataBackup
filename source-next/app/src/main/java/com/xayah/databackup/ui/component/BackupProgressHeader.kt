package com.xayah.databackup.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xayah.databackup.util.SymbolHelper

@Composable
fun BackupProgressHeader(
    progress: String,
    statusLabel: String,
    showLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(start = 16.dp, top = 40.dp, bottom = 12.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = progress,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
                val percent by remember { mutableStateOf(SymbolHelper.PERCENT.toString()) }
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = percent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            AnimatedContent(
                targetState = statusLabel,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "statusLabelAnimation",
            ) { label ->
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        FadeVisibility(visible = showLoading) {
            ContainedLoadingIndicator(modifier = Modifier.size(64.dp))
        }
    }
}
