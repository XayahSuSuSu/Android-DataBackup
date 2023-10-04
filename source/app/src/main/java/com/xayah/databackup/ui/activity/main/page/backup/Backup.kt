package com.xayah.databackup.ui.activity.main.page.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.directory.router.DirectoryRoutes
import com.xayah.databackup.ui.activity.main.router.MainRoutes
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.CardActionButton
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.Module
import com.xayah.databackup.ui.component.OverLookBackupCard
import com.xayah.databackup.ui.component.VerticalGrid
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.IntentUtil
import kotlinx.coroutines.launch

@ExperimentalLayoutApi
@ExperimentalMaterial3Api
@Composable
fun PageBackup() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navController = LocalSlotScope.current!!.navController
    LazyColumn(
        modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)
    ) {
        item {
            Spacer(modifier = Modifier.paddingTop(CommonTokens.PaddingMedium))
            OverLookBackupCard()
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
                        IntentUtil.toDirectoryActivity(context = context, route = DirectoryRoutes.DirectoryBackup)
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
                        IntentUtil.toOperationActivity(context = context, route = OperationRoutes.PackageBackup)
                    },
                    {
                        IntentUtil.toOperationActivity(context = context, route = OperationRoutes.MediaBackup)
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
