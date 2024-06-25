package com.xayah.feature.main.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dotlottie.dlplayer.Mode
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.xayah.core.ui.component.BodyLargeText
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.component.TitleSmallText
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.material3.Card
import com.xayah.core.ui.material3.SnackbarHost
import com.xayah.core.ui.material3.SnackbarHostState
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.value

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun SettingsScaffold(
    scrollBehavior: TopAppBarScrollBehavior,
    snackbarHostState: SnackbarHostState? = null,
    title: StringResourceToken,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (BoxScope.(innerPadding: PaddingValues) -> Unit)
) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryLargeTopBar(
                scrollBehavior = scrollBehavior,
                title = title,
                actions = actions
            )
        },
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(
                    modifier = Modifier.paddingBottom(SizeTokens.Level24 + SizeTokens.Level4),
                    hostState = snackbarHostState,
                )
            }
        },
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f), content = { content(this, innerPadding) })
        }
    }
}

@Composable
fun DotLottieView() {
    DotLottieAnimation(
        source = DotLottieSource.Asset("squirrel.lottie"),
        autoplay = true,
        loop = true,
        playMode = Mode.FORWARD,
        modifier = Modifier.background(Color.Transparent)
    )
    BodyLargeText(
        text = StringResourceToken.fromStringId(R.string.it_is_empty).value,
        textAlign = TextAlign.Center,
        color = com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens.OnSurfaceVariant.toColor()
    )
}

@ExperimentalMaterial3Api
@Composable
fun ContributorCard(avatar: String, name: String, desc: String, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(SizeTokens.Level16), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16)) {
            Box {
                Icon(
                    modifier = Modifier.size(SizeTokens.Level48),
                    tint = ColorSchemeKeyTokens.OnSurface.toColor(),
                    imageVector = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_emoticon).value,
                    contentDescription = null
                )
                AsyncImage(
                    modifier = Modifier
                        .size(SizeTokens.Level48)
                        .clip(CircleShape),
                    model = ImageRequest.Builder(context)
                        .data(avatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = null
                )
            }
            Column {
                TitleMediumText(text = name, fontWeight = FontWeight.Bold, color = ColorSchemeKeyTokens.OnSurface.toColor())
                TitleSmallText(text = desc, fontWeight = FontWeight.Bold, color = ColorSchemeKeyTokens.Outline.toColor())
            }
        }
    }
}