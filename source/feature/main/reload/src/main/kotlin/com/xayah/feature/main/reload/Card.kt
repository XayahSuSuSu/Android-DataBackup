package com.xayah.feature.main.reload

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.model.DataType
import com.xayah.core.ui.component.BodySmallText
import com.xayah.core.ui.component.Card
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.util.DateUtil

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ReloadCard(entity: MediaRestoreEntity) {
    ReloadCard(name = entity.name, timestamp = entity.timestamp, onCardClick = {}) {
        RoundChip(text = entity.sizeDisplay)
        RoundChip(text = DataType.MEDIA_MEDIA.type.uppercase())
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ReloadCard(entity: PackageRestoreEntire) {
    ReloadCard(name = entity.packageName, timestamp = entity.timestamp, onCardClick = {}) {
        RoundChip(text = entity.sizeDisplay)
        if (entity.apkExists) RoundChip(text = DataType.PACKAGE_APK.type.uppercase())
        if (entity.dataExists) {
            RoundChip(text = DataType.PACKAGE_USER.type.uppercase())
            RoundChip(text = DataType.PACKAGE_USER_DE.type.uppercase())
            RoundChip(text = DataType.PACKAGE_DATA.type.uppercase())
            RoundChip(text = DataType.PACKAGE_OBB.type.uppercase())
            RoundChip(text = DataType.PACKAGE_MEDIA.type.uppercase())
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ReloadCard(
    modifier: Modifier = Modifier,
    name: String,
    timestamp: Long,
    onCardClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        enabled = true,
        onClick = onCardClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .paddingHorizontal(PaddingTokens.Level3)
                .paddingTop(PaddingTokens.Level3)
                .paddingBottom(PaddingTokens.Level1)
        ) {
            Column {
                TitleLargeText(text = name, fontWeight = FontWeight.Bold)
                BodySmallText(text = DateUtil.formatTimestamp(timestamp), fontWeight = FontWeight.Bold)
                Divider(modifier = Modifier
                    .paddingTop(PaddingTokens.Level1)
                    .paddingBottom(PaddingTokens.Level2))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .paddingBottom(PaddingTokens.Level1),
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1),
                    verticalAlignment = Alignment.CenterVertically,
                    content = {
                        chipGroup()
                    }
                )
            }
        }
    }
}
