package com.xayah.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.xayah.core.ui.R
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
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
        block: @Composable (T) -> Unit,
    ): Pair<Boolean, T> {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { dismiss() }
            content = {
                val uiState by remember { mutableStateOf(initialState) }
                AlertDialog(
                    onDismissRequest = {
                        dismiss()
                        continuation.resume(Pair(false, uiState))
                    },
                    confirmButton = {
                        TextButton(text = confirmText ?: StringResourceToken.fromStringId(R.string.confirm), onClick = {
                            dismiss()
                            continuation.resume(Pair(true, uiState))
                        })
                    },
                    dismissButton = {
                        TextButton(text = dismissText ?: StringResourceToken.fromStringId(R.string.cancel), onClick = {
                            dismiss()
                            continuation.resume(Pair(false, uiState))
                        })
                    },
                    title = { Text(text = title.value) },
                    icon = icon?.let { { Icon(imageVector = icon.value, contentDescription = null) } },
                    text = {
                        block(uiState)
                    },
                )
            }
        }
    }
}

suspend fun DialogState.openConfirm(text: StringResourceToken) = open(
    initialState = false,
    title = StringResourceToken.fromStringId(R.string.prompt),
    icon = ImageVectorToken.fromVector(Icons.Outlined.Info),
    block = { _ -> Text(text = text.value) }
)
