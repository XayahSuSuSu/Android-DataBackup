package com.xayah.databackup.ui.activity.guide.components

import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import com.xayah.databackup.data.LoadingState

data class ItemEnvironment(
    @StringRes val itemId: Int,
    var cardState: MutableState<LoadingState>,
    val onCheck: suspend () -> LoadingState
)

data class IntroductionItem(
    @StringRes val titleId: Int,
    @StringRes val subtitleId: Int,
    @StringRes val contentId: Int,
)

data class ItemUpdate(
    val version: String,
    val content: String,
    val link: String,
)
