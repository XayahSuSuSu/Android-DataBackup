package com.xayah.databackup.compose.ui.activity.crash.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.components.Scaffold

@ExperimentalMaterial3Api
@Composable
fun CrashScaffold(crashInfo: String, onSaveClick: () -> Unit) {
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val iconMediumSize = dimensionResource(R.dimen.icon_medium_size)
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSaveClick,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_round_save),
                    contentDescription = null
                )
            }
        },
        topPaddingRate = 2,
        content = {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = colorOnSurfaceVariant,
                        modifier = Modifier
                            .size(iconMediumSize)
                            .padding(nonePadding, nonePadding, nonePadding, smallPadding)
                    )
                    Text(
                        text = stringResource(R.string.app_crashed),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            item {
                Text(
                    text = crashInfo,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    )
}
