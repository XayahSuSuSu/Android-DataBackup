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
import androidx.compose.ui.unit.dp

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
                .padding(16.dp),
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
                    modifier = Modifier.size(80.dp),
                    segments = listOf(
                        Segment(progress = { backups }, color = MaterialTheme.colorScheme.primaryContainer, trackColor = MaterialTheme.colorScheme.primary),
                        Segment(progress = { other }, color = MaterialTheme.colorScheme.secondaryContainer, trackColor = MaterialTheme.colorScheme.secondary),
                        Segment(progress = { free }, trackProgress = { 0f }, color = MaterialTheme.colorScheme.tertiaryContainer, trackColor = MaterialTheme.colorScheme.tertiary),
                    ),
                    segmentGap = 0.02f,
                    strokeWidth = 6.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = progress, style = MaterialTheme.typography.titleSmall)
                    Text(text = storage, style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(modifier = Modifier.padding(top = 4.dp), text = subtitle, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(12.dp), shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) { }
                    Text(text = "Free", modifier = Modifier.padding(start = 4.dp, end = 8.dp), style = MaterialTheme.typography.bodySmall)
                    Surface(modifier = Modifier.size(12.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondary) { }
                    Text(text = "Other", modifier = Modifier.padding(start = 4.dp, end = 8.dp), style = MaterialTheme.typography.bodySmall)
                    Surface(modifier = Modifier.size(12.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) { }
                    Text(text = "Backups", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}