package com.xayah.databackup.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@ExperimentalMaterial3Api
@Composable
fun BottomSheet(isOpen: MutableState<Boolean>, content: @Composable (ColumnScope.() -> Unit)) {
    if (isOpen.value) {
        val bottomSheetState = rememberSheetState()
        ModalBottomSheet(
            onDismissRequest = { isOpen.value = false },
            sheetState = bottomSheetState,
        ) {
            content()
        }
    }
}
