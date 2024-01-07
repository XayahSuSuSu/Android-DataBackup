package com.xayah.core.ui.component

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.model.SortType
import com.xayah.core.ui.material3.DisabledAlpha
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.ChipTokens
import com.xayah.core.ui.token.ModalMenuTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value

@Composable
fun RoundChip(modifier: Modifier = Modifier, text: String, enabled: Boolean = true) {
    val onSurfaceVariant = ColorSchemeKeyTokens.OnSurfaceVariant.toColor()
    val surface = ColorSchemeKeyTokens.Surface.toColor()
    Surface(
        shape = CircleShape,
        modifier = modifier.height(ChipTokens.DefaultHeight),
        color = if (enabled) onSurfaceVariant else onSurfaceVariant.copy(alpha = DisabledAlpha)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TitleSmallText(
                modifier = Modifier.paddingHorizontal(ChipTokens.DefaultPadding),
                text = text,
                color = if (enabled) surface else surface.copy(alpha = DisabledAlpha),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun AnimatedRoundChip(modifier: Modifier = Modifier, text: String, enabled: Boolean = true) {
    val onSurfaceVariant = ColorSchemeKeyTokens.OnSurfaceVariant.toColor()
    val surface = ColorSchemeKeyTokens.Surface.toColor()
    Surface(
        shape = CircleShape,
        modifier = modifier.height(ChipTokens.DefaultHeight),
        color = if (enabled) onSurfaceVariant else onSurfaceVariant.copy(alpha = DisabledAlpha)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedTextContainer(targetState = text) { text ->
                TitleSmallText(
                    modifier = Modifier.paddingHorizontal(ChipTokens.DefaultPadding),
                    text = text,
                    color = if (enabled) surface else surface.copy(alpha = DisabledAlpha),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AssistChip(
    enabled: Boolean,
    label: StringResourceToken,
    leadingIcon: ImageVectorToken,
    trailingIcon: ImageVectorToken?,
    shape: Shape = AssistChipDefaults.shape,
    color: ColorSchemeKeyTokens = ColorSchemeKeyTokens.Primary,
    containerColor: ColorSchemeKeyTokens = ColorSchemeKeyTokens.Transparent,
    onClick: () -> Unit,
) {
    AssistChip(
        enabled = enabled,
        onClick = onClick,
        label = { Text(text = label.value) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon.value,
                tint = color.toColor(),
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        trailingIcon = if (trailingIcon != null) {
            {
                Icon(
                    imageVector = trailingIcon.value,
                    tint = color.toColor(),
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
        shape = shape,
        colors = AssistChipDefaults.assistChipColors(labelColor = color.toColor(), containerColor = containerColor.toColor()),
    )
}

@Composable
fun AssistChip(
    enabled: Boolean,
    title: StringResourceToken,
    subtitle: StringResourceToken,
    leadingIcon: ImageVectorToken,
    trailingIcon: ImageVectorToken?,
    shape: Shape = AssistChipDefaults.shape,
    color: ColorSchemeKeyTokens = ColorSchemeKeyTokens.Primary,
    containerColor: ColorSchemeKeyTokens = ColorSchemeKeyTokens.Transparent,
    onClick: () -> Unit,
) {
    AssistChip(
        enabled = enabled,
        onClick = onClick,
        label = {
            Column(modifier = Modifier.paddingVertical(PaddingTokens.Level1)) {
                Text(text = title.value)
                LabelSmallText(text = subtitle.value)
            }
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon.value,
                tint = color.toColor(),
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        trailingIcon = if (trailingIcon != null) {
            {
                Icon(
                    imageVector = trailingIcon.value,
                    tint = color.toColor(),
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
        shape = shape,
        colors = AssistChipDefaults.assistChipColors(labelColor = color.toColor(), containerColor = containerColor.toColor()),
    )
}

@Composable
fun SortChip(
    enabled: Boolean,
    leadingIcon: ImageVectorToken,
    selectedIndex: Int,
    type: SortType = SortType.ASCENDING,
    list: List<String>,
    onSelected: (index: Int, selected: String) -> Unit,
    onClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedIcon = if (type == SortType.ASCENDING)
        ImageVectorToken.fromVector(Icons.Rounded.KeyboardArrowUp)
    else
        ImageVectorToken.fromVector(Icons.Rounded.KeyboardArrowDown)
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        AssistChip(
            enabled = enabled,
            onClick = {
                onClick()
                if (list.isNotEmpty()) expanded = true
            },
            label = StringResourceToken.fromString(list[selectedIndex]),
            leadingIcon = leadingIcon,
            trailingIcon = selectedIcon
        )

        ModalStringListDropdownMenu(
            expanded = expanded,
            selectedIndex = selectedIndex,
            selectedIcon = selectedIcon,
            list = list,
            maxDisplay = ModalMenuTokens.DefaultMaxDisplay,
            onSelected = onSelected,
            onDismissRequest = { expanded = false }
        )
    }
}

@Composable
fun FilterChip(
    enabled: Boolean,
    leadingIcon: ImageVectorToken,
    trailingIcon: ImageVectorToken? = null,
    label: StringResourceToken,
    selectedIndex: Int,
    list: List<String>,
    onSelected: (index: Int, selected: String) -> Unit,
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
            onSelected = onSelected,
            onDismissRequest = { expanded = false }
        )
    }
}

@Composable
fun MultipleSelectionFilterChip(
    enabled: Boolean,
    leadingIcon: ImageVectorToken,
    trailingIcon: ImageVectorToken? = null,
    label: StringResourceToken,
    selectedIndexList: List<Int>,
    list: List<String>,
    onSelected: (indexList: List<Int>) -> Unit,
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

        ModalStringListMultipleSelectionDropdownMenu(
            expanded = expanded,
            selectedIndexList = selectedIndexList,
            list = list,
            maxDisplay = ModalMenuTokens.DefaultMaxDisplay,
            onSelected = onSelected,
            onDismissRequest = { expanded = false }
        )
    }
}

@Composable
fun FilterChip(
    enabled: Boolean,
    leadingIcon: ImageVectorToken,
    trailingIcon: ImageVectorToken? = null,
    selectedIndex: Int,
    list: List<String>,
    onSelected: (index: Int, selected: String) -> Unit,
    onClick: () -> Unit,
) {
    FilterChip(
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        label = StringResourceToken.fromString(list[selectedIndex]),
        selectedIndex = selectedIndex,
        list = list,
        onSelected = onSelected,
        onClick = onClick
    )
}

@ExperimentalMaterial3Api
@Composable
fun ActionChip(
    enabled: Boolean,
    label: StringResourceToken,
    leadingIcon: ImageVectorToken,
    trailingIcon: ImageVectorToken? = null,
    onClick: () -> Unit,
) {
    FilterChip(
        enabled = enabled,
        selected = true,
        onClick = onClick,
        label = { Text(text = label.value) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon.value,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        trailingIcon = if (trailingIcon != null) {
            {
                Icon(
                    imageVector = trailingIcon.value,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        } else {
            null
        }
    )
}

