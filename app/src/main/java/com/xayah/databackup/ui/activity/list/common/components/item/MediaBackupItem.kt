package com.xayah.databackup.ui.activity.list.common.components.item

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.xayah.databackup.R
import com.xayah.databackup.data.MediaInfoBackup
import com.xayah.databackup.ui.activity.guide.components.card.SerialSize
import com.xayah.databackup.ui.components.ConfirmDialog
import com.xayah.databackup.ui.components.TextButton
import com.xayah.databackup.util.GlobalObject

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun MediaBackupItem(
    mediaInfoBackup: MediaInfoBackup,
    modifier: Modifier = Modifier,
    onItemUpdate: () -> Unit
) {
    val context = LocalContext.current

    ListItem(
        modifier = modifier,
        icon = rememberDrawablePainter(
            drawable = mediaInfoBackup.icon
        ),
        title = mediaInfoBackup.name,
        subtitle = mediaInfoBackup.path,
        appSelected = null,
        dataSelected = mediaInfoBackup.selectData,
        chipContent = {
            if (mediaInfoBackup.sizeBytes != 0.0) {
                SerialSize(serial = mediaInfoBackup.sizeDisplay)
            }
        },
        actionContent = {
            val isDialogOpen = remember {
                mutableStateOf(false)
            }
            ConfirmDialog(
                isOpen = isDialogOpen,
                icon = Icons.Rounded.Info,
                title = stringResource(id = R.string.delete),
                content = {
                    Text(
                        text = stringResource(id = R.string.delete_confirm) +
                                stringResource(id = R.string.symbol_question),
                    )
                }) {
                GlobalObject.getInstance().mediaInfoBackupMap.value.remove(
                    mediaInfoBackup.name
                )
                onItemUpdate()
            }
            TextButton(text = stringResource(R.string.delete)) {
                isDialogOpen.value = true
            }
        },
        onClick = {
            mediaInfoBackup.selectData.value = mediaInfoBackup.selectData.value.not()
        }
    )
}
