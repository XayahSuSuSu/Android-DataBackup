package com.xayah.databackup.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.unit.dp

@ExperimentalMaterial3Api
@Composable
fun BottomSheet(isOpen: MutableState<Boolean>, content: @Composable (ColumnScope.() -> Unit)) {
    if (isOpen.value) {
        val bottomSheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { isOpen.value = false },
            sheetState = bottomSheetState,
            windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
        ) {
            content()
        }
    }
}
