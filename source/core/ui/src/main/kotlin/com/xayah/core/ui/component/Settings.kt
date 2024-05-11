package com.xayah.core.ui.component

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.readStoreBoolean
import com.xayah.core.datastore.saveStoreBoolean
import com.xayah.core.ui.material3.Surface
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.value
import kotlinx.coroutines.launch

@Composable
fun Clickable(enabled: Boolean = true, desc: StringResourceToken? = null, onClick: () -> Unit, indication: Indication? = rememberRipple(), content: @Composable BoxScope.() -> Unit) {
    Column {
        Surface(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = onClick, indication = indication) {
            Box(
                modifier = Modifier
                    .paddingHorizontal(SizeTokens.Level24)
                    .paddingVertical(SizeTokens.Level16)
            ) {
                content()
            }
        }
        if (desc != null)
            TitleSmallText(
                modifier = Modifier.paddingHorizontal(SizeTokens.Level24),
                enabled = enabled,
                text = desc.value,
                color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
                fontWeight = FontWeight.Normal
            )
    }
}

@ExperimentalAnimationApi
@Composable
fun Clickable(
    enabled: Boolean = true,
    readOnly: Boolean = false,
    title: StringResourceToken,
    value: StringResourceToken,
    desc: StringResourceToken? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Clickable(enabled = enabled, desc = desc, onClick = onClick, indication = if (readOnly) null else rememberRipple()) {
        Row(modifier = Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16)) {
            if (leadingContent != null) leadingContent()
            Column(modifier = Modifier.weight(1f)) {
                AnimatedTextContainer(targetState = title.value) { text ->
                    TitleLargeText(enabled = enabled, text = text, color = ColorSchemeKeyTokens.OnSurface.toColor(enabled), fontWeight = FontWeight.Normal)
                }
                AnimatedTextContainer(targetState = value.value) { text ->
                    TitleSmallText(enabled = enabled, text = text, color = ColorSchemeKeyTokens.Outline.toColor(enabled), fontWeight = FontWeight.Normal)
                }
            }
            if (trailingContent != null) trailingContent()
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun Clickable(
    enabled: Boolean = true,
    icon: ImageVectorToken? = null,
    title: StringResourceToken,
    value: StringResourceToken,
    desc: StringResourceToken? = null,
    onClick: () -> Unit = {}
) {
    Clickable(
        enabled = enabled,
        title = title,
        value = value,
        desc = desc,
        leadingContent = {
            if (icon != null) Icon(imageVector = icon.value, contentDescription = null)
        },
        onClick = onClick
    )
}

@ExperimentalAnimationApi
@Composable
fun Selectable(
    enabled: Boolean = true,
    title: StringResourceToken,
    value: StringResourceToken,
    desc: StringResourceToken? = null,
    current: StringResourceToken,
    onClick: suspend () -> Unit = suspend {}
) {
    val scope = rememberCoroutineScope()
    Clickable(
        enabled = enabled,
        title = title,
        value = value,
        desc = desc,
        trailingContent = {
            FilledTonalButton(onClick = { scope.launch { onClick() } }) {

                Text(text = current.value)
            }
        },
        onClick = { scope.launch { onClick() } }
    )
}

@ExperimentalAnimationApi
@Composable
fun Switchable(
    enabled: Boolean = true,
    checked: Boolean,
    icon: ImageVectorToken? = null,
    title: StringResourceToken,
    checkedText: StringResourceToken,
    notCheckedText: StringResourceToken = checkedText,
    desc: StringResourceToken? = null,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Clickable(
        enabled = enabled,
        title = title,
        value = if (checked) checkedText else notCheckedText,
        desc = desc,
        leadingContent = {
            if (icon != null) Icon(imageVector = icon.value, contentDescription = null)
        },
        trailingContent = {
            Divider(
                modifier = Modifier
                    .height(SizeTokens.Level36)
                    .width(SizeTokens.Level1)
                    .fillMaxHeight()
            )
            Switch(
                modifier = Modifier,
                enabled = enabled,
                checked = checked,
                onCheckedChange = { onCheckedChange.invoke(checked) }
            )
        },
        onClick = {
            onCheckedChange.invoke(checked)
        }
    )
}

@ExperimentalAnimationApi
@Composable
fun Switchable(
    enabled: Boolean = true,
    key: Preferences.Key<Boolean>,
    defValue: Boolean = true,
    icon: ImageVectorToken? = null,
    title: StringResourceToken,
    checkedText: StringResourceToken,
    notCheckedText: StringResourceToken = checkedText,
    desc: StringResourceToken? = null,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stored by context.readStoreBoolean(key = key, defValue = defValue).collectAsStateWithLifecycle(initialValue = defValue)
    val onClick: suspend (Boolean) -> Unit = {
        context.saveStoreBoolean(key = key, value = it.not())
        onCheckedChange(it.not())
    }

    Switchable(
        enabled = enabled,
        checked = stored,
        icon = icon,
        title = title,
        checkedText = checkedText,
        notCheckedText = notCheckedText,
        desc = desc,
        onCheckedChange = {
            scope.launch {
                onClick(stored)
            }
        }
    )
}

@ExperimentalAnimationApi
@Composable
fun Checkable(
    enabled: Boolean = true,
    checked: Boolean,
    icon: ImageVectorToken? = null,
    title: StringResourceToken,
    value: StringResourceToken,
    desc: StringResourceToken? = null,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Clickable(
        enabled = enabled,
        title = title,
        value = value,
        desc = desc,
        leadingContent = {
            if (icon != null) Icon(imageVector = icon.value, contentDescription = null)
        },
        trailingContent = {
            Divider(
                modifier = Modifier
                    .height(SizeTokens.Level36)
                    .width(SizeTokens.Level1)
                    .fillMaxHeight()
            )
            Checkbox(modifier = Modifier, enabled = enabled, checked = checked, onCheckedChange = { onCheckedChange(checked) })
        },
        onClick = {
            onCheckedChange(checked)
        }
    )
}

@Composable
fun Title(enabled: Boolean = true, title: StringResourceToken, content: @Composable ColumnScope.() -> Unit) {
    Column {
        TitleSmallText(
            modifier = Modifier
                .paddingHorizontal(SizeTokens.Level24)
                .paddingVertical(SizeTokens.Level12),
            enabled = enabled,
            text = title.value,
            color = ColorSchemeKeyTokens.Primary.toColor(),
            fontWeight = FontWeight.Medium
        )
        content()
    }
}
