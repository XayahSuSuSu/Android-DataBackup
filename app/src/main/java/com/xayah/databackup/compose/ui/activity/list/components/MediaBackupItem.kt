package com.xayah.databackup.compose.ui.activity.list.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import com.xayah.databackup.R
import com.xayah.databackup.data.MediaInfoBackup

@ExperimentalMaterial3Api
@Composable
fun MediaBackupItem(mediaInfoBackup: MediaInfoBackup) {
    val iconSmallSize = dimensionResource(R.dimen.icon_small_size)
    val tinyPadding = dimensionResource(R.dimen.padding_tiny)
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(mediumPadding))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                mediaInfoBackup.selectData = mediaInfoBackup.selectData.not()
            }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                modifier = Modifier.size(iconSmallSize),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_round_android),
                contentDescription = null
            )
            Column(
                modifier = Modifier
                    .padding(smallPadding, nonePadding)
                    .weight(1f)
            ) {
                Text(
                    text = mediaInfoBackup.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = mediaInfoBackup.path,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            FilledIconToggleButton(
                checked = mediaInfoBackup.selectData,
                onCheckedChange = { mediaInfoBackup.selectData = it }
            ) {
                if (mediaInfoBackup.selectData) {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_round_database),
                        contentDescription = stringResource(id = R.string.data)
                    )
                } else {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_round_database),
                        contentDescription = stringResource(id = R.string.data)
                    )
                }
            }
        }
        Divider(modifier = Modifier.padding(nonePadding, tinyPadding))
    }
}
