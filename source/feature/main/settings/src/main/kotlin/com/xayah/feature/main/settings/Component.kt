package com.xayah.feature.main.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.ui.component.BodySmallText
import com.xayah.core.ui.component.LabelSmallText
import com.xayah.core.ui.component.ModalStringListDropdownMenu
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.component.TitleSmallText
import com.xayah.core.ui.component.paddingEnd
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingStart
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.ModalMenuTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.value

@Composable
fun SettingsInfo(modifier: Modifier = Modifier, info: SettingsInfoItem) {
    Column(modifier = modifier) {
        Icon(imageVector = info.icon.value, contentDescription = null)
        CompositionLocalProvider(LocalContentColor provides ColorSchemeKeyTokens.OnSurfaceVariant.toColor()) {
            LabelSmallText(
                modifier = Modifier.paddingTop(PaddingTokens.Level1),
                text = info.title.value,
                color = if (info.onWarning) ColorSchemeKeyTokens.Error.toColor() else Color.Unspecified
            )
        }
        TitleMediumText(
            text = info.content.value,
            color = if (info.onWarning) ColorSchemeKeyTokens.Error.toColor() else Color.Unspecified,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsTitle(modifier: Modifier = Modifier, title: StringResourceToken) {
    TitleSmallText(
        modifier = modifier.paddingHorizontal(PaddingTokens.Level7),
        text = title.value,
        color = ColorSchemeKeyTokens.Primary.toColor(),
        fontWeight = FontWeight.Bold
    )
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
            .paddingVertical(PaddingTokens.Level3)
            .paddingEnd(PaddingTokens.Level3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(PaddingTokens.Level7), contentAlignment = Alignment.Center) {
            leadingContent?.invoke()
        }
        Column(modifier = Modifier.weight(1f)) {
            headlineContent.invoke()
            supportingContent?.let { content ->
                CompositionLocalProvider(LocalContentColor provides ColorSchemeKeyTokens.OnSurfaceVariant.toColor(), content = content)
            }
        }
        trailingContent?.invoke()
    }
}

@Composable
fun SettingsSwitch(
    modifier: Modifier = Modifier,
    icon: ImageVectorToken? = null,
    title: StringResourceToken,
    content: StringResourceToken,
    checked: Boolean = false,
    onCheckedChange: () -> Unit,
) {
    SettingsClickable(
        modifier = modifier,
        leadingContent = {
            icon?.let { Icon(imageVector = it.value, contentDescription = null) }
        },
        headlineContent = {
            TitleMediumText(text = title.value)
        },
        supportingContent = {
            BodySmallText(text = content.value, fontWeight = FontWeight.Bold)
        },
        trailingContent = {
            Switch(modifier = Modifier.paddingStart(PaddingTokens.Level3), checked = checked, onCheckedChange = {
                onCheckedChange()
            })
        }
    ) {
        onCheckedChange()
    }
}

@Composable
fun SettingsModalDropdownMenu(
    modifier: Modifier = Modifier,
    icon: ImageVectorToken? = null,
    title: StringResourceToken,
    content: StringResourceToken,
    selected: String,
    selectedIndex: Int = 0,
    list: List<String>,
    onClick: (() -> Unit)? = null,
    onSelected: (index: Int, selected: String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    SettingsClickable(
        modifier = modifier,
        leadingContent = {
            icon?.let { Icon(imageVector = it.value, contentDescription = null) }
        },
        headlineContent = {
            TitleMediumText(text = title.value)
        },
        supportingContent = {
            BodySmallText(text = content.value, fontWeight = FontWeight.Bold)
        },
        trailingContent = {
            Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                TitleMediumText(modifier = Modifier.paddingStart(PaddingTokens.Level3), text = selected)

                ModalStringListDropdownMenu(
                    expanded = expanded,
                    selectedIndex = selectedIndex,
                    list = list,
                    maxDisplay = ModalMenuTokens.DefaultMaxDisplay,
                    onSelected = { index, selected ->
                        expanded = false
                        onSelected(index, selected)
                    },
                    onDismissRequest = { expanded = false }
                )
            }
        }
    ) {
        expanded = true
        onClick?.invoke()
    }
}
