package com.xayah.databackup.ui.activity.list.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
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
import com.xayah.databackup.data.MediaInfoBackup
import com.xayah.databackup.ui.components.animation.ItemExpandAnimation
import com.xayah.databackup.util.GlobalObject

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun MediaBackupItem(
    mediaInfoBackup: MediaInfoBackup,
    modifier: Modifier = Modifier,
    onItemUpdate: () -> Unit
) {
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

        var expand by remember { mutableStateOf(false) }
        Row {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(mediumPadding)
            ) {
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
        ItemExpandAnimation(expand) {
            if (it) {
                Row {
                    val isDialogOpen = remember {
                        mutableStateOf(false)
                    }
                    ConfirmDialog(
                        isOpen = isDialogOpen,
                        icon = Icons.Rounded.Info,
                        title = stringResource(id = R.string.delete),
                        content = {
                            Text(
                                text = stringResource(id = R.string.delete_confirm) +
                                        stringResource(id = R.string.symbol_question),
                            )
                        }) {
                        GlobalObject.getInstance().mediaInfoBackupMap.value.remove(
                            mediaInfoBackup.name
                        )
                        onItemUpdate()
                    }
                    TextButton(onClick = {
                        isDialogOpen.value = true
                    }) { Text(stringResource(R.string.delete)) }
                }
            }
        }
        Divider(modifier = Modifier.padding(nonePadding, tinyPadding))
    }
}
