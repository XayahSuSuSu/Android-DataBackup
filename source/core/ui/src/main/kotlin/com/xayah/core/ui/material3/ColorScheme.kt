/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xayah.core.ui.material3

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.xayah.core.datastore.readMonet
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.theme.Blue10
import com.xayah.core.ui.theme.Blue20
import com.xayah.core.ui.theme.Blue30
import com.xayah.core.ui.theme.Blue40
import com.xayah.core.ui.theme.Blue80
import com.xayah.core.ui.theme.Blue90
import com.xayah.core.ui.theme.Green10
import com.xayah.core.ui.theme.Green20
import com.xayah.core.ui.theme.Green30
import com.xayah.core.ui.theme.Green40
import com.xayah.core.ui.theme.Green80
import com.xayah.core.ui.theme.Green90
import com.xayah.core.ui.theme.Yellow10
import com.xayah.core.ui.theme.Yellow20
import com.xayah.core.ui.theme.Yellow30
import com.xayah.core.ui.theme.Yellow40
import com.xayah.core.ui.theme.Yellow80
import com.xayah.core.ui.theme.Yellow90

/**
 * Helper function for component color tokens. Here is an example on how to use component color
 * tokens:
 * ``MaterialTheme.colorScheme.fromToken(ExtendedFabBranded.BrandedContainerColor)``
 */
@Composable
internal fun ColorScheme.fromToken(value: ColorSchemeKeyTokens): Color {
    val context = LocalContext.current
    // Dynamic color is available on Android 12+
    val dynamicColor by context.readMonet().collectAsState(initial = true)
    val isDarkTheme = isSystemInDarkTheme()
    val tonalPalette = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicTonalPalette(context) else BaselineTonalPalette

    return when (value) {
        ColorSchemeKeyTokens.Transparent -> Color.Transparent
        ColorSchemeKeyTokens.LocalContent -> LocalContentColor.current
        ColorSchemeKeyTokens.Unspecified -> Color.Unspecified
        ColorSchemeKeyTokens.Background -> background
        ColorSchemeKeyTokens.Error -> error
        ColorSchemeKeyTokens.ErrorContainer -> errorContainer
        ColorSchemeKeyTokens.InverseOnSurface -> inverseOnSurface
        ColorSchemeKeyTokens.InversePrimary -> inversePrimary
        ColorSchemeKeyTokens.InverseSurface -> inverseSurface
        ColorSchemeKeyTokens.OnBackground -> onBackground
        ColorSchemeKeyTokens.OnError -> onError
        ColorSchemeKeyTokens.OnErrorContainer -> onErrorContainer
        ColorSchemeKeyTokens.OnPrimary -> onPrimary
        ColorSchemeKeyTokens.OnPrimaryContainer -> onPrimaryContainer
        ColorSchemeKeyTokens.OnSecondary -> onSecondary
        ColorSchemeKeyTokens.OnSecondaryContainer -> onSecondaryContainer
        ColorSchemeKeyTokens.OnSurface -> onSurface
        ColorSchemeKeyTokens.OnSurfaceVariant -> onSurfaceVariant
        ColorSchemeKeyTokens.SurfaceTint -> surfaceTint
        ColorSchemeKeyTokens.OnTertiary -> onTertiary
        ColorSchemeKeyTokens.OnTertiaryContainer -> onTertiaryContainer
        ColorSchemeKeyTokens.Outline -> outline
        ColorSchemeKeyTokens.OutlineVariant -> outlineVariant
        ColorSchemeKeyTokens.Primary -> primary
        ColorSchemeKeyTokens.PrimaryContainer -> primaryContainer
        ColorSchemeKeyTokens.Scrim -> scrim
        ColorSchemeKeyTokens.Secondary -> secondary
        ColorSchemeKeyTokens.SecondaryContainer -> secondaryContainer
        ColorSchemeKeyTokens.Surface -> surface
        ColorSchemeKeyTokens.SurfaceVariant -> surfaceVariant
        ColorSchemeKeyTokens.Tertiary -> tertiary
        ColorSchemeKeyTokens.TertiaryContainer -> tertiaryContainer

        ColorSchemeKeyTokens.YellowPrimary -> if (isDarkTheme) Yellow80 else Yellow40
        ColorSchemeKeyTokens.YellowPrimaryContainer -> if (isDarkTheme) Yellow30 else Yellow90
        ColorSchemeKeyTokens.YellowOnPrimaryContainer -> if (isDarkTheme) Yellow90 else Yellow10
        ColorSchemeKeyTokens.BluePrimary -> if (isDarkTheme) Blue80 else Blue40
        ColorSchemeKeyTokens.BluePrimaryContainer -> if (isDarkTheme) Blue30 else Blue90
        ColorSchemeKeyTokens.BlueOnPrimaryContainer -> if (isDarkTheme) Blue90 else Blue10
        ColorSchemeKeyTokens.GreenPrimary -> if (isDarkTheme) Green80 else Green40
        ColorSchemeKeyTokens.GreenPrimaryContainer -> if (isDarkTheme) Green30 else Green90
        ColorSchemeKeyTokens.GreenOnPrimaryContainer -> if (isDarkTheme) Green90 else Green10

        ColorSchemeKeyTokens.PrimaryL80D20 -> if (isDarkTheme) tonalPalette.primary20 else tonalPalette.primary80
        ColorSchemeKeyTokens.SecondaryL80D20 -> if (isDarkTheme) tonalPalette.secondary20 else tonalPalette.secondary80
        ColorSchemeKeyTokens.TertiaryL80D20 -> if (isDarkTheme) tonalPalette.tertiary20 else tonalPalette.tertiary80
        ColorSchemeKeyTokens.YellowL80D20 -> if (isDarkTheme) Yellow20 else Yellow80
        ColorSchemeKeyTokens.BlueL80D20 -> if (isDarkTheme) Blue20 else Blue80
        ColorSchemeKeyTokens.GreenL80D20 -> if (isDarkTheme) Green20 else Green80

        ColorSchemeKeyTokens.PrimaryFixed -> if (isDarkTheme) tonalPalette.primary90 else tonalPalette.primary90
        ColorSchemeKeyTokens.PrimaryFixedDim -> if (isDarkTheme) tonalPalette.primary80 else tonalPalette.primary80
        ColorSchemeKeyTokens.SecondaryFixed -> if (isDarkTheme) tonalPalette.secondary90 else tonalPalette.secondary90
        ColorSchemeKeyTokens.SecondaryFixedDim -> if (isDarkTheme) tonalPalette.secondary80 else tonalPalette.secondary80
        ColorSchemeKeyTokens.TertiaryFixed -> if (isDarkTheme) tonalPalette.tertiary90 else tonalPalette.tertiary90
        ColorSchemeKeyTokens.TertiaryFixedDim -> if (isDarkTheme) tonalPalette.tertiary80 else tonalPalette.tertiary80
    }
}

/**
 * A low level of alpha used to represent disabled components, such as text in a disabled Button.
 */
internal const val DisabledAlpha = 0.38f

/** Converts a color token key to the local color scheme provided by the theme */
@Composable
fun ColorSchemeKeyTokens.toColor(enabled: Boolean = true): Color {
    return if (enabled) MaterialTheme.colorScheme.fromToken(this) else MaterialTheme.colorScheme.fromToken(this).copy(alpha = DisabledAlpha)
}
