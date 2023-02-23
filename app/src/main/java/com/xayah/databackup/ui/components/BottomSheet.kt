package com.xayah.databackup.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.window.Dialog
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Composable
fun BottomSheet(isOpen: MutableState<Boolean>, content: @Composable (ColumnScope.() -> Unit)) {
    if (isOpen.value) {
        Dialog(onDismissRequest = { isOpen.value = false }) {
            val nonePadding = dimensionResource(R.dimen.padding_none)
            val mediumPadding = dimensionResource(R.dimen.padding_medium)
            Surface(
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(
                    modifier = Modifier.padding(
                        nonePadding,
                        mediumPadding,
                        nonePadding,
                        nonePadding
                    )
                ) {
                    content()
                }
            }
        }
    }
}
