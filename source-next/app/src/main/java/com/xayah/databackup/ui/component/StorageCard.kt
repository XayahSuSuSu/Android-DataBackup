package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R

@Composable
fun StorageCard(modifier: Modifier, free: Float, other: Float, backups: Float, title: String, subtitle: String, progress: String, storage: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth(),
                contentAlignment = Alignment.Center
            ) {
                SegmentCircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    segments = listOf(
                        Segment(progress = { backups }, color = MaterialTheme.colorScheme.outlineVariant, trackColor = MaterialTheme.colorScheme.primary),
                        Segment(progress = { other }, color = MaterialTheme.colorScheme.outlineVariant, trackColor = MaterialTheme.colorScheme.secondary),
                        Segment(progress = { free }, trackProgress = { 0f }, color = MaterialTheme.colorScheme.outlineVariant, trackColor = MaterialTheme.colorScheme.outlineVariant),
                    ),
                    segmentGap = 0.02f,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = progress, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = storage, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(12.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) { }
                    Text(text = stringResource(R.string.backups), modifier = Modifier.padding(start = 4.dp, end = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(modifier = Modifier.size(12.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondary) { }
                    Text(text = stringResource(R.string.other), modifier = Modifier.padding(start = 4.dp, end = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(modifier = Modifier.size(12.dp), shape = CircleShape, color = MaterialTheme.colorScheme.outlineVariant) { }
                    Text(text = stringResource(R.string.free), modifier = Modifier.padding(start = 4.dp, end = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}