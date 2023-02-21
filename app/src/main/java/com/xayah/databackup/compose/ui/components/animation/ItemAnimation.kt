package com.xayah.databackup.compose.ui.components.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun ItemExpandAnimation(
    expanded: Boolean,
    content: @Composable (AnimatedVisibilityScope.(Boolean) -> Unit)
) {
    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            fadeIn(animationSpec = tween(150, 150)) with
                    fadeOut(animationSpec = tween(150)) using
                    SizeTransform { initialSize, targetSize ->
                        if (targetState) {
                            keyframes {
                                // Expand horizontally first.
                                IntSize(targetSize.width, initialSize.height) at 150
                                durationMillis = 300
                            }
                        } else {
                            keyframes {
                                // Shrink vertically first.
                                IntSize(initialSize.width, targetSize.height) at 150
                                durationMillis = 300
                            }
                        }
                    }
        },
        content = content
    )
}

@ExperimentalMaterial3Api
@Composable
fun ContentFade(
    visibleState: MutableTransitionState<Boolean>,
    content: @Composable() (AnimatedVisibilityScope.() -> Unit)
) {
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(),
        exit = fadeOut(),
        content = content
    )
}

@ExperimentalMaterial3Api
@Composable
fun ContentFadeSlideHorizontallyAnimation(
    visibleState: MutableTransitionState<Boolean>,
    content: @Composable() AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn() + slideInHorizontally(),
        exit = fadeOut() + slideOutHorizontally(),
        content = content
    )
}