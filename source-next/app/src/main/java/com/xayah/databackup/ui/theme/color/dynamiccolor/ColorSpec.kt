/*
 * Copyright 2025 Google LLC
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
package com.xayah.databackup.ui.theme.color.dynamiccolor

import com.xayah.databackup.ui.theme.color.hct.Hct
import com.xayah.databackup.ui.theme.color.palettes.TonalPalette

/** An interface defining all the necessary methods that could be different between specs. */
interface ColorSpec {
    /** All available spec versions. */
    enum class SpecVersion {
        SPEC_2021,
        SPEC_2025,
    }

    // ////////////////////////////////////////////////////////////////
    // Main Palettes //
    // ////////////////////////////////////////////////////////////////
    fun primaryPaletteKeyColor(): DynamicColor

    fun secondaryPaletteKeyColor(): DynamicColor

    fun tertiaryPaletteKeyColor(): DynamicColor

    fun neutralPaletteKeyColor(): DynamicColor

    fun neutralVariantPaletteKeyColor(): DynamicColor

    fun errorPaletteKeyColor(): DynamicColor

    // ////////////////////////////////////////////////////////////////
    // Surfaces [S] //
    // ////////////////////////////////////////////////////////////////
    fun background(): DynamicColor

    fun onBackground(): DynamicColor

    fun surface(): DynamicColor

    fun surfaceDim(): DynamicColor

    fun surfaceBright(): DynamicColor

    fun surfaceContainerLowest(): DynamicColor

    fun surfaceContainerLow(): DynamicColor

    fun surfaceContainer(): DynamicColor

    fun surfaceContainerHigh(): DynamicColor

    fun surfaceContainerHighest(): DynamicColor

    fun onSurface(): DynamicColor

    fun surfaceVariant(): DynamicColor

    fun onSurfaceVariant(): DynamicColor

    fun inverseSurface(): DynamicColor

    fun inverseOnSurface(): DynamicColor

    fun outline(): DynamicColor

    fun outlineVariant(): DynamicColor

    fun shadow(): DynamicColor

    fun scrim(): DynamicColor

    fun surfaceTint(): DynamicColor

    // ////////////////////////////////////////////////////////////////
    // Primaries [P] //
    // ////////////////////////////////////////////////////////////////
    fun primary(): DynamicColor

    fun primaryDim(): DynamicColor?

    fun onPrimary(): DynamicColor

    fun primaryContainer(): DynamicColor

    fun onPrimaryContainer(): DynamicColor

    fun inversePrimary(): DynamicColor

    // ////////////////////////////////////////////////////////////////
    // Secondaries [Q] //
    // ////////////////////////////////////////////////////////////////
    fun secondary(): DynamicColor

    fun secondaryDim(): DynamicColor?

    fun onSecondary(): DynamicColor

    fun secondaryContainer(): DynamicColor

    fun onSecondaryContainer(): DynamicColor

    // ////////////////////////////////////////////////////////////////
    // Tertiaries [T] //
    // ////////////////////////////////////////////////////////////////
    fun tertiary(): DynamicColor

    fun tertiaryDim(): DynamicColor?

    fun onTertiary(): DynamicColor

    fun tertiaryContainer(): DynamicColor

    fun onTertiaryContainer(): DynamicColor

    // ////////////////////////////////////////////////////////////////
    // Errors [E] //
    // ////////////////////////////////////////////////////////////////
    fun error(): DynamicColor

    fun errorDim(): DynamicColor?

    fun onError(): DynamicColor

    fun errorContainer(): DynamicColor

    fun onErrorContainer(): DynamicColor

    // ////////////////////////////////////////////////////////////////
    // Primary Fixed Colors [PF] //
    // ////////////////////////////////////////////////////////////////
    fun primaryFixed(): DynamicColor

    fun primaryFixedDim(): DynamicColor

    fun onPrimaryFixed(): DynamicColor

    fun onPrimaryFixedVariant(): DynamicColor

    // ////////////////////////////////////////////////////////////////
    // Secondary Fixed Colors [QF] //
    // ////////////////////////////////////////////////////////////////
    fun secondaryFixed(): DynamicColor

    fun secondaryFixedDim(): DynamicColor

    fun onSecondaryFixed(): DynamicColor

    fun onSecondaryFixedVariant(): DynamicColor

    // ////////////////////////////////////////////////////////////////
    // Tertiary Fixed Colors [TF] //
    // ////////////////////////////////////////////////////////////////
    fun tertiaryFixed(): DynamicColor

    fun tertiaryFixedDim(): DynamicColor

    fun onTertiaryFixed(): DynamicColor

    fun onTertiaryFixedVariant(): DynamicColor

    // ////////////////////////////////////////////////////////////////
    // Android-only Colors //
    // ////////////////////////////////////////////////////////////////
    fun controlActivated(): DynamicColor

    fun controlNormal(): DynamicColor

    fun controlHighlight(): DynamicColor

    fun textPrimaryInverse(): DynamicColor

    fun textSecondaryAndTertiaryInverse(): DynamicColor

    fun textPrimaryInverseDisableOnly(): DynamicColor

    fun textSecondaryAndTertiaryInverseDisabled(): DynamicColor

    fun textHintInverse(): DynamicColor

    // ////////////////////////////////////////////////////////////////
    // Other //
    // ////////////////////////////////////////////////////////////////
    fun highestSurface(s: DynamicScheme): DynamicColor

    // ///////////////////////////////////////////////////////////////
    // Color value calculations //
    // ///////////////////////////////////////////////////////////////
    fun getHct(scheme: DynamicScheme, color: DynamicColor): Hct

    fun getTone(scheme: DynamicScheme, color: DynamicColor): Double

    // ////////////////////////////////////////////////////////////////
    // Scheme Palettes //
    // ////////////////////////////////////////////////////////////////
    fun getPrimaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette

    fun getSecondaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette

    fun getTertiaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette

    fun getNeutralPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette

    fun getNeutralVariantPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette

    fun getErrorPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette
}
