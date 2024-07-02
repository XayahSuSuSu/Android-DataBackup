package com.xayah.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.AlertDialog
import com.xayah.core.ui.material3.Surface
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.DialogCheckBoxItem
import com.xayah.core.ui.model.DialogRadioItem
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.value
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Creates a [DialogState] and acts as a slot with [DialogState.Insert].
 */
@Composable
fun rememberDialogState(): DialogState {
    val state = remember { DialogState() }
    state.Insert()
    return state
}

class DialogState {
    private var content: (@Composable () -> Unit)? by mutableStateOf(null)

    @Composable
    internal fun Insert() = content?.invoke()

    private fun dismiss() {
        content = null
    }

    /**
     * Return **Pair<Boolean, T>**.
     *
     * If user clicks **confirmButton**, then return **Pair(true, T)**,
     * otherwise return **Pair(false, T)**.
     */
    suspend fun <T> open(
        initialState: T,
        title: StringResourceToken,
        icon: ImageVectorToken? = null,
        confirmText: StringResourceToken? = null,
        dismissText: StringResourceToken? = null,
        contentHorizontalPadding: Boolean = true,
        block: @Composable (MutableState<T>) -> Unit,
    ): Pair<Boolean, T> {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { dismiss() }
            content = {
                val uiState = remember { mutableStateOf(initialState) }
                AlertDialog(
                    onDismissRequest = {
                        dismiss()
                        continuation.resume(Pair(false, initialState))
                    },
                    confirmButton = {
                        TextButton(text = confirmText ?: StringResourceToken.fromStringId(R.string.confirm), onClick = {
                            dismiss()
                            continuation.resume(Pair(true, uiState.value))
                        })
                    },
                    dismissButton = {
                        TextButton(text = dismissText ?: StringResourceToken.fromStringId(R.string.cancel), onClick = {
                            dismiss()
                            continuation.resume(Pair(false, initialState))
                        })
                    },
                    title = { Text(text = title.value) },
                    icon = icon?.let { { Icon(imageVector = icon.value, contentDescription = null) } },
                    text = {
                        block(uiState)
                    },
                    textHorizontalPadding = contentHorizontalPadding,
                )
            }
        }
    }
}

suspend fun DialogState.confirm(title: StringResourceToken, text: StringResourceToken) = open(
    initialState = false,
    title = title,
    icon = null,
    block = { _ -> Text(text = text.value) }
).first

@Composable
fun RadioItem(enabled: Boolean = true, selected: Boolean, title: StringResourceToken, desc: StringResourceToken?, onClick: () -> Unit) {
    Surface(enabled = true, modifier = Modifier.fillMaxWidth(), onClick = onClick, color = ColorSchemeKeyTokens.Transparent.toColor()) {
        Row(
            modifier = Modifier
                .paddingVertical(SizeTokens.Level8)
                .paddingStart(SizeTokens.Level12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                enabled = enabled
            )
            Column {
                BodyLargeText(text = title.value, color = ColorSchemeKeyTokens.OnSurface.toColor(), fontWeight = FontWeight.Normal, enabled = enabled)
                if (desc != null)
                    BodyMediumText(text = desc.value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(), fontWeight = FontWeight.Normal, enabled = enabled)
            }
        }
    }
}

@Composable
fun CheckBoxItem(enabled: Boolean = true, checked: Boolean, title: StringResourceToken, desc: StringResourceToken?, onClick: () -> Unit) {
    Surface(enabled = true, modifier = Modifier.fillMaxWidth(), onClick = onClick, color = ColorSchemeKeyTokens.Transparent.toColor()) {
        Row(
            modifier = Modifier
                .paddingVertical(SizeTokens.Level8)
                .paddingHorizontal(SizeTokens.Level24),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BodyLargeText(text = title.value, color = ColorSchemeKeyTokens.OnSurface.toColor(), fontWeight = FontWeight.Normal, enabled = enabled)
                if (desc != null)
                    BodyMediumText(text = desc.value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(), fontWeight = FontWeight.Normal, enabled = enabled)
            }
            CheckIconButton(enabled = enabled, checked = checked, onCheckedChange = { onClick() })
        }
    }
}

suspend inline fun <reified T> DialogState.select(title: StringResourceToken, defIndex: Int = 0, items: List<DialogRadioItem<T>>) = open(
    initialState = defIndex,
    title = title,
    icon = null,
    contentHorizontalPadding = false,
    block = { uiState ->
        var selectedIndex by remember { mutableIntStateOf(defIndex) }
        LazyColumn(modifier = Modifier.selectableGroup()) {
            items(items.size) {
                RadioItem(selected = selectedIndex == it, title = items[it].title, desc = items[it].desc) {
                    selectedIndex = it
                    uiState.value = it
                }
                if (it != items.size - 1)
                    Divider()
            }
        }
    }
)

suspend inline fun <reified T> DialogState.select(title: StringResourceToken, def: List<Boolean>, items: List<DialogCheckBoxItem<T>>) = open(
    initialState = def,
    title = title,
    icon = null,
    contentHorizontalPadding = false,
    block = { uiState ->
        var checkedList by remember { mutableStateOf(def) }
        LazyColumn {
            items(items.size) {
                CheckBoxItem(checked = checkedList[it], title = items[it].title, desc = items[it].desc) {
                    val tmp = checkedList.toMutableList()
                    tmp[it] = tmp[it].not()
                    checkedList = tmp.toList()
                    uiState.value = checkedList
                }
                if (it != items.size - 1)
                    Divider()
            }
        }
    }
)

suspend fun DialogState.edit(
    title: StringResourceToken,
    defValue: String = "",
    singleLine: Boolean = true,
    label: StringResourceToken? = null,
    desc: StringResourceToken? = null,
) = open(
    initialState = defValue,
    title = title,
    icon = null,
    block = { uiState ->
        Column {
            OutlinedTextField(
                modifier = Modifier.paddingTop(SizeTokens.Level8),
                value = uiState.value,
                singleLine = singleLine,
                onValueChange = {
                    uiState.value = it
                },
                label = if (label == null) {
                    null
                } else {
                    { Text(text = label.value) }
                },
                supportingText = if (desc == null) {
                    null
                } else {
                    { Text(text = desc.value) }
                },
            )
        }
    }
)
