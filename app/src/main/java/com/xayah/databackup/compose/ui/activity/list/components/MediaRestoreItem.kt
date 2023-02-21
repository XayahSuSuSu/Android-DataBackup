package com.xayah.databackup.compose.ui.activity.list.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import com.xayah.databackup.R
import com.xayah.databackup.data.MediaInfoRestore
import com.xayah.databackup.util.Command

@ExperimentalMaterial3Api
@Composable
fun MediaRestoreItem(mediaInfoRestore: MediaInfoRestore, modifier: Modifier = Modifier) {
    val iconSmallSize = dimensionResource(R.dimen.icon_small_size)
    val tinyPadding = dimensionResource(R.dimen.padding_tiny)
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(mediumPadding))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                mediaInfoRestore.selectData = mediaInfoRestore.selectData.not()
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
                    text = mediaInfoRestore.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = mediaInfoRestore.path,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            FilledIconToggleButton(
                checked = mediaInfoRestore.selectData,
                onCheckedChange = { mediaInfoRestore.selectData = it }
            ) {
                if (mediaInfoRestore.selectData) {
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
        var expand by remember { mutableStateOf(false) }
        Row {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(mediumPadding)
            ) {
                if (mediaInfoRestore.detailRestoreList.isNotEmpty()) {
                    var dateMenu by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                        SuggestionChip(
                            onClick = { dateMenu = true },
                            label = { Text(Command.getDate(mediaInfoRestore.detailRestoreList[mediaInfoRestore.restoreIndex].date)) }
                        )
                        DropdownMenu(
                            expanded = dateMenu,
                            onDismissRequest = { dateMenu = false }
                        ) {
                            val items = mutableListOf<String>()
                            mediaInfoRestore.detailRestoreList.forEach {
                                items.add(
                                    Command.getDate(
                                        it.date
                                    )
                                )
                            }
                            for ((index, i) in items.withIndex()) {
                                DropdownMenuItem(
                                    text = { Text(i) },
                                    onClick = {
                                        mediaInfoRestore.restoreIndex = index
                                        dateMenu = false
                                    })
                            }
                        }
                    }
                }
            }
            IconToggleButton(checked = expand, onCheckedChange = { expand = it }) {
                if (expand) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = null
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }
        }
        if (expand) {
            Row {
                TextButton(onClick = { }) { Text(stringResource(R.string.delete)) }
            }
        }
        Divider(modifier = Modifier.padding(nonePadding, tinyPadding))
    }
}
