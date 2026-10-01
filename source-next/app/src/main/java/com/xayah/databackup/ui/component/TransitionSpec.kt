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

fun <T> fadeSlideContentTransitionSpec(): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
    ((fadeIn(tween(220, delayMillis = 80)) + slideInVertically(tween(300)) { it / 12 }) togetherWith
            (fadeOut(tween(120)) + slideOutVertically(tween(220)) { -it / 12 }))
        .using(SizeTransform { _, _ -> tween(300) })
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
