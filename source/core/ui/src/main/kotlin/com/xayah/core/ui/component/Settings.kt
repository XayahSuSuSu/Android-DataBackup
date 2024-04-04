package com.xayah.core.ui.component

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
private fun Clickable(enabled: Boolean = true, desc: StringResourceToken? = null, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Column {
        Surface(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = onClick) {
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

@Composable
fun Clickable(
    enabled: Boolean = true,
    title: StringResourceToken,
    value: StringResourceToken,
    desc: StringResourceToken? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Clickable(enabled = enabled, desc = desc, onClick = onClick) {
        Row(modifier = Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16)) {
            if (leadingContent != null) leadingContent()
            Column(modifier = Modifier.weight(1f)) {
                TitleLargeText(enabled = enabled, text = title.value, color = ColorSchemeKeyTokens.OnSurface.toColor(), fontWeight = FontWeight.Normal)
                TitleSmallText(enabled = enabled, text = value.value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(), fontWeight = FontWeight.Normal)
            }
            if (trailingContent != null) trailingContent()
        }
    }
}

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

@Composable
fun Switchable(
    enabled: Boolean = true,
    key: Preferences.Key<Boolean>,
    defValue: Boolean = true,
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

    Clickable(
        enabled = enabled,
        title = title,
        value = if (stored) checkedText else notCheckedText,
        desc = desc,
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
                checked = stored,
                onCheckedChange = {
                    scope.launch {
                        onClick(stored)
                    }
                }
            )
        },
        onClick = {
            scope.launch {
                onClick(stored)
            }
        }
    )
}

@Composable
fun Title(enabled: Boolean = true, title: StringResourceToken, content: @Composable ColumnScope.() -> Unit) {
    Column {
        TitleSmallText(
            modifier = Modifier
                .paddingHorizontal(SizeTokens.Level24)
                .paddingBottom(SizeTokens.Level12),
            enabled = enabled,
            text = title.value,
            color = ColorSchemeKeyTokens.Primary.toColor(),
            fontWeight = FontWeight.Medium
        )
        content()
    }
}
