package com.xayah.databackup.compose.ui.activity.crash.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@ExperimentalMaterial3Api
@Composable
fun CrashScaffold(crashInfo: String, onSaveClick: () -> Unit) {
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
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
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(mediumPadding, nonePadding),
            verticalArrangement = Arrangement.spacedBy(mediumPadding),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                innerPadding.calculateTopPadding() * 2
                            )
                    )
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
            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            innerPadding.calculateBottomPadding()
                        )
                )
            }
        }
    }
}