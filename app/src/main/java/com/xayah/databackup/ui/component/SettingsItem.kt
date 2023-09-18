package com.xayah.databackup.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.MenuTokens
import com.xayah.databackup.ui.token.SettingsItemTokens
import com.xayah.librootservice.util.withIOContext
import kotlinx.coroutines.launch

@Composable
fun SettingsTitle(modifier: Modifier = Modifier, title: String) {
    TitleSmallBoldText(modifier = modifier.paddingHorizontal(SettingsItemTokens.SettingsTitlePadding), text = title, color = ColorScheme.primary())
}

data class SettingsGridItemConfig(
    val icon: ImageVector,
    val title: String,
    val content: String,
)

@Composable
fun SettingsGridItem(modifier: Modifier = Modifier, config: SettingsGridItemConfig) {
    Column(modifier = modifier) {
        Icon(imageVector = config.icon, contentDescription = null)
        CompositionLocalProvider(LocalContentColor provides ColorScheme.onSurfaceVariant()) {
            LabelSmallText(modifier = Modifier.paddingTop(SettingsItemTokens.SettingsGridItemPadding), text = config.title)
        }
        TitleMediumBoldText(text = config.content)
    }
}

@Composable
fun SettingsClickable(
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    headlineContent: @Composable () -> Unit,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .paddingVertical(SettingsItemTokens.SettingsItemPadding)
            .paddingEnd(SettingsItemTokens.SettingsItemPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(SettingsItemTokens.SettingsTitlePadding), contentAlignment = Alignment.Center) {
            leadingContent?.invoke()
        }
        Column(modifier = Modifier.weight(1f)) {
            headlineContent.invoke()
            supportingContent?.let { content ->
                CompositionLocalProvider(LocalContentColor provides ColorScheme.onSurfaceVariant(), content = content)
            }
        }
        trailingContent?.invoke()
    }
}

@Composable
fun SettingsSwitch(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String,
    content: String,
    defaultValue: Boolean = false,
    onCheckedChange: (isChecked: Boolean) -> Unit,
) {
    var isChecked by remember { mutableStateOf(defaultValue) }
    SettingsClickable(
        modifier = modifier,
        leadingContent = {
            icon?.let { Icon(imageVector = it, contentDescription = null) }
        },
        headlineContent = {
            TitleMediumText(text = title)
        },
        supportingContent = {
            BodySmallBoldText(text = content)
        },
        trailingContent = {
            Switch(checked = isChecked, onCheckedChange = {
                isChecked = isChecked.not()
                onCheckedChange(isChecked)
            })
        }
    ) {
        isChecked = isChecked.not()
        onCheckedChange(isChecked)
    }
}

@Composable
fun SettingsModalDropdownMenu(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String,
    content: String,
    defaultValue: Int = 0,
    list: List<String>,
    onSelected: (index: Int, selected: String) -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(defaultValue) }
    var expanded by remember { mutableStateOf(false) }

    SettingsClickable(
        modifier = modifier,
        leadingContent = {
            icon?.let { Icon(imageVector = it, contentDescription = null) }
        },
        headlineContent = {
            TitleMediumText(text = title)
        },
        supportingContent = {
            BodySmallBoldText(text = content)
        },
        trailingContent = {
            Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                TitleMediumText(modifier = Modifier.paddingStart(SettingsItemTokens.SettingsItemPadding), text = list.getOrNull(selectedIndex) ?: "")

                ModalStringListDropdownMenu(
                    expanded = expanded,
                    selectedIndex = selectedIndex,
                    list = list,
                    maxDisplay = MenuTokens.DefaultMaxDisplay,
                    onSelected = { index, selected ->
                        expanded = false
                        onSelected(index, selected)
                        selectedIndex = index
                    },
                    onDismissRequest = { expanded = false }
                )
            }
        }
    ) {
        if (list.isNotEmpty()) expanded = true
    }
}

@Composable
fun SettingsModalDropdownMenu(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String,
    content: String,
    selectedIndex: Int,
    displayValue: String,
    onLoading: suspend () -> List<String>,
    onSelected: (index: Int, selected: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var list: List<String> by remember { mutableStateOf(listOf()) }

    SettingsClickable(
        modifier = modifier,
        leadingContent = {
            icon?.let { Icon(imageVector = it, contentDescription = null) }
        },
        headlineContent = {
            TitleMediumText(text = title)
        },
        supportingContent = {
            BodySmallBoldText(text = content)
        },
        trailingContent = {
            Box(
                modifier = Modifier
                    .paddingStart(SettingsItemTokens.SettingsItemPadding)
                    .wrapContentSize(Alignment.Center)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(SettingsItemTokens.SettingsMenuIndicatorSize))
                else TitleMediumText(text = displayValue)

                ModalStringListDropdownMenu(
                    expanded = expanded,
                    selectedIndex = selectedIndex,
                    list = list,
                    maxDisplay = MenuTokens.DefaultMaxDisplay,
                    onSelected = { index, selected ->
                        expanded = false
                        onSelected(index, selected)
                    },
                    onDismissRequest = { expanded = false }
                )
            }
        }
    ) {
        if (isLoading.not()) {
            scope.launch {
                withIOContext {
                    isLoading = true
                    list = onLoading()
                    isLoading = false
                    if (list.isNotEmpty()) expanded = true
                }
            }
        }
    }
}
