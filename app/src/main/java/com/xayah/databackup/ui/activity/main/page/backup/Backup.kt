package com.xayah.databackup.ui.activity.main.page.backup

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.router.MainRoutes
import com.xayah.databackup.ui.activity.operation.OperationActivity
import com.xayah.databackup.ui.component.CardActionButton
import com.xayah.databackup.ui.component.DialogState
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.Module
import com.xayah.databackup.ui.component.OverLookCard
import com.xayah.databackup.ui.component.RadioButtonGroup
import com.xayah.databackup.ui.component.VerticalGrid
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.RadioTokens
import com.xayah.databackup.util.ConstantUtil
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.readBackupSavePath
import com.xayah.databackup.util.readExternalBackupSaveChild
import com.xayah.databackup.util.readInternalBackupSaveChild
import com.xayah.databackup.util.saveBackupSavePath
import com.xayah.databackup.util.saveExternalBackupSaveChild
import com.xayah.databackup.util.saveInternalBackupSaveChild
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.ExceptionUtil.tryService
import kotlinx.coroutines.launch

enum class StorageItemType {
    Internal,
    External,
    Custom,
}

data class StorageItem(
    var title: String,
    var type: StorageItemType,
    var progress: Float,
    var parent: String,      // default: /storage/emulated/0
    var child: String,       // default: DataBackup
    var display: String,     // default: /storage/emulated/0/DataBackup
    var enabled: Boolean,
)

@ExperimentalMaterial3Api
private suspend fun DialogState.openDirectoryDialog(context: Context) {
    val items = mutableListOf<StorageItem>()

    val (state, item) = open(
        initialState = StorageItem(
            title = "",
            type = StorageItemType.Internal,
            progress = 0f,
            parent = "",
            child = "",
            display = "",
            enabled = true
        ),
        title = context.getString(R.string.backup_dir),
        icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_folder_open),
        onLoading = {
            val remoteRootService = RemoteRootService(context)

            // Internal storage
            val internalParent = ConstantUtil.DefaultBackupParent
            val internalChild = context.readInternalBackupSaveChild()
            val internalPath = "${internalParent}/${internalChild}"
            val internalItem = StorageItem(
                title = context.getString(R.string.internal_storage),
                type = StorageItemType.Internal,
                progress = 0f,
                parent = internalParent,
                child = internalChild,
                display = internalPath,
                enabled = true
            )

            tryService(onFailed = { msg ->
                internalItem.display =
                    "${context.getString(R.string.fetch_failed)}: $msg\n${context.getString(R.string.remote_service_err_info)}"
            }) {
                val internalStatFs = remoteRootService.readStatFs(internalParent)
                internalItem.progress = internalStatFs.availableBytes.toFloat() / internalStatFs.totalBytes
            }
            items.add(internalItem)

            // External storage
            val externalList = PreparationUtil.listExternalStorage()
            val externalChild = context.readExternalBackupSaveChild()
            for (storageItem in externalList) {
                // e.g. /mnt/media_rw/E7F9-FA61 exfat
                try {
                    val (parent, type) = storageItem.split(" ")
                    val externalPath = "${parent}/${externalChild}"
                    val item = StorageItem(
                        title = "${context.getString(R.string.external_storage)} $type",
                        type = StorageItemType.External,
                        progress = 0f,
                        parent = parent,
                        child = externalChild,
                        display = externalPath,
                        enabled = true
                    )
                    tryService(onFailed = { msg ->
                        item.display =
                            "${context.getString(R.string.fetch_failed)}: $msg\n${context.getString(R.string.remote_service_err_info)}"
                    }) {
                        val externalPathStatFs = remoteRootService.readStatFs(parent)
                        item.progress = externalPathStatFs.availableBytes.toFloat() / externalPathStatFs.totalBytes
                    }
                    // Check the format
                    val supported = type.lowercase() in ConstantUtil.SupportedExternalStorageFormat
                    if (supported.not()) {
                        item.title = "${context.getString(R.string.unsupported_format)}: $type"
                        item.enabled = false
                    }
                    items.add(item)
                } catch (_: Exception) {
                }
            }
            remoteRootService.destroyService()
        },
        block = { uiState ->
            var defIndex = items.indexOfFirst { it.display == context.readBackupSavePath() }
            if (defIndex == -1) {
                // The save path is not in storage items, reset it.
                context.saveBackupSavePath(ConstantUtil.DefaultBackupSavePath)
                defIndex = 0
            }
            RadioButtonGroup(
                items = items.toList(),
                defSelected = items[defIndex],
                itemVerticalArrangement = Arrangement.spacedBy(RadioTokens.ItemVerticalPadding),
                onItemClick = {
                    uiState.value = it
                },
                onItemEnabled = { it.enabled }
            ) { item ->
                Column(verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingTiny)) {
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
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    )
    if (state) {
        when (item.type) {
            StorageItemType.Internal -> {
                context.saveInternalBackupSaveChild(item.child)
            }

            StorageItemType.External -> {
                context.saveExternalBackupSaveChild(item.child)
            }

            else -> {}
        }
        context.saveBackupSavePath("${item.parent}/${item.child}")
    }
}

@ExperimentalLayoutApi
@ExperimentalMaterial3Api
@Composable
fun PageBackup() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    val navController = LocalSlotScope.current!!.navController
    LazyColumn(
        modifier = Modifier.padding(CommonTokens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)
    ) {
        item {
            OverLookCard()
        }

        item {
            Module(title = stringResource(R.string.utilities)) {
                val actions = listOf(
                    stringResource(R.string.directory),
                    stringResource(R.string.structure),
                    stringResource(R.string.log)
                )
                val icons = listOf(
                    ImageVector.vectorResource(R.drawable.ic_rounded_folder_open),
                    ImageVector.vectorResource(R.drawable.ic_rounded_account_tree),
                    ImageVector.vectorResource(R.drawable.ic_rounded_bug_report)
                )
                val onClicks = listOf<suspend () -> Unit>(
                    {
                        dialogSlot.openDirectoryDialog(context)
                    },
                    {
                        navController.navigate(MainRoutes.Tree.route)
                    },
                    {
                        navController.navigate(MainRoutes.Log.route)
                    }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(actions.size) { index ->
                        CardActionButton(
                            modifier = Modifier.weight(1f),
                            text = actions[index],
                            icon = icons[index],
                            onClick = {
                                scope.launch {
                                    onClicks[index]()
                                }
                            }
                        )
                        if (index != actions.size - 1) Spacer(modifier = Modifier.weight(0.25f))
                    }
                }
            }
        }

        item {
            Module(title = stringResource(R.string.activities)) {
                val items = listOf(
                    stringResource(R.string.app_and_data),
                    stringResource(R.string.media),
                    stringResource(R.string.telephony)
                )
                val icons = listOf(
                    ImageVector.vectorResource(R.drawable.ic_rounded_palette),
                    ImageVector.vectorResource(R.drawable.ic_rounded_image),
                    ImageVector.vectorResource(R.drawable.ic_rounded_call),
                )
                val onClicks = listOf(
                    {
                        context.startActivity(Intent(context, OperationActivity::class.java))
                    },
                    {},
                    {})
                VerticalGrid(
                    columns = 2,
                    count = items.size,
                    horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)
                ) { index ->
                    AssistChip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onClicks[index],
                        label = { Text(items[index]) },
                        leadingIcon = {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
        }
    }
}
