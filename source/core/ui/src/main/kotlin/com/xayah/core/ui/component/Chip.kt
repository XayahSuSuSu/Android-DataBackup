package com.xayah.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.xayah.core.model.DataType
import com.xayah.core.model.SortType
import com.xayah.core.ui.material3.CardDefaults
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.ModalMenuTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.icon

@Composable
fun AssistChip(
    enabled: Boolean,
    label: String,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    shape: Shape = AssistChipDefaults.shape,
    color: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.Primary,
    containerColor: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.Transparent,
    border: BorderStroke? = AssistChipDefaults.assistChipBorder(enabled),
    onClick: () -> Unit = {},
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = label) },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    tint = color.value.withState(enabled),
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
        trailingIcon = if (trailingIcon != null) {
            {
                Icon(
                    imageVector = trailingIcon,
                    tint = color.value.withState(enabled),
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
        shape = shape,
        colors = AssistChipDefaults.assistChipColors(labelColor = color.value.withState(enabled), containerColor = containerColor.value.withState(enabled)),
        border = border
    )
}

@Composable
fun SortChip(
    enabled: Boolean,
    dismissOnSelected: Boolean = false,
    leadingIcon: ImageVector,
    selectedIndex: Int,
    type: SortType = SortType.ASCENDING,
    list: List<String>,
    onSelected: (index: Int, selected: String) -> Unit,
    onClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedIcon = if (type == SortType.ASCENDING)
        Icons.Rounded.KeyboardArrowUp
    else
        Icons.Rounded.KeyboardArrowDown
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        AssistChip(
            enabled = enabled,
            onClick = {
                onClick()
                if (list.isNotEmpty()) expanded = true
            },
            label = list[selectedIndex],
            leadingIcon = leadingIcon,
            trailingIcon = selectedIcon,
            color = ThemedColorSchemeKeyTokens.Primary,
            containerColor = ThemedColorSchemeKeyTokens.PrimaryContainer,
            border = null,
        )

        ModalStringListDropdownMenu(
            expanded = expanded,
            selectedIndex = selectedIndex,
            selectedIcon = selectedIcon,
            list = list,
            maxDisplay = ModalMenuTokens.DefaultMaxDisplay,
            onSelected = { index: Int, selected: String ->
                onSelected(index, selected)
                if (dismissOnSelected) expanded = false
            },
            onDismissRequest = { expanded = false }
        )
    }
}

@Composable
fun FilterChip(
    enabled: Boolean,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector? = null,
    label: String,
    selectedIndex: Int,
    list: List<String>,
    onSelected: (index: Int, selected: String) -> Unit,
    dismissOnSelected: Boolean = false,
    onClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        AssistChip(
            enabled = enabled,
            onClick = {
                onClick()
                if (list.isNotEmpty()) expanded = true
            },
            label = label,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon
        )

        ModalStringListDropdownMenu(
            expanded = expanded,
            selectedIndex = selectedIndex,
            list = list,
            maxDisplay = ModalMenuTokens.DefaultMaxDisplay,
            onSelected = { index, selected ->
                onSelected(index, selected)
                if (dismissOnSelected) expanded = false
            },
            onDismissRequest = { expanded = false }
        )
    }
}

@Composable
fun MultipleSelectionFilterChip(
    enabled: Boolean,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector? = null,
    label: String,
    selectedIndexList: List<Int>,
    list: List<String>,
    onSelected: (indexList: List<Int>) -> Unit,
    dismissOnSelected: Boolean = false,
    onClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        AssistChip(
            enabled = enabled,
            onClick = {
                expanded = true
                onClick()
            },
            label = label,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon
        )

        ModalStringListMultipleSelectionDropdownMenu(
            expanded = expanded,
            selectedIndexList = selectedIndexList,
            list = list,
            maxDisplay = ModalMenuTokens.DefaultMaxDisplay,
            onSelected = { indexList ->
                onSelected(indexList)
                if (dismissOnSelected) expanded = false
            },
            onDismissRequest = { expanded = false }
        )
    }
}

@Composable
fun FilterChip(
    enabled: Boolean,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector? = null,
    selectedIndex: Int,
    list: List<String>,
    onSelected: (index: Int, selected: String) -> Unit,
    dismissOnSelected: Boolean = false,
    onClick: () -> Unit,
) {
    FilterChip(
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        label = list[selectedIndex],
        selectedIndex = selectedIndex,
        list = list,
        onSelected = onSelected,
        dismissOnSelected = dismissOnSelected,
        onClick = onClick
    )
}

@Composable
fun ChipRow(horizontalSpace: Dp = SizeTokens.Level16, chipGroup: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpace)
    ) {
        Spacer(modifier = Modifier.size(PaddingTokens.Level0))

        chipGroup()

        Spacer(modifier = Modifier.size(PaddingTokens.Level0))
    }
}

@Composable
fun FilterChip(
    modifier: Modifier = Modifier,
    label: String,
    trailingIcon: ImageVector? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        modifier = modifier,
        onClick = onClick,
        label = {
            Text(text = label, maxLines = 1)
        },
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
        trailingIcon = if (trailingIcon != null) {
            {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
    )
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun DataChip(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    title: String,
    subtitle: String?,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector?,
    shape: Shape = AssistChipDefaults.shape,
    border: BorderStroke? = outlinedCardBorder(),
    color: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.Primary,
    containerColor: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.Transparent,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        onLongClick = {},
        border = border,
        shape = shape,
        colors = if (enabled) CardDefaults.cardColors(containerColor = containerColor.value.withState(), contentColor = color.value.withState()) else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .paddingHorizontal(PaddingTokens.Level2)
                .heightIn(min = SizeTokens.Level52),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2)
        ) {
            Icon(
                imageVector = leadingIcon,
                tint = if (enabled) color.value.withState() else LocalContentColor.current,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .paddingVertical(PaddingTokens.Level2)
            ) {
                LabelLargeText(modifier = Modifier.basicMarquee(), text = title, maxLines = 1)
                if (subtitle != null)
                    LabelSmallText(modifier = Modifier.basicMarquee(), text = subtitle, maxLines = 1)
            }
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    tint = if (enabled) color.value.withState() else LocalContentColor.current,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun PackageDataChip(modifier: Modifier = Modifier, enabled: Boolean = true, dataType: DataType, selected: Boolean, subtitle: String? = null, onClick: () -> Unit) {
    ActionChip(
        modifier = modifier,
        enabled = enabled,
        icon = dataType.icon,
        selected = selected,
        title = dataType.type.uppercase(),
        subtitle = subtitle,
        onClick = onClick
    )
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun ActionChip(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector,
    selected: Boolean,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ActionButton(
        modifier = modifier,
        enabled = enabled,
        icon = if (selected) Icons.Rounded.Check else icon,
        colorContainer = if (selected) ThemedColorSchemeKeyTokens.PrimaryContainer else ThemedColorSchemeKeyTokens.SurfaceContainerHigh,
        colorL80D20 = if (selected) ThemedColorSchemeKeyTokens.OnPrimaryContainer else ThemedColorSchemeKeyTokens.SurfaceDim,
        onColorContainer = if (selected) ThemedColorSchemeKeyTokens.PrimaryContainer else ThemedColorSchemeKeyTokens.OnSurface,
        onClick = onClick
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LabelLargeText(
                text = title,
                color = ThemedColorSchemeKeyTokens.OnSurface.value.withState(enabled),
                maxLines = 1,
                enabled = enabled
            )
            AnimatedVisibility(subtitle != null) {
                LabelSmallText(
                    modifier = Modifier.basicMarquee(),
                    text = subtitle!!,
                    maxLines = 1,
                    enabled = enabled
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun RoundChip(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, label: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
        com.xayah.core.ui.material3.Surface(
            modifier = modifier,
            onClick = { onClick?.invoke() },
            shape = CircleShape,
            color = ThemedColorSchemeKeyTokens.PrimaryContainer.value.withState(),
            indication = if (onClick != null) rememberRipple() else null,
        ) {
            Box(
                modifier = Modifier.wrapContentSize(),
                contentAlignment = Alignment.Center
            ) {
                label.invoke()
            }
        }
    }
}