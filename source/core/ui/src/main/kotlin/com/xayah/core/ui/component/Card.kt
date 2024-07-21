package com.xayah.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.readRestoreUser
import com.xayah.core.model.OperationState
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.CardColors
import com.xayah.core.ui.material3.CardDefaults
import com.xayah.core.ui.material3.CardElevation
import com.xayah.core.ui.material3.ShapeDefaults
import com.xayah.core.ui.material3.tokens.OutlinedCardTokens
import com.xayah.core.ui.model.ProcessingCardItem
import com.xayah.core.ui.model.ReportAppItemInfo
import com.xayah.core.ui.model.ReportFileItemInfo
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.StateView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun outlinedCardBorder(enabled: Boolean = true, borderColor: Color? = null): BorderStroke {
    val tint = borderColor ?: OutlinedCardTokens.OutlineColor.value
    val color = if (enabled) {
        tint
    } else {
        tint.copy(alpha = OutlinedCardTokens.DisabledOutlineOpacity)
            .compositeOver(
                MaterialTheme.colorScheme.surfaceColorAtElevation(
                    OutlinedCardTokens.DisabledContainerElevation
                )
            )
    }
    return remember(color) { BorderStroke(OutlinedCardTokens.OutlineWidth, color) }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    performHapticFeedback: Boolean = false,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = rememberRipple(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = onClick,
        onLongClick = {
            if (performHapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick?.invoke()
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = colors.containerColor(enabled).value,
        contentColor = colors.contentColor(enabled).value,
        tonalElevation = elevation.tonalElevation(enabled, interactionSource).value,
        shadowElevation = elevation.shadowElevation(enabled, interactionSource).value,
        border = border,
        interactionSource = interactionSource,
        indication = indication,
    ) {
        Column(content = content)
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun ProcessingCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    progress: Float,
    title: String,
    packageName: String? = null,
    defExpanded: Boolean = false,
    expandable: Boolean = false,
    maxDisplayNum: Int = -1,
    items: List<ProcessingCardItem>,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val context = LocalContext.current
    var _expanded by remember { mutableStateOf(defExpanded || expandable.not()) }
    val successCount by remember(items) { mutableIntStateOf(items.count { it.state == OperationState.DONE || it.state == OperationState.SKIP }) }
    val failedCount by remember(items) { mutableIntStateOf(items.count { it.state == OperationState.ERROR }) }
    val totalCount by remember(items.size) { mutableIntStateOf(items.size) }
    val _state by remember(successCount, failedCount, totalCount) {
        mutableStateOf(
            if (failedCount != 0) OperationState.ERROR
            else if (successCount + failedCount == 0) OperationState.IDLE
            else if (successCount == totalCount) OperationState.DONE
            else OperationState.PROCESSING
        )
    }
    val displayItems by remember(maxDisplayNum, items) {
        mutableStateOf(
            if (maxDisplayNum == -1 || items.size <= maxDisplayNum) {
                items
            } else {
                val processingIndex = items.indexOfFirst { it.state == OperationState.PROCESSING }
                val halfCount = (maxDisplayNum - 1) / 2
                val startIndex: Int
                val endIndex: Int
                if (processingIndex - halfCount < 0) {
                    startIndex = 0
                    endIndex = maxDisplayNum
                } else if (processingIndex + halfCount > items.size - 1) {
                    startIndex = items.size - maxDisplayNum
                    endIndex = items.size
                } else {
                    startIndex = processingIndex - halfCount
                    endIndex = processingIndex + maxDisplayNum / 2 + 1
                }
                items.subList(startIndex, endIndex)
            }
        )
    }

    Card(
        modifier = modifier,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = (if (_expanded) ThemedColorSchemeKeyTokens.SurfaceVariantDim else ThemedColorSchemeKeyTokens.Transparent).value.withState(enabled)),
        indication = null,
    ) {
        Column {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    color = (if (_expanded) ThemedColorSchemeKeyTokens.SurfaceVariant else ThemedColorSchemeKeyTokens.Surface).value.withState(enabled),
                    shape = ShapeDefaults.Medium,
                    onClick = {
                        if (totalCount != 0 && expandable) {
                            _expanded = _expanded.not()
                        }
                    }
                ) {
                    Row(modifier = Modifier.padding(SizeTokens.Level16), horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16), verticalAlignment = Alignment.CenterVertically) {
                        _state.StateView(enabled = enabled, expanded = _expanded, progress = progress)

                        TitleMediumText(
                            modifier = Modifier.weight(1f),
                            text = title,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value.withState(enabled)
                        )

                        if (packageName != null)
                            PackageIconImage(packageName = packageName, size = SizeTokens.Level24)

                        actions()
                        if (totalCount != 0) {
                            LabelSmallText(text = "${successCount + failedCount}/${totalCount}", color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled))
                            if (expandable) Icon(
                                imageVector = if (_expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = ThemedColorSchemeKeyTokens.OnSurface.value.withState(enabled)
                            )
                        }
                    }
                }
                AnimatedVisibility(_expanded) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(count = displayItems.size, key = { "$it-${displayItems[it].title}" }) {
                            val item = displayItems[it]
                            var logExpanded by remember { mutableStateOf(false) }
                            val log = item.log
                            Surface(
                                modifier = Modifier
                                    .animateItemPlacement()
                                    .fillMaxWidth(),
                                enabled = enabled,
                                color = ThemedColorSchemeKeyTokens.Transparent.value.withState(enabled),
                                onClick = {
                                    if (log.isNotEmpty())
                                        logExpanded = logExpanded.not()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(SizeTokens.Level16),
                                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    item.state.StateView(enabled = enabled, expanded = false, progress = item.progress)
                                    TitleSmallText(modifier = Modifier.weight(1f), text = item.title, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled))
                                    LabelSmallText(text = item.content, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled))
                                    if (log.isNotEmpty())
                                        Icon(
                                            imageVector = if (logExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = ThemedColorSchemeKeyTokens.OnSurface.value.withState(enabled)
                                        )
                                }
                            }
                            AnimatedVisibility(logExpanded) {
                                LabelSmallText(modifier = Modifier.paddingHorizontal(SizeTokens.Level56), text = item.log, color = ThemedColorSchemeKeyTokens.Outline.value.withState(enabled))
                            }
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun ReportItem(
    enabled: Boolean = true,
    icon: ImageVector,
    iconTint: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.OnSurfaceVariant,
    title: String,
    titleTint: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.OnSurfaceVariant,
    content: String,
    expandedContent: (@Composable () -> Unit)? = null
) {
    val expandable by remember(expandedContent) { mutableStateOf(expandedContent != null) }
    var expanded by remember { mutableStateOf(true) }

    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            color = ThemedColorSchemeKeyTokens.Transparent.value.withState(enabled),
            onClick = {
                if (expandable)
                    expanded = expanded.not()
            }
        ) {
            Row(
                modifier = Modifier.padding(SizeTokens.Level16),
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(SizeTokens.Level24),
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint.value.withState(enabled)
                )
                TitleSmallText(modifier = Modifier.weight(1f), text = title, color = titleTint.value.withState(enabled))
                LabelSmallText(text = content, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled))
                if (expandable)
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = ThemedColorSchemeKeyTokens.OnSurface.value.withState(enabled)
                    )
            }
        }
        AnimatedVisibility(expanded) {
            expandedContent?.invoke()
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun ReportAppItem(enabled: Boolean, color: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.OnSurfaceVariant, item: ReportAppItemInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        enabled = true,
        color = ThemedColorSchemeKeyTokens.Transparent.value.withState(enabled),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(SizeTokens.Level16),
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(SizeTokens.Level24))
            LabelMediumText(modifier = Modifier.weight(1f), text = item.label, color = color.value.withState(enabled))
            LabelSmallText(text = item.user, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled))
            PackageIconImage(enabled = enabled, packageName = item.packageName, size = SizeTokens.Level24)
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun ReportFileItem(enabled: Boolean, color: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.OnSurfaceVariant, item: ReportFileItemInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        enabled = true,
        color = ThemedColorSchemeKeyTokens.Transparent.value.withState(enabled),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(SizeTokens.Level16),
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(SizeTokens.Level24))
            LabelMediumText(modifier = Modifier.weight(1f), text = item.name, color = color.value.withState(enabled))
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun AppsReportCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scope: CoroutineScope,
    pagerState: PagerState,
    title: String,
    timer: String,
    showRestoreUser: Boolean = false,
    packageSize: String,
    succeed: List<ReportAppItemInfo>,
    failed: List<ReportAppItemInfo>,
) {
    Card(
        modifier = modifier,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = ThemedColorSchemeKeyTokens.SurfaceVariantDim.value.withState(enabled)),
        indication = null,
    ) {
        Column {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    color = ThemedColorSchemeKeyTokens.SurfaceVariant.value.withState(enabled),
                    shape = ShapeDefaults.Medium,
                    onClick = {}
                ) {
                    Row(modifier = Modifier.padding(SizeTokens.Level16), horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            modifier = Modifier.size(SizeTokens.Level24),
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled)
                        )

                        TitleMediumText(
                            modifier = Modifier.weight(1f),
                            text = title,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value.withState(enabled)
                        )

                        LabelSmallText(text = packageSize, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled))
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    ReportItem(
                        icon = Icons.Filled.Timer,
                        iconTint = ThemedColorSchemeKeyTokens.Primary,
                        title = stringResource(id = R.string.time),
                        titleTint = ThemedColorSchemeKeyTokens.Primary,
                        content = timer
                    )
                    if (showRestoreUser) {
                        val context = LocalContext.current
                        val restoreUser by context.readRestoreUser().collectAsStateWithLifecycle(initialValue = -1)
                        ReportItem(
                            icon = Icons.Filled.AccountCircle,
                            iconTint = ThemedColorSchemeKeyTokens.YellowPrimary,
                            title = stringResource(id = R.string.restore_user),
                            titleTint = ThemedColorSchemeKeyTokens.YellowPrimary,
                            content = if (restoreUser == -1) stringResource(id = R.string.backup_user) else restoreUser.toString()
                        )
                    }

                    ReportItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_cancel_circle),
                        iconTint = ThemedColorSchemeKeyTokens.Error,
                        title = stringResource(id = R.string.failed),
                        titleTint = ThemedColorSchemeKeyTokens.Error,
                        content = failed.size.toString(),
                        expandedContent = if (failed.isEmpty()) null else {
                            {
                                Column {
                                    failed.forEach {
                                        ReportAppItem(enabled = enabled, color = ThemedColorSchemeKeyTokens.Error, item = it) {
                                            scope.launch {
                                                pagerState.animateScrollToPage(it.index)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                    ReportItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_check_circle),
                        iconTint = ThemedColorSchemeKeyTokens.GreenPrimary,
                        title = stringResource(id = R.string.succeed),
                        titleTint = ThemedColorSchemeKeyTokens.GreenPrimary,
                        content = succeed.size.toString(),
                        expandedContent = if (succeed.isEmpty()) null else {
                            {
                                Column {
                                    succeed.forEach {
                                        ReportAppItem(enabled = enabled, color = ThemedColorSchemeKeyTokens.GreenPrimary, item = it) {
                                            scope.launch {
                                                pagerState.animateScrollToPage(it.index)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun FilesReportCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scope: CoroutineScope,
    pagerState: PagerState,
    title: String,
    timer: String,
    showRestoreUser: Boolean = false,
    mediaSize: String,
    succeed: List<ReportFileItemInfo>,
    failed: List<ReportFileItemInfo>,
) {
    Card(
        modifier = modifier,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = ThemedColorSchemeKeyTokens.SurfaceVariantDim.value.withState(enabled)),
        indication = null,
    ) {
        Column {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    color = ThemedColorSchemeKeyTokens.SurfaceVariant.value.withState(enabled),
                    shape = ShapeDefaults.Medium,
                    onClick = {}
                ) {
                    Row(modifier = Modifier.padding(SizeTokens.Level16), horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            modifier = Modifier.size(SizeTokens.Level24),
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled)
                        )

                        TitleMediumText(
                            modifier = Modifier.weight(1f),
                            text = title,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value.withState(enabled)
                        )

                        LabelSmallText(text = mediaSize, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled))
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    ReportItem(
                        icon = Icons.Filled.Timer,
                        iconTint = ThemedColorSchemeKeyTokens.Primary,
                        title = stringResource(id = R.string.time),
                        titleTint = ThemedColorSchemeKeyTokens.Primary,
                        content = timer
                    )
                    if (showRestoreUser) {
                        val context = LocalContext.current
                        val restoreUser by context.readRestoreUser().collectAsStateWithLifecycle(initialValue = -1)
                        ReportItem(
                            icon = Icons.Filled.AccountCircle,
                            iconTint = ThemedColorSchemeKeyTokens.YellowPrimary,
                            title = stringResource(id = R.string.restore_user),
                            titleTint = ThemedColorSchemeKeyTokens.YellowPrimary,
                            content = if (restoreUser == -1) stringResource(id = R.string.backup_user) else restoreUser.toString()
                        )
                    }

                    ReportItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_cancel_circle),
                        iconTint = ThemedColorSchemeKeyTokens.Error,
                        title = stringResource(id = R.string.failed),
                        titleTint = ThemedColorSchemeKeyTokens.Error,
                        content = failed.size.toString(),
                        expandedContent = if (failed.isEmpty()) null else {
                            {
                                Column {
                                    failed.forEach {
                                        ReportFileItem(enabled = enabled, color = ThemedColorSchemeKeyTokens.Error, item = it) {
                                            scope.launch {
                                                pagerState.animateScrollToPage(it.index)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                    ReportItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_check_circle),
                        iconTint = ThemedColorSchemeKeyTokens.GreenPrimary,
                        title = stringResource(id = R.string.succeed),
                        titleTint = ThemedColorSchemeKeyTokens.GreenPrimary,
                        content = succeed.size.toString(),
                        expandedContent = if (succeed.isEmpty()) null else {
                            {
                                Column {
                                    succeed.forEach {
                                        ReportFileItem(enabled = enabled, color = ThemedColorSchemeKeyTokens.GreenPrimary, item = it) {
                                            scope.launch {
                                                pagerState.animateScrollToPage(it.index)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun OverviewCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    colorContainer: ThemedColorSchemeKeyTokens,
    onColorContainer: ThemedColorSchemeKeyTokens,
    content: @Composable ColumnScope.() -> Unit,
    actionIcon: ImageVector?,
    onClick: () -> Unit = {},
) {
    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = colorContainer.value),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(SizeTokens.Level16)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .paddingBottom(SizeTokens.Level8)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level6)
                ) {
                    Icon(
                        modifier = Modifier.intrinsicIcon(),
                        imageVector = icon,
                        tint = onColorContainer.value,
                        contentDescription = null,
                    )
                    LabelLargeText(
                        text = title,
                        color = onColorContainer.value,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                content()
            }

            if (actionIcon != null)
                Icon(
                    imageVector = actionIcon,
                    tint = onColorContainer.value,
                    contentDescription = null
                )
        }
    }
}
