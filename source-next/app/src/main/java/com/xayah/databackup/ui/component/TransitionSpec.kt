package com.xayah.databackup.ui.component

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

private const val ContentFadeInDurationMillis = 220
private const val ContentFadeOutDurationMillis = 140
private const val ContentSizeDurationMillis = 220

fun <T> fadeContentTransitionSpec(): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
    (fadeIn(animationSpec = tween(durationMillis = ContentFadeInDurationMillis)) togetherWith
            fadeOut(animationSpec = tween(durationMillis = ContentFadeOutDurationMillis))).using(
        SizeTransform(
            clip = false,
            sizeAnimationSpec = { _, _ -> tween(durationMillis = ContentSizeDurationMillis) },
        )
    )
}

fun textTransitionSpec(): AnimatedContentTransitionScope<String>.() -> ContentTransform = {
    if (targetState > initialState) {
        slideInVertically { height -> height } + fadeIn() togetherWith
                slideOutVertically { height -> -height } + fadeOut()
    } else {
        slideInVertically { height -> -height } + fadeIn() togetherWith
                slideOutVertically { height -> height } + fadeOut()
    }.using(
        SizeTransform(clip = false)
    )
}
