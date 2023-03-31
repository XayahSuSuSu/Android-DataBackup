package com.xayah.databackup.ui.activity.list.common.components.item

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.guide.components.card.SerialText
import com.xayah.databackup.ui.components.LabelSmallText
import com.xayah.databackup.ui.components.TitleMediumText
import com.xayah.databackup.ui.components.animation.ItemExpandAnimation

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    icon: Painter,
    title: String,
    subtitle: String,
    appSelected: MutableState<Boolean>?,
    appEnabled: MutableState<Boolean> = mutableStateOf(true),
    dataSelected: MutableState<Boolean>?,
    dataEnabled: MutableState<Boolean> = mutableStateOf(true),
    chipContent: @Composable RowScope.() -> Unit,
    actionContent: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
) {
    val iconSmallSize = dimensionResource(R.dimen.icon_small_size)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    Card(
        modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
    ) {
        Column(
            modifier = Modifier
                .padding(mediumPadding, smallPadding)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(smallPadding)) {
                Image(
                    modifier = Modifier.size(iconSmallSize),
                    painter = icon,
                    contentDescription = null
                )
                Column(modifier = Modifier.weight(1f)) {
                    TitleMediumText(text = title)
                    LabelSmallText(text = subtitle, bold = false)
                }

                if (appSelected != null)
                    FilledIconToggleButton(
                        checked = appSelected.value,
                        enabled = appEnabled.value,
                        onCheckedChange = { appSelected.value = it }
                    ) {
                        if (appSelected.value) {
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
                if (dataSelected != null)
                    FilledIconToggleButton(
                        checked = dataSelected.value,
                        enabled = dataEnabled.value,
                        onCheckedChange = { dataSelected.value = it }
                    ) {
                        if (dataSelected.value) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(smallPadding),
                    content = {
                        val app = appSelected != null && appSelected.value
                        val data = dataSelected != null && dataSelected.value
                        var display = ""
                        if (app && data) display = "${stringResource(id = R.string.apk)} + ${stringResource(id = R.string.data)}"
                        else if (app) display = stringResource(R.string.apk)
                        else if (data) display = stringResource(R.string.data)
                        if (display.isNotEmpty())
                            SerialText(serial = display, surfaceColor = MaterialTheme.colorScheme.primary, textColor = MaterialTheme.colorScheme.onPrimary)
                        chipContent()
                    }
                )
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
                    Row(content = actionContent)
                }
            }
        }
    }
}
