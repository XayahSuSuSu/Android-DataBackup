package com.xayah.databackup.ui.activity.main.page.restore

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.directory.router.DirectoryRoutes
import com.xayah.databackup.ui.activity.main.router.MainRoutes
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.CardActionButton
import com.xayah.databackup.ui.component.DialogState
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.Module
import com.xayah.databackup.ui.component.OverLookRestoreCard
import com.xayah.databackup.ui.component.VerticalGrid
import com.xayah.databackup.ui.component.ignorePaddingHorizontal
import com.xayah.databackup.ui.component.openConfirmDialog
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.IntentUtil
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
private suspend fun DialogState.openReloadDialog(viewModel: RestoreViewModel, context: Context) {
    openLoading(
        title = context.getString(R.string.prompt),
        icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_folder_open),
        onLoading = {
            viewModel.reload(context = context)
        },
    )
}

@ExperimentalLayoutApi
@ExperimentalMaterial3Api
@Composable
fun PageRestore() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val viewModel = hiltViewModel<RestoreViewModel>()
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
                            if (confirmed) {
                                dialogSlot.openReloadDialog(viewModel = viewModel, context = context)
                            }
                        }
                    },
                    {
                        IntentUtil.toDirectoryActivity(context = context, route = DirectoryRoutes.DirectoryRestore)
                    },
                    {
                        navController.navigate(MainRoutes.Tree.route)
                    },
                    {
                        navController.navigate(MainRoutes.Log.route)
                    }
                )
                Row(
                    modifier = Modifier
                        .ignorePaddingHorizontal(CommonTokens.PaddingMedium)
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    Spacer(modifier = Modifier.width(CommonTokens.PaddingMedium))
                    Row(horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)) {
                        repeat(actions.size) { index ->
                            CardActionButton(
                                text = actions[index],
                                icon = icons[index],
                                onClick = {
                                    scope.launch {
                                        onClicks[index]()
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(CommonTokens.PaddingMedium))
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
                    {
                        IntentUtil.toOperationActivity(context = context, route = OperationRoutes.MediaRestore)
                    },
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
