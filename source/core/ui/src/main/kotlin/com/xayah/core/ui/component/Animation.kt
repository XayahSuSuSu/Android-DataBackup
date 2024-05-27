package com.xayah.core.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.xayah.core.ui.token.AnimationTokens

@ExperimentalAnimationApi
@Composable
fun AnimatedTextContainer(
    targetState: String,
    content: @Composable AnimatedVisibilityScope.(targetState: String) -> Unit,
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

@Composable
@SuppressLint("UnusedTransitionTargetStateParameter")
fun <T> emphasizedOffset(targetState: T): State<Dp> {
    val transition = updateTransition(targetState, label = AnimationTokens.EmphasizedOffsetLabel)
    return transition.animateDp(transitionSpec = {
        keyframes {
            durationMillis = 300
            0.dp at 0 with FastOutSlowInEasing
            (-10).dp at 25 with FastOutSlowInEasing
            0.dp at 50 with FastOutSlowInEasing
            10.dp at 75 with FastOutSlowInEasing
            0.dp at 100 with FastOutSlowInEasing
            (-8).dp at 125 with FastOutSlowInEasing
            0.dp at 150 with FastOutSlowInEasing
            8.dp at 175 with FastOutSlowInEasing
            0.dp at 200 with FastOutSlowInEasing
            (-5).dp at 225 with FastOutSlowInEasing
            0.dp at 250 with FastOutSlowInEasing
            5.dp at 275 with FastOutSlowInEasing
            0.dp at 300 with FastOutSlowInEasing
        }
    }, label = AnimationTokens.EmphasizedOffsetLabel, targetValueByState = { _ -> 0.dp })
}

@Composable
fun AnimatedNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        contentAlignment = contentAlignment,
        route = route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        exitTransition = {
            slideOutHorizontally()
        },
        popEnterTransition = {
            slideInHorizontally()
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        builder = builder,
    )
}
