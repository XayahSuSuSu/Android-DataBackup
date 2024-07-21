package com.xayah.core.ui.component

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value

@Composable
fun Divider(modifier: Modifier = Modifier, color: Color = ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.3f)) =
    HorizontalDivider(modifier = modifier, color = color)
