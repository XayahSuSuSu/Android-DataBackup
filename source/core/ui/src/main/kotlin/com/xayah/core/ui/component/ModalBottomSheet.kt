package com.xayah.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xayah.core.model.SortType
import com.xayah.core.ui.token.SizeTokens

@ExperimentalMaterial3Api
@Composable
fun FullscreenModalBottomSheet(
    title: String,
    onDismissRequest: () -> Unit,
    actions: @Composable (RowScope.() -> Unit) = {},
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp
    with(LocalDensity.current) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            dragHandle = {
                TopAppBar(
                    title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    actions = actions,
                )
            },
            contentWindowInsets = { WindowInsets.ime }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        maxHeight.dp - WindowInsets.safeDrawing
                            .getTop(this@with)
                            .toDp()
                    )
            ) {
                content()
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    with(LocalDensity.current) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            contentWindowInsets = { remember { WindowInsets(0, 0, 0, 0) } }
        ) {
            content()
            Spacer(modifier = Modifier.height(WindowInsets.safeDrawing.getBottom(this@with).toDp()))
        }
    }
}

@Composable
fun Title(text: String) {
    TitleLargeText(
        modifier = Modifier
            .paddingHorizontal(SizeTokens.Level24)
            .paddingVertical(SizeTokens.Level12),
        text = text
    )
}

@Composable
fun BottomButton(text: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .paddingTop(SizeTokens.Level12)
            .paddingHorizontal(SizeTokens.Level24),
        enabled = true,
        onClick = onClick
    ) {
        Text(text = text)
    }
}

@Composable
fun TitleSort(text: String, sortType: SortType, onSort: () -> Unit) {
    Row(
        modifier = Modifier
            .paddingHorizontal(SizeTokens.Level24)
            .paddingVertical(SizeTokens.Level12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TitleLargeText(text = text)
        IconButton(
            icon = when (sortType) {
                SortType.ASCENDING -> Icons.Outlined.ArrowDropUp
                SortType.DESCENDING -> Icons.Outlined.ArrowDropDown
            },
            onClick = onSort
        )
    }
}

@Composable
fun CheckBox(
    checked: Boolean,
    text: String,
    onValueChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onValueChange,
                role = Role.Checkbox
            )
            .paddingHorizontal(SizeTokens.Level24)
            .paddingVertical(SizeTokens.Level12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        BodyLargeText(modifier = Modifier.paddingStart(SizeTokens.Level16), text = text)
    }
}

@Composable
fun RadioButtons(selected: Int, items: List<String>, onSelect: (Int) -> Unit) {
    Column(Modifier.selectableGroup()) {
        items.forEachIndexed { index, text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (index == selected),
                        onClick = {
                            onSelect(index)
                        },
                        role = Role.RadioButton
                    )
                    .paddingHorizontal(SizeTokens.Level24)
                    .paddingVertical(SizeTokens.Level12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = (index == selected), onClick = null)
                BodyLargeText(modifier = Modifier.paddingStart(SizeTokens.Level16), text = text)
            }
        }
    }
}
