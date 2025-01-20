package com.xayah.feature.main.details

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.core.model.OpType
import com.xayah.core.model.database.LabelEntity
import com.xayah.core.model.database.LabelFileCrossRefEntity
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.ui.component.ActionSegmentedButton
import com.xayah.core.ui.component.AnimatedModalDropdownMenu
import com.xayah.core.ui.component.BodyLargeText
import com.xayah.core.ui.component.BottomButton
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.DropdownMenuItem
import com.xayah.core.ui.component.HeadlineMediumText
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.ModalBottomSheet
import com.xayah.core.ui.component.PackageIconImage
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.edit
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.util.DateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun FileDetails(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    uiState: DetailsUiState.Success.File,
    onAddLabel: (String) -> Unit,
    onDeleteLabel: (String) -> Unit,
    onSelectLabel: (Boolean, LabelFileCrossRefEntity?) -> Unit,
    onBlock: (Boolean) -> Unit,
    onProtect: () -> Unit,
    onDelete: () -> Unit
) {
    var isShow by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val onDismissRequest: () -> Unit = {
        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                isShow = false
            }
        }
    }
    val file = uiState.file
    val opType = file.indexInfo.opType

    LabelsBottomSheet(isShow, sheetState, onDismissRequest, file, uiState.refs, uiState.labels, onAddLabel, onDeleteLabel, onSelectLabel)

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(SizeTokens.Level12))

        PackageIconImage(icon = Icons.Rounded.Folder, packageName = "", inCircleShape = true, size = SizeTokens.Level128)

        Spacer(Modifier.height(SizeTokens.Level12))

        HeadlineMediumText(text = file.name, color = ThemedColorSchemeKeyTokens.OnSurface.value)
        BodyLargeText(text = file.path, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value)
        LabelsFlow(opType = opType, file = file, refs = uiState.refs) { isShow = true }

        Spacer(Modifier.height(SizeTokens.Level12))

        ActionsRow(opType = opType, blocked = file.extraInfo.blocked, protected = file.preserveId != 0L, onBlock = onBlock, onProtect = onProtect, onDelete = onDelete)

        Spacer(Modifier.height(SizeTokens.Level12))

        Info(file = file)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelsFlow(opType: OpType, file: MediaEntity, refs: List<LabelFileCrossRefEntity>, onAdd: () -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .paddingHorizontal(SizeTokens.Level24),
        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(-SizeTokens.Level8)
    ) {
        when (opType) {
            OpType.BACKUP -> {
                if (file.extraInfo.blocked) {
                    FilterChip(
                        onClick = { },
                        selected = true,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemedColorSchemeKeyTokens.ErrorContainer.value, selectedLabelColor = ThemedColorSchemeKeyTokens.OnErrorContainer.value),
                        label = { Text(stringResource(R.string.blacklist)) },
                    )
                }
            }

            OpType.RESTORE -> {
                if (file.preserveId != 0L) {
                    FilterChip(
                        onClick = { },
                        selected = true,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemedColorSchemeKeyTokens.YellowPrimaryContainer.value, selectedLabelColor = ThemedColorSchemeKeyTokens.YellowOnPrimaryContainer.value),
                        label = { Text(stringResource(R.string._protected)) },
                    )
                }
            }
        }

        refs.forEach { item ->
            AssistChip(
                onClick = { },
                label = { Text(item.label) },
            )
        }
        IconButton(onClick = onAdd) {
            Icon(Icons.Rounded.Add, contentDescription = null)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
private fun LabelsBottomSheet(
    isShow: Boolean,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    file: MediaEntity,
    refs: List<LabelFileCrossRefEntity>,
    labels: List<LabelEntity>,
    onAddLabel: (String) -> Unit,
    onDeleteLabel: (String) -> Unit,
    onSelectLabel: (Boolean, LabelFileCrossRefEntity?) -> Unit,
) {
    val context = LocalContext.current
    val dialogState = LocalSlotScope.current!!.dialogSlot
    if (isShow) {
        ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
            val selectedLabels by remember(refs) { mutableStateOf(refs.map { it.label }) }

            Title(text = stringResource(id = R.string.labels))

            if (labels.isEmpty()) {
                BodyLargeText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    text = stringResource(R.string.no_labels_here),
                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                )
            } else {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                    verticalArrangement = Arrangement.spacedBy(-SizeTokens.Level8)
                ) {
                    labels.forEach { item ->
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                            val interactionSource = remember { MutableInteractionSource() }
                            val selected by remember(item.label, selectedLabels) { mutableStateOf(item.label in selectedLabels) }
                            Box {
                                FilterChip(
                                    onClick = {},
                                    label = { Text(item.label) },
                                    selected = selected,
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Done,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    interactionSource = interactionSource,
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .combinedClickable(
                                            onLongClick = { expanded = true },
                                            onClick = { onSelectLabel(selected, LabelFileCrossRefEntity(item.label, file.path, file.preserveId)) },
                                            interactionSource = interactionSource,
                                            indication = null,
                                        )
                                )
                            }

                            AnimatedModalDropdownMenu(
                                targetState = null,
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = stringResource(id = R.string.delete),
                                    leadingIcon = Icons.Rounded.DeleteForever,
                                    onClick = { onDeleteLabel(item.label) },
                                )
                            }
                        }
                    }
                }
            }

            BottomButton(text = stringResource(id = R.string.add_label)) {
                dialogState.edit(context.getString(R.string.add_label), label = context.getString(R.string.label), onConfirm = onAddLabel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoiceSegmentedButtonRowScope.ActionItem(
    enabled: Boolean = true,
    index: Int,
    count: Int,
    title: String,
    icon: ImageVector,
    containerColor: Color = ThemedColorSchemeKeyTokens.SurfaceContainer.value,
    onClick: () -> Unit,
) {
    ActionSegmentedButton(
        enabled = enabled,
        onClick = onClick,
        containerColor = containerColor,
        index = index,
        count = count
    ) {
        CompositionLocalProvider(LocalContentColor provides LocalContentColor.current.withState(enabled)) {
            Column(modifier = Modifier.paddingVertical(SizeTokens.Level8), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = icon, contentDescription = null)
                Text(text = title)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionsRow(
    opType: OpType,
    blocked: Boolean,
    protected: Boolean,
    onBlock: (Boolean) -> Unit,
    onProtect: () -> Unit,
    onDelete: () -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .paddingHorizontal(SizeTokens.Level24)
            .height(IntrinsicSize.Min),
        space = SizeTokens.Level0
    ) {
        when (opType) {
            OpType.BACKUP -> {
                BackupActions(blocked, onBlock, onDelete)
            }

            OpType.RESTORE -> {
                RestoreActions(protected, onProtect, onDelete)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoiceSegmentedButtonRowScope.BackupActions(blocked: Boolean, onBlock: (Boolean) -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val dialogState = LocalSlotScope.current!!.dialogSlot
    ActionItem(
        index = 0,
        count = 2,
        title = stringResource(if (blocked) R.string.unblock else R.string.block),
        icon = Icons.Rounded.Block
    ) {
        dialogState.confirm(
            title = context.getString(R.string.prompt),
            text = context.getString(if (blocked) R.string.confirm_remove_from_blacklist else R.string.confirm_add_to_blacklist),
            onConfirm = {
                onBlock(blocked)
            }
        )
    }
    ActionItem(
        index = 1,
        count = 2,
        title = stringResource(R.string.delete),
        icon = Icons.Outlined.DeleteForever,
        containerColor = ThemedColorSchemeKeyTokens.ErrorContainer.value
    ) {
        dialogState.confirm(
            title = context.getString(R.string.delete),
            text = context.getString(R.string.delete_desc),
            onConfirm = {
                onDelete()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoiceSegmentedButtonRowScope.RestoreActions(protected: Boolean, onProtect: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val dialogState = LocalSlotScope.current!!.dialogSlot
    ActionItem(
        enabled = protected.not(),
        index = 0,
        count = 2,
        title = stringResource(R.string._protected),
        icon = Icons.Outlined.Shield
    ) {
        dialogState.confirm(
            title = context.getString(R.string.protect),
            text = context.getString(R.string.protect_desc),
            onConfirm = {
                onProtect()
            }
        )
    }
    ActionItem(
        index = 1,
        count = 2,
        title = stringResource(R.string.delete),
        icon = Icons.Outlined.DeleteForever,
        containerColor = ThemedColorSchemeKeyTokens.ErrorContainer.value
    ) {
        dialogState.confirm(
            title = context.getString(R.string.delete),
            text = context.getString(R.string.delete_desc),
            onConfirm = {
                onDelete()
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Info(file: MediaEntity) {
    if (file.preserveId != 0L || file.extraInfo.lastBackupTime != 0L) {
        Title(title = stringResource(id = R.string.info)) {
            if (file.extraInfo.lastBackupTime != 0L) {
                Clickable(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_acute),
                    title = stringResource(id = R.string.last_backup),
                    value = DateUtil.formatTimestamp(file.extraInfo.lastBackupTime, DateUtil.PATTERN_YMD_HMS),
                )
            }

            if (file.preserveId != 0L) {
                Clickable(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(id = R.string._protected),
                    value = DateUtil.formatTimestamp(file.preserveId, DateUtil.PATTERN_FINISH),
                )
            }
        }
    }
}
