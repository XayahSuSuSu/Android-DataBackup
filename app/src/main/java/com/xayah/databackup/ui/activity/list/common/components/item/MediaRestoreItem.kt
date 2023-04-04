package com.xayah.databackup.ui.activity.list.common.components.item

import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.xayah.databackup.R
import com.xayah.databackup.data.MediaInfoRestore
import com.xayah.databackup.ui.activity.guide.components.card.SerialDate
import com.xayah.databackup.ui.activity.guide.components.card.SerialSize
import com.xayah.databackup.ui.components.ConfirmDialog
import com.xayah.databackup.ui.components.TextButton
import com.xayah.databackup.util.Dates
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.command.Command
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun MediaRestoreItem(
    mediaInfoRestore: MediaInfoRestore,
    modifier: Modifier = Modifier,
    onItemUpdate: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ListItem(
        modifier = modifier,
        icon = rememberDrawablePainter(drawable = mediaInfoRestore.icon),
        title = mediaInfoRestore.name,
        subtitle = mediaInfoRestore.path,
        appSelected = null,
        dataSelected = mediaInfoRestore.selectData,
        chipContent = {
            if (mediaInfoRestore.detailRestoreList.isNotEmpty()) {
                var dateMenu by remember { mutableStateOf(false) }

                Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                    var date = mediaInfoRestore.date
                    try {
                        date = Dates.getShortRelativeTimeSpanString(mediaInfoRestore.date.toLong()).toString()
                    } catch (_: Exception) {
                    }
                    SerialDate(serial = date, onClick = { dateMenu = true })
                    DropdownMenu(
                        expanded = dateMenu,
                        onDismissRequest = { dateMenu = false }
                    ) {
                        val items = mutableListOf<String>()
                        mediaInfoRestore.detailRestoreList.forEach {
                            items.add(Command.getDate(it.date))
                        }
                        for ((index, i) in items.withIndex()) {
                            DropdownMenuItem(
                                text = { Text(i) },
                                onClick = {
                                    mediaInfoRestore.restoreIndex = index
                                    dateMenu = false
                                })
                        }
                    }
                }
            }
            if (mediaInfoRestore.sizeBytes != 0.0) {
                SerialSize(serial = mediaInfoRestore.sizeDisplay)
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
                scope.launch {
                    val success = deleteMediaInfoRestoreItem(mediaInfoRestore, onItemUpdate)
                    Toast.makeText(
                        context,
                        if (success) GlobalString.success else GlobalString.failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            TextButton(text = stringResource(R.string.delete)) {
                isDialogOpen.value = true
            }
        },
        onClick = {
            mediaInfoRestore.selectData.value = mediaInfoRestore.selectData.value.not()
        }
    )
}
