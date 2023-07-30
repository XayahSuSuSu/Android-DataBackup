package com.xayah.databackup.ui.activity.main.page.main

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.CommonButton
import com.xayah.databackup.ui.component.DialogState
import com.xayah.databackup.ui.component.Loader
import com.xayah.databackup.ui.component.SlotScope
import com.xayah.databackup.ui.component.TextButton
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.component.paddingVertical
import com.xayah.databackup.ui.theme.JetbrainsMonoFamily
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.DateUtil
import com.xayah.databackup.util.ExceptionUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.CommonUtil.copyToClipboard
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.readBackupSavePath
import com.xayah.librootservice.service.RemoteRootService
import kotlinx.coroutines.launch

private suspend fun DialogState.openSaveDialog(context: Context, text: String) {
    val remoteRootService = RemoteRootService(context)
    val filePath = "${PathUtil.getTreePath()}/tree_${DateUtil.getTimestamp()}"
    var msg: String? = null

    open(
        initialState = false,
        title = context.getString(R.string.save_directory_structure),
        icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_account_tree),
        onLoading = {
            ExceptionUtil.tryService(onFailed = { msg = it }) {
                remoteRootService.writeText(text, filePath, context)
            }
            remoteRootService.destroyService()
        },
        block = { _ ->
            Text(
                text = if (msg == null) "${context.getString(R.string.succeed)}: $filePath"
                else "${context.getString(R.string.failed)}: $msg\n${context.getString(R.string.remote_service_err_reboot)}"
            )
        }
    )
}

@Composable
fun SlotScope.PageTree() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }

    Loader(
        modifier = Modifier.fillMaxSize(),
        onLoading = {
            text = PreparationUtil.tree(context.readBackupSavePath())
        },
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .paddingTop(CommonTokens.PaddingSmall)
                        .paddingHorizontal(CommonTokens.PaddingSmall)
                ) {
                    SelectionContainer() {
                        Text(
                            text = text,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                                .padding(CommonTokens.PaddingSmall),
                            fontFamily = JetbrainsMonoFamily,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingVertical(CommonTokens.PaddingSmall)
                        .paddingHorizontal(CommonTokens.PaddingMedium),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(text = stringResource(R.string.copy)) {
                        context.copyToClipboard(text)
                        Toast.makeText(context, context.getString(R.string.succeed), Toast.LENGTH_SHORT).show()
                    }
                    Spacer(modifier = Modifier.width(CommonTokens.PaddingMedium))
                    CommonButton(text = stringResource(R.string.save)) {
                        scope.launch {
                            dialogSlot.openSaveDialog(context, text)
                        }
                    }
                }
            }
        })
}
