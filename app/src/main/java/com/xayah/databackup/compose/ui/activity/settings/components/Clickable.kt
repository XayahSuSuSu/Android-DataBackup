package com.xayah.databackup.compose.ui.activity.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun ClickablePreview() {
    Clickable(
        title = stringResource(id = R.string.backup_user),
        subtitle = stringResource(id = R.string.settings_backup_user_subtitle),
        icon = ImageVector.vectorResource(id = R.drawable.ic_round_person),
        content = "0",
        onClick = {}
    )
}

@ExperimentalMaterial3Api
@Composable
fun Clickable(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: String,
    onClick: () -> Unit,
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
            Text(
                text = content,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        }
    )
}
