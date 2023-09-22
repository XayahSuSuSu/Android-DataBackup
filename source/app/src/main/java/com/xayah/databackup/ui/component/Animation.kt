package com.xayah.databackup.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xayah.databackup.ui.token.AnimationTokens

@Composable
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