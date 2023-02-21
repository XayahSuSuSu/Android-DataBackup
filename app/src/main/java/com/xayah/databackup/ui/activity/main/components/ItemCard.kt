package com.xayah.databackup.ui.activity.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun ItemCardPreview() {
    val colorYellow = colorResource(id = R.color.yellow)
    ItemCard(
        icon = ImageVector.vectorResource(id = R.drawable.ic_round_apps),
        iconTint = colorYellow,
        title = stringResource(id = R.string.application),
        subtitle = stringResource(R.string.card_app_subtitle),
        onBackupClick = {},
        onRestoreClick = {}
    )
}

@ExperimentalMaterial3Api
@Composable
fun ItemCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
) {
    val colorTertiary = MaterialTheme.colorScheme.tertiary
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val iconSmallSize = dimensionResource(R.dimen.icon_small_size)
    Card(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(Modifier.padding(mediumPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(smallPadding)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSmallSize)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                modifier = Modifier.padding(nonePadding, smallPadding),
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
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