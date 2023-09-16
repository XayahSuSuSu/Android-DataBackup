package com.xayah.databackup.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.SettingsItemTokens

@Composable
fun SettingsTitle(title: String) {
    TitleSmallBoldText(modifier = Modifier.paddingHorizontal(SettingsItemTokens.SettingsTitlePadding), text = title, color = ColorScheme.primary())
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
