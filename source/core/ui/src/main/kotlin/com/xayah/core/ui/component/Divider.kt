package com.xayah.core.ui.component

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens

@Composable
fun Divider(modifier: Modifier = Modifier, color: Color = ColorSchemeKeyTokens.OutlineVariant.toColor().copy(alpha = 0.3f)) =
    HorizontalDivider(modifier = modifier, color = color)
