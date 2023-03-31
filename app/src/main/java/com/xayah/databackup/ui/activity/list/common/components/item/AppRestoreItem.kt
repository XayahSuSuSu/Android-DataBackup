package com.xayah.databackup.ui.activity.list.common.components.item

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.core.graphics.drawable.toDrawable
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.librootservice.RootService
import com.xayah.databackup.ui.activity.guide.components.card.SerialDate
import com.xayah.databackup.ui.activity.guide.components.card.SerialInstalled
import com.xayah.databackup.ui.activity.guide.components.card.SerialSize
import com.xayah.databackup.ui.components.ConfirmDialog
import com.xayah.databackup.ui.components.TextButton
import com.xayah.databackup.util.Dates
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Path
import com.xayah.databackup.util.command.Command
import com.xayah.databackup.util.readIsReadIcon
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun AppRestoreItem(
    appInfoRestore: AppInfoRestore,
    modifier: Modifier = Modifier,
    onItemUpdate: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isOnThisDevice = appInfoRestore.isOnThisDevice.collectAsState()

    if (appInfoRestore.detailBase.appIcon == null) {
        appInfoRestore.detailBase.appIcon =
            AppCompatResources.getDrawable(context, R.drawable.ic_round_android)
        if (App.globalContext.readIsReadIcon()) {
            try {
                val bytes = RootService.getInstance()
                    .readBytesByDescriptor("${Path.getBackupDataSavePath()}/${appInfoRestore.detailBase.packageName}/icon.png")
                appInfoRestore.detailBase.appIcon =
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        .toDrawable(context.resources)
            } catch (_: Exception) {
            }
        }
    }

    ListItem(
        modifier = modifier,
        icon = rememberDrawablePainter(drawable = appInfoRestore.detailBase.appIcon),
        title = appInfoRestore.detailBase.appName,
        subtitle = appInfoRestore.detailBase.packageName,
        appSelected = appInfoRestore.selectApp,
        appEnabled = appInfoRestore.hasApp,
        dataSelected = appInfoRestore.selectData,
        dataEnabled = appInfoRestore.hasData,
        chipContent = {
            if (appInfoRestore.detailRestoreList.isNotEmpty()) {
                var dateMenu by remember { mutableStateOf(false) }

                if (isOnThisDevice.value) {
                    SerialInstalled()
                }
                Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                    var date = appInfoRestore.date
                    try {
                        date = Dates.getShortRelativeTimeSpanString(appInfoRestore.date.toLong()).toString()
                    } catch (_: Exception) {
                    }
                    SerialDate(serial = date, onClick = { dateMenu = true })
                    DropdownMenu(
                        expanded = dateMenu,
                        onDismissRequest = { dateMenu = false }
                    ) {
                        val appInfoRestores = mutableListOf<String>()
                        appInfoRestore.detailRestoreList.forEach {
                            appInfoRestores.add(Command.getDate(it.date))
                        }
                        for ((index, i) in appInfoRestores.withIndex()) {
                            DropdownMenuItem(
                                text = { Text(i) },
                                onClick = {
                                    appInfoRestore.restoreIndex = index
                                    dateMenu = false
                                })
                        }
                    }
                }
            }
            if (appInfoRestore.sizeBytes != 0.0) {
                SerialSize(serial = appInfoRestore.sizeDisplay)
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
                    val success = deleteAppInfoRestoreItem(appInfoRestore, onItemUpdate)
                    Toast.makeText(
                        context,
                        if (success) GlobalString.success else GlobalString.failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            Row {
                TextButton(text = stringResource(R.string.delete)) {
                    isDialogOpen.value = true
                }
            }
        },
        onClick = {
            if ((appInfoRestore.selectApp.value && appInfoRestore.selectData.value).not() &&
                (appInfoRestore.selectApp.value || appInfoRestore.selectData.value)
            ) {
                if (appInfoRestore.selectApp.value.not()) {
                    if (appInfoRestore.hasApp.value)
                        appInfoRestore.selectApp.value = appInfoRestore.selectApp.value.not()
                } else {
                    if (appInfoRestore.hasData.value)
                        appInfoRestore.selectData.value = appInfoRestore.selectData.value.not()
                }
            } else {
                if (appInfoRestore.hasApp.value)
                    appInfoRestore.selectApp.value = appInfoRestore.selectApp.value.not()
                if (appInfoRestore.hasData.value)
                    appInfoRestore.selectData.value = appInfoRestore.selectData.value.not()
            }
        }
    )
}
