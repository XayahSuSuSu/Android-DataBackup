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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.readRestoreUser
import com.xayah.core.model.OperationState
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.CardColors
import com.xayah.core.ui.material3.CardDefaults
import com.xayah.core.ui.material3.CardElevation
import com.xayah.core.ui.material3.ShapeDefaults
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.material3.tokens.OutlinedCardTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.ProcessingCardItem
import com.xayah.core.ui.model.ReportAppItemInfo
import com.xayah.core.ui.model.ReportFileItemInfo
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.StateView
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.getValue
import com.xayah.core.ui.util.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun outlinedCardBorder(enabled: Boolean = true, borderColor: Color? = null): BorderStroke {
    val tint = borderColor ?: OutlinedCardTokens.OutlineColor.toColor()
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
    title: StringResourceToken,
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
        colors = CardDefaults.cardColors(containerColor = (if (_expanded) ColorSchemeKeyTokens.SurfaceVariantDim else ColorSchemeKeyTokens.Transparent).toColor(enabled)),
        indication = null,
    ) {
        Column {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    color = (if (_expanded) ColorSchemeKeyTokens.SurfaceVariant else ColorSchemeKeyTokens.Surface).toColor(enabled),
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
                            text = title.value,
                            color = ColorSchemeKeyTokens.OnSurface.toColor(enabled)
                        )

                        if (packageName != null)
                            PackageIconImage(packageName = packageName, label = title.value, size = SizeTokens.Level24)

                        actions()
                        if (totalCount != 0) {
                            LabelSmallText(text = "${successCount + failedCount}/${totalCount}", color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(enabled))
                            if (expandable) Icon(
                                imageVector = if (_expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = ColorSchemeKeyTokens.OnSurface.toColor(enabled)
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
                            val log = item.log.getValue(context = context)
                            Surface(
                                modifier = Modifier
                                    .animateItemPlacement()
                                    .fillMaxWidth(),
                                enabled = enabled,
                                color = ColorSchemeKeyTokens.Transparent.toColor(enabled),
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
                                    TitleSmallText(modifier = Modifier.weight(1f), text = item.title.value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(enabled))
                                    LabelSmallText(text = item.content.value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(enabled))
                                    if (log.isNotEmpty())
                                        Icon(
                                            imageVector = if (logExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = ColorSchemeKeyTokens.OnSurface.toColor(enabled)
                                        )
                                }
                            }
                            AnimatedVisibility(logExpanded) {
                                LabelSmallText(modifier = Modifier.paddingHorizontal(SizeTokens.Level56), text = item.log.value, color = ColorSchemeKeyTokens.Outline.toColor(enabled))
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
    icon: ImageVectorToken,
    iconTint: ColorSchemeKeyTokens = ColorSchemeKeyTokens.OnSurfaceVariant,
    title: StringResourceToken,
    titleTint: ColorSchemeKeyTokens = ColorSchemeKeyTokens.OnSurfaceVariant,
    content: StringResourceToken,
    expandedContent: (@Composable () -> Unit)? = null
) {
    val expandable by remember(expandedContent) { mutableStateOf(expandedContent != null) }
    var expanded by remember { mutableStateOf(true) }

    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            color = ColorSchemeKeyTokens.Transparent.toColor(enabled),
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
                    imageVector = icon.value,
                    contentDescription = null,
                    tint = iconTint.toColor(enabled)
                )
                TitleSmallText(modifier = Modifier.weight(1f), text = title.value, color = titleTint.toColor(enabled))
                LabelSmallText(text = content.value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(enabled))
                if (expandable)
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = ColorSchemeKeyTokens.OnSurface.toColor(enabled)
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
private fun ReportAppItem(enabled: Boolean, color: ColorSchemeKeyTokens = ColorSchemeKeyTokens.OnSurfaceVariant, item: ReportAppItemInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        enabled = true,
        color = ColorSchemeKeyTokens.Transparent.toColor(enabled),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(SizeTokens.Level16),
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(SizeTokens.Level24))
            LabelMediumText(modifier = Modifier.weight(1f), text = item.label, color = color.toColor(enabled))
            LabelSmallText(text = item.user, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(enabled))
            PackageIconImage(enabled = enabled, packageName = item.packageName, size = SizeTokens.Level24)
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun ReportFileItem(enabled: Boolean, color: ColorSchemeKeyTokens = ColorSchemeKeyTokens.OnSurfaceVariant, item: ReportFileItemInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        enabled = true,
        color = ColorSchemeKeyTokens.Transparent.toColor(enabled),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(SizeTokens.Level16),
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(SizeTokens.Level24))
            LabelMediumText(modifier = Modifier.weight(1f), text = item.name, color = color.toColor(enabled))
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
    title: StringResourceToken,
    timer: StringResourceToken,
    showRestoreUser: Boolean = false,
    packageSize: StringResourceToken,
    succeed: List<ReportAppItemInfo>,
    failed: List<ReportAppItemInfo>,
) {
    Card(
        modifier = modifier,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = ColorSchemeKeyTokens.SurfaceVariantDim.toColor(enabled)),
        indication = null,
    ) {
        Column {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    color = ColorSchemeKeyTokens.SurfaceVariant.toColor(enabled),
                    shape = ShapeDefaults.Medium,
                    onClick = {}
                ) {
                    Row(modifier = Modifier.padding(SizeTokens.Level16), horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            modifier = Modifier.size(SizeTokens.Level24),
                            imageVector = ImageVectorToken.fromVector(Icons.Outlined.Description).value,
                            contentDescription = null,
                            tint = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(enabled)
                        )

                        TitleMediumText(
                            modifier = Modifier.weight(1f),
                            text = title.value,
                            color = ColorSchemeKeyTokens.OnSurface.toColor(enabled)
                        )

                        LabelSmallText(text = packageSize.value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(enabled))
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    ReportItem(
                        icon = ImageVectorToken.fromVector(Icons.Filled.Timer),
                        iconTint = ColorSchemeKeyTokens.Primary,
                        title = StringResourceToken.fromStringId(R.string.time),
                        titleTint = ColorSchemeKeyTokens.Primary,
                        content = timer
                    )
                    if (showRestoreUser) {
                        val context = LocalContext.current
                        val restoreUser by context.readRestoreUser().collectAsStateWithLifecycle(initialValue = -1)
                        ReportItem(
                            icon = ImageVectorToken.fromVector(Icons.Filled.AccountCircle),
                            iconTint = ColorSchemeKeyTokens.YellowPrimary,
                            title = StringResourceToken.fromStringId(R.string.restore_user),
                            titleTint = ColorSchemeKeyTokens.YellowPrimary,
                            content = if (restoreUser == -1) StringResourceToken.fromStringId(R.string.backup_user) else StringResourceToken.fromString(restoreUser.toString())
                        )
                    }

                    ReportItem(
                        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_cancel_circle),
                        iconTint = ColorSchemeKeyTokens.Error,
                        title = StringResourceToken.fromStringId(R.string.failed),
                        titleTint = ColorSchemeKeyTokens.Error,
                        content = StringResourceToken.fromString(failed.size.toString()),
                        expandedContent = if (failed.isEmpty()) null else {
                            {
                                Column {
                                    failed.forEach {
                                        ReportAppItem(enabled = enabled, color = ColorSchemeKeyTokens.Error, item = it) {
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
                        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_check_circle),
                        iconTint = ColorSchemeKeyTokens.GreenPrimary,
                        title = StringResourceToken.fromStringId(R.string.succeed),
                        titleTint = ColorSchemeKeyTokens.GreenPrimary,
                        content = StringResourceToken.fromString(succeed.size.toString()),
                        expandedContent = if (succeed.isEmpty()) null else {
                            {
                                Column {
                                    succeed.forEach {
                                        ReportAppItem(enabled = enabled, color = ColorSchemeKeyTokens.GreenPrimary, item = it) {
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
    title: StringResourceToken,
    timer: StringResourceToken,
    showRestoreUser: Boolean = false,
    packageSize: StringResourceToken,
    succeed: List<ReportFileItemInfo>,
    failed: List<ReportFileItemInfo>,
) {
    Card(
        modifier = modifier,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = ColorSchemeKeyTokens.SurfaceVariantDim.toColor(enabled)),
        indication = null,
    ) {
        Column {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    color = ColorSchemeKeyTokens.SurfaceVariant.toColor(enabled),
                    shape = ShapeDefaults.Medium,
                    onClick = {}
                ) {
                    Row(modifier = Modifier.padding(SizeTokens.Level16), horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            modifier = Modifier.size(SizeTokens.Level24),
                            imageVector = ImageVectorToken.fromVector(Icons.Outlined.Description).value,
                            contentDescription = null,
                            tint = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(enabled)
                        )

                        TitleMediumText(
                            modifier = Modifier.weight(1f),
                            text = title.value,
                            color = ColorSchemeKeyTokens.OnSurface.toColor(enabled)
                        )

                        LabelSmallText(text = packageSize.value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(enabled))
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    ReportItem(
                        icon = ImageVectorToken.fromVector(Icons.Filled.Timer),
                        iconTint = ColorSchemeKeyTokens.Primary,
                        title = StringResourceToken.fromStringId(R.string.time),
                        titleTint = ColorSchemeKeyTokens.Primary,
                        content = timer
                    )
                    if (showRestoreUser) {
                        val context = LocalContext.current
                        val restoreUser by context.readRestoreUser().collectAsStateWithLifecycle(initialValue = -1)
                        ReportItem(
                            icon = ImageVectorToken.fromVector(Icons.Filled.AccountCircle),
                            iconTint = ColorSchemeKeyTokens.YellowPrimary,
                            title = StringResourceToken.fromStringId(R.string.restore_user),
                            titleTint = ColorSchemeKeyTokens.YellowPrimary,
                            content = if (restoreUser == -1) StringResourceToken.fromStringId(R.string.backup_user) else StringResourceToken.fromString(restoreUser.toString())
                        )
                    }

                    ReportItem(
                        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_cancel_circle),
                        iconTint = ColorSchemeKeyTokens.Error,
                        title = StringResourceToken.fromStringId(R.string.failed),
                        titleTint = ColorSchemeKeyTokens.Error,
                        content = StringResourceToken.fromString(failed.size.toString()),
                        expandedContent = if (failed.isEmpty()) null else {
                            {
                                Column {
                                    failed.forEach {
                                        ReportFileItem(enabled = enabled, color = ColorSchemeKeyTokens.Error, item = it) {
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
                        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_check_circle),
                        iconTint = ColorSchemeKeyTokens.GreenPrimary,
                        title = StringResourceToken.fromStringId(R.string.succeed),
                        titleTint = ColorSchemeKeyTokens.GreenPrimary,
                        content = StringResourceToken.fromString(succeed.size.toString()),
                        expandedContent = if (succeed.isEmpty()) null else {
                            {
                                Column {
                                    succeed.forEach {
                                        ReportFileItem(enabled = enabled, color = ColorSchemeKeyTokens.GreenPrimary, item = it) {
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
    title: StringResourceToken,
    icon: ImageVectorToken,
    colorContainer: ColorSchemeKeyTokens,
    onColorContainer: ColorSchemeKeyTokens,
    content: @Composable ColumnScope.() -> Unit,
    actionIcon: ImageVectorToken?,
    onClick: () -> Unit = {},
) {
    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = colorContainer.toColor()),
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
                        imageVector = icon.value,
                        tint = onColorContainer.toColor(),
                        contentDescription = null,
                    )
                    LabelLargeText(
                        text = title.value,
                        color = onColorContainer.toColor(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                content()
            }

            if (actionIcon != null)
                Icon(
                    imageVector = actionIcon.value,
                    tint = onColorContainer.toColor(),
                    contentDescription = null
                )
        }
    }
}
