package com.xayah.databackup.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FadeVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable() AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = fadeIn(), exit = fadeOut(), content = content)
}
