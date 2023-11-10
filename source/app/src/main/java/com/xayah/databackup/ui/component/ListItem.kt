package com.xayah.databackup.ui.component

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toDrawable
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xayah.core.database.model.DirectoryEntity
import com.xayah.core.database.model.MediaBackupWithOpEntity
import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.database.model.MediaRestoreWithOpEntity
import com.xayah.core.database.model.OperationMask
import com.xayah.core.model.OperationState
import com.xayah.core.database.model.PackageBackupEntire
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.model.StorageType
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataType
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.directory.page.DirectoryViewModel
import com.xayah.databackup.ui.activity.operation.page.media.backup.MediaBackupListViewModel
import com.xayah.databackup.ui.activity.operation.page.media.backup.OpType
import com.xayah.databackup.ui.activity.operation.page.media.restore.MediaRestoreListViewModel
import com.xayah.databackup.ui.component.material3.Card
import com.xayah.databackup.ui.component.material3.outlinedCardBorder
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.ListItemTokens
import com.xayah.databackup.util.ConstantUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.activity.PickerType
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.ExceptionUtil
import com.xayah.librootservice.util.ExceptionUtil.tryOn
import com.xayah.librootservice.util.withIOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import com.xayah.databackup.ui.activity.operation.page.packages.backup.ListViewModel as BackupListViewModel
import com.xayah.databackup.ui.activity.operation.page.packages.restore.ListViewModel as RestoreListViewModel

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ListItemPackage(
    modifier: Modifier = Modifier,
    packageName: String,
    label: String,
    icon: Any,
    apkSelected: Boolean,
    dataSelected: Boolean,
    onApkSelected: () -> Unit,
    onDataSelected: () -> Unit,
    selected: Boolean,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = onCardClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCardLongClick()
        },
        border = if (selected) outlinedCardBorder(lineColor = ColorScheme.primary()) else null,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingMedium)
        ) {
            Row(
                modifier = Modifier
                    .paddingHorizontal(ListItemTokens.PaddingMedium)
                    .paddingTop(ListItemTokens.PaddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingSmall)
            ) {
                AsyncImage(
                    modifier = Modifier.size(ListItemTokens.IconSize),
                    model = ImageRequest.Builder(context)
                        .data(icon)
                        .crossfade(true)
                        .build(),
                    contentDescription = null
                )
                Column(modifier = Modifier.weight(1f)) {
                    TitleMediumBoldText(text = label)
                    LabelSmallText(text = packageName)
                }
                if (selected) Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Top),
                    tint = ColorScheme.primary(),
                )
            }
            Row(
                modifier = Modifier.paddingHorizontal(ListItemTokens.PaddingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingSmall),
                    content = {
                        chipGroup()
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = ColorScheme.inverseOnSurface())
                    .paddingHorizontal(ListItemTokens.PaddingMedium),
                horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingMedium, Alignment.End)
            ) {
                ApkChip(selected = apkSelected, onClick = onApkSelected)
                DataChip(selected = dataSelected, onClick = onDataSelected)
            }
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ListItemPackageBackup(
    modifier: Modifier = Modifier,
    packageInfo: PackageBackupEntire,
    selectionMode: Boolean,
    onSelectedChange: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<BackupListViewModel>()
    val scope = rememberCoroutineScope()
    var icon by remember { mutableStateOf<Any>(0) }
    val selected by packageInfo.selected

    ListItemPackage(
        modifier = modifier,
        packageName = packageInfo.packageName,
        label = packageInfo.label,
        icon = icon,
        apkSelected = OperationMask.isApkSelected(packageInfo.operationCode),
        dataSelected = OperationMask.isDataSelected(packageInfo.operationCode),
        onApkSelected = {
            scope.launch {
                withIOContext {
                    packageInfo.operationCode = packageInfo.operationCode xor OperationMask.Apk
                    viewModel.updatePackage(packageInfo)
                }
            }
        },
        onDataSelected = {
            scope.launch {
                withIOContext {
                    packageInfo.operationCode = packageInfo.operationCode xor OperationMask.Data
                    viewModel.updatePackage(packageInfo)
                }
            }
        },
        selected = selected,
        onCardClick = {
            if (selectionMode.not()) {
                scope.launch {
                    withIOContext {
                        packageInfo.operationCode =
                            if (packageInfo.operationCode == OperationMask.Both) OperationMask.None else OperationMask.Both
                        viewModel.updatePackage(packageInfo)
                    }
                }
            } else {
                onSelectedChange()
            }
        },
        onCardLongClick = onSelectedChange
    ) {
        if (packageInfo.versionName.isNotEmpty()) Serial(serial = packageInfo.versionName)
        Serial(serial = packageInfo.sizeDisplay)
    }

    LaunchedEffect(null) {
        // Read icon from cached internal dir.
        withIOContext {
            tryOn {
                val bytes = File(PathUtil.getIconPath(context, packageInfo.packageName)).readBytes()
                icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toDrawable(context.resources)
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ListItemPackageRestore(
    modifier: Modifier = Modifier,
    packageInfo: PackageRestoreEntire,
    selectionMode: Boolean,
    onSelectedChange: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<RestoreListViewModel>()
    val scope = rememberCoroutineScope()
    var icon by remember { mutableStateOf<Any>(0) }
    val selected by packageInfo.selected

    ListItemPackage(
        modifier = modifier,
        packageName = packageInfo.packageName,
        label = packageInfo.label,
        icon = icon,
        apkSelected = OperationMask.isApkSelected(packageInfo.operationCode),
        dataSelected = OperationMask.isDataSelected(packageInfo.operationCode),
        onApkSelected = {
            scope.launch {
                withIOContext {
                    packageInfo.operationCode = packageInfo.operationCode xor OperationMask.Apk
                    viewModel.updatePackage(packageInfo)
                }
            }
        },
        onDataSelected = {
            scope.launch {
                withIOContext {
                    packageInfo.operationCode = packageInfo.operationCode xor OperationMask.Data
                    viewModel.updatePackage(packageInfo)
                }
            }
        },
        selected = selected,
        onCardClick = {
            if (selectionMode.not()) {
                scope.launch {
                    withIOContext {
                        packageInfo.operationCode =
                            if (packageInfo.operationCode == OperationMask.Both) OperationMask.None else OperationMask.Both
                        viewModel.updatePackage(packageInfo)
                    }
                }
            } else {
                onSelectedChange()
            }
        },
        onCardLongClick = onSelectedChange
    ) {
        if (packageInfo.versionName.isNotEmpty()) Serial(serial = packageInfo.versionName)
        LaunchedEffect(null) {
            viewModel.updatePackage(context, packageInfo)
        }
        AnimatedSerial(serial = packageInfo.sizeDisplay)
        AnimatedSerial(serial = if (packageInfo.installed) stringResource(id = R.string.installed) else stringResource(id = R.string.not_installed))
    }

    LaunchedEffect(null) {
        // Read icon from cached internal dir.
        withIOContext {
            tryOn {
                val bytes = File(PathUtil.getIconPath(context, packageInfo.packageName)).readBytes()
                icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toDrawable(context.resources)
            }
        }
    }
}

@Composable
fun ListItemManifestHorizontal(icon: ImageVector, title: String, content: String, onButtonClick: () -> Unit) {
    Row(modifier = Modifier.paddingVertical(ListItemTokens.ManifestItemPadding), verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(
            modifier = Modifier.size(ListItemTokens.ManifestIconButtonSize),
            onClick = onButtonClick,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = ColorScheme.secondary()),
        ) {
            Icon(
                modifier = Modifier.size(ListItemTokens.ManifestIconSize),
                imageVector = icon,
                tint = ColorScheme.onSecondary(),
                contentDescription = null
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .paddingHorizontal(ListItemTokens.PaddingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TitleMediumBoldText(text = title)
            HeadlineLargeBoldText(text = content)
        }
    }
}

@Composable
fun ListItemManifestVertical(icon: ImageVector, title: String, content: String, onButtonClick: () -> Unit) {
    Row(modifier = Modifier.paddingVertical(ListItemTokens.ManifestItemPadding), verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(
            modifier = Modifier.size(ListItemTokens.ManifestIconButtonSize),
            onClick = onButtonClick,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = ColorScheme.secondary()),
        ) {
            Icon(
                modifier = Modifier.size(ListItemTokens.ManifestIconSize),
                imageVector = icon,
                tint = ColorScheme.onSecondary(),
                contentDescription = null
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .paddingHorizontal(ListItemTokens.PaddingMedium),
        ) {
            TitleMediumBoldText(text = title)
            LabelSmallText(text = content)
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ListItemDirectory(
    modifier: Modifier = Modifier,
    entity: DirectoryEntity,
    onCardClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<DirectoryViewModel>()
    val scope = rememberCoroutineScope()
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    val haptic = LocalHapticFeedback.current
    val selected = entity.selected
    val enabled = entity.enabled
    val progress = 1 - entity.availableBytes.toFloat() / entity.totalBytes
    val path = entity.path
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        enabled = enabled,
        onClick = onCardClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            expanded = true
        },
        border = if (selected) outlinedCardBorder(lineColor = ColorScheme.primary()) else null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ListItemTokens.PaddingMedium)
        ) {
            Column {
                Row {
                    HeadlineMediumBoldText(text = entity.title)
                    Spacer(modifier = Modifier.weight(1f))
                    if (selected) Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Top),
                        tint = ColorScheme.primary(),
                    )
                }
                BodySmallBoldText(text = path)
                if (entity.error.isNotEmpty()) BodySmallBoldText(text = entity.error, color = ColorScheme.error(), enabled = enabled)
                Divider(modifier = Modifier.paddingVertical(ListItemTokens.PaddingSmall))
                Row(
                    modifier = Modifier.paddingBottom(ListItemTokens.PaddingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingMedium)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .clip(CircleShape)
                            .weight(1f),
                        color = if (enabled) ColorScheme.primary() else ColorScheme.primary().copy(alpha = CommonTokens.DisabledAlpha),
                        trackColor = if (enabled) ColorScheme.inverseOnSurface() else ColorScheme.inverseOnSurface().copy(alpha = CommonTokens.DisabledAlpha),
                        progress = if (progress.isNaN()) 0f else progress
                    )
                    Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                        val actions = remember(entity) {
                            listOf(
                                ActionMenuItem(
                                    title = context.getString(R.string.delete),
                                    icon = Icons.Rounded.Delete,
                                    enabled = entity.storageType == StorageType.CUSTOM,
                                    onClick = {
                                        scope.launch {
                                            withIOContext {
                                                expanded = false
                                                dialogSlot.openConfirmDialog(context, context.getString(R.string.confirm_delete)).also { (confirmed, _) ->
                                                    if (confirmed) {
                                                        if (entity.selected) viewModel.reset(context = context)
                                                        viewModel.delete(entity)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            )
                        }

                        LabelSmallText(text = "${entity.usedBytesDisplay} / ${entity.totalBytesDisplay}")

                        ModalActionDropdownMenu(expanded = expanded, actionList = actions, onDismissRequest = { expanded = false })
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingSmall),
                    content = {
                        chipGroup()
                    }
                )
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ListItemMedia(
    modifier: Modifier = Modifier,
    name: String,
    path: String,
    archivePath: String? = null,
    state: Boolean,
    mediaOpLog: String,
    isProcessing: Boolean,
    selectedInList: Boolean,
    mediaOpProcessing: Boolean,
    mediaOpDone: Boolean,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    menuPlaceholder: @Composable (BoxScope.() -> Unit),
    chipGroup: @Composable (RowScope.() -> Unit),
) {
    val haptic = LocalHapticFeedback.current
    val isPathMissing = path.isEmpty()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = {
            if (isProcessing.not()) {
                onCardClick()
            }
        },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (isProcessing.not()) {
                onCardLongClick()
            }
        },
        border = if (selectedInList) outlinedCardBorder(lineColor = ColorScheme.primary()) else null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingMedium)) {
            Row(
                modifier = Modifier
                    .paddingTop(ListItemTokens.PaddingMedium)
                    .paddingHorizontal(ListItemTokens.PaddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingSmall)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    TitleMediumBoldText(text = name)
                    if (isPathMissing) {
                        archivePath?.apply {
                            LabelSmallText(text = this)
                        }
                        LabelSmallText(text = stringResource(R.string.media_target_path_missing), color = ColorScheme.error())
                    } else {
                        LabelSmallText(text = path)
                    }
                }
                if (selectedInList) Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Top),
                    tint = ColorScheme.primary(),
                )
                if (mediaOpProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(ListItemTokens.OpIndicatorSize),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingHorizontal(ListItemTokens.PaddingMedium),
            ) {
                menuPlaceholder()
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingSmall),
                    content = {
                        chipGroup()
                        if (mediaOpDone) {
                            AnimatedSerial(
                                serial = if (state) stringResource(id = R.string.succeed) else stringResource(id = R.string.failed)
                            )
                        }
                    }
                )
            }

            // TODO: java.lang.IllegalArgumentException: Can't represent a size of xxx in Constraints
            if (mediaOpProcessing || mediaOpDone) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = ColorScheme.inverseOnSurface())
                        .paddingHorizontal(ListItemTokens.PaddingMedium),
                ) {
                    Box(
                        modifier
                            .weight(1f)
                            .paddingVertical(ListItemTokens.PaddingSmall),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        LabelSmallText(text = mediaOpLog)
                    }
                }
            } else {
                Spacer(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ListItemMediaBackup(
    modifier: Modifier = Modifier,
    entity: MediaBackupWithOpEntity,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<MediaBackupListViewModel>()
    val scope = rememberCoroutineScope()
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    val uiState by viewModel.uiState
    val media = entity.media
    val opList = entity.opList
    val isProcessing = remember(uiState) { uiState.opType == OpType.PROCESSING }
    var selectedInList by remember(entity, isProcessing) { mutableStateOf(media.selected && isProcessing.not()) }
    val mediaOpIndex by remember(entity, uiState) { mutableIntStateOf(entity.opList.indexOfLast { it.timestamp == uiState.timestamp }) }
    val mediaOpProcessing by remember(entity, mediaOpIndex) { mutableStateOf(mediaOpIndex != -1 && opList[mediaOpIndex].opState == OperationState.PROCESSING) }
    val mediaOpDone by remember(
        entity,
        mediaOpIndex
    ) { mutableStateOf(mediaOpIndex != -1 && (opList[mediaOpIndex].opState != OperationState.IDLE && opList[mediaOpIndex].opState != OperationState.PROCESSING)) }
    val mediaOpLog by remember(
        entity,
        mediaOpProcessing
    ) { mutableStateOf(if (mediaOpProcessing || mediaOpDone) opList[mediaOpIndex].opLog else context.getString(R.string.idle)) }
    var expanded by remember { mutableStateOf(false) }

    ListItemMedia(
        modifier = modifier,
        name = media.name,
        path = media.path,
        state = if (mediaOpDone) opList[mediaOpIndex].state else false,
        mediaOpLog = mediaOpLog,
        isProcessing = isProcessing,
        selectedInList = selectedInList,
        mediaOpProcessing = mediaOpProcessing,
        mediaOpDone = mediaOpDone,
        onCardClick = {
            scope.launch {
                withIOContext {
                    media.selected = media.selected.not()
                    viewModel.upsertBackup(media)
                    selectedInList = media.selected
                }
            }
        },
        onCardLongClick = {
            expanded = true
        },
        menuPlaceholder = {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .wrapContentSize(Alignment.Center)
            ) {
                val actions = remember(entity) {
                    listOf(
                        ActionMenuItem(
                            title = context.getString(R.string.delete),
                            icon = Icons.Rounded.Delete,
                            enabled = ConstantUtil.DefaultMediaList.indexOfFirst { it.second == entity.media.path } == -1,
                            onClick = {
                                scope.launch {
                                    withIOContext {
                                        expanded = false
                                        dialogSlot.openConfirmDialog(context, context.getString(R.string.confirm_delete)).also { (confirmed, _) ->
                                            if (confirmed) {
                                                viewModel.deleteBackup(entity.media)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.align(Alignment.BottomEnd))

                ModalActionDropdownMenu(expanded = expanded, actionList = actions, onDismissRequest = { expanded = false })
            }
        },
        chipGroup = {
            LaunchedEffect(null) {
                viewModel.updateMediaSizeBytes(context, entity.media)
            }
            AnimatedSerial(serial = entity.media.sizeDisplay)
        }
    )
}

@ExperimentalMaterial3Api
private suspend fun DialogState.openMediaRestoreDeleteDialog(
    context: Context,
    scope: CoroutineScope,
    viewModel: MediaRestoreListViewModel,
    entity: MediaRestoreEntity,
) {
    openLoading(
        title = context.getString(R.string.prompt),
        icon = Icons.Rounded.Delete,
        onLoading = {
            viewModel.deleteRestore(entity)
            val remoteRootService = RemoteRootService(context)
            ExceptionUtil.tryService(onFailed = { msg ->
                scope.launch {
                    withIOContext {
                        Toast.makeText(
                            context,
                            "${context.getString(R.string.fetch_failed)}: $msg\n${context.getString(R.string.remote_service_err_info)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }) {
                val path =
                    "${PathUtil.getRestoreMediumSavePath()}/${entity.name}/${entity.timestamp}/${DataType.MEDIA_MEDIA.type}.${CompressionType.TAR.suffix}"
                remoteRootService.deleteRecursively(path)
                remoteRootService.clearEmptyDirectoriesRecursively(PathUtil.getRestoreMediumSavePath())
            }
            remoteRootService.destroyService()
            viewModel.initialize()
        },
    )
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ListItemMediaRestore(
    modifier: Modifier = Modifier,
    entity: MediaRestoreWithOpEntity,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<MediaRestoreListViewModel>()
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState
    val media = entity.media
    val opList = entity.opList
    val isPathMissing = entity.media.path.isEmpty()
    val isProcessing = remember(uiState) { uiState.opType == OpType.PROCESSING }
    var selectedInList by remember(entity, isProcessing) { mutableStateOf(media.selected && isProcessing.not()) }
    val mediaOpIndex by remember(entity, uiState) { mutableIntStateOf(entity.opList.indexOfLast { it.timestamp == uiState.timestamp }) }
    val mediaOpProcessing by remember(entity, mediaOpIndex) { mutableStateOf(mediaOpIndex != -1 && opList[mediaOpIndex].opState == OperationState.PROCESSING) }
    val mediaOpDone by remember(
        entity,
        mediaOpIndex
    ) { mutableStateOf(mediaOpIndex != -1 && (opList[mediaOpIndex].opState != OperationState.IDLE && opList[mediaOpIndex].opState != OperationState.PROCESSING)) }
    val mediaOpLog by remember(
        entity,
        mediaOpProcessing
    ) { mutableStateOf(if (mediaOpProcessing || mediaOpDone) opList[mediaOpIndex].opLog else context.getString(R.string.idle)) }
    var expanded by remember { mutableStateOf(false) }
    val archivePath = remember {
        "${PathUtil.getRestoreMediumSavePath()}/${media.name}/${media.timestamp}/${DataType.MEDIA_MEDIA.type}.${CompressionType.TAR.suffix}"
    }

    ListItemMedia(
        modifier = modifier,
        name = media.name,
        path = media.path,
        archivePath = archivePath,
        state = if (mediaOpDone) opList[mediaOpIndex].state else false,
        mediaOpLog = mediaOpLog,
        isProcessing = isProcessing,
        selectedInList = selectedInList,
        mediaOpProcessing = mediaOpProcessing,
        mediaOpDone = mediaOpDone,
        onCardClick = {
            if (isPathMissing) {
                Toast.makeText(context, context.getString(R.string.media_target_path_missing), Toast.LENGTH_SHORT).show()
            } else {
                scope.launch {
                    withIOContext {
                        media.selected = media.selected.not()
                        viewModel.upsertRestore(media)
                        selectedInList = media.selected
                    }
                }
            }
        },
        onCardLongClick = {
            expanded = true
        },
        menuPlaceholder = {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .wrapContentSize(Alignment.Center)
            ) {
                val actions = remember(entity) {
                    listOf(
                        ActionMenuItem(
                            title = context.getString(R.string.media_set_path),
                            icon = Icons.Rounded.Settings,
                            enabled = true,
                            onClick = {
                                PickYouLauncher().apply {
                                    setTitle(context.getString(R.string.select_target_directory))
                                    setType(PickerType.DIRECTORY)
                                    setLimitation(1)
                                    launch((context as ComponentActivity)) { pathList ->
                                        pathList.forEach { pathString ->
                                            viewModel.setPath(pathString = pathString, media = media)
                                        }
                                    }
                                }
                            }
                        ),
                        ActionMenuItem(
                            title = context.getString(R.string.delete),
                            icon = Icons.Rounded.Delete,
                            enabled = true,
                            onClick = {
                                scope.launch {
                                    withIOContext {
                                        expanded = false
                                        dialogSlot.openConfirmDialog(context, context.getString(R.string.confirm_delete_selected_restoring_items))
                                            .also { (confirmed, _) ->
                                                if (confirmed) {
                                                    dialogSlot.openMediaRestoreDeleteDialog(
                                                        context = context,
                                                        scope = scope,
                                                        viewModel = viewModel,
                                                        entity = entity.media
                                                    )
                                                }
                                            }
                                    }
                                }
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.align(Alignment.BottomEnd))

                ModalActionDropdownMenu(expanded = expanded, actionList = actions, onDismissRequest = { expanded = false })
            }
        },
        chipGroup = {
            LaunchedEffect(null) {
                viewModel.updateMediaSizeBytes(context, entity.media)
            }
            AnimatedSerial(serial = entity.media.sizeDisplay)
        }
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ListItemReload(modifier: Modifier = Modifier, title: String, subtitle: String, chipGroup: @Composable (RowScope.() -> Unit)) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = {},
        onLongClick = {},
        border = null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingMedium)) {
            Row(
                modifier = Modifier
                    .paddingTop(ListItemTokens.PaddingMedium)
                    .paddingHorizontal(ListItemTokens.PaddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingSmall)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    TitleMediumBoldText(text = title)
                    LabelSmallText(text = subtitle)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingHorizontal(ListItemTokens.PaddingMedium),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingSmall),
                    content = {
                        chipGroup()
                    }
                )
            }

            Spacer(modifier = Modifier.fillMaxWidth())
        }
    }
}
