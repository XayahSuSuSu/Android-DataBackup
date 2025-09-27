/*
 * Copyright 2022 Google LLC
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
import com.xayah.databackup.ui.theme.color.utils.MathUtils
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.min

/**
 * Provides important settings for creating colors dynamically, and 6 color palettes. Requires: 1. A
 * color. (source color) 2. A theme. (Variant) 3. Whether or not its dark mode. 4. Contrast level.
 * (-1 to 1, currently contrast ratio 3.0 and 7.0)
 */
open class DynamicScheme(
    /** The source color of the scheme in HCT format. */
    val sourceColorHct: Hct,
    /** The variant of the scheme. */
    val variant: Variant,
    /** Whether or not the scheme is dark mode. */
    val isDark: Boolean,
    /**
     * Value from -1 to 1. -1 represents minimum contrast. 0 represents standard (i.e. the design as
     * spec'd), and 1 represents maximum contrast.
     */
    val contrastLevel: Double,
    /** The platform on which this scheme is intended to be used. */
    val platform: Platform = DEFAULT_PLATFORM,
    /** The spec version of the scheme. */
    val specVersion: ColorSpec.SpecVersion = DEFAULT_SPEC_VERSION,
    val primaryPalette: TonalPalette,
    val secondaryPalette: TonalPalette,
    val tertiaryPalette: TonalPalette,
    val neutralPalette: TonalPalette,
    val neutralVariantPalette: TonalPalette,
    val errorPalette: TonalPalette,
) {

    /** The source color of the scheme in ARGB format. */
    val sourceColorArgb: Int = sourceColorHct.toInt()

    /** The platform on which this scheme is intended to be used. */
    enum class Platform {
        PHONE,
        WATCH,
    }

    fun getHct(dynamicColor: DynamicColor): Hct {
        return dynamicColor.getHct(this)
    }

    fun getArgb(dynamicColor: DynamicColor): Int {
        return dynamicColor.getArgb(this)
    }

    override fun toString(): String {
        return "Scheme: variant=${variant.name}, mode=${if (isDark) "dark" else "light"}, platform=${
            platform.name.lowercase(
                Locale.ENGLISH
            )
        }, contrastLevel=${DecimalFormat("0.0").format(contrastLevel)}, seed=$sourceColorHct, specVersion=$specVersion"
    }

    val primaryPaletteKeyColor: Int
        get() = getArgb(MaterialDynamicColors().primaryPaletteKeyColor())

    val secondaryPaletteKeyColor: Int
        get() = getArgb(MaterialDynamicColors().secondaryPaletteKeyColor())

    val tertiaryPaletteKeyColor: Int
        get() = getArgb(MaterialDynamicColors().tertiaryPaletteKeyColor())

    val neutralPaletteKeyColor: Int
        get() = getArgb(MaterialDynamicColors().neutralPaletteKeyColor())

    val neutralVariantPaletteKeyColor: Int
        get() = getArgb(MaterialDynamicColors().neutralVariantPaletteKeyColor())

    val background: Int
        get() = getArgb(MaterialDynamicColors().background())

    val onBackground: Int
        get() = getArgb(MaterialDynamicColors().onBackground())

    val surface: Int
        get() = getArgb(MaterialDynamicColors().surface())

    val surfaceDim: Int
        get() = getArgb(MaterialDynamicColors().surfaceDim())

    val surfaceBright: Int
        get() = getArgb(MaterialDynamicColors().surfaceBright())

    val surfaceContainerLowest: Int
        get() = getArgb(MaterialDynamicColors().surfaceContainerLowest())

    val surfaceContainerLow: Int
        get() = getArgb(MaterialDynamicColors().surfaceContainerLow())

    val surfaceContainer: Int
        get() = getArgb(MaterialDynamicColors().surfaceContainer())

    val surfaceContainerHigh: Int
        get() = getArgb(MaterialDynamicColors().surfaceContainerHigh())

    val surfaceContainerHighest: Int
        get() = getArgb(MaterialDynamicColors().surfaceContainerHighest())

    val onSurface: Int
        get() = getArgb(MaterialDynamicColors().onSurface())

    val surfaceVariant: Int
        get() = getArgb(MaterialDynamicColors().surfaceVariant())

    val onSurfaceVariant: Int
        get() = getArgb(MaterialDynamicColors().onSurfaceVariant())

    val inverseSurface: Int
        get() = getArgb(MaterialDynamicColors().inverseSurface())

    val inverseOnSurface: Int
        get() = getArgb(MaterialDynamicColors().inverseOnSurface())

    val outline: Int
        get() = getArgb(MaterialDynamicColors().outline())

    val outlineVariant: Int
        get() = getArgb(MaterialDynamicColors().outlineVariant())

    val shadow: Int
        get() = getArgb(MaterialDynamicColors().shadow())

    val scrim: Int
        get() = getArgb(MaterialDynamicColors().scrim())

    val surfaceTint: Int
        get() = getArgb(MaterialDynamicColors().surfaceTint())

    val primary: Int
        get() = getArgb(MaterialDynamicColors().primary())

    val onPrimary: Int
        get() = getArgb(MaterialDynamicColors().onPrimary())

    val primaryContainer: Int
        get() = getArgb(MaterialDynamicColors().primaryContainer())

    val onPrimaryContainer: Int
        get() = getArgb(MaterialDynamicColors().onPrimaryContainer())

    val inversePrimary: Int
        get() = getArgb(MaterialDynamicColors().inversePrimary())

    val secondary: Int
        get() = getArgb(MaterialDynamicColors().secondary())

    val onSecondary: Int
        get() = getArgb(MaterialDynamicColors().onSecondary())

    val secondaryContainer: Int
        get() = getArgb(MaterialDynamicColors().secondaryContainer())

    val onSecondaryContainer: Int
        get() = getArgb(MaterialDynamicColors().onSecondaryContainer())

    val tertiary: Int
        get() = getArgb(MaterialDynamicColors().tertiary())

    val onTertiary: Int
        get() = getArgb(MaterialDynamicColors().onTertiary())

    val tertiaryContainer: Int
        get() = getArgb(MaterialDynamicColors().tertiaryContainer())

    val onTertiaryContainer: Int
        get() = getArgb(MaterialDynamicColors().onTertiaryContainer())

    val error: Int
        get() = getArgb(MaterialDynamicColors().error())

    val onError: Int
        get() = getArgb(MaterialDynamicColors().onError())

    val errorContainer: Int
        get() = getArgb(MaterialDynamicColors().errorContainer())

    val onErrorContainer: Int
        get() = getArgb(MaterialDynamicColors().onErrorContainer())

    val primaryFixed: Int
        get() = getArgb(MaterialDynamicColors().primaryFixed())

    val primaryFixedDim: Int
        get() = getArgb(MaterialDynamicColors().primaryFixedDim())

    val onPrimaryFixed: Int
        get() = getArgb(MaterialDynamicColors().onPrimaryFixed())

    val onPrimaryFixedVariant: Int
        get() = getArgb(MaterialDynamicColors().onPrimaryFixedVariant())

    val secondaryFixed: Int
        get() = getArgb(MaterialDynamicColors().secondaryFixed())

    val secondaryFixedDim: Int
        get() = getArgb(MaterialDynamicColors().secondaryFixedDim())

    val onSecondaryFixed: Int
        get() = getArgb(MaterialDynamicColors().onSecondaryFixed())

    val onSecondaryFixedVariant: Int
        get() = getArgb(MaterialDynamicColors().onSecondaryFixedVariant())

    val tertiaryFixed: Int
        get() = getArgb(MaterialDynamicColors().tertiaryFixed())

    val tertiaryFixedDim: Int
        get() = getArgb(MaterialDynamicColors().tertiaryFixedDim())

    val onTertiaryFixed: Int
        get() = getArgb(MaterialDynamicColors().onTertiaryFixed())

    val onTertiaryFixedVariant: Int
        get() = getArgb(MaterialDynamicColors().onTertiaryFixedVariant())

    val controlActivated: Int
        get() = getArgb(MaterialDynamicColors().controlActivated())

    val controlNormal: Int
        get() = getArgb(MaterialDynamicColors().controlNormal())

    val controlHighlight: Int
        get() = getArgb(MaterialDynamicColors().controlHighlight())

    val textPrimaryInverse: Int
        get() = getArgb(MaterialDynamicColors().textPrimaryInverse())

    val textSecondaryAndTertiaryInverse: Int
        get() = getArgb(MaterialDynamicColors().textSecondaryAndTertiaryInverse())

    val textPrimaryInverseDisableOnly: Int
        get() = getArgb(MaterialDynamicColors().textPrimaryInverseDisableOnly())

    val textSecondaryAndTertiaryInverseDisabled: Int
        get() = getArgb(MaterialDynamicColors().textSecondaryAndTertiaryInverseDisabled())

    val textHintInverse: Int
        get() = getArgb(MaterialDynamicColors().textHintInverse())

    companion object {
        val DEFAULT_SPEC_VERSION = ColorSpec.SpecVersion.SPEC_2021
        val DEFAULT_PLATFORM = Platform.PHONE

        @JvmStatic
        fun from(other: DynamicScheme, isDark: Boolean): DynamicScheme {
            return from(other, isDark, other.contrastLevel)
        }

        @JvmStatic
        fun from(other: DynamicScheme, isDark: Boolean, contrastLevel: Double): DynamicScheme {
            return DynamicScheme(
                other.sourceColorHct,
                other.variant,
                isDark,
                contrastLevel,
                other.platform,
                other.specVersion,
                other.primaryPalette,
                other.secondaryPalette,
                other.tertiaryPalette,
                other.neutralPalette,
                other.neutralVariantPalette,
                other.errorPalette,
            )
        }

        /**
         * Returns a new hue based on a piecewise function and input color hue.
         *
         * For example, for the following function:
         * ```
         * result = 26, if 0 <= hue < 101;
         * result = 39, if 101 <= hue < 210;
         * result = 28, if 210 <= hue < 360.
         * ```
         *
         * call the function as:
         * ```
         * double[] hueBreakpoints = {0, 101, 210, 360};
         * double[] hues = {26, 39, 28};
         * double result = scheme.piecewise(sourceColor, hueBreakpoints, hues);
         * ```
         *
         * @param sourceColorHct The input value.
         * @param hueBreakpoints The breakpoints, in sorted order. No default lower or upper bounds are
         *   assumed.
         * @param hues The hues that should be applied when source color's hue is >= the same index in
         *   hueBreakpoints array, and < the hue at the next index in hueBreakpoints array. Otherwise,
         *   the source color's hue is returned.
         */
        @JvmStatic
        fun getPiecewiseValue(
            sourceColorHct: Hct,
            hueBreakpoints: DoubleArray,
            hues: DoubleArray,
        ): Double {
            val size = min(hueBreakpoints.size - 1, hues.size)
            val sourceHue = sourceColorHct.hue
            for (i in 0 until size) {
                if (sourceHue >= hueBreakpoints[i] && sourceHue < hueBreakpoints[i + 1]) {
                    return MathUtils.sanitizeDegreesDouble(hues[i])
                }
            }
            // No condition matched, return the source value.
            return sourceHue
        }

        /**
         * Returns a shifted hue based on a piecewise function and input color hue.
         *
         * For example, for the following function:
         * ```
         * result = hue + 26, if 0 <= hue < 101;
         * result = hue - 39, if 101 <= hue < 210;
         * result = hue + 28, if 210 <= hue < 360.
         * ```
         *
         * call the function as:
         * ```
         * double[] hueBreakpoints = {0, 101, 210, 360};
         * double[] rotations = {26, -39, 28};
         * double result = scheme.getRotatedHue(sourceColor, hueBreakpoints, rotations);
         * ```
         *
         * @param sourceColorHct the source color of the theme, in HCT.
         * @param hueBreakpoints The "breakpoints", i.e. the hues at which a rotation should be apply.
         *   No default lower or upper bounds are assumed.
         * @param rotations The rotation that should be applied when source color's hue is >= the same
         *   index in hues array, and < the hue at the next index in hues array. Otherwise, the source
         *   color's hue is returned.
         */
        @JvmStatic
        fun getRotatedHue(
            sourceColorHct: Hct,
            hueBreakpoints: DoubleArray,
            rotations: DoubleArray,
        ): Double {
            var rotation = getPiecewiseValue(sourceColorHct, hueBreakpoints, rotations)
            if (min(hueBreakpoints.size - 1, rotations.size) <= 0) {
                // No condition matched, return the source hue.
                rotation = 0.0
            }
            return MathUtils.sanitizeDegreesDouble(sourceColorHct.hue + rotation)
        }
    }
}
