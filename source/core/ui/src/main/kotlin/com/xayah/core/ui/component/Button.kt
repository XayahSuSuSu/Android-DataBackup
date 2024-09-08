package com.xayah.core.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.xayah.core.ui.R
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.SizeTokens

@Composable
fun IconButton(modifier: Modifier = Modifier, icon: ImageVector, tint: Color = LocalContentColor.current, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(modifier = modifier, enabled = enabled, onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.withState(enabled),
        )
    }
}

@Composable
fun FilledIconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    containerColor: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.Primary,
    contentColor: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.OnPrimary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    FilledIconButton(
        modifier = modifier,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = containerColor.value.withState(enabled), contentColor = contentColor.value.withState(enabled)),
        enabled = enabled,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )
    }
}

@Composable
fun ArrowBackButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, icon = Icons.Rounded.ArrowBack, onClick = onClick)
}

@Composable
fun TextButton(
    modifier: Modifier = Modifier,
    text: String,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        content = { Text(text = text, fontWeight = fontWeight, color = color) },
        contentPadding = ButtonDefaults.ContentPadding
    )
}

@Composable
fun CheckIconButton(modifier: Modifier = Modifier, enabled: Boolean = true, checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    IconButton(
        modifier = modifier,
        enabled = enabled,
        icon = if (checked) ImageVector.vectorResource(id = R.drawable.ic_rounded_check_circle) else Icons.Rounded.Circle,
        tint = if (checked) ThemedColorSchemeKeyTokens.Primary.value else ThemedColorSchemeKeyTokens.SurfaceVariant.value
    ) {
        onCheckedChange?.invoke(checked)
    }
}

@Composable
fun FilledTonalIconTextButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    FilledTonalButton(modifier = modifier, enabled = enabled, onClick = onClick, contentPadding = PaddingValues(SizeTokens.Level16, SizeTokens.Level8)) {
        Icon(
            modifier = Modifier.size(SizeTokens.Level20),
            tint = ThemedColorSchemeKeyTokens.Primary.value,
            imageVector = icon,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(SizeTokens.Level8))
        AutoSizeText(modifier = Modifier.weight(1f), text = text, textAlign = TextAlign.Center)
    }
}

@Composable
fun OutlinedButtonIconTextButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(modifier = modifier, enabled = enabled, onClick = onClick, contentPadding = PaddingValues(SizeTokens.Level16, SizeTokens.Level8)) {
        Icon(
            modifier = Modifier.size(SizeTokens.Level20),
            tint = ThemedColorSchemeKeyTokens.Primary.value,
            imageVector = icon,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(SizeTokens.Level8))
        AutoSizeText(modifier = Modifier.weight(1f), text = text, textAlign = TextAlign.Center)
    }
}