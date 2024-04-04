package com.xayah.feature.main.directory

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.xayah.core.model.database.DirectoryEntity
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.Card
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.component.SegmentProgressIndicator
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.intrinsicIcon
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.material3.CardDefaults
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.SegmentProgress
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun DirectoryScaffold(scrollBehavior: TopAppBarScrollBehavior, title: StringResourceToken, actions: @Composable RowScope.() -> Unit = {}, content: @Composable (BoxScope.() -> Unit)) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryLargeTopBar(
                scrollBehavior = scrollBehavior,
                title = title,
                actions = actions
            )
        },
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f), content = content)
        }
    }
}

@ExperimentalFoundationApi
@SuppressLint("StringFormatInvalid")
@ExperimentalMaterial3Api
@Composable
fun DirectoryCard(
    enabled: Boolean = true,
    selected: Boolean,
    performHapticFeedback: Boolean = false,
    title: StringResourceToken,
    icon: ImageVectorToken,
    path: StringResourceToken,
    error: StringResourceToken? = null,
    used: SegmentProgress? = null,
    backupUsed: SegmentProgress? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        enabled = enabled,
        performHapticFeedback = performHapticFeedback,
        colors = CardDefaults.cardColors(containerColor = (if (selected) ColorSchemeKeyTokens.PrimaryContainer else ColorSchemeKeyTokens.Surface).toColor()),
        border = if (selected) null else CardDefaults.outlinedCardBorder(),
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Column(
            modifier = Modifier
                .padding(SizeTokens.Level16)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level6)
            ) {
                Icon(
                    modifier = Modifier.intrinsicIcon(),
                    imageVector = icon.value,
                    tint = (if (selected) ColorSchemeKeyTokens.OnPrimaryContainer else ColorSchemeKeyTokens.OnSurface).toColor(enabled),
                    contentDescription = null,
                )
                TitleLargeText(
                    enabled = enabled,
                    text = title.value,
                    color = (if (selected) ColorSchemeKeyTokens.OnPrimaryContainer else ColorSchemeKeyTokens.OnSurface).toColor(),
                )
            }
            BodyMediumText(
                enabled = enabled,
                text = path.value,
                color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
            )
            if (used != null && used.progress.isNaN().not()) {
                SegmentProgressIndicator(
                    modifier = Modifier
                        .paddingTop(SizeTokens.Level8)
                        .paddingBottom(SizeTokens.Level4),
                    enabled = enabled,
                    progress = used.progress,
                    color = if (selected) ColorSchemeKeyTokens.Secondary else ColorSchemeKeyTokens.Primary,
                    trackColor = ColorSchemeKeyTokens.SecondaryL80D20,
                )

                BodyMediumText(
                    enabled = enabled,
                    text = "${context.getString(R.string.args_used, (used.progress * 100).toInt())} (${used.usedFormat} / ${used.totalFormat})",
                    color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
                )
            }
            if (backupUsed != null && backupUsed.progress.isNaN().not()) {
                SegmentProgressIndicator(
                    modifier = Modifier
                        .paddingTop(SizeTokens.Level8)
                        .paddingBottom(SizeTokens.Level4),
                    enabled = enabled,
                    progress = backupUsed.progress,
                    color = ColorSchemeKeyTokens.Primary,
                    trackColor = ColorSchemeKeyTokens.SecondaryL80D20,
                )
                BodyMediumText(
                    enabled = enabled,
                    text = "${context.getString(R.string.args_used_by_backups, (backupUsed.progress * 100).toInt())} (${backupUsed.usedFormat} / ${backupUsed.totalFormat})",
                    color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
                )
            }

            if (error != null) {
                BodyMediumText(
                    modifier = Modifier.paddingTop(SizeTokens.Level8),
                    enabled = enabled,
                    text = error.value,
                    color = ColorSchemeKeyTokens.Error.toColor(),
                )
            }
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun DirectoryCard(item: DirectoryEntity, performHapticFeedback: Boolean = false, onLongClick: () -> Unit = {}, onClick: () -> Unit) {
    DirectoryCard(
        selected = item.selected,
        performHapticFeedback = performHapticFeedback,
        title = StringResourceToken.fromString(item.title),
        icon = item.icon(),
        path = StringResourceToken.fromString(item.pathDisplay()),
        error = if (item.error.isEmpty()) null else StringResourceToken.fromString(item.error),
        used = SegmentProgress(used = item.usedBytes, total = item.totalBytes),
        backupUsed = if (item.selected) SegmentProgress(used = item.childUsedBytes, total = item.totalBytes) else null,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun CustomDirectoryCard(enabled: Boolean, onClick: () -> Unit) {
    DirectoryCard(
        enabled = enabled,
        selected = false,
        title = StringResourceToken.fromStringArgs(
            StringResourceToken.fromStringId(R.string.custom_directory),
            StringResourceToken.fromString("..."),
        ),
        icon = ImageVectorToken.fromVector(Icons.Rounded.AddCircleOutline),
        path = StringResourceToken.fromStringId(R.string.pick_custom_directory_desc),
        onClick = onClick,
    )
}
