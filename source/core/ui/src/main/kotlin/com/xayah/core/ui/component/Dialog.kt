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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.AlertDialog
import com.xayah.core.ui.material3.Surface
import com.xayah.core.ui.model.DialogCheckBoxItem
import com.xayah.core.ui.model.DialogRadioItem
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.SizeTokens
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

enum class DismissState {
    DISMISS,
    CANCEL,
    CONFIRM;

    val isConfirm get() = this == CONFIRM
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
        title: String,
        icon: ImageVector? = null,
        confirmText: String? = null,
        dismissText: String? = null,
        contentHorizontalPadding: Boolean = true,
        block: @Composable (MutableState<T>) -> Unit,
    ): Pair<DismissState, T> {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { dismiss() }
            content = {
                val uiState = remember { mutableStateOf(initialState) }
                AlertDialog(
                    onDismissRequest = {
                        dismiss()
                        continuation.resume(Pair(DismissState.DISMISS, initialState))
                    },
                    confirmButton = {
                        TextButton(text = confirmText ?: stringResource(id = R.string.confirm), onClick = {
                            dismiss()
                            continuation.resume(Pair(DismissState.CONFIRM, uiState.value))
                        })
                    },
                    dismissButton = {
                        TextButton(text = dismissText ?: stringResource(id = R.string.cancel), onClick = {
                            dismiss()
                            continuation.resume(Pair(DismissState.CANCEL, initialState))
                        })
                    },
                    title = { Text(text = title) },
                    icon = icon?.let { { Icon(imageVector = icon, contentDescription = null) } },
                    text = {
                        block(uiState)
                    },
                    textHorizontalPadding = contentHorizontalPadding,
                )
            }
        }
    }

    fun open(
        title: String,
        icon: ImageVector? = null,
        confirmText: @Composable () -> String,
        dismissText: @Composable () -> String,
        onConfirm: () -> Unit,
        block: @Composable () -> Unit,
    ) {
        content = {
            AlertDialog(
                onDismissRequest = {
                    dismiss()
                },
                confirmButton = {
                    TextButton(text = confirmText(), onClick = {
                        onConfirm()
                        dismiss()
                    })
                },
                dismissButton = {
                    TextButton(text = dismissText(), onClick = {
                        dismiss()
                    })
                },
                title = { Text(text = title) },
                icon = icon?.let { { Icon(imageVector = icon, contentDescription = null) } },
                text = {
                    block()
                },
            )
        }
    }

    fun open(
        title: String,
        defValue: String,
        singleLine: Boolean,
        label: String?,
        desc: String?,
        icon: ImageVector?,
        confirmText: @Composable () -> String,
        dismissText: @Composable () -> String,
        onConfirm: (String) -> Unit,
    ) {
        content = {
            var content by rememberSaveable { mutableStateOf(defValue) }

            AlertDialog(
                onDismissRequest = {
                    dismiss()
                },
                confirmButton = {
                    TextButton(text = confirmText(), onClick = {
                        onConfirm(content)
                        dismiss()
                    })
                },
                dismissButton = {
                    TextButton(text = dismissText(), onClick = {
                        dismiss()
                    })
                },
                title = { Text(text = title) },
                icon = icon?.let { { Icon(imageVector = icon, contentDescription = null) } },
                text = {
                    OutlinedTextField(
                        modifier = Modifier.paddingTop(SizeTokens.Level8),
                        value = content,
                        singleLine = singleLine,
                        onValueChange = {
                            content = it
                        },
                        label = if (label == null) {
                            null
                        } else {
                            { Text(text = label) }
                        },
                        supportingText = if (desc == null) {
                            null
                        } else {
                            { Text(text = desc) }
                        },
                    )
                },
            )
        }
    }
}

suspend fun DialogState.confirm(title: String, text: String) = open(
    initialState = false,
    title = title,
    icon = null,
    block = { _ -> Text(text = text) }
).first.isConfirm

fun DialogState.confirm(title: String, text: String, onConfirm: () -> Unit) = open(
    title = title,
    icon = null,
    confirmText = { stringResource(id = R.string.confirm) },
    dismissText = { stringResource(id = R.string.cancel) },
    onConfirm = onConfirm,
    block = { Text(text = text) }
)

@Composable
fun RadioItem(enabled: Boolean = true, selected: Boolean, title: String, desc: String?, onClick: () -> Unit) {
    Surface(enabled = true, modifier = Modifier.fillMaxWidth(), onClick = onClick, color = ThemedColorSchemeKeyTokens.Transparent.value) {
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
                BodyLargeText(text = title, color = ThemedColorSchemeKeyTokens.OnSurface.value, fontWeight = FontWeight.Normal, enabled = enabled)
                if (desc != null)
                    BodyMediumText(text = desc, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value, fontWeight = FontWeight.Normal, enabled = enabled)
            }
        }
    }
}

@Composable
fun CheckBoxItem(enabled: Boolean = true, checked: Boolean, title: String, desc: String?, onClick: () -> Unit) {
    Surface(enabled = true, modifier = Modifier.fillMaxWidth(), onClick = onClick, color = ThemedColorSchemeKeyTokens.Transparent.value) {
        Row(
            modifier = Modifier
                .paddingVertical(SizeTokens.Level8)
                .paddingHorizontal(SizeTokens.Level24),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BodyLargeText(text = title, color = ThemedColorSchemeKeyTokens.OnSurface.value, fontWeight = FontWeight.Normal, enabled = enabled)
                if (desc != null)
                    BodyMediumText(text = desc, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value, fontWeight = FontWeight.Normal, enabled = enabled)
            }
            CheckIconButton(enabled = enabled, checked = checked, onCheckedChange = { onClick() })
        }
    }
}

suspend inline fun <reified T> DialogState.select(title: String, defIndex: Int = 0, items: List<DialogRadioItem<T>>) = open(
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

suspend inline fun <reified T> DialogState.select(title: String, def: List<Boolean>, items: List<DialogCheckBoxItem<T>>) = open(
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
    title: String,
    defValue: String = "",
    singleLine: Boolean = true,
    label: String? = null,
    desc: String? = null,
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
                    { Text(text = label) }
                },
                supportingText = if (desc == null) {
                    null
                } else {
                    { Text(text = desc) }
                },
            )
        }
    }
)

fun DialogState.edit(
    title: String,
    defValue: String = "",
    singleLine: Boolean = true,
    label: String? = null,
    desc: String? = null,
    onConfirm: (String) -> Unit
) = open(
    title = title,
    defValue = defValue,
    singleLine = singleLine,
    label = label,
    desc = desc,
    icon = null,
    confirmText = { stringResource(id = R.string.confirm) },
    dismissText = { stringResource(id = R.string.cancel) },
    onConfirm = onConfirm,
)
