package com.xayah.databackup.ui.activity.guide.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun PermissionCardPreview() {
    PermissionCard(
        serial = "1",
        title = stringResource(id = R.string.start),
        subtitle = stringResource(id = R.string.start_subtitle),
        content = stringResource(id = R.string.start_desc)
    )
}

@ExperimentalMaterial3Api
@Composable
fun PermissionCard(
    serial: String,
    title: String,
    subtitle: String,
    content: String,
) {
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val colorSurface = MaterialTheme.colorScheme.surface
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val cardSerialCircleSize = dimensionResource(R.dimen.card_serial_circle_size)
    Card(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(Modifier.padding(mediumPadding)) {
            Surface(
                shape = CircleShape,
                modifier = Modifier.size(cardSerialCircleSize),
                color = colorOnSurfaceVariant
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = serial,
                        color = colorSurface,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                modifier = Modifier.padding(nonePadding, smallPadding),
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                modifier = Modifier.padding(nonePadding, smallPadding),
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                modifier = Modifier.padding(nonePadding, smallPadding, nonePadding, nonePadding),
                text = content,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}