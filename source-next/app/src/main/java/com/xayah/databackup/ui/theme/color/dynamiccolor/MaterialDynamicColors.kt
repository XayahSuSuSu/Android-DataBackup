/*
 * Copyright 2023 Google LLC
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

/** Named colors, otherwise known as tokens, or roles, in the Material Design system. */
class MaterialDynamicColors {
    private val colorSpec: ColorSpec = ColorSpec2025()

    fun highestSurface(s: DynamicScheme): DynamicColor {
        return colorSpec.highestSurface(s)
    }

    // ////////////////////////////////////////////////////////////////
    // Main Palettes //
    // ////////////////////////////////////////////////////////////////
    fun primaryPaletteKeyColor(): DynamicColor {
        return colorSpec.primaryPaletteKeyColor()
    }

    fun secondaryPaletteKeyColor(): DynamicColor {
        return colorSpec.secondaryPaletteKeyColor()
    }

    fun tertiaryPaletteKeyColor(): DynamicColor {
        return colorSpec.tertiaryPaletteKeyColor()
    }

    fun neutralPaletteKeyColor(): DynamicColor {
        return colorSpec.neutralPaletteKeyColor()
    }

    fun neutralVariantPaletteKeyColor(): DynamicColor {
        return colorSpec.neutralVariantPaletteKeyColor()
    }

    fun errorPaletteKeyColor(): DynamicColor {
        return colorSpec.errorPaletteKeyColor()
    }

    // ////////////////////////////////////////////////////////////////
    // Surfaces [S] //
    // ////////////////////////////////////////////////////////////////
    fun background(): DynamicColor {
        return colorSpec.background()
    }

    fun onBackground(): DynamicColor {
        return colorSpec.onBackground()
    }

    fun surface(): DynamicColor {
        return colorSpec.surface()
    }

    fun surfaceDim(): DynamicColor {
        return colorSpec.surfaceDim()
    }

    fun surfaceBright(): DynamicColor {
        return colorSpec.surfaceBright()
    }

    fun surfaceContainerLowest(): DynamicColor {
        return colorSpec.surfaceContainerLowest()
    }

    fun surfaceContainerLow(): DynamicColor {
        return colorSpec.surfaceContainerLow()
    }

    fun surfaceContainer(): DynamicColor {
        return colorSpec.surfaceContainer()
    }

    fun surfaceContainerHigh(): DynamicColor {
        return colorSpec.surfaceContainerHigh()
    }

    fun surfaceContainerHighest(): DynamicColor {
        return colorSpec.surfaceContainerHighest()
    }

    fun onSurface(): DynamicColor {
        return colorSpec.onSurface()
    }

    fun surfaceVariant(): DynamicColor {
        return colorSpec.surfaceVariant()
    }

    fun onSurfaceVariant(): DynamicColor {
        return colorSpec.onSurfaceVariant()
    }

    fun inverseSurface(): DynamicColor {
        return colorSpec.inverseSurface()
    }

    fun inverseOnSurface(): DynamicColor {
        return colorSpec.inverseOnSurface()
    }

    fun outline(): DynamicColor {
        return colorSpec.outline()
    }

    fun outlineVariant(): DynamicColor {
        return colorSpec.outlineVariant()
    }

    fun shadow(): DynamicColor {
        return colorSpec.shadow()
    }

    fun scrim(): DynamicColor {
        return colorSpec.scrim()
    }

    fun surfaceTint(): DynamicColor {
        return colorSpec.surfaceTint()
    }

    // ////////////////////////////////////////////////////////////////
    // Primaries [P] //
    // ////////////////////////////////////////////////////////////////
    fun primary(): DynamicColor {
        return colorSpec.primary()
    }

    fun primaryDim(): DynamicColor? {
        return colorSpec.primaryDim()
    }

    fun onPrimary(): DynamicColor {
        return colorSpec.onPrimary()
    }

    fun primaryContainer(): DynamicColor {
        return colorSpec.primaryContainer()
    }

    fun onPrimaryContainer(): DynamicColor {
        return colorSpec.onPrimaryContainer()
    }

    fun inversePrimary(): DynamicColor {
        return colorSpec.inversePrimary()
    }

    // ///////////////////////////////////////////////////////////////
    // Primary Fixed Colors [PF] //
    // ///////////////////////////////////////////////////////////////
    fun primaryFixed(): DynamicColor {
        return colorSpec.primaryFixed()
    }

    fun primaryFixedDim(): DynamicColor {
        return colorSpec.primaryFixedDim()
    }

    fun onPrimaryFixed(): DynamicColor {
        return colorSpec.onPrimaryFixed()
    }

    fun onPrimaryFixedVariant(): DynamicColor {
        return colorSpec.onPrimaryFixedVariant()
    }

    // ////////////////////////////////////////////////////////////////
    // Secondaries [Q] //
    // ////////////////////////////////////////////////////////////////
    fun secondary(): DynamicColor {
        return colorSpec.secondary()
    }

    fun secondaryDim(): DynamicColor? {
        return colorSpec.secondaryDim()
    }

    fun onSecondary(): DynamicColor {
        return colorSpec.onSecondary()
    }

    fun secondaryContainer(): DynamicColor {
        return colorSpec.secondaryContainer()
    }

    fun onSecondaryContainer(): DynamicColor {
        return colorSpec.onSecondaryContainer()
    }

    // ///////////////////////////////////////////////////////////////
    // Secondary Fixed Colors [QF] //
    // ///////////////////////////////////////////////////////////////
    fun secondaryFixed(): DynamicColor {
        return colorSpec.secondaryFixed()
    }

    fun secondaryFixedDim(): DynamicColor {
        return colorSpec.secondaryFixedDim()
    }

    fun onSecondaryFixed(): DynamicColor {
        return colorSpec.onSecondaryFixed()
    }

    fun onSecondaryFixedVariant(): DynamicColor {
        return colorSpec.onSecondaryFixedVariant()
    }

    // ////////////////////////////////////////////////////////////////
    // Tertiaries [T] //
    // ////////////////////////////////////////////////////////////////
    fun tertiary(): DynamicColor {
        return colorSpec.tertiary()
    }

    fun tertiaryDim(): DynamicColor? {
        return colorSpec.tertiaryDim()
    }

    fun onTertiary(): DynamicColor {
        return colorSpec.onTertiary()
    }

    fun tertiaryContainer(): DynamicColor {
        return colorSpec.tertiaryContainer()
    }

    fun onTertiaryContainer(): DynamicColor {
        return colorSpec.onTertiaryContainer()
    }

    // ///////////////////////////////////////////////////////////////
    // Tertiary Fixed Colors [TF] //
    // ///////////////////////////////////////////////////////////////
    fun tertiaryFixed(): DynamicColor {
        return colorSpec.tertiaryFixed()
    }

    fun tertiaryFixedDim(): DynamicColor {
        return colorSpec.tertiaryFixedDim()
    }

    fun onTertiaryFixed(): DynamicColor {
        return colorSpec.onTertiaryFixed()
    }

    fun onTertiaryFixedVariant(): DynamicColor {
        return colorSpec.onTertiaryFixedVariant()
    }

    // ////////////////////////////////////////////////////////////////
    // Errors [E] //
    // ////////////////////////////////////////////////////////////////
    fun error(): DynamicColor {
        return colorSpec.error()
    }

    fun errorDim(): DynamicColor? {
        return colorSpec.errorDim()
    }

    fun onError(): DynamicColor {
        return colorSpec.onError()
    }

    fun errorContainer(): DynamicColor {
        return colorSpec.errorContainer()
    }

    fun onErrorContainer(): DynamicColor {
        return colorSpec.onErrorContainer()
    }

    // ////////////////////////////////////////////////////////////////
    // Android-only colors //
    // ////////////////////////////////////////////////////////////////
    /**
     * These colors were present in Android framework before Android U, and used by MDC controls. They
     * should be avoided, if possible. It's unclear if they're used on multiple backgrounds, and if
     * they are, they can't be adjusted for contrast.* For now, they will be set with no background,
     * and those won't adjust for contrast, avoiding issues.
     * * For example, if the same color is on a white background _and_ black background, there's no
     *   way to increase contrast with either without losing contrast with the other.
     */
    // colorControlActivated documented as colorAccent in M3 & GM3.
    // colorAccent documented as colorSecondary in M3 and colorPrimary in GM3.
    // Android used Material's Container as Primary/Secondary/Tertiary at launch.
    // Therefore, this is a duplicated version of Primary Container.
    fun controlActivated(): DynamicColor {
        return colorSpec.controlActivated()
    }

    // colorControlNormal documented as textColorSecondary in M3 & GM3.
    // In Material, textColorSecondary points to onSurfaceVariant in the non-disabled state,
    // which is Neutral Variant T30/80 in light/dark.
    fun controlNormal(): DynamicColor {
        return colorSpec.controlNormal()
    }

    // colorControlHighlight documented, in both M3 & GM3:
    // Light mode: #1f000000 dark mode: #33ffffff.
    // These are black and white with some alpha.
    // 1F hex = 31 decimal; 31 / 255 = 12% alpha.
    // 33 hex = 51 decimal; 51 / 255 = 20% alpha.
    // DynamicColors do not support alpha currently, and _may_ not need it for this use case,
    // depending on how MDC resolved alpha for the other cases.
    // Returning black in dark mode, white in light mode.
    fun controlHighlight(): DynamicColor {
        return colorSpec.controlHighlight()
    }

    // textColorPrimaryInverse documented, in both M3 & GM3, documented as N10/N90.
    fun textPrimaryInverse(): DynamicColor {
        return colorSpec.textPrimaryInverse()
    }

    // textColorSecondaryInverse and textColorTertiaryInverse both documented, in both M3 & GM3, as
    // NV30/NV80
    fun textSecondaryAndTertiaryInverse(): DynamicColor {
        return colorSpec.textSecondaryAndTertiaryInverse()
    }

    // textColorPrimaryInverseDisableOnly documented, in both M3 & GM3, as N10/N90
    fun textPrimaryInverseDisableOnly(): DynamicColor {
        return colorSpec.textPrimaryInverseDisableOnly()
    }

    // textColorSecondaryInverse and textColorTertiaryInverse in disabled state both documented,
    // in both M3 & GM3, as N10/N90
    fun textSecondaryAndTertiaryInverseDisabled(): DynamicColor {
        return colorSpec.textSecondaryAndTertiaryInverseDisabled()
    }

    // textColorHintInverse documented, in both M3 & GM3, as N10/N90
    fun textHintInverse(): DynamicColor {
        return colorSpec.textHintInverse()
    }

    // ////////////////////////////////////////////////////////////////
    // All Colors //
    // ////////////////////////////////////////////////////////////////
    /** All dynamic colors in Material Design system. */
    val allDynamicColors: List<() -> DynamicColor?> by lazy {
        listOf(
            this::primaryPaletteKeyColor,
            this::secondaryPaletteKeyColor,
            this::tertiaryPaletteKeyColor,
            this::neutralPaletteKeyColor,
            this::neutralVariantPaletteKeyColor,
            this::errorPaletteKeyColor,
            this::background,
            this::onBackground,
            this::surface,
            this::surfaceDim,
            this::surfaceBright,
            this::surfaceContainerLowest,
            this::surfaceContainerLow,
            this::surfaceContainer,
            this::surfaceContainerHigh,
            this::surfaceContainerHighest,
            this::onSurface,
            this::surfaceVariant,
            this::onSurfaceVariant,
            this::outline,
            this::outlineVariant,
            this::inverseSurface,
            this::inverseOnSurface,
            this::shadow,
            this::scrim,
            this::surfaceTint,
            this::primary,
            this::primaryDim,
            this::onPrimary,
            this::primaryContainer,
            this::onPrimaryContainer,
            this::primaryFixed,
            this::primaryFixedDim,
            this::onPrimaryFixed,
            this::onPrimaryFixedVariant,
            this::inversePrimary,
            this::secondary,
            this::secondaryDim,
            this::onSecondary,
            this::secondaryContainer,
            this::onSecondaryContainer,
            this::secondaryFixed,
            this::secondaryFixedDim,
            this::onSecondaryFixed,
            this::onSecondaryFixedVariant,
            this::tertiary,
            this::tertiaryDim,
            this::onTertiary,
            this::tertiaryContainer,
            this::onTertiaryContainer,
            this::tertiaryFixed,
            this::tertiaryFixedDim,
            this::onTertiaryFixed,
            this::onTertiaryFixedVariant,
            this::error,
            this::errorDim,
            this::onError,
            this::errorContainer,
            this::onErrorContainer,
            this::controlActivated,
            this::controlNormal,
            this::controlHighlight,
            this::textPrimaryInverse,
            this::textSecondaryAndTertiaryInverse,
            this::textPrimaryInverseDisableOnly,
            this::textSecondaryAndTertiaryInverseDisabled,
            this::textHintInverse,
        )
    }
}
