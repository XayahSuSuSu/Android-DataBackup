package com.xayah.databackup.ui.activity.settings.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.MutableState

data class SingleChoiceDescClickableItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconId: Int,
    val content: String,
    val onPrepare: suspend () -> Pair<List<String>, String>,
    val onConfirm: (value: String) -> Unit,
)

data class SingleChoiceTextClickableItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconId: Int,
    val content: String,
    val onPrepare: suspend () -> Pair<List<String>, String>,
    val onConfirm: (value: String) -> Unit,
)

data class SwitchItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconId: Int,
    val isChecked: MutableState<Boolean>,
    val isEnabled: Boolean = true,
    val onCheckedChange: (isChecked: Boolean) -> Unit,
)

data class DescItem(
    val title: String,
    val subtitle: String,
    val enabled: Boolean = true,
)

data class StorageRadioDialogItem(
    var title: String,
    var progress: Float,
    var path: String,
    var display: String,
    var enabled: Boolean = true,
)