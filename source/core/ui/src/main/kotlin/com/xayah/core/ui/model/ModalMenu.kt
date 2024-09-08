package com.xayah.core.ui.model

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.xayah.core.ui.R
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens

data class ActionMenuItem(
    val title: String,
    val icon: ImageVector? = null,
    val color: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.Unspecified,
    val backgroundColor: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.OnPrimary,
    val enabled: Boolean,
    val countdown: Int = 0,
    val dismissOnClick: Boolean = false,
    val secondaryMenu: List<ActionMenuItem>,
    val onClick: (suspend () -> Unit)? = null,
)

fun getActionMenuReturnItem(context: Context, onClick: (suspend () -> Unit)? = null) = ActionMenuItem(
    title = context.getString(R.string.word_return),
    icon = Icons.Rounded.ArrowBack,
    enabled = true,
    secondaryMenu = listOf(),
    onClick = onClick
)

fun getActionMenuConfirmItem(context: Context, onClick: suspend () -> Unit) = ActionMenuItem(
    title = context.getString(R.string.confirm),
    icon = Icons.Rounded.Warning,
    color = ThemedColorSchemeKeyTokens.Error,
    backgroundColor = ThemedColorSchemeKeyTokens.ErrorContainer,
    enabled = true,
    countdown = 1,
    secondaryMenu = listOf(),
    onClick = onClick
)

fun getActionMenuDeleteItem(context: Context, onClick: suspend () -> Unit) = ActionMenuItem(
    title = context.getString(R.string.delete),
    icon = Icons.Rounded.Delete,
    color = ThemedColorSchemeKeyTokens.Error,
    backgroundColor = ThemedColorSchemeKeyTokens.ErrorContainer,
    enabled = true,
    countdown = 1,
    secondaryMenu = listOf(),
    onClick = onClick
)
