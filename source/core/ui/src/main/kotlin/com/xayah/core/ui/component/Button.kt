package com.xayah.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value

@Composable
fun IconButton(modifier: Modifier = Modifier, icon: ImageVectorToken, tint: Color = LocalContentColor.current, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(modifier = modifier, enabled = enabled, onClick = onClick) {
        Icon(
            imageVector = icon.value,
            contentDescription = null,
            tint = tint,
        )
    }
}

@Composable
fun FilledIconButton(modifier: Modifier = Modifier, icon: ImageVectorToken, onClick: () -> Unit) {
    FilledIconButton(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = icon.value,
            contentDescription = null
        )
    }
}

@Composable
fun ArrowBackButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, icon = ImageVectorToken.fromVector(Icons.Rounded.ArrowBack), onClick = onClick)
}

@Composable
fun AddIconButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, icon = ImageVectorToken.fromVector(Icons.Rounded.Add), onClick = onClick)
}

@Composable
fun CheckIconButton(modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(modifier = modifier, enabled = enabled, icon = ImageVectorToken.fromVector(Icons.Rounded.Check), onClick = onClick)
}

@Composable
fun DeleteIconButton(modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(modifier = modifier, enabled = enabled, icon = ImageVectorToken.fromVector(Icons.Rounded.Delete), onClick = onClick)
}

@Composable
fun ChecklistIconButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, icon = ImageVectorToken.fromVector(Icons.Rounded.Checklist), onClick = onClick)
}

@Composable
fun SettingsIconButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, icon = ImageVectorToken.fromVector(Icons.Outlined.Settings), onClick = onClick)
}

@Composable
fun ExtendedFab(modifier: Modifier = Modifier, expanded: Boolean = true, icon: ImageVectorToken, text: StringResourceToken, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        modifier = modifier,
        onClick = onClick,
        expanded = expanded,
        icon = { Icon(imageVector = icon.value, contentDescription = null) },
        text = { Text(text = text.value) },
    )
}

@Composable
fun TextButton(modifier: Modifier = Modifier, text: StringResourceToken, onClick: () -> Unit) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        content = { TitleSmallText(text = text.value, fontWeight = FontWeight.Bold) },
        contentPadding = ButtonDefaults.ContentPadding
    )
}

@Composable
fun FilledIconTextButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    icon: ImageVectorToken,
    text: StringResourceToken,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        onClick = onClick,
        content = {
            Icon(
                imageVector = icon.value,
                contentDescription = null,
                modifier = Modifier.paddingEnd(PaddingTokens.Level4),
            )
            TitleSmallText(text = text.value, fontWeight = FontWeight.Bold)
        }
    )
}

@Composable
fun IconTextButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVectorToken? = null,
    trailingIcon: ImageVectorToken? = null,
    text: StringResourceToken,
    onClick: () -> Unit,
) {
    TextButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        content = {
            if (leadingIcon != null) Icon(
                imageVector = leadingIcon.value,
                contentDescription = null,
                modifier = Modifier.paddingEnd(PaddingTokens.Level4),
            )
            TitleSmallText(text = text.value, fontWeight = FontWeight.Bold)
            if (trailingIcon != null) Icon(
                imageVector = trailingIcon.value,
                contentDescription = null,
                modifier = Modifier.paddingStart(PaddingTokens.Level4),
            )
        }
    )
}

@Composable
fun CheckIconButton(modifier: Modifier = Modifier, checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    IconButton(
        modifier = modifier,
        icon = if (checked) ImageVectorToken.fromDrawable(R.drawable.ic_rounded_check_circle) else ImageVectorToken.fromVector(Icons.Rounded.Circle),
        tint = if (checked) ColorSchemeKeyTokens.Primary.toColor() else ColorSchemeKeyTokens.SurfaceVariant.toColor()
    ) {
        onCheckedChange?.invoke(checked)
    }
}
