package com.xayah.databackup.ui.component

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.SerialTokens

@Composable
fun Serial(modifier: Modifier = Modifier, serial: Char) {
    Surface(
        shape = CircleShape,
        modifier = modifier.size(SerialTokens.CircleSize),
        color = ColorScheme.onSurfaceVariant()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TitleSmallBoldText(
                text = serial.toString(),
                color = ColorScheme.surface()
            )
        }
    }
}

@Composable
fun Serial(modifier: Modifier = Modifier, serial: String, enabled: Boolean = true) {
    Surface(
        shape = CircleShape,
        modifier = modifier.height(SerialTokens.CircleSize),
        color = if (enabled) ColorScheme.onSurfaceVariant() else ColorScheme.onSurfaceVariant().copy(alpha = SerialTokens.DisabledAlpha)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TitleSmallBoldText(
                modifier = Modifier.paddingHorizontal(SerialTokens.PaddingHorizontal),
                text = serial,
                color = if (enabled) ColorScheme.surface() else ColorScheme.surface().copy(alpha = SerialTokens.DisabledAlpha)
            )
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun AnimatedSerial(modifier: Modifier = Modifier, serial: String, enabled: Boolean = true) {
    Surface(
        shape = CircleShape,
        modifier = modifier.height(SerialTokens.CircleSize),
        color = if (enabled) ColorScheme.onSurfaceVariant() else ColorScheme.onSurfaceVariant().copy(alpha = SerialTokens.DisabledAlpha)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedText(targetState = serial) {
                TitleSmallBoldText(
                    modifier = Modifier.paddingHorizontal(SerialTokens.PaddingHorizontal),
                    text = serial,
                    color = if (enabled) ColorScheme.surface() else ColorScheme.surface().copy(alpha = SerialTokens.DisabledAlpha)
                )
            }
        }
    }
}
