package com.xayah.databackup.compose.ui.activity.list.components

import androidx.compose.animation.ExperimentalAnimationApi
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
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.activity.list.components.animation.ExpandAnimation
import com.xayah.databackup.data.AppInfoBackup

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun AppBackupItem(appInfoBackup: AppInfoBackup, modifier: Modifier = Modifier) {
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
                if ((appInfoBackup.selectApp && appInfoBackup.selectData).not() &&
                    (appInfoBackup.selectApp || appInfoBackup.selectData)
                ) {
                    if (appInfoBackup.selectApp.not()) {
                        appInfoBackup.selectApp = appInfoBackup.selectApp.not()
                    } else {
                        appInfoBackup.selectData = appInfoBackup.selectData.not()
                    }
                } else {
                    appInfoBackup.selectApp = appInfoBackup.selectApp.not()
                    appInfoBackup.selectData = appInfoBackup.selectData.not()
                }
            }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                modifier = Modifier.size(iconSmallSize),
                painter = rememberDrawablePainter(drawable = appInfoBackup.detailBase.appIcon),
                contentDescription = null
            )
            Column(
                modifier = Modifier
                    .padding(smallPadding, nonePadding)
                    .weight(1f)
            ) {
                Text(
                    text = appInfoBackup.detailBase.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = appInfoBackup.detailBase.packageName,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            FilledIconToggleButton(
                checked = appInfoBackup.selectApp,
                onCheckedChange = { appInfoBackup.selectApp = it }
            ) {
                if (appInfoBackup.selectApp) {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_round_apps),
                        contentDescription = stringResource(id = R.string.application)
                    )
                } else {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_round_apps),
                        contentDescription = stringResource(id = R.string.application)
                    )
                }
            }
            FilledIconToggleButton(
                checked = appInfoBackup.selectData,
                onCheckedChange = { appInfoBackup.selectData = it }
            ) {
                if (appInfoBackup.selectData) {
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
                if (appInfoBackup.detailBackup.versionName.isNotEmpty()) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text(appInfoBackup.detailBackup.versionName) }
                    )
                }
                if (appInfoBackup.storageStats.sizeBytes != 0L) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text(appInfoBackup.storageStats.sizeDisplay) }
                    )
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
        ExpandAnimation(expand) {
            if (it) {
                Row {
                    TextButton(onClick = { }) { Text(stringResource(R.string.blacklist)) }
                }
            }
        }
        Divider(modifier = Modifier.padding(nonePadding, tinyPadding))
    }
}
