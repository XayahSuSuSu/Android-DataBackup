package com.xayah.databackup.ui.activity.blacklist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Composable
fun BlackListItemClickable(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onIconButtonClick: () -> Unit,
) {
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    ListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(mediumPadding))
            .clickable { onClick() },
        headlineText = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        supportingText = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        },
        trailingContent = {
            IconButton(onClick = onIconButtonClick) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
            }
        },
    )
}
