package com.xayah.databackup.ui.activity.settings.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch

data class SingleChoiceTextClickableItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconId: Int,
    val content: String,
    val onPrepare: suspend () -> Pair<List<String>, String>,
    val onConfirm: (value: String) -> Unit,
)

@ExperimentalMaterial3Api
@Composable
fun SingleChoiceTextClickable(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: String,
    onPrepare: suspend () -> Pair<List<String>, String>,
    onConfirm: (value: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val contentState = remember {
        mutableStateOf(content)
    }
    val isDialogOpen = remember {
        mutableStateOf(false)
    }
    val items = remember {
        mutableStateOf(listOf(""))
    }
    val selected = remember {
        mutableStateOf("")
    }
    RadioButtonTextDialog(
        isOpen = isDialogOpen,
        icon = icon,
        title = title,
        items = items.value,
        selected = selected,
        onConfirm = {
            contentState.value = items.value[it]
            onConfirm(items.value[it])
        }
    )
    Clickable(
        title = title,
        subtitle = subtitle,
        icon = icon,
        content = contentState.value
    ) {
        scope.launch {
            val (list, value) = onPrepare()
            items.value = list
            selected.value = value
            isDialogOpen.value = true
        }
    }
}
