package com.xayah.feature.main.packages

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.dotlottie.dlplayer.Mode
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.xayah.core.ui.component.AnimatedLinearProgressIndicator
import com.xayah.core.ui.component.BodyLargeText
import com.xayah.core.ui.component.BodySmallText
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LinearProgressIndicator
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.model.RefreshState
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.SizeTokens

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun ListScaffold(
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    progress: Float? = null,
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    innerBottomSpacer: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    content: @Composable (BoxScope.(innerPadding: PaddingValues) -> Unit)
) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                SecondaryTopBar(
                    scrollBehavior = scrollBehavior,
                    title = title,
                    subtitle = subtitle,
                    actions = actions,
                    onBackClick = onBackClick,
                )
                if (progress == -1F) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    var targetProgress by remember { mutableFloatStateOf(0f) }
                    val animatedProgress = animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(),
                        label = AnimationTokens.AnimatedProgressLabel
                    )
                    if (progress != null) {
                        targetProgress = progress
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = animatedProgress.value)
                    }
                }
            }
        },
        floatingActionButtonPosition = floatingActionButtonPosition,
        floatingActionButton = floatingActionButton,
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f), content = {
                content(this, innerPadding)
            })

            if (innerBottomSpacer) InnerBottomSpacer(innerPadding = innerPadding)
        }
    }
}

@Composable
fun DotLottieView(isRefreshing: Boolean, refreshState: RefreshState) {
    DotLottieAnimation(
        source = if (isRefreshing) DotLottieSource.Asset("loading.lottie") else DotLottieSource.Asset("squirrel.lottie"),
        autoplay = true,
        loop = true,
        playMode = Mode.FORWARD,
        modifier = Modifier.background(Color.Transparent)
    )
    if (isRefreshing) {
        BodyLargeText(text = refreshState.user, color = ThemedColorSchemeKeyTokens.OnSurface.value)
        AnimatedLinearProgressIndicator(
            modifier = Modifier.paddingVertical(SizeTokens.Level8),
            progress = refreshState.progress,
            strokeCap = StrokeCap.Round
        )
        BodySmallText(text = refreshState.pkg, textAlign = TextAlign.Center, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value)
    } else {
        BodyLargeText(text = stringResource(id = R.string.pull_down_to_refresh), textAlign = TextAlign.Center, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value)
    }
}

@Composable
fun DotLottieView(isLoading: Boolean) {
    DotLottieAnimation(
        source = DotLottieSource.Asset("bear.lottie"),
        autoplay = true,
        loop = true,
        playMode = Mode.FORWARD,
        modifier = Modifier.background(Color.Transparent)
    )
    BodyLargeText(
        text = (
                if (isLoading)
                    stringResource(id = R.string.loading)
                else
                    stringResource(id = R.string.no_backups_found_warning)
                ),
        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
        textAlign = TextAlign.Center
    )
}
