package com.xayah.core.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Creates a [ActionsState] and acts as a slot with [ActionsState.Insert].
 */
@Composable
fun rememberActionsState(): ActionsState {
    return remember { ActionsState() }
}

class ActionsState {
    private var content: (@Composable RowScope.() -> Unit)? by mutableStateOf(null)

    @Composable
    fun Insert(rowScope: RowScope) = content?.invoke(rowScope)

    fun setActions(actions: @Composable RowScope.() -> Unit) {
        content = actions
    }

    fun clearActions() {
        content = null
    }
}

val LocalActionsState: ProvidableCompositionLocal<ActionsState?> = staticCompositionLocalOf { null }
