package com.xayah.feature.main.directory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.database.model.DirectoryEntity
import com.xayah.core.ui.component.BodySmallText
import com.xayah.core.ui.component.Card
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.HeadlineMediumText
import com.xayah.core.ui.component.LabelSmallText
import com.xayah.core.ui.component.AnimatedLinearProgressIndicator
import com.xayah.core.ui.component.ModalActionDropdownMenu
import com.xayah.core.ui.component.outlinedCardBorder
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.component.shimmer
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.token.PaddingTokens

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun DirectoryCard(
    modifier: Modifier = Modifier,
    entity: DirectoryEntity,
    actions: List<ActionMenuItem>,
    onCardClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    DirectoryCard(
        modifier = modifier,
        shimmering = false,
        title = entity.title,
        error = entity.error,
        usedBytesDisplay = entity.usedBytesDisplay,
        totalBytesDisplay = entity.totalBytesDisplay,
        path = entity.path,
        progress = 1 - entity.availableBytes.toFloat() / entity.totalBytes,
        selected = entity.selected,
        enabled = entity.enabled,
        actions = actions,
        onCardClick = onCardClick,
        chipGroup = chipGroup
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun DirectoryCardShimmer(
    modifier: Modifier = Modifier,
) {
    DirectoryCard(
        modifier = modifier,
        shimmering = true,
        title = "Shimmer",
        error = "",
        usedBytesDisplay = "Shimmer",
        totalBytesDisplay = "Shimmer",
        path = "ShimmerShimmerShimmer",
        progress = 0f,
        selected = false,
        enabled = true,
        actions = listOf(),
        onCardClick = {},
        chipGroup = {}
    )
}

private fun Modifier.directoryCardShimmer(visible: Boolean) = shimmer(visible, 0.5f, 0.3f)

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun DirectoryCard(
    modifier: Modifier = Modifier,
    shimmering: Boolean,
    title: String,
    error: String,
    usedBytesDisplay: String,
    totalBytesDisplay: String,
    path: String,
    progress: Float,
    selected: Boolean,
    enabled: Boolean,
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
        enabled = enabled,
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
                .padding(PaddingTokens.Level3)
        ) {
            Column {
                Row(modifier = Modifier.paddingBottom(PaddingTokens.Level1)) {
                    HeadlineMediumText(modifier = Modifier.directoryCardShimmer(shimmering), text = title, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    if (selected) Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Top),
                        tint = ColorSchemeKeyTokens.Primary.toColor(),
                    )
                }
                BodySmallText(modifier = Modifier.directoryCardShimmer(shimmering), text = path, fontWeight = FontWeight.Bold)
                if (error.isNotEmpty()) BodySmallText(text = error, color = ColorSchemeKeyTokens.Error.toColor(), enabled = enabled)
                Divider(modifier = Modifier.paddingVertical(PaddingTokens.Level1))
                Row(
                    modifier = Modifier.paddingBottom(PaddingTokens.Level1),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)
                ) {
                    AnimatedLinearProgressIndicator(
                        modifier = Modifier
                            .clip(CircleShape)
                            .weight(1f)
                            .directoryCardShimmer(shimmering),
                        color = ColorSchemeKeyTokens.Primary.toColor(),
                        trackColor = ColorSchemeKeyTokens.InverseOnSurface.toColor(),
                        progress = if (progress.isNaN()) 0f else progress,
                        enabled = enabled,
                    )
                    Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                        LabelSmallText(modifier = Modifier.directoryCardShimmer(shimmering), text = "$usedBytesDisplay / $totalBytesDisplay")

                        ModalActionDropdownMenu(expanded = expanded, actionList = actions, onDismissRequest = { expanded = false })
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1),
                    content = {
                        chipGroup()
                    }
                )
            }
        }
    }
}
