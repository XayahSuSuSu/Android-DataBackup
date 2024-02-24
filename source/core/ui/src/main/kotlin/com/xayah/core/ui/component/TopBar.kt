package com.xayah.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.value

@ExperimentalMaterial3Api
@Composable
fun PrimaryTopBar(scrollBehavior: TopAppBarScrollBehavior?, title: String, actions: @Composable RowScope.() -> Unit = {}) {
    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = title) },
        scrollBehavior = scrollBehavior,
        actions = actions,
    )
}

@ExperimentalMaterial3Api
@Composable
fun SecondaryTopBar(
    scrollBehavior: TopAppBarScrollBehavior?,
    title: StringResourceToken,
    actions: @Composable RowScope.() -> Unit = {},
    onBackClick: (() -> Unit)? = null
) {
    val navController = LocalNavController.current!!
    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = title.value) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            ArrowBackButton {
                if (onBackClick != null) onBackClick.invoke()
                else navController.popBackStack()
            }
        },
        actions = actions,
    )
}

@ExperimentalMaterial3Api
@Composable
fun SecondaryMediumTopBar(
    scrollBehavior: TopAppBarScrollBehavior?,
    title: StringResourceToken,
    actions: @Composable RowScope.() -> Unit = {},
    onBackClick: (() -> Unit)? = null
) {
    val navController = LocalNavController.current!!
    MediumTopAppBar(
        title = { TopBarTitle(text = title.value) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            ArrowBackButton {
                if (onBackClick != null) onBackClick.invoke()
                else navController.popBackStack()
            }
        },
        actions = actions,
    )
}

@ExperimentalMaterial3Api
@Composable
fun SecondaryLargeTopBar(
    scrollBehavior: TopAppBarScrollBehavior?,
    title: StringResourceToken,
    actions: @Composable RowScope.() -> Unit = {},
    onBackClick: (() -> Unit)? = null
) {
    val navController = LocalNavController.current!!
    LargeTopAppBar(
        title = { Text(text = title.value) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            ArrowBackButton {
                if (onBackClick != null) onBackClick.invoke()
                else navController.popBackStack()
            }
        },
        actions = actions,
    )
}

@ExperimentalMaterial3Api
@Composable
fun SecondaryTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    topBarState: TopBarState,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column {
        SecondaryTopBar(
            scrollBehavior = scrollBehavior,
            title = topBarState.title,
            actions = actions,
        )
        if (topBarState.indeterminate) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            if (topBarState.progress != 1f) {
                var targetProgress by remember { mutableFloatStateOf(0f) }
                val animatedProgress = animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(),
                    label = AnimationTokens.AnimatedProgressLabel
                )
                targetProgress = topBarState.progress
                if (animatedProgress.value != 1f)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = animatedProgress.value)
            }
        }

    }
}

@ExperimentalMaterial3Api
@Composable
fun SecondaryMediumTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    topBarState: TopBarState,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column {
        SecondaryMediumTopBar(
            scrollBehavior = scrollBehavior,
            title = topBarState.title,
            actions = actions,
        )
        if (topBarState.indeterminate) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            if (topBarState.progress != 1f) {
                var targetProgress by remember { mutableFloatStateOf(0f) }
                val animatedProgress = animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(),
                    label = AnimationTokens.AnimatedProgressLabel
                )
                targetProgress = topBarState.progress
                if (animatedProgress.value != 1f)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = animatedProgress.value)
            }
        }

    }
}
