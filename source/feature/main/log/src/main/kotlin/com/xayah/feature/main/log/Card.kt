package com.xayah.feature.main.log

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.component.BodySmallText
import com.xayah.core.ui.component.Card
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.ModalActionDropdownMenu
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.outlinedCardBorder
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.shimmer
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromVector
import com.xayah.core.util.DateUtil

internal fun Modifier.logCardShimmer(visible: Boolean) = shimmer(visible, 0.5f, 0.3f)

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun LogCard(
    modifier: Modifier = Modifier,
    item: LogCardItem,
    selected: Boolean,
    actions: List<ActionMenuItem>,
    onCardClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    LogCard(
        modifier = modifier,
        shimmering = false,
        name = item.name,
        sizeBytes = item.sizeBytes,
        timestamp = item.timestamp,
        selected = selected,
        actions = actions,
        onCardClick = onCardClick,
        chipGroup = chipGroup,
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun LogCardShimmer(modifier: Modifier = Modifier) {
    LogCard(
        modifier = modifier,
        shimmering = true,
        name = "Shimmer",
        sizeBytes = 0.toDouble(),
        timestamp = 0L,
        selected = false,
        actions = listOf(),
        onCardClick = {},
        chipGroup = {}
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun LogCard(
    modifier: Modifier = Modifier,
    shimmering: Boolean,
    name: String,
    sizeBytes: Double,
    timestamp: Long,
    selected: Boolean,
    actions: List<ActionMenuItem>,
    onCardClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        enabled = true,
        onClick = onCardClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            expanded = true
        },
        border = if (selected) outlinedCardBorder(borderColor = ColorSchemeKeyTokens.Primary.toColor()) else null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .paddingHorizontal(PaddingTokens.Level4)
                .paddingTop(PaddingTokens.Level4)
                .paddingBottom(PaddingTokens.Level2)
        ) {
            Column {
                Row(modifier = Modifier.paddingBottom(PaddingTokens.Level2)) {
                    TitleLargeText(modifier = Modifier.logCardShimmer(shimmering), text = name, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    if (selected) Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Top),
                        tint = ColorSchemeKeyTokens.Primary.toColor(),
                    )
                }
                BodySmallText(modifier = Modifier.logCardShimmer(shimmering), text = DateUtil.formatTimestamp(timestamp), fontWeight = FontWeight.Bold)
                Divider(modifier = Modifier.paddingTop(PaddingTokens.Level2))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                    verticalAlignment = Alignment.CenterVertically,
                    content = {
                        RoundChip(modifier = Modifier.logCardShimmer(shimmering), text = sizeBytes.formatSize(), enabled = true)
                        if (shimmering.not()) {
                            chipGroup()
                            Spacer(modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                                IconButton(icon = ImageVectorToken.fromVector(Icons.Rounded.MoreVert), onClick = { expanded = true })

                                ModalActionDropdownMenu(expanded = expanded, actionList = actions, onDismissRequest = { expanded = false })
                            }
                        } else {
                            Spacer(modifier = Modifier.height(PaddingTokens.Level7))
                        }
                    }
                )
            }
        }
    }
}
