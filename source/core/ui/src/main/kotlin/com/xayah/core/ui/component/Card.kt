package com.xayah.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import com.xayah.core.ui.material3.CardColors
import com.xayah.core.ui.material3.CardDefaults
import com.xayah.core.ui.material3.CardElevation
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.OutlinedCardTokens

@Composable
fun outlinedCardBorder(enabled: Boolean = true, borderColor: Color? = null): BorderStroke {
    val tint = borderColor ?: OutlinedCardTokens.OutlineColor.toColor()
    val color = if (enabled) {
        tint
    } else {
        tint.copy(alpha = OutlinedCardTokens.DisabledOutlineOpacity)
            .compositeOver(
                MaterialTheme.colorScheme.surfaceColorAtElevation(
                    OutlinedCardTokens.DisabledContainerElevation
                )
            )
    }
    return remember(color) { BorderStroke(OutlinedCardTokens.OutlineWidth, color) }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun Card(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = colors.containerColor(enabled).value,
        contentColor = colors.contentColor(enabled).value,
        tonalElevation = elevation.tonalElevation(enabled, interactionSource).value,
        shadowElevation = elevation.shadowElevation(enabled, interactionSource).value,
        border = border,
        interactionSource = interactionSource,
    ) {
        Column(content = content)
    }
}
