package com.xayah.feature.main.medium

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.component.Card
import com.xayah.core.ui.component.FilledIconTextButton
import com.xayah.core.ui.component.HeaderItem
import com.xayah.core.ui.component.InfoItem
import com.xayah.core.ui.component.LabelSmallText
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.component.outlinedCardBorder
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun MediaCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    name: String,
    path: String,
    cardSelected: Boolean,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    infoChipGroup: @Composable (RowScope.() -> Unit),
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        enabled = enabled,
        onClick = onCardClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCardLongClick()
        },
        border = if (cardSelected) outlinedCardBorder(borderColor = ColorSchemeKeyTokens.Primary.toColor()) else null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingTokens.Level4)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level2)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        TitleMediumText(text = name, fontWeight = FontWeight.Bold)
                        LabelSmallText(text = path)
                    }
                    if (cardSelected) Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Top),
                        tint = ColorSchemeKeyTokens.Primary.toColor(),
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                    verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                    content = {
                        infoChipGroup()
                    }
                )
            }
        }
    }
}

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun OpItem(
    title: StringResourceToken,
    btnText: StringResourceToken,
    btnIcon: ImageVectorToken,
    isRefreshing: Boolean,
    activatedState: Boolean,
    itemState: MediaEntity,
    onBtnClick: () -> Unit,
    infoContent: @Composable ColumnScope.() -> Unit,
    btnContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    var expand by remember { mutableStateOf(false) }
    HeaderItem(expand = expand, title = title) {
        expand = expand.not()
    }
    AnimatedContent(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        targetState = expand,
        label = AnimationTokens.AnimatedContentLabel
    ) { targetState ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level2)
        ) {
            InfoItem(
                title = StringResourceToken.fromStringId(R.string.id),
                content = StringResourceToken.fromString(itemState.indexInfo.preserveId.toString())
            )
            InfoItem(
                title = StringResourceToken.fromStringId(R.string.data_size),
                content = StringResourceToken.fromString(itemState.mediaInfo.displayBytes.toDouble().formatSize())
            )
            infoContent()
            if (targetState) {
                btnContent?.invoke(this)
            }
            FilledIconTextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingBottom(PaddingTokens.Level2),
                text = if (activatedState)
                    StringResourceToken.fromStringId(R.string.task_is_in_progress)
                else
                    btnText,
                icon = btnIcon,
                enabled = isRefreshing.not() && activatedState.not(),
                onClick = onBtnClick
            )
        }
    }
}
