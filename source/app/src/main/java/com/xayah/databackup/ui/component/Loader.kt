package com.xayah.databackup.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xayah.databackup.ui.token.AnimationTokens

@Composable
fun Loader(modifier: Modifier, onLoading: suspend () -> Unit = {}, content: @Composable () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(null) {
        onLoading()
        isLoading = false
    }
    Crossfade(
        targetState = isLoading,
        label = AnimationTokens.CrossFadeLabel
    ) { state ->
        when (state) {
            true -> Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            false -> content()
        }
    }
}

@Composable
fun Loader(modifier: Modifier, isLoading: Boolean, content: @Composable () -> Unit) {
    Crossfade(
        targetState = isLoading,
        label = AnimationTokens.CrossFadeLabel
    ) { state ->
        when (state) {
            true -> Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            false -> content()
        }
    }
}

@Composable
fun <T> Loader(
    modifier: Modifier,
    onLoading: suspend () -> Unit = {},
    uiState: T,
    content: @Composable (T) -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(null) {
        onLoading()
        isLoading = false
    }
    Crossfade(
        targetState = isLoading,
        label = AnimationTokens.CrossFadeLabel
    ) { state ->
        when (state) {
            true -> Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            false -> content(uiState)
        }
    }
}
