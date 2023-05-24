package com.xayah.databackup.ui.token

import androidx.compose.ui.unit.dp

sealed class State {
    object Loading : State()
    object Succeed : State()
    object Failed : State()
}

object StateTokens {
    val IconSize = 48.dp
    val IndicatorPadding = 8.dp
    const val CrossFadeLabel = "StateCrossFade"
}