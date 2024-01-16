package com.xayah.feature.main.packages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.core.data.util.typeIconToken
import com.xayah.core.data.util.typeNameToken
import com.xayah.core.model.DataType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.component.Card
import com.xayah.core.ui.component.DataChip
import com.xayah.core.ui.component.FilledIconTextButton
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.LabelMediumText
import com.xayah.core.ui.component.LabelSmallText
import com.xayah.core.ui.component.LinearProgressIndicator
import com.xayah.core.ui.component.ModalStringListDropdownMenu
import com.xayah.core.ui.component.PackageIconImage
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.component.outlinedCardBorder
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.ModalMenuTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.feature.main.packages.detail.IndexUiIntent
import com.xayah.feature.main.packages.detail.IndexViewModel

@ExperimentalMaterial3Api
@Composable
internal fun TopBar(scrollBehavior: TopAppBarScrollBehavior, topBarState: TopBarState) {
    Column {
        SecondaryTopBar(
            scrollBehavior = scrollBehavior,
            title = topBarState.title
        )
        if (topBarState.progress != 1f) {
            var targetProgress by remember { mutableFloatStateOf(0f) }
            val animatedProgress = animateFloatAsState(
                targetValue = targetProgress,
                animationSpec = tween(),
                label = AnimationTokens.AnimatedProgressLabel
            )
            targetProgress = topBarState.progress
            if (animatedProgress.value != 1f)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = animatedProgress.value)
        }
    }
}

@Composable
internal fun ChipRow(chipGroup: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
    ) {
        Spacer(modifier = Modifier.size(PaddingTokens.Level0))

        chipGroup()

        Spacer(modifier = Modifier.size(PaddingTokens.Level0))
    }
}

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PackageCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String,
    packageName: String,
    cardSelected: Boolean,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    infoChipGroup: @Composable (RowScope.() -> Unit),
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        enabled = enabled,
        onClick = onCardClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCardLongClick()
        },
        border = if (cardSelected) outlinedCardBorder(borderColor = ColorSchemeKeyTokens.Primary.toColor()) else null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingTokens.Level4)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level2)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2)
                ) {
                    PackageIconImage(packageName = packageName, size = SizeTokens.Level5)
                    Column(modifier = Modifier.weight(1f)) {
                        TitleMediumText(text = label, fontWeight = FontWeight.Bold)
                        LabelSmallText(text = packageName)
                    }
                    if (cardSelected) Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Top),
                        tint = ColorSchemeKeyTokens.Primary.toColor(),
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                    verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                    content = {
                        infoChipGroup()
                    }
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun PackageDetailDataChip(modifier: Modifier = Modifier, enabled: Boolean, item: PackageDataChipItem, onClick: () -> Unit) {
    DataChip(
        modifier = modifier,
        enabled = enabled,
        title = item.dataType.typeNameToken,
        subtitle = if (item.dataBytes == null) null else StringResourceToken.fromString(item.dataBytes.toDouble().formatSize()),
        leadingIcon = item.dataType.typeIconToken,
        trailingIcon = if (item.selected) ImageVectorToken.fromDrawable(R.drawable.ic_rounded_check_circle) else null,
        border = if (item.selected) null else outlinedCardBorder(),
        color = if (item.selected) ColorSchemeKeyTokens.OnSecondaryContainer else ColorSchemeKeyTokens.OnSurfaceVariant,
        containerColor = if (item.selected) ColorSchemeKeyTokens.SecondaryContainer else ColorSchemeKeyTokens.Transparent,
        onClick = onClick
    )
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun PackageApkChip(modifier: Modifier = Modifier, enabled: Boolean, item: PackageDataChipItem, onClick: () -> Unit) {
    DataChip(
        modifier = modifier,
        enabled = enabled,
        title = StringResourceToken.fromStringId(R.string.apk),
        subtitle = if (item.dataBytes == null) null else StringResourceToken.fromString(item.dataBytes.toDouble().formatSize()),
        leadingIcon = item.dataType.typeIconToken,
        trailingIcon = if (item.selected) ImageVectorToken.fromDrawable(R.drawable.ic_rounded_check_circle) else null,
        border = if (item.selected) null else outlinedCardBorder(),
        color = if (item.selected) ColorSchemeKeyTokens.OnSecondaryContainer else ColorSchemeKeyTokens.OnSurfaceVariant,
        containerColor = if (item.selected) ColorSchemeKeyTokens.SecondaryContainer else ColorSchemeKeyTokens.Transparent,
        onClick = onClick
    )
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun PackageDataChip(modifier: Modifier = Modifier, enabled: Boolean, items: List<PackageDataChipItem>, onClick: () -> Unit) {
    val context = LocalContext.current
    DataChip(
        modifier = modifier,
        enabled = enabled,
        title = StringResourceToken.fromStringId(R.string.data),
        subtitle = if (items.dataBytes == null) null
        else StringResourceToken.fromString(
            "${items.dataBytes!!.toDouble().formatSize()}, ${items.countItems(context)}"
        ),
        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_database),
        trailingIcon = when (items.selectionState) {
            SelectionState.All -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_check_circle)
            SelectionState.None -> null
            SelectionState.PART -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_error)
        },
        border = if (items.selectionState != SelectionState.None) null else outlinedCardBorder(),
        color = if (items.selectionState != SelectionState.None) ColorSchemeKeyTokens.OnSecondaryContainer else ColorSchemeKeyTokens.OnSurfaceVariant,
        containerColor = when (items.selectionState) {
            SelectionState.All -> ColorSchemeKeyTokens.SecondaryContainer
            SelectionState.None -> ColorSchemeKeyTokens.Transparent
            SelectionState.PART -> ColorSchemeKeyTokens.ErrorContainer
        },
        onClick = onClick
    )
}

@Composable
fun InfoItem(title: StringResourceToken, content: StringResourceToken) {
    Row {
        LabelMediumText(
            text = title.value,
            color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor()
        )
        Spacer(modifier = Modifier.weight(1f))
        LabelMediumText(
            text = content.value,
            color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
        )
    }
}

@ExperimentalMaterial3Api
@Composable
fun InfoItem(
    title: StringResourceToken,
    content: StringResourceToken,
    selectedIndex: Int,
    list: List<String>,
    onSelected: (index: Int, selected: String) -> Unit,
) {
    Row {
        LabelMediumText(
            text = title.value,
            color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor()
        )
        Spacer(modifier = Modifier.weight(1f))
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            LabelMediumText(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = true,
                    onClick = {
                        if (list.isNotEmpty()) expanded = true
                    }
                ),
                text = content.value,
                color = ColorSchemeKeyTokens.Primary.toColor(),
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
            )

            ModalStringListDropdownMenu(
                expanded = expanded,
                selectedIndex = selectedIndex,
                list = list,
                maxDisplay = ModalMenuTokens.DefaultMaxDisplay,
                onSelected = { index, selected ->
                    onSelected(index, selected)
                    expanded = false
                },
                onDismissRequest = { expanded = false }
            )
        }
    }
}

@Composable
fun HeaderItem(expand: Boolean, title: StringResourceToken, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TitleMediumText(
            modifier = Modifier.paddingBottom(PaddingTokens.Level1),
            text = title.value,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(icon = ImageVectorToken.fromVector(if (expand) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore), onClick = onClick)
    }
}

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun OpItem(
    title: StringResourceToken,
    btnText: StringResourceToken,
    btnIcon: ImageVectorToken,
    isRefreshing: Boolean,
    activatedState: Boolean,
    chipsState: List<PackageDataChipItem>,
    itemState: PackageEntity,
    onBtnClick: () -> Unit,
    infoContent: @Composable ColumnScope.() -> Unit,
    btnContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val viewModel = hiltViewModel<IndexViewModel>()
    var expand by remember { mutableStateOf(false) }
    HeaderItem(expand = expand, title = title) {
        expand = expand.not()
    }
    AnimatedContent(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        targetState = expand,
        label = AnimationTokens.AnimatedContentLabel
    ) { targetState ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level2)
        ) {
            InfoItem(
                title = StringResourceToken.fromStringId(R.string.id),
                content = StringResourceToken.fromString(itemState.extraInfo.preserveId.toString())
            )
            infoContent()
            if (targetState.not()) {
                if (chipsState.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2)) {
                        val apkItem = chipsState[0]
                        PackageApkChip(
                            modifier = Modifier.weight(1f),
                            enabled = isRefreshing.not(),
                            item = apkItem,
                        ) {
                            viewModel.emitIntent(IndexUiIntent.UpdatePackage(apkItem.reversedPackage(itemState)))
                        }
                        val items = chipsState.filter { it.dataType != DataType.PACKAGE_APK }
                        PackageDataChip(
                            modifier = Modifier.weight(1f),
                            enabled = isRefreshing.not(),
                            items = items,
                        ) {
                            viewModel.emitIntent(IndexUiIntent.UpdatePackage(items.dataReversedPackage(itemState)))
                        }
                    }
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                    verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                    maxItemsInEachRow = 2,
                ) {
                    chipsState.forEach { item ->
                        PackageDetailDataChip(
                            modifier = Modifier.weight(1f),
                            enabled = isRefreshing.not(),
                            item = item,
                        ) {
                            viewModel.emitIntent(IndexUiIntent.UpdatePackage(item.reversedPackage(itemState)))
                        }
                    }
                }
                btnContent?.invoke(this)
            }
            FilledIconTextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingBottom(PaddingTokens.Level2),
                text = if (activatedState)
                    StringResourceToken.fromStringId(R.string.task_is_in_progress)
                else
                    btnText,
                icon = btnIcon,
                enabled = isRefreshing.not() && activatedState.not(),
                onClick = onBtnClick
            )
        }
    }
}