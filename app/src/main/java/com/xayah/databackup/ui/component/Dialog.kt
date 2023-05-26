package com.xayah.databackup.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
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

    suspend fun <T> open(
        initialState: T,
        title: String,
        icon: ImageVector? = null,
        confirmText: String? = null,
        dismissText: String? = null,
        block: @Composable (MutableState<T>) -> Unit
    ): T {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { dismiss() }
            content = {
                val uiState = remember { mutableStateOf(initialState) }
                AlertDialog(
                    onDismissRequest = {
                        dismiss()
                        continuation.resume(uiState.value)
                    },
                    confirmButton = {
                        TextButton(text = confirmText ?: stringResource(id = R.string.confirm), onClick = {
                            dismiss()
                            continuation.resume(uiState.value)
                        })
                    },
                    dismissButton = {
                        TextButton(text = dismissText ?: stringResource(id = R.string.cancel), onClick = {
                            dismiss()
                            continuation.resume(uiState.value)
                        })
                    },
                    title = { Text(text = title) },
                    icon = icon?.let { { Icon(imageVector = icon, contentDescription = null) } },
                    text = { block(uiState) },
                )
            }
        }
    }
}
