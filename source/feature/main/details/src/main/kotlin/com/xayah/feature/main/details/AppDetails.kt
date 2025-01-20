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
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded._123
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
import com.xayah.core.common.util.toLineString
import com.xayah.core.model.OpType
import com.xayah.core.model.database.LabelAppCrossRefEntity
import com.xayah.core.model.database.LabelEntity
import com.xayah.core.model.database.PackageDataStates
import com.xayah.core.model.database.PackageDataStates.Companion.setSelected
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.PackagePermission
import com.xayah.core.ui.component.ActionSegmentedButton
import com.xayah.core.ui.component.AnimatedModalDropdownMenu
import com.xayah.core.ui.component.BodyLargeText
import com.xayah.core.ui.component.BottomButton
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.DataChips
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
internal fun AppDetails(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    uiState: DetailsUiState.Success.App,
    onSetDataStates: (Long, PackageDataStates) -> Unit,
    onAddLabel: (String) -> Unit,
    onDeleteLabel: (String) -> Unit,
    onSelectLabel: (Boolean, LabelAppCrossRefEntity?) -> Unit,
    onBlock: (Boolean) -> Unit,
    onFreeze: (Boolean) -> Unit,
    onLaunch: () -> Unit,
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
    val app = uiState.app
    val opType = app.indexInfo.opType

    LabelsBottomSheet(isShow, sheetState, onDismissRequest, app, uiState.refs, uiState.labels, onAddLabel, onDeleteLabel, onSelectLabel)

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(SizeTokens.Level12))

        PackageIconImage(packageName = app.packageName, size = SizeTokens.Level128)

        Spacer(Modifier.height(SizeTokens.Level12))

        HeadlineMediumText(text = app.packageInfo.label, color = ThemedColorSchemeKeyTokens.OnSurface.value)
        BodyLargeText(text = app.packageName, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value)
        LabelsFlow(opType = opType, app = app, refs = uiState.refs) { isShow = true }

        Spacer(Modifier.height(SizeTokens.Level12))

        ActionsRow(opType = opType, blocked = app.extraInfo.blocked, frozen = app.extraInfo.enabled.not(), protected = app.preserveId != 0L, onBlock = onBlock, onFreeze = onFreeze, onLaunch = onLaunch, onProtect = onProtect, onDelete = onDelete)

        Spacer(Modifier.height(SizeTokens.Level12))

        BackupParts(app = app, isCalculating = uiState.isRefreshing, onSetDataStates = onSetDataStates)

        Info(app = app)

        Permissions(permissions = app.extraInfo.permissions)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelsFlow(opType: OpType, app: PackageEntity, refs: List<LabelAppCrossRefEntity>, onAdd: () -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .paddingHorizontal(SizeTokens.Level24),
        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(-SizeTokens.Level8)
    ) {
        when (opType) {
            OpType.BACKUP -> {
                if (app.extraInfo.enabled.not()) {
                    FilterChip(
                        onClick = { },
                        selected = true,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemedColorSchemeKeyTokens.ErrorContainer.value, selectedLabelColor = ThemedColorSchemeKeyTokens.OnErrorContainer.value),
                        label = { Text(stringResource(R.string.disabled)) },
                    )
                }
                if (app.extraInfo.blocked) {
                    FilterChip(
                        onClick = { },
                        selected = true,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemedColorSchemeKeyTokens.ErrorContainer.value, selectedLabelColor = ThemedColorSchemeKeyTokens.OnErrorContainer.value),
                        label = { Text(stringResource(R.string.blacklist)) },
                    )
                }
            }

            OpType.RESTORE -> {
                if (app.preserveId != 0L) {
                    FilterChip(
                        onClick = { },
                        selected = true,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemedColorSchemeKeyTokens.YellowPrimaryContainer.value, selectedLabelColor = ThemedColorSchemeKeyTokens.YellowOnPrimaryContainer.value),
                        label = { Text(stringResource(R.string._protected)) },
                    )
                }
            }
        }

        if (app.extraInfo.hasKeystore) {
            FilterChip(
                onClick = { },
                selected = true,
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemedColorSchemeKeyTokens.BluePrimaryContainer.value, selectedLabelColor = ThemedColorSchemeKeyTokens.BlueOnPrimaryContainer.value),
                label = { Text(stringResource(R.string.keystore)) },
            )
        }
        if (app.extraInfo.ssaid.isNotEmpty()) {
            FilterChip(
                onClick = { },
                selected = true,
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemedColorSchemeKeyTokens.BluePrimaryContainer.value, selectedLabelColor = ThemedColorSchemeKeyTokens.BlueOnPrimaryContainer.value),
                label = { Text(stringResource(R.string.ssaid)) },
            )
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
    app: PackageEntity,
    refs: List<LabelAppCrossRefEntity>,
    labels: List<LabelEntity>,
    onAddLabel: (String) -> Unit,
    onDeleteLabel: (String) -> Unit,
    onSelectLabel: (Boolean, LabelAppCrossRefEntity?) -> Unit,
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
                                            onClick = { onSelectLabel(selected, LabelAppCrossRefEntity(item.label, app.packageName, app.userId, app.preserveId)) },
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
    frozen: Boolean,
    protected: Boolean,
    onBlock: (Boolean) -> Unit,
    onFreeze: (Boolean) -> Unit,
    onLaunch: () -> Unit,
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
                BackupActions(blocked, frozen, onBlock, onFreeze, onLaunch)
            }

            OpType.RESTORE -> {
                RestoreActions(protected, onProtect, onDelete)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoiceSegmentedButtonRowScope.BackupActions(blocked: Boolean, frozen: Boolean, onBlock: (Boolean) -> Unit, onFreeze: (Boolean) -> Unit, onLaunch: () -> Unit) {
    val context = LocalContext.current
    val dialogState = LocalSlotScope.current!!.dialogSlot
    ActionItem(
        index = 0,
        count = 3,
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
        count = 3,
        title = stringResource(if (frozen) R.string.unfreeze else R.string.freeze),
        icon = Icons.Rounded.AcUnit
    ) {
        dialogState.confirm(
            title = context.getString(R.string.prompt),
            text = context.getString(if (frozen) R.string.confirm_unfreeze else R.string.confirm_freeze),
            onConfirm = {
                onFreeze(frozen)
            }
        )
    }
    ActionItem(
        enabled = frozen.not(),
        index = 2,
        count = 3,
        title = context.getString(R.string.launch),
        icon = Icons.Rounded.RocketLaunch
    ) {
        onLaunch()
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

@Composable
private fun BackupParts(app: PackageEntity, isCalculating: Boolean, onSetDataStates: (Long, PackageDataStates) -> Unit) {
    Title(title = stringResource(id = R.string.backup_parts)) {
        DataChips(selections = app.dataStates, displayStats = app.displayStats, isCalculating = isCalculating) { type, selected ->
            onSetDataStates(app.id, type.setSelected(app.dataStates, selected.not()))
        }
        Spacer(Modifier.height(SizeTokens.Level12))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Info(app: PackageEntity) {
    Title(title = stringResource(id = R.string.info)) {
        Clickable(
            icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_person),
            title = stringResource(id = R.string.user),
            value = app.userId.toString()
        )
        Clickable(
            icon = Icons.Rounded._123,
            title = stringResource(id = R.string.uid),
            value = app.extraInfo.uid.toString()
        )
        Clickable(
            icon = Icons.Rounded.Apps,
            title = stringResource(id = R.string.version),
            value = app.packageInfo.versionName
        )
        if (app.packageInfo.firstInstallTime != 0L) {
            Clickable(
                icon = Icons.Rounded.Download,
                title = stringResource(id = R.string.first_install),
                value = DateUtil.formatTimestamp(app.packageInfo.firstInstallTime, DateUtil.PATTERN_YMD),
            )
        }
        if (app.packageInfo.lastUpdateTime != 0L) {
            Clickable(
                icon = Icons.Rounded.Update,
                title = stringResource(id = R.string.last_update),
                value = DateUtil.formatTimestamp(app.packageInfo.lastUpdateTime, DateUtil.PATTERN_YMD_HMS),
            )
        }
        if (app.extraInfo.lastBackupTime != 0L) {
            Clickable(
                icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_acute),
                title = stringResource(id = R.string.last_backup),
                value = DateUtil.formatTimestamp(app.extraInfo.lastBackupTime, DateUtil.PATTERN_YMD_HMS),
            )
        }
        if (app.extraInfo.ssaid.isNotEmpty()) {
            Clickable(
                icon = Icons.Rounded.RemoveRedEye,
                title = stringResource(id = R.string.ssaid),
                value = app.extraInfo.ssaid,
            )
        }
        if (app.preserveId != 0L) {
            Clickable(
                icon = Icons.Outlined.Shield,
                title = stringResource(id = R.string._protected),
                value = DateUtil.formatTimestamp(app.preserveId, DateUtil.PATTERN_FINISH),
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Permissions(permissions: List<PackagePermission>) {
    val granted by remember(permissions) { mutableStateOf(permissions.filter { it.isGranted || it.isOpsAllowed }.map { it.name }) }
    val denied by remember(permissions) { mutableStateOf(permissions.filter { it.isGranted.not() && it.isOpsAllowed.not() }.map { it.name }) }
    if (granted.isNotEmpty() || denied.isNotEmpty()) {
        Title(title = stringResource(id = R.string.permissions)) {
            if (granted.isNotEmpty()) {
                Clickable(
                    title = stringResource(R.string.granted),
                    value = granted.toLineString(),
                )
            }
            if (denied.isNotEmpty()) {
                Clickable(
                    title = stringResource(R.string.denied),
                    value = denied.toLineString(),
                )
            }
        }
    }
}