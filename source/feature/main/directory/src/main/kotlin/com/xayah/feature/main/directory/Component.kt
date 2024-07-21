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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.xayah.core.model.database.DirectoryEntity
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.Card
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LinearProgressIndicator
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.component.SegmentProgressIndicator
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.intrinsicIcon
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.material3.CardDefaults
import com.xayah.core.ui.model.SegmentProgress
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.joinOf

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun DirectoryScaffold(
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    isLoading: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (BoxScope.() -> Unit)
) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                SecondaryLargeTopBar(
                    scrollBehavior = scrollBehavior,
                    title = title,
                    actions = actions
                )
                if (isLoading)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

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
    title: String,
    icon: ImageVector,
    path: String,
    error: String? = null,
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
        colors = CardDefaults.cardColors(containerColor = (if (selected) ThemedColorSchemeKeyTokens.PrimaryContainer else ThemedColorSchemeKeyTokens.Surface).value),
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
                    imageVector = icon,
                    tint = (if (selected) ThemedColorSchemeKeyTokens.OnPrimaryContainer else ThemedColorSchemeKeyTokens.OnSurface).value.withState(enabled),
                    contentDescription = null,
                )
                TitleLargeText(
                    enabled = enabled,
                    text = title,
                    color = (if (selected) ThemedColorSchemeKeyTokens.OnPrimaryContainer else ThemedColorSchemeKeyTokens.OnSurface).value,
                )
            }
            BodyMediumText(
                enabled = enabled,
                text = path,
                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
            )
            if (used != null && used.progress.isNaN().not()) {
                SegmentProgressIndicator(
                    modifier = Modifier
                        .paddingTop(SizeTokens.Level8)
                        .paddingBottom(SizeTokens.Level4),
                    enabled = enabled,
                    progress = used.progress,
                    color = if (selected) ThemedColorSchemeKeyTokens.Secondary else ThemedColorSchemeKeyTokens.Primary,
                    trackColor = ThemedColorSchemeKeyTokens.SecondaryL80D20,
                )

                BodyMediumText(
                    enabled = enabled,
                    text = "${context.getString(R.string.args_used, (used.progress * 100).toInt())} (${used.usedFormat} / ${used.totalFormat})",
                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                )
            }
            if (backupUsed != null && backupUsed.progress.isNaN().not()) {
                SegmentProgressIndicator(
                    modifier = Modifier
                        .paddingTop(SizeTokens.Level8)
                        .paddingBottom(SizeTokens.Level4),
                    enabled = enabled,
                    progress = backupUsed.progress,
                    color = ThemedColorSchemeKeyTokens.Primary,
                    trackColor = ThemedColorSchemeKeyTokens.SecondaryL80D20,
                )
                BodyMediumText(
                    enabled = enabled,
                    text = "${context.getString(R.string.args_used_by_backups, (backupUsed.progress * 100).toInt())} (${backupUsed.usedFormat} / ${backupUsed.totalFormat})",
                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                )
            }

            if (error != null) {
                BodyMediumText(
                    modifier = Modifier.paddingTop(SizeTokens.Level8),
                    enabled = enabled,
                    text = error,
                    color = ThemedColorSchemeKeyTokens.Error.value,
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
        title = stringResource(id = item.titleResId),
        icon = item.icon(),
        path = item.pathDisplay(),
        error = item.error.ifEmpty { null },
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
        title = joinOf(stringResource(id = R.string.custom_directory), "..."),
        icon = Icons.Rounded.AddCircleOutline,
        path = stringResource(id = R.string.pick_custom_directory_desc),
        onClick = onClick,
    )
}
