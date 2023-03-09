package com.xayah.databackup.ui.activity.processing.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.ui.activity.list.common.components.FilterItem
import com.xayah.databackup.ui.activity.list.common.components.menu.ListBottomSheet
import com.xayah.databackup.ui.activity.list.common.components.menu.item.FilterItem
import com.xayah.databackup.ui.activity.list.common.components.menu.top.MenuTopActionButton
import com.xayah.databackup.ui.activity.processing.ProcessingViewModel
import com.xayah.databackup.ui.activity.processing.action.onBackupAppProcessing
import com.xayah.databackup.ui.activity.processing.action.onBackupMediaProcessing
import com.xayah.databackup.ui.activity.processing.action.onRestoreAppProcessing
import com.xayah.databackup.ui.activity.processing.action.onRestoreMediaProcessing
import com.xayah.databackup.util.GlobalObject

@ExperimentalMaterial3Api
@Composable
fun EndPageBottomSheet(
    isOpen: MutableState<Boolean>,
    viewModel: ProcessingViewModel
) {
    val context = LocalContext.current
    ListBottomSheet(
        isOpen = isOpen,
        actions = {
            item {
                MenuTopActionButton(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_round_refresh),
                    title = stringResource(R.string.retry)
                ) {
                    isOpen.value = false
                    viewModel.progress.value = 0
                    viewModel.allDone.targetState = false
                    viewModel.isFirst.value = true
                    viewModel.filter.value = ProcessingTaskFilter.Failed
                    viewModel.refreshTaskList()
                    val globalObject = GlobalObject.getInstance()
                    when (viewModel.listType) {
                        TypeBackupApp -> {
                            onBackupAppProcessing(
                                viewModel = viewModel,
                                context = context,
                                globalObject = globalObject,
                                retry = true
                            )
                        }
                        TypeBackupMedia -> {
                            onBackupMediaProcessing(
                                viewModel = viewModel,
                                context = context,
                                globalObject = globalObject,
                                retry = true
                            )
                        }
                        TypeRestoreApp -> {
                            onRestoreAppProcessing(
                                viewModel = viewModel,
                                context = context,
                                globalObject = globalObject,
                                retry = true
                            )
                        }
                        TypeRestoreMedia -> {
                            onRestoreMediaProcessing(
                                viewModel = viewModel,
                                context = context,
                                globalObject = globalObject,
                                retry = true
                            )
                        }
                    }
                }
            }
        },
        content = {
            // 过滤
            val filterList = listOf(
                FilterItem(
                    text = stringResource(id = R.string.all),
                    type = ProcessingTaskFilter.None
                ),
                FilterItem(
                    text = stringResource(R.string.succeed),
                    type = ProcessingTaskFilter.Succeed
                ),
                FilterItem(
                    text = stringResource(id = R.string.failed),
                    type = ProcessingTaskFilter.Failed
                )
            )
            FilterItem(
                title = stringResource(id = R.string.filter),
                list = filterList,
                filter = viewModel.filter,
                onClick = {
                    viewModel.refreshTaskList()
                }
            )
        }
    )
}