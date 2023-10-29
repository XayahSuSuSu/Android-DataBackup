package com.xayah.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import com.xayah.core.ui.token.AnimationTokens

@ExperimentalAnimationApi
@Composable
fun AnimatedTextContainer(
    targetState: String,
    content: @Composable() AnimatedVisibilityScope.(targetState: String) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            // Compare the incoming number with the previous number.
            if (targetState > initialState) {
                // If the target number is larger, it slides up and fades in
                // while the initial (smaller) number slides up and fades out.
                (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
            } else {
                // If the target number is smaller, it slides down and fades in
                // while the initial number slides down and fades out.
                (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
            }.using(
                // Disable clipping since the faded slide-in/out should
                // be displayed out of bounds.
                SizeTransform(clip = false)
            )
        },
        label = AnimationTokens.AnimatedTextLabel,
        content = content
    )
}
