package com.xayah.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
fun ArrowBackButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, icon = ImageVectorToken.fromVector(Icons.Rounded.ArrowBack), onClick = onClick)
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
fun CheckIconButton(modifier: Modifier = Modifier, enabled: Boolean = true, checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    IconButton(
        modifier = modifier,
        enabled = enabled,
        icon = if (checked) ImageVectorToken.fromDrawable(R.drawable.ic_rounded_check_circle) else ImageVectorToken.fromVector(Icons.Rounded.Circle),
        tint = if (checked) ColorSchemeKeyTokens.Primary.toColor() else ColorSchemeKeyTokens.SurfaceVariant.toColor()
    ) {
        onCheckedChange?.invoke(checked)
    }
}
