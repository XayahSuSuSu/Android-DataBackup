package com.xayah.feature.main.restore

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.ui.component.AnimatedTextContainer
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LabelMediumText
import com.xayah.core.ui.component.OverviewCard
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.component.Surface
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.TitleSmallText
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.value
import com.xayah.core.util.DateUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.iconDir
import com.xayah.core.util.withIOContext
import kotlin.math.min

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun RestoreScaffold(scrollBehavior: TopAppBarScrollBehavior, title: StringResourceToken, actions: @Composable RowScope.() -> Unit = {}, content: @Composable (BoxScope.() -> Unit)) {
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

@SuppressLint("StringFormatInvalid")
@ExperimentalMaterial3Api
@Composable
fun OverviewLastBackupCard(modifier: Modifier, lastBackupTime: Long) {
    val context = LocalContext.current
    val relativeTime by remember(lastBackupTime) {
        mutableStateOf(DateUtil.getShortRelativeTimeSpanString(context = context, time1 = lastBackupTime, time2 = DateUtil.getTimestamp()))
    }
    val finishTime by remember(lastBackupTime) {
        mutableStateOf(context.getString(R.string.args_finished_at, DateUtil.formatTimestamp(lastBackupTime, DateUtil.PATTERN_FINISH)))
    }
    OverviewCard(
        modifier = modifier,
        title = StringResourceToken.fromStringId(R.string.last_backup),
        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_package_2),
        colorContainer = ColorSchemeKeyTokens.PrimaryContainer,
        onColorContainer = ColorSchemeKeyTokens.OnPrimaryContainer,
        content = {
            TitleLargeText(
                text = (if (lastBackupTime == 0L) StringResourceToken.fromStringId(R.string.never) else StringResourceToken.fromString(relativeTime)).value,
                color = ColorSchemeKeyTokens.OnSurface.toColor()
            )
            if (lastBackupTime != 0L)
                BodyMediumText(
                    text = StringResourceToken.fromString(finishTime).value,
                    color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
                )
        },
        actionIcon = null,
    )
}

@ExperimentalAnimationApi
@Composable
fun Clickable(
    enabled: Boolean = true,
    title: StringResourceToken,
    value: StringResourceToken,
    desc: StringResourceToken? = null,
    leadingIcon: ImageVectorToken? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    com.xayah.core.ui.component.Clickable(enabled = enabled, desc = desc, onClick = onClick, indication = rememberRipple()) {
        Row(modifier = Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16)) {
            if (leadingIcon != null) {
                Icon(imageVector = leadingIcon.value, contentDescription = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                AnimatedTextContainer(targetState = title.value) { text ->
                    TitleLargeText(enabled = enabled, text = text, color = ColorSchemeKeyTokens.OnSurface.toColor(enabled), fontWeight = FontWeight.Normal)
                }
                AnimatedTextContainer(targetState = value.value) { text ->
                    TitleSmallText(enabled = enabled, text = text, color = ColorSchemeKeyTokens.Outline.toColor(enabled), fontWeight = FontWeight.Normal)
                }
                content?.invoke(this)
            }
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun PackageIcons(modifier: Modifier, packages: List<PackageEntity>, maxDisplayNum: Int = 6, size: Dp = SizeTokens.Level24) {
    val context = LocalContext.current
    var icons by remember { mutableStateOf(listOf<Drawable?>()) }
    LaunchedEffect(packages) {
        // Read icon from cached internal dir.
        withIOContext {
            val tmp = mutableListOf<Drawable?>()
            for (i in 0 until min(maxDisplayNum, packages.size)) {
                tmp.add(BaseUtil.readIcon(context, "${context.iconDir()}/${PathUtil.getPackageIconRelativePath(packages[i].packageName)}"))
            }
            icons = tmp
        }
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(-SizeTokens.Level4)) {
        for ((index, icon) in icons.withIndex()) {
            if (index == icons.size - 1) break
            if (icon == null) {
                Surface(modifier = Modifier.size(size), shape = ClippedCircleShape, color = ColorSchemeKeyTokens.PrimaryContainer.toColor()) {
                    Box(contentAlignment = Alignment.Center) {
                        LabelMediumText(text = "${packages[index].packageInfo.label.firstOrNull() ?: ""}", color = ColorSchemeKeyTokens.OnPrimaryContainer.toColor())
                    }
                }
            } else {
                AsyncImage(
                    modifier = Modifier
                        .size(size)
                        .clip(ClippedCircleShape),
                    model = ImageRequest.Builder(context)
                        .data(icon)
                        .crossfade(true)
                        .build(),
                    contentDescription = null
                )
            }
        }

        if (packages.size <= maxDisplayNum && icons.isNotEmpty()) {
            val last = icons.last()
            if (last == null) {
                Surface(modifier = Modifier.size(size), shape = CircleShape, color = ColorSchemeKeyTokens.PrimaryContainer.toColor()) {
                    Box(contentAlignment = Alignment.Center) {
                        LabelMediumText(text = "${packages[icons.lastIndex].packageInfo.label.firstOrNull() ?: ""}", color = ColorSchemeKeyTokens.OnPrimaryContainer.toColor())
                    }
                }
            } else {
                AsyncImage(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    model = ImageRequest.Builder(context)
                        .data(last)
                        .crossfade(true)
                        .build(),
                    contentDescription = null
                )
            }
        } else if (packages.size - maxDisplayNum > 0) {
            Surface(modifier = Modifier.size(size), shape = CircleShape, color = ColorSchemeKeyTokens.PrimaryContainer.toColor()) {
                Box(contentAlignment = Alignment.Center) {
                    LabelMediumText(text = "+${packages.size - maxDisplayNum}", color = ColorSchemeKeyTokens.OnPrimaryContainer.toColor())
                }
            }
        }
    }
}