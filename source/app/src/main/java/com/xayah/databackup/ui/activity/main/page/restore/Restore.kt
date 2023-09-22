package com.xayah.databackup.ui.activity.main.page.restore

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
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
import com.xayah.databackup.ui.activity.main.page.backup.StorageItem
import com.xayah.databackup.ui.activity.main.page.backup.StorageItemType
import com.xayah.databackup.ui.activity.main.router.MainRoutes
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.CardActionButton
import com.xayah.databackup.ui.component.DialogState
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.Module
import com.xayah.databackup.ui.component.OverLookRestoreCard
import com.xayah.databackup.ui.component.RadioButtonGroup
import com.xayah.databackup.ui.component.VerticalGrid
import com.xayah.databackup.ui.component.openConfirmDialog
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.RadioTokens
import com.xayah.databackup.util.ConstantUtil
import com.xayah.databackup.util.IntentUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.command.toLineString
import com.xayah.databackup.util.databasePath
import com.xayah.databackup.util.iconPath
import com.xayah.databackup.util.readExternalRestoreSaveChild
import com.xayah.databackup.util.readInternalRestoreSaveChild
import com.xayah.databackup.util.readRestoreSavePath
import com.xayah.databackup.util.saveExternalRestoreSaveChild
import com.xayah.databackup.util.saveInternalRestoreSaveChild
import com.xayah.databackup.util.saveRestoreSavePath
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.ExceptionUtil.tryOn
import com.xayah.librootservice.util.ExceptionUtil.tryService
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

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
        title = context.getString(R.string.restore_dir),
        icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_folder_open),
        onLoading = {
            val remoteRootService = RemoteRootService(context)

            // Internal storage
            val internalParent = ConstantUtil.DefaultRestoreParent
            val internalChild = context.readInternalRestoreSaveChild()
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
            val externalChild = context.readExternalRestoreSaveChild()
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
            var defIndex = items.indexOfFirst { it.display == context.readRestoreSavePath() }
            if (defIndex == -1) {
                // The save path is not in storage items, reset it.
                context.saveRestoreSavePath(ConstantUtil.DefaultRestoreSavePath)
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
                context.saveInternalRestoreSaveChild(item.child)
            }

            StorageItemType.External -> {
                context.saveExternalRestoreSaveChild(item.child)
            }

            else -> {}
        }
        context.saveRestoreSavePath("${item.parent}/${item.child}")
    }
}

@ExperimentalMaterial3Api
private suspend fun DialogState.openReloadDialog(context: Context) {
    val textList = mutableListOf<String>()
    open(
        title = context.getString(R.string.prompt),
        icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_folder_open),
        onLoading = {
            // Copy the databases and icons from restore save path.
            var isSuccess = true
            PreparationUtil.copyRecursivelyAndPreserve(path = PathUtil.getDatabaseSavePath(), targetPath = PathUtil.getParentPath(context.databasePath()))
                .also { (succeed, out) ->
                    if (succeed.not()) {
                        isSuccess = false
                        textList.add("${context.getString(R.string.databases_reload_failed)}: ${out}.")
                    }
                }
            PreparationUtil.copyRecursivelyAndPreserve(path = PathUtil.getIconSavePath(), targetPath = PathUtil.getParentPath(context.iconPath()))
                .also { (succeed, out) ->
                    if (succeed.not()) {
                        isSuccess = false
                        textList.add("${context.getString(R.string.icon_reload_failed)}: ${out}.")
                    }
                }

            // Try to restart application.
            if (isSuccess) tryOn(block = {
                context.packageManager.getLaunchIntentForPackage(context.packageName).also { intent: Intent? ->
                    intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.applicationContext.startActivity(intent)
                    exitProcess(0)
                }
            }, onException = {
                textList.add(context.getString(R.string.restart_failed))
            })
        },
        block = { Text(text = textList.toLineString().trim()) }
    )
}

@ExperimentalLayoutApi
@ExperimentalMaterial3Api
@Composable
fun PageRestore() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    val navController = LocalSlotScope.current!!.navController
    LazyColumn(
        modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)
    ) {
        item {
            Spacer(modifier = Modifier.paddingTop(CommonTokens.PaddingMedium))
            OverLookRestoreCard()
        }

        item {
            Module(title = stringResource(R.string.utilities)) {
                val actions = listOf(
                    stringResource(R.string.reload),
                    stringResource(R.string.directory),
                    stringResource(R.string.structure),
                    stringResource(R.string.log)
                )
                val icons = listOf(
                    Icons.Rounded.Refresh,
                    ImageVector.vectorResource(R.drawable.ic_rounded_folder_open),
                    ImageVector.vectorResource(R.drawable.ic_rounded_account_tree),
                    ImageVector.vectorResource(R.drawable.ic_rounded_bug_report)
                )
                val onClicks = listOf<suspend () -> Unit>(
                    {
                        dialogSlot.openConfirmDialog(context, context.getString(R.string.confirm_reload)).also { (confirmed, _) ->
                            if (confirmed) dialogSlot.openReloadDialog(context)
                        }
                    },
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
                        IntentUtil.toOperationActivity(context = context, route = OperationRoutes.PackageRestore)
                    },
                    {},
                    {}
                )
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
            Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingMedium))
        }
    }
}
