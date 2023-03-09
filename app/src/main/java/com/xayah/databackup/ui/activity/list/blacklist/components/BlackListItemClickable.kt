package com.xayah.databackup.ui.activity.list.blacklist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.BodySmallText
import com.xayah.databackup.ui.components.TitleMediumText

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
            TitleMediumText(text = title)
        },
        supportingText = {
            BodySmallText(text = subtitle)
        },
        trailingContent = {
            IconButton(onClick = onIconButtonClick) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
            }
        },
    )
}
