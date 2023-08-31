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
import androidx.compose.material3.OutlinedCard
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
import com.xayah.databackup.ui.component.JetbrainsMonoLabelMediumText
import com.xayah.databackup.ui.component.Loader
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.TextButton
import com.xayah.databackup.ui.component.openFileOpDialog
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.component.paddingVertical
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.DateUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.CommonUtil.copyToClipboard
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.readBackupSavePath
import kotlinx.coroutines.launch

private suspend fun loadTree(context: Context) = PreparationUtil.tree(context.readBackupSavePath())

@Composable
fun PageTree() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    var text by remember { mutableStateOf("") }

    Loader(
        modifier = Modifier.fillMaxSize(),
        onLoading = {
            text = loadTree(context)
        },
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .paddingTop(CommonTokens.PaddingSmall)
                        .paddingHorizontal(CommonTokens.PaddingSmall)
                ) {
                    SelectionContainer {
                        JetbrainsMonoLabelMediumText(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                                .padding(CommonTokens.PaddingSmall),
                            text = text,
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
                            val filePath = PathUtil.getTreeSavePath(timestamp = DateUtil.getTimestamp())
                            dialogSlot.openFileOpDialog(
                                context = context,
                                title = context.getString(R.string.save_directory_structure),
                                filePath = filePath,
                                icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_account_tree),
                                text = text
                            )
                            text = loadTree(context)
                        }
                    }
                }
            }
        }
    )
}
