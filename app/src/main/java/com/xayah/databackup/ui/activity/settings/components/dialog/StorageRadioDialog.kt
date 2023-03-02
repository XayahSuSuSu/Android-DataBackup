package com.xayah.databackup.ui.activity.settings.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.settings.components.StorageRadioDialogItem

@ExperimentalMaterial3Api
@Composable
fun StorageRadioDialog(
    isOpen: MutableState<Boolean>,
    icon: ImageVector,
    items: List<StorageRadioDialogItem>,
    selected: MutableState<StorageRadioDialogItem>,
    onConfirm: (index: Int) -> Unit,
) {
    val tinyPadding = dimensionResource(R.dimen.padding_tiny)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    RadioButtonDialog(
        isOpen = isOpen,
        icon = icon,
        title = stringResource(id = R.string.backup_dir),
        items = items,
        selected = selected,
        content = {
            RadioButtonGroup(
                items = items,
                selected = selected,
                itemVerticalArrangement = Arrangement.spacedBy(smallPadding)
            ) { item ->
                Column(verticalArrangement = Arrangement.spacedBy(tinyPadding)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.clip(CircleShape),
                        progress = item.progress
                    )
                    Text(
                        text = item.display,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        onConfirm = onConfirm,
    )
}
