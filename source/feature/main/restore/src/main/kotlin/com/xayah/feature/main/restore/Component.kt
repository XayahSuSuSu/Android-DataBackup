package com.xayah.feature.main.restore

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import com.dotlottie.dlplayer.Mode
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.xayah.core.ui.component.BodyLargeText
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.OverviewCard
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.util.DateUtil

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun RestoreScaffold(
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    actions: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable (BoxScope.() -> Unit)
) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryLargeTopBar(
                scrollBehavior = scrollBehavior,
                title = title,
            )
        },
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }

            if (actions != null) {
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SizeTokens.Level16),
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12, Alignment.End),
                ) {
                    actions()
                }

                InnerBottomSpacer(innerPadding = innerPadding)
            }
        }
    }
}

@SuppressLint("StringFormatInvalid")
@ExperimentalMaterial3Api
@Composable
fun OverviewLastRestoreCard(modifier: Modifier, lastRestoreTime: Long) {
    val context = LocalContext.current
    val relativeTime by remember(lastRestoreTime) {
        mutableStateOf(DateUtil.getShortRelativeTimeSpanString(context = context, time1 = lastRestoreTime, time2 = DateUtil.getTimestamp()))
    }
    val finishTime by remember(lastRestoreTime) {
        mutableStateOf(context.getString(R.string.args_finished_at, DateUtil.formatTimestamp(lastRestoreTime, DateUtil.PATTERN_FINISH)))
    }
    OverviewCard(
        modifier = modifier,
        title = stringResource(id = R.string.last_restore),
        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_package_2),
        colorContainer = ThemedColorSchemeKeyTokens.PrimaryContainer,
        onColorContainer = ThemedColorSchemeKeyTokens.OnPrimaryContainer,
        content = {
            TitleLargeText(
                text = if (lastRestoreTime == 0L) stringResource(id = R.string.never) else relativeTime,
                color = ThemedColorSchemeKeyTokens.OnSurface.value
            )
            if (lastRestoreTime != 0L)
                BodyMediumText(
                    text = finishTime,
                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                )
        },
        actionIcon = null,
    )
}

@Composable
fun DotLottieView(isRefreshing: Boolean, text: String) {
    DotLottieAnimation(
        source = if (isRefreshing) DotLottieSource.Asset("loading.lottie") else DotLottieSource.Asset("squirrel.lottie"),
        autoplay = true,
        loop = true,
        playMode = Mode.FORWARD,
        modifier = Modifier.background(Color.Transparent)
    )
    BodyLargeText(
        text = text,
        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
        textAlign = TextAlign.Center
    )
}
