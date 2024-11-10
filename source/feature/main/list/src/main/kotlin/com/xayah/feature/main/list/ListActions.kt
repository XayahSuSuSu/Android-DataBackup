package com.xayah.feature.main.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowRight
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.model.OpType
import com.xayah.core.model.Target
import com.xayah.core.ui.component.AnimatedModalDropdownMenu
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.DropdownMenuItem
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.ModalDropdownMenu
import com.xayah.core.ui.component.confirm
import com.xayah.libpickyou.PickYouLauncher
import com.xayah.libpickyou.ui.model.PermissionType
import com.xayah.libpickyou.ui.model.PickerType

@Composable
internal fun ListActions(
    viewModel: ListActionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ListActions(uiState, viewModel)
}

@Composable
internal fun ListActions(
    uiState: ListActionsUiState,
    viewModel: ListActionsViewModel,
) {
    if (uiState is ListActionsUiState.Success) {
        val context = LocalContext.current
        val target by remember(uiState) {
            mutableStateOf(
                when (uiState) {
                    is ListActionsUiState.Success.Apps -> Target.Apps
                    is ListActionsUiState.Success.Files -> Target.Files
                }
            )
        }

        FilterAction(viewModel::showFilterSheet)

        ListAction(
            target = target,
            opType = uiState.opType,
            selected = uiState.selected,
            viewModel = viewModel,
        )

        var moreExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            IconButton(icon = Icons.Rounded.MoreVert) {
                moreExpanded = true
            }
            ModalDropdownMenu(
                expanded = moreExpanded,
                onDismissRequest = { moreExpanded = false }
            ) {
                RefreshItem(enabled = uiState.isUpdating.not()) {
                    moreExpanded = false
                    viewModel.refresh()
                }

                when (target) {
                    Target.Apps -> {}

                    Target.Files -> {
                        if (uiState.opType == OpType.BACKUP) {
                            AddItem(enabled = uiState.isUpdating.not()) {
                                moreExpanded = false
                                PickYouLauncher(
                                    checkPermission = true,
                                    title = context.getString(R.string.select_target_directory),
                                    pickerType = PickerType.DIRECTORY,
                                    permissionType = PermissionType.ROOT,
                                ).apply {
                                    launch(context) {
                                        viewModel.addFiles(listOf(it))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterAction(onFilter: () -> Unit) {
    IconButton(icon = Icons.Outlined.FilterList, onClick = onFilter)
}

@Composable
private fun ListAction(target: Target, opType: OpType, selected: Long, viewModel: ListActionsViewModel) {
    var checkListExpanded by remember { mutableStateOf(false) }
    var checkListSelectedExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(icon = Icons.Rounded.Checklist) {
            checkListSelectedExpanded = false
            checkListExpanded = true
        }
        AnimatedModalDropdownMenu(
            targetState = checkListSelectedExpanded,
            expanded = checkListExpanded,
            onDismissRequest = { checkListExpanded = false }
        ) {
            if (it.not()) {
                SelectAllItem {
                    checkListExpanded = false
                    viewModel.selectAll()
                }
                UnselectAllItem {
                    checkListExpanded = false
                    viewModel.unselectAll()
                }
                ReverseItem {
                    checkListExpanded = false
                    viewModel.reverseAll()
                }
                Divider(modifier = Modifier.fillMaxWidth())
                ForSelectedItem {
                    checkListSelectedExpanded = true
                }
            } else {
                    when (target) {
                        Target.Apps -> {
                            AppsListActions(
                                enabled = selected != 0L,
                                opType = opType,
                                checkListExpanded = { checkListExpanded = false },
                                onBlockSelected = viewModel::blockSelected,
                                onSelectDataItems = viewModel::showDataItemsSheet,
                                onDeleteSelected = viewModel::deleteSelected,
                            )
                        }

                        Target.Files -> {
                            FilesListActions(
                                enabled = selected != 0L,
                                opType = opType,
                                checkListExpanded = { checkListExpanded = false },
                                onBlockSelected = viewModel::blockSelected,
                                onDeleteSelected = viewModel::deleteSelected,
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun AppsListActions(
    enabled: Boolean,
    opType: OpType,
    checkListExpanded: () -> Unit,
    onBlockSelected: () -> Unit,
    onSelectDataItems: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    val context = LocalContext.current
    val dialogState = LocalSlotScope.current!!.dialogSlot

    when (opType) {
        OpType.BACKUP -> {
            BlockItem(enabled) {
                checkListExpanded()
                dialogState.confirm(
                    title = context.getString(R.string.prompt),
                    text = context.getString(R.string.confirm_add_to_blacklist)
                ) {
                    onBlockSelected()
                }
            }
            DetailedDataItem(enabled) {
                checkListExpanded()
                onSelectDataItems()
            }
        }

        OpType.RESTORE -> {
            DeleteItem(enabled) {
                checkListExpanded()
                dialogState.confirm(
                    title = context.getString(R.string.prompt),
                    text = context.getString(R.string.confirm_delete)
                ) {
                    onDeleteSelected()
                }
            }
            DetailedDataItem(enabled) {
                checkListExpanded()
                onSelectDataItems()
            }
        }
    }
}

@Composable
private fun FilesListActions(
    enabled: Boolean,
    opType: OpType,
    checkListExpanded: () -> Unit,
    onBlockSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    val context = LocalContext.current
    val dialogState = LocalSlotScope.current!!.dialogSlot

    when (opType) {
        OpType.BACKUP -> {
            BlockItem(enabled) {
                checkListExpanded()
                dialogState.confirm(
                    title = context.getString(R.string.prompt),
                    text = context.getString(R.string.confirm_add_to_blacklist)
                ) {
                    onBlockSelected()
                }
            }
            DeleteItem(enabled) {
                checkListExpanded()
                dialogState.confirm(
                    title = context.getString(R.string.prompt),
                    text = context.getString(R.string.confirm_delete)
                ) {
                    onDeleteSelected()
                }
            }
        }

        OpType.RESTORE -> {
            DeleteItem(enabled) {
                checkListExpanded()
                dialogState.confirm(
                    title = context.getString(R.string.prompt),
                    text = context.getString(R.string.confirm_delete)
                ) {
                    onDeleteSelected()
                }
            }
        }
    }
}

@Composable
private fun SelectAllItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = stringResource(id = R.string.select_all),
        leadingIcon = Icons.Rounded.CheckBox,
        onClick = onClick,
    )
}

@Composable
private fun UnselectAllItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = stringResource(id = R.string.unselect_all),
        leadingIcon = Icons.Rounded.CheckBoxOutlineBlank,
        onClick = onClick,
    )
}

@Composable
private fun ReverseItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = stringResource(id = R.string.reverse_selection),
        leadingIcon = Icons.Rounded.RestartAlt,
        onClick = onClick,
    )
}

@Composable
private fun ForSelectedItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = stringResource(id = R.string.for_selected),
        leadingIcon = Icons.Rounded.MoreVert,
        trailingIcon = Icons.Rounded.ArrowRight,
        onClick = onClick,
    )
}

@Composable
private fun BlockItem(enabled: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = stringResource(id = R.string.block),
        leadingIcon = Icons.Rounded.Block,
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
private fun DetailedDataItem(enabled: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = stringResource(id = R.string.detailed_data_items),
        leadingIcon = Icons.Rounded.Rule,
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
private fun DeleteItem(enabled: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = stringResource(id = R.string.delete),
        leadingIcon = Icons.Rounded.Delete,
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
private fun RefreshItem(enabled: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = stringResource(id = R.string.refresh),
        leadingIcon = Icons.Rounded.Refresh,
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
private fun AddItem(enabled: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = stringResource(id = R.string.add),
        leadingIcon = Icons.Rounded.Add,
        onClick = onClick,
        enabled = enabled,
    )
}
