package com.xayah.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.ui.material3.DisabledAlpha
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.token.ChipTokens

@Composable
fun RoundChip(modifier: Modifier = Modifier, text: String, enabled: Boolean = true) {
    val onSurfaceVariant = ColorSchemeKeyTokens.OnSurfaceVariant.toColor()
    val surface = ColorSchemeKeyTokens.Surface.toColor()
    Surface(
        shape = CircleShape,
        modifier = modifier.height(ChipTokens.DefaultHeight),
        color = if (enabled) onSurfaceVariant else onSurfaceVariant.copy(alpha = DisabledAlpha)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TitleSmallText(
                modifier = Modifier.paddingHorizontal(ChipTokens.DefaultPadding),
                text = text,
                color = if (enabled) surface else surface.copy(alpha = DisabledAlpha),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
