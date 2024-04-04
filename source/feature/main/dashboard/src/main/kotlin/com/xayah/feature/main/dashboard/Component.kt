package com.xayah.feature.main.dashboard

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.ui.component.ActionButton
import com.xayah.core.ui.component.AutoLabelLargeText
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.LabelLargeText
import com.xayah.core.ui.component.SegmentProgressIndicator
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.intrinsicIcon
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.SegmentProgress
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.core.util.DateUtil

@ExperimentalMaterial3Api
@Composable
fun OverviewCard(
    title: StringResourceToken,
    icon: ImageVectorToken,
    colorContainer: ColorSchemeKeyTokens,
    onColorContainer: ColorSchemeKeyTokens,
    content: @Composable ColumnScope.() -> Unit,
    actionIcon: ImageVectorToken,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = colorContainer.toColor()),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(SizeTokens.Level16)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .paddingBottom(SizeTokens.Level8)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level6)
                ) {
                    Icon(
                        modifier = Modifier.intrinsicIcon(),
                        imageVector = icon.value,
                        tint = onColorContainer.toColor(),
                        contentDescription = null,
                    )
                    LabelLargeText(
                        text = title.value,
                        color = onColorContainer.toColor(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                content()
            }

            Icon(
                imageVector = actionIcon.value,
                tint = onColorContainer.toColor(),
                contentDescription = null
            )
        }
    }
}

@SuppressLint("StringFormatInvalid")
@ExperimentalMaterial3Api
@Composable
fun OverviewStorageCard(
    title: StringResourceToken,
    used: SegmentProgress? = null,
    backupUsed: SegmentProgress? = null,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    OverviewCard(
        title = StringResourceToken.fromStringId(R.string.storage),
        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_folder_open),
        colorContainer = ColorSchemeKeyTokens.SecondaryContainer,
        onColorContainer = ColorSchemeKeyTokens.OnSecondaryContainer,
        content = {
            TitleLargeText(text = title.value, color = ColorSchemeKeyTokens.OnSurface.toColor())
            if (used != null && used.progress.isNaN().not()) {
                SegmentProgressIndicator(
                    modifier = Modifier
                        .paddingTop(SizeTokens.Level8)
                        .paddingBottom(SizeTokens.Level4),
                    enabled = true,
                    progress = used.progress,
                    color = ColorSchemeKeyTokens.Secondary,
                    trackColor = ColorSchemeKeyTokens.SecondaryL80D20,
                )

                BodyMediumText(
                    enabled = true,
                    text = "${context.getString(R.string.args_used, (used.progress * 100).toInt())} (${used.usedFormat} / ${used.totalFormat})",
                    color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
                )
            }
            if (backupUsed != null && backupUsed.progress.isNaN().not()) {
                SegmentProgressIndicator(
                    modifier = Modifier
                        .paddingTop(SizeTokens.Level8)
                        .paddingBottom(SizeTokens.Level4),
                    enabled = true,
                    progress = backupUsed.progress,
                    color = ColorSchemeKeyTokens.Primary,
                    trackColor = ColorSchemeKeyTokens.SecondaryL80D20,
                )
                BodyMediumText(
                    enabled = true,
                    text = "${context.getString(R.string.args_used_by_backups, (backupUsed.progress * 100).toInt())} (${backupUsed.usedFormat} / ${backupUsed.totalFormat})",
                    color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
                )
            }
        },
        actionIcon = ImageVectorToken.fromVector(Icons.Outlined.Settings),
        onClick = onClick
    )
}

@SuppressLint("StringFormatInvalid")
@ExperimentalMaterial3Api
@Composable
fun OverviewLastBackupCard(nullBackupDir: Boolean, lastBackupTime: Long, onClick: () -> Unit) {
    val context = LocalContext.current
    val relativeTime by remember(lastBackupTime) {
        mutableStateOf(DateUtil.getShortRelativeTimeSpanString(context = context, time1 = lastBackupTime, time2 = DateUtil.getTimestamp()))
    }
    val finishTime by remember(lastBackupTime) {
        mutableStateOf(context.getString(R.string.args_finished_at, DateUtil.formatTimestamp(lastBackupTime, DateUtil.PATTERN_FINISH)))
    }
    OverviewCard(
        title = StringResourceToken.fromStringId(R.string.last_backup),
        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_package_2),
        colorContainer = if (nullBackupDir) ColorSchemeKeyTokens.SurfaceContainerHigh else ColorSchemeKeyTokens.PrimaryContainer,
        onColorContainer = if (nullBackupDir) ColorSchemeKeyTokens.OnSurface else ColorSchemeKeyTokens.OnPrimaryContainer,
        content = {
            TitleLargeText(
                text = (if (nullBackupDir || lastBackupTime == 0L) StringResourceToken.fromStringId(R.string.never) else StringResourceToken.fromString(relativeTime)).value,
                color = (if (nullBackupDir) ColorSchemeKeyTokens.OnSurfaceVariant else ColorSchemeKeyTokens.OnSurface).toColor()
            )
            if (nullBackupDir || lastBackupTime != 0L)
                BodyMediumText(
                    text = (if (nullBackupDir) StringResourceToken.fromStringId(R.string.setup_required) else StringResourceToken.fromString(finishTime)).value,
                    color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
                )
        },
        actionIcon = ImageVectorToken.fromVector(Icons.Rounded.KeyboardArrowRight),
        onClick = onClick,
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun QuickActionsButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    title: StringResourceToken,
    icon: ImageVectorToken,
    colorContainer: ColorSchemeKeyTokens,
    colorL80D20: ColorSchemeKeyTokens,
    onColorContainer: ColorSchemeKeyTokens,
    actionIcon: ImageVectorToken? = null
) {
    ActionButton(modifier = modifier, enabled = enabled, icon = icon, colorContainer = colorContainer, colorL80D20 = colorL80D20, onColorContainer = onColorContainer, actionIcon = actionIcon) {
        AutoLabelLargeText(modifier = Modifier.weight(1f), text = title.value, color = onColorContainer.toColor(enabled), enabled = enabled)
    }
}
