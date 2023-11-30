package com.xayah.core.ui.component

import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens

@Composable
fun Divider(modifier: Modifier = Modifier, color: Color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor().copy(alpha = 0.3f)) =
    Divider(modifier = modifier, color = color)
