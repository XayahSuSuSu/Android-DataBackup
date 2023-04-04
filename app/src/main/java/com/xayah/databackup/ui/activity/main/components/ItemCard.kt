package com.xayah.databackup.ui.activity.main.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.BodySmallText
import com.xayah.databackup.ui.components.TitleLargeText
import com.xayah.databackup.ui.components.paddingVertical

@ExperimentalMaterial3Api
@Composable
fun ItemCard(
    icon: Painter,
    title: String,
    subtitle: String,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
) {
    val colorTertiary = MaterialTheme.colorScheme.tertiary
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val iconTinySize = dimensionResource(R.dimen.icon_tiny_size)
    Card(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()) {
        Column(Modifier.padding(mediumPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(smallPadding)
            ) {
                Image(
                    modifier = Modifier.size(iconTinySize),
                    painter = icon,
                    contentDescription = null
                )
                TitleLargeText(text = title)
            }
            BodySmallText(text = subtitle, modifier = Modifier.paddingVertical(smallPadding))
            Row {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentSize(),
                    onClick = onBackupClick,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_round_acute),
                            contentDescription = stringResource(id = R.string.backup),
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(id = R.string.backup))
                    }
                }
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = colorTertiary),
                    onClick = onRestoreClick,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_outline_restore),
                            contentDescription = stringResource(id = R.string.restore),
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(id = R.string.restore))
                    }
                }
            }
        }
    }
}