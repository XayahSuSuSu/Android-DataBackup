package com.xayah.databackup.ui.activity.list.components.item

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.BodySmallText
import com.xayah.databackup.ui.components.TitleMediumText
import com.xayah.databackup.ui.components.animation.ItemExpandAnimation
import com.xayah.databackup.ui.components.paddingHorizontal
import com.xayah.databackup.ui.components.paddingVertical

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    icon: Painter,
    title: String,
    subtitle: String,
    appSelected: MutableState<Boolean>?,
    dataSelected: MutableState<Boolean>?,
    chipContent: @Composable RowScope.() -> Unit,
    actionContent: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
) {
    val iconSmallSize = dimensionResource(R.dimen.icon_small_size)
    val tinyPadding = dimensionResource(R.dimen.padding_tiny)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(mediumPadding))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                modifier = Modifier.size(iconSmallSize),
                painter = icon,
                contentDescription = null
            )
            Column(
                modifier = Modifier
                    .paddingHorizontal(smallPadding)
                    .weight(1f)
            ) {
                TitleMediumText(text = title)
                BodySmallText(text = subtitle)
            }

            if (appSelected != null)
                FilledIconToggleButton(
                    checked = appSelected.value,
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
        Row {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(mediumPadding),
                content = chipContent
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
        Divider(modifier = Modifier.paddingVertical(tinyPadding))
    }
}

