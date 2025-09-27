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

import com.xayah.databackup.ui.theme.color.contrast.Contrast
import com.xayah.databackup.ui.theme.color.dislike.DislikeAnalyzer
import com.xayah.databackup.ui.theme.color.hct.Hct
import com.xayah.databackup.ui.theme.color.palettes.TonalPalette
import com.xayah.databackup.ui.theme.color.temperature.TemperatureCache
import com.xayah.databackup.ui.theme.color.utils.MathUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** [ColorSpec] implementation for the 2021 spec. */
open class ColorSpec2021 : ColorSpec {
    // ////////////////////////////////////////////////////////////////
    // Main Palettes //
    // ////////////////////////////////////////////////////////////////
    override fun primaryPaletteKeyColor(): DynamicColor {
        return DynamicColor.Builder()
            .setName("primary_palette_key_color")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme -> s.primaryPalette.keyColor.tone }
            .build()
    }

    override fun secondaryPaletteKeyColor(): DynamicColor {
        return DynamicColor.Builder()
            .setName("secondary_palette_key_color")
            .setPalette { s: DynamicScheme -> s.secondaryPalette }
            .setTone { s: DynamicScheme -> s.secondaryPalette.keyColor.tone }
            .build()
    }

    override fun tertiaryPaletteKeyColor(): DynamicColor {
        return DynamicColor.Builder()
            .setName("tertiary_palette_key_color")
            .setPalette { s: DynamicScheme -> s.tertiaryPalette }
            .setTone { s: DynamicScheme -> s.tertiaryPalette.keyColor.tone }
            .build()
    }

    override fun neutralPaletteKeyColor(): DynamicColor {
        return DynamicColor.Builder()
            .setName("neutral_palette_key_color")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> s.neutralPalette.keyColor.tone }
            .build()
    }

    override fun neutralVariantPaletteKeyColor(): DynamicColor {
        return DynamicColor.Builder()
            .setName("neutral_variant_palette_key_color")
            .setPalette { s: DynamicScheme -> s.neutralVariantPalette }
            .setTone { s: DynamicScheme -> s.neutralVariantPalette.keyColor.tone }
            .build()
    }

    override fun errorPaletteKeyColor(): DynamicColor {
        return DynamicColor.Builder()
            .setName("error_palette_key_color")
            .setPalette { s: DynamicScheme -> s.errorPalette }
            .setTone { s: DynamicScheme -> s.errorPalette.keyColor.tone }
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Surfaces [S] //
    // ////////////////////////////////////////////////////////////////
    override fun background(): DynamicColor {
        return DynamicColor.Builder()
            .setName("background")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 6.0 else 98.0 }
            .setIsBackground(true)
            .build()
    }

    override fun onBackground(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_background")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 90.0 else 10.0 }
            .setBackground { s: DynamicScheme -> background() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 3.0, 4.5, 7.0) }
            .build()
    }

    override fun surface(): DynamicColor {
        return DynamicColor.Builder()
            .setName("surface")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 6.0 else 98.0 }
            .setIsBackground(true)
            .build()
    }

    override fun surfaceDim(): DynamicColor {
        return DynamicColor.Builder()
            .setName("surface_dim")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme ->
                if (s.isDark) 6.0 else ContrastCurve(87.0, 87.0, 80.0, 75.0).get(s.contrastLevel)
            }
            .setIsBackground(true)
            .build()
    }

    override fun surfaceBright(): DynamicColor {
        return DynamicColor.Builder()
            .setName("surface_bright")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme ->
                if (s.isDark) ContrastCurve(24.0, 24.0, 29.0, 34.0).get(s.contrastLevel) else 98.0
            }
            .setIsBackground(true)
            .build()
    }

    override fun surfaceContainerLowest(): DynamicColor {
        return DynamicColor.Builder()
            .setName("surface_container_lowest")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme ->
                if (s.isDark) ContrastCurve(4.0, 4.0, 2.0, 0.0).get(s.contrastLevel) else 100.0
            }
            .setIsBackground(true)
            .build()
    }

    override fun surfaceContainerLow(): DynamicColor {
        return DynamicColor.Builder()
            .setName("surface_container_low")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme ->
                if (s.isDark) {
                    ContrastCurve(10.0, 10.0, 11.0, 12.0).get(s.contrastLevel)
                } else {
                    ContrastCurve(96.0, 96.0, 96.0, 95.0).get(s.contrastLevel)
                }
            }
            .setIsBackground(true)
            .build()
    }

    override fun surfaceContainer(): DynamicColor {
        return DynamicColor.Builder()
            .setName("surface_container")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme ->
                if (s.isDark) {
                    ContrastCurve(12.0, 12.0, 16.0, 20.0).get(s.contrastLevel)
                } else {
                    ContrastCurve(94.0, 94.0, 92.0, 90.0).get(s.contrastLevel)
                }
            }
            .setIsBackground(true)
            .build()
    }

    override fun surfaceContainerHigh(): DynamicColor {
        return DynamicColor.Builder()
            .setName("surface_container_high")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme ->
                if (s.isDark) {
                    ContrastCurve(17.0, 17.0, 21.0, 25.0).get(s.contrastLevel)
                } else {
                    ContrastCurve(92.0, 92.0, 88.0, 85.0).get(s.contrastLevel)
                }
            }
            .setIsBackground(true)
            .build()
    }

    override fun surfaceContainerHighest(): DynamicColor {
        return DynamicColor.Builder()
            .setName("surface_container_highest")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme ->
                if (s.isDark) {
                    ContrastCurve(22.0, 22.0, 26.0, 30.0).get(s.contrastLevel)
                } else {
                    ContrastCurve(90.0, 90.0, 84.0, 80.0).get(s.contrastLevel)
                }
            }
            .setIsBackground(true)
            .build()
    }

    override fun onSurface(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_surface")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 90.0 else 10.0 }
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()
    }

    override fun surfaceVariant(): DynamicColor {
        return DynamicColor.Builder()
            .setName("surface_variant")
            .setPalette { s: DynamicScheme -> s.neutralVariantPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 30.0 else 90.0 }
            .setIsBackground(true)
            .build()
    }

    override fun onSurfaceVariant(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_surface_variant")
            .setPalette { s: DynamicScheme -> s.neutralVariantPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 80.0 else 30.0 }
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()
    }

    override fun inverseSurface(): DynamicColor {
        return DynamicColor.Builder()
            .setName("inverse_surface")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 90.0 else 20.0 }
            .setIsBackground(true)
            .build()
    }

    override fun inverseOnSurface(): DynamicColor {
        return DynamicColor.Builder()
            .setName("inverse_on_surface")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 20.0 else 95.0 }
            .setBackground { s: DynamicScheme -> inverseSurface() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()
    }

    override fun outline(): DynamicColor {
        return DynamicColor.Builder()
            .setName("outline")
            .setPalette { s: DynamicScheme -> s.neutralVariantPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 60.0 else 50.0 }
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(1.5, 3.0, 4.5, 7.0) }
            .build()
    }

    override fun outlineVariant(): DynamicColor {
        return DynamicColor.Builder()
            .setName("outline_variant")
            .setPalette { s: DynamicScheme -> s.neutralVariantPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 30.0 else 80.0 }
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .build()
    }

    override fun shadow(): DynamicColor {
        return DynamicColor.Builder()
            .setName("shadow")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> 0.0 }
            .build()
    }

    override fun scrim(): DynamicColor {
        return DynamicColor.Builder()
            .setName("scrim")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> 0.0 }
            .build()
    }

    override fun surfaceTint(): DynamicColor {
        return DynamicColor.Builder()
            .setName("surface_tint")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 80.0 else 40.0 }
            .setIsBackground(true)
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Primaries [P] //
    // ////////////////////////////////////////////////////////////////
    override fun primary(): DynamicColor {
        return DynamicColor.Builder()
            .setName("primary")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme ->
                if (isMonochrome(s)) {
                    if (s.isDark) 100.0 else 0.0
                } else {
                    if (s.isDark) 80.0 else 40.0
                }
            }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 7.0) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(primaryContainer(), primary(), 10.0, TonePolarity.NEARER, false)
            }
            .build()
    }

    override fun primaryDim(): DynamicColor? {
        return null
    }

    override fun onPrimary(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_primary")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme ->
                if (isMonochrome(s)) {
                    if (s.isDark) 10.0 else 90.0
                } else {
                    if (s.isDark) 20.0 else 100.0
                }
            }
            .setBackground { s: DynamicScheme -> primary() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()
    }

    override fun primaryContainer(): DynamicColor {
        return DynamicColor.Builder()
            .setName("primary_container")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme ->
                if (isFidelity(s)) {
                    s.sourceColorHct.tone
                } else if (isMonochrome(s)) {
                    if (s.isDark) 85.0 else 25.0
                } else {
                    if (s.isDark) 30.0 else 90.0
                }
            }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(primaryContainer(), primary(), 10.0, TonePolarity.NEARER, false)
            }
            .build()
    }

    override fun onPrimaryContainer(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_primary_container")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme ->
                if (isFidelity(s)) {
                    DynamicColor.foregroundTone(primaryContainer().tone.invoke(s), 4.5)
                } else if (isMonochrome(s)) {
                    if (s.isDark) 0.0 else 100.0
                } else {
                    if (s.isDark) 90.0 else 30.0
                }
            }
            .setBackground { s: DynamicScheme -> primaryContainer() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()
    }

    override fun inversePrimary(): DynamicColor {
        return DynamicColor.Builder()
            .setName("inverse_primary")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 40.0 else 80.0 }
            .setBackground { s: DynamicScheme -> inverseSurface() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 7.0) }
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Secondaries [Q] //
    // ////////////////////////////////////////////////////////////////
    override fun secondary(): DynamicColor {
        return DynamicColor.Builder()
            .setName("secondary")
            .setPalette { s: DynamicScheme -> s.secondaryPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 80.0 else 40.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 7.0) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(secondaryContainer(), secondary(), 10.0, TonePolarity.NEARER, false)
            }
            .build()
    }

    override fun secondaryDim(): DynamicColor? {
        return null
    }

    override fun onSecondary(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_secondary")
            .setPalette { s: DynamicScheme -> s.secondaryPalette }
            .setTone { s: DynamicScheme ->
                if (isMonochrome(s)) {
                    if (s.isDark) 10.0 else 100.0
                } else {
                    if (s.isDark) 20.0 else 100.0
                }
            }
            .setBackground { s: DynamicScheme -> secondary() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()
    }

    override fun secondaryContainer(): DynamicColor {
        return DynamicColor.Builder()
            .setName("secondary_container")
            .setPalette { s: DynamicScheme -> s.secondaryPalette }
            .setTone { s: DynamicScheme ->
                val initialTone = if (s.isDark) 30.0 else 90.0
                if (isMonochrome(s)) {
                    if (s.isDark) 30.0 else 85.0
                } else if (!isFidelity(s)) {
                    initialTone
                } else {
                    findDesiredChromaByTone(
                        s.secondaryPalette.hue,
                        s.secondaryPalette.chroma,
                        initialTone,
                        !s.isDark,
                    )
                }
            }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(secondaryContainer(), secondary(), 10.0, TonePolarity.NEARER, false)
            }
            .build()
    }

    override fun onSecondaryContainer(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_secondary_container")
            .setPalette { s: DynamicScheme -> s.secondaryPalette }
            .setTone { s: DynamicScheme ->
                if (isMonochrome(s)) {
                    if (s.isDark) 90.0 else 10.0
                } else if (!isFidelity(s)) {
                    if (s.isDark) 90.0 else 30.0
                } else {
                    DynamicColor.foregroundTone(secondaryContainer().tone.invoke(s), 4.5)
                }
            }
            .setBackground { s: DynamicScheme -> secondaryContainer() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Tertiaries [T] //
    // ////////////////////////////////////////////////////////////////
    override fun tertiary(): DynamicColor {
        return DynamicColor.Builder()
            .setName("tertiary")
            .setPalette { s: DynamicScheme -> s.tertiaryPalette }
            .setTone { s: DynamicScheme ->
                if (isMonochrome(s)) {
                    if (s.isDark) 90.0 else 25.0
                } else {
                    if (s.isDark) 80.0 else 40.0
                }
            }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 7.0) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(tertiaryContainer(), tertiary(), 10.0, TonePolarity.NEARER, false)
            }
            .build()
    }

    override fun tertiaryDim(): DynamicColor? {
        return null
    }

    override fun onTertiary(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_tertiary")
            .setPalette { s: DynamicScheme -> s.tertiaryPalette }
            .setTone { s: DynamicScheme ->
                if (isMonochrome(s)) {
                    if (s.isDark) 10.0 else 90.0
                } else {
                    if (s.isDark) 20.0 else 100.0
                }
            }
            .setBackground { s: DynamicScheme -> tertiary() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()
    }

    override fun tertiaryContainer(): DynamicColor {
        return DynamicColor.Builder()
            .setName("tertiary_container")
            .setPalette { s: DynamicScheme -> s.tertiaryPalette }
            .setTone { s: DynamicScheme ->
                if (isMonochrome(s)) {
                    if (s.isDark) 60.0 else 49.0
                } else if (!isFidelity(s)) {
                    if (s.isDark) 30.0 else 90.0
                } else {
                    val proposedHct = s.tertiaryPalette.getHct(s.sourceColorHct.tone)
                    DislikeAnalyzer.fixIfDisliked(proposedHct).tone
                }
            }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(tertiaryContainer(), tertiary(), 10.0, TonePolarity.NEARER, false)
            }
            .build()
    }

    override fun onTertiaryContainer(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_tertiary_container")
            .setPalette { s: DynamicScheme -> s.tertiaryPalette }
            .setTone { s: DynamicScheme ->
                if (isMonochrome(s)) {
                    if (s.isDark) 0.0 else 100.0
                } else if (!isFidelity(s)) {
                    if (s.isDark) 90.0 else 30.0
                } else {
                    DynamicColor.foregroundTone(tertiaryContainer().tone.invoke(s), 4.5)
                }
            }
            .setBackground { s: DynamicScheme -> tertiaryContainer() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Errors [E] //
    // ////////////////////////////////////////////////////////////////
    override fun error(): DynamicColor {
        return DynamicColor.Builder()
            .setName("error")
            .setPalette { s: DynamicScheme -> s.errorPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 80.0 else 40.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 7.0) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(errorContainer(), error(), 10.0, TonePolarity.NEARER, false)
            }
            .build()
    }

    override fun errorDim(): DynamicColor? {
        return null
    }

    override fun onError(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_error")
            .setPalette { s: DynamicScheme -> s.errorPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 20.0 else 100.0 }
            .setBackground { s: DynamicScheme -> error() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()
    }

    override fun errorContainer(): DynamicColor {
        return DynamicColor.Builder()
            .setName("error_container")
            .setPalette { s: DynamicScheme -> s.errorPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 30.0 else 90.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(errorContainer(), error(), 10.0, TonePolarity.NEARER, false)
            }
            .build()
    }

    override fun onErrorContainer(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_error_container")
            .setPalette { s: DynamicScheme -> s.errorPalette }
            .setTone { s: DynamicScheme ->
                if (isMonochrome(s)) {
                    if (s.isDark) 90.0 else 10.0
                } else {
                    if (s.isDark) 90.0 else 30.0
                }
            }
            .setBackground { s: DynamicScheme -> errorContainer() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Primary Fixed Colors [PF] //
    // ////////////////////////////////////////////////////////////////
    override fun primaryFixed(): DynamicColor {
        return DynamicColor.Builder()
            .setName("primary_fixed")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme -> if (isMonochrome(s)) 40.0 else 90.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(primaryFixed(), primaryFixedDim(), 10.0, TonePolarity.LIGHTER, true)
            }
            .build()
    }

    override fun primaryFixedDim(): DynamicColor {
        return DynamicColor.Builder()
            .setName("primary_fixed_dim")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme -> if (isMonochrome(s)) 30.0 else 80.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(primaryFixed(), primaryFixedDim(), 10.0, TonePolarity.LIGHTER, true)
            }
            .build()
    }

    override fun onPrimaryFixed(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_primary_fixed")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme -> if (isMonochrome(s)) 100.0 else 10.0 }
            .setBackground { s: DynamicScheme -> primaryFixedDim() }
            .setSecondBackground { s: DynamicScheme -> primaryFixed() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()
    }

    override fun onPrimaryFixedVariant(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_primary_fixed_variant")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme -> if (isMonochrome(s)) 90.0 else 30.0 }
            .setBackground { s: DynamicScheme -> primaryFixedDim() }
            .setSecondBackground { s: DynamicScheme -> primaryFixed() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Secondary Fixed Colors [QF] //
    // ////////////////////////////////////////////////////////////////
    override fun secondaryFixed(): DynamicColor {
        return DynamicColor.Builder()
            .setName("secondary_fixed")
            .setPalette { s: DynamicScheme -> s.secondaryPalette }
            .setTone { s: DynamicScheme -> if (isMonochrome(s)) 80.0 else 90.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(secondaryFixed(), secondaryFixedDim(), 10.0, TonePolarity.LIGHTER, true)
            }
            .build()
    }

    override fun secondaryFixedDim(): DynamicColor {
        return DynamicColor.Builder()
            .setName("secondary_fixed_dim")
            .setPalette { s: DynamicScheme -> s.secondaryPalette }
            .setTone { s: DynamicScheme -> if (isMonochrome(s)) 70.0 else 80.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(secondaryFixed(), secondaryFixedDim(), 10.0, TonePolarity.LIGHTER, true)
            }
            .build()
    }

    override fun onSecondaryFixed(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_secondary_fixed")
            .setPalette { s: DynamicScheme -> s.secondaryPalette }
            .setTone { s: DynamicScheme -> 10.0 }
            .setBackground { s: DynamicScheme -> secondaryFixedDim() }
            .setSecondBackground { s: DynamicScheme -> secondaryFixed() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()
    }

    override fun onSecondaryFixedVariant(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_secondary_fixed_variant")
            .setPalette { s: DynamicScheme -> s.secondaryPalette }
            .setTone { s: DynamicScheme -> if (isMonochrome(s)) 25.0 else 30.0 }
            .setBackground { s: DynamicScheme -> secondaryFixedDim() }
            .setSecondBackground { s: DynamicScheme -> secondaryFixed() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Tertiary Fixed Colors [TF] //
    // ////////////////////////////////////////////////////////////////
    override fun tertiaryFixed(): DynamicColor {
        return DynamicColor.Builder()
            .setName("tertiary_fixed")
            .setPalette { s: DynamicScheme -> s.tertiaryPalette }
            .setTone { s: DynamicScheme -> if (isMonochrome(s)) 40.0 else 90.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(tertiaryFixed(), tertiaryFixedDim(), 10.0, TonePolarity.LIGHTER, true)
            }
            .build()
    }

    override fun tertiaryFixedDim(): DynamicColor {
        return DynamicColor.Builder()
            .setName("tertiary_fixed_dim")
            .setPalette { s: DynamicScheme -> s.tertiaryPalette }
            .setTone { s: DynamicScheme -> if (isMonochrome(s)) 30.0 else 80.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(tertiaryFixed(), tertiaryFixedDim(), 10.0, TonePolarity.LIGHTER, true)
            }
            .build()
    }

    override fun onTertiaryFixed(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_tertiary_fixed")
            .setPalette { s: DynamicScheme -> s.tertiaryPalette }
            .setTone { s: DynamicScheme -> if (isMonochrome(s)) 100.0 else 10.0 }
            .setBackground { s: DynamicScheme -> tertiaryFixedDim() }
            .setSecondBackground { s: DynamicScheme -> tertiaryFixed() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()
    }

    override fun onTertiaryFixedVariant(): DynamicColor {
        return DynamicColor.Builder()
            .setName("on_tertiary_fixed_variant")
            .setPalette { s: DynamicScheme -> s.tertiaryPalette }
            .setTone { s: DynamicScheme -> if (isMonochrome(s)) 90.0 else 30.0 }
            .setBackground { s: DynamicScheme -> tertiaryFixedDim() }
            .setSecondBackground { s: DynamicScheme -> tertiaryFixed() }
            .setContrastCurve { s: DynamicScheme -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Android-only Colors //
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
    override fun controlActivated(): DynamicColor {
        return DynamicColor.Builder()
            .setName("control_activated")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 30.0 else 90.0 }
            .setIsBackground(true)
            .build()
    }

    // colorControlNormal documented as textColorSecondary in M3 & GM3.
    // In Material, textColorSecondary points to onSurfaceVariant in the non-disabled state,
    // which is Neutral Variant T30/80 in light/dark.
    override fun controlNormal(): DynamicColor {
        return DynamicColor.Builder()
            .setName("control_normal")
            .setPalette { s: DynamicScheme -> s.neutralVariantPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 80.0 else 30.0 }
            .build()
    }

    // colorControlHighlight documented, in both M3 & GM3:
    // Light mode: #1f000000 dark mode: #33ffffff.
    // These are black and white with some alpha.
    // 1F hex = 31 decimal; 31 / 255 = 12% alpha.
    // 33 hex = 51 decimal; 51 / 255 = 20% alpha.
    // DynamicColors do not support alpha currently, and _may_ not need it for this use case,
    // depending on how MDC resolved alpha for the other cases.
    // Returning black in dark mode, white in light mode.
    override fun controlHighlight(): DynamicColor {
        return DynamicColor.Builder()
            .setName("control_highlight")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 100.0 else 0.0 }
            .setOpacity { s: DynamicScheme -> if (s.isDark) 0.20 else 0.12 }
            .build()
    }

    // textColorPrimaryInverse documented, in both M3 & GM3, documented as N10/N90.
    override fun textPrimaryInverse(): DynamicColor {
        return DynamicColor.Builder()
            .setName("text_primary_inverse")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 10.0 else 90.0 }
            .build()
    }

    // textColorSecondaryInverse and textColorTertiaryInverse both documented, in both M3 & GM3, as
    // NV30/NV80
    override fun textSecondaryAndTertiaryInverse(): DynamicColor {
        return DynamicColor.Builder()
            .setName("text_secondary_and_tertiary_inverse")
            .setPalette { s: DynamicScheme -> s.neutralVariantPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 30.0 else 80.0 }
            .build()
    }

    // textColorPrimaryInverseDisableOnly documented, in both M3 & GM3, as N10/N90
    override fun textPrimaryInverseDisableOnly(): DynamicColor {
        return DynamicColor.Builder()
            .setName("text_primary_inverse_disable_only")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 10.0 else 90.0 }
            .build()
    }

    // textColorSecondaryInverse and textColorTertiaryInverse in disabled state both documented,
    // in both M3 & GM3, as N10/N90
    override fun textSecondaryAndTertiaryInverseDisabled(): DynamicColor {
        return DynamicColor.Builder()
            .setName("text_secondary_and_tertiary_inverse_disabled")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 10.0 else 90.0 }
            .build()
    }

    // textColorHintInverse documented, in both M3 & GM3, as N10/N90
    override fun textHintInverse(): DynamicColor {
        return DynamicColor.Builder()
            .setName("text_hint_inverse")
            .setPalette { s: DynamicScheme -> s.neutralPalette }
            .setTone { s: DynamicScheme -> if (s.isDark) 10.0 else 90.0 }
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Other //
    // ////////////////////////////////////////////////////////////////
    override fun highestSurface(s: DynamicScheme): DynamicColor {
        return if (s.isDark) surfaceBright() else surfaceDim()
    }

    private fun isFidelity(scheme: DynamicScheme): Boolean {
        return scheme.variant == Variant.FIDELITY || scheme.variant == Variant.CONTENT
    }

    private fun isMonochrome(scheme: DynamicScheme): Boolean {
        return scheme.variant == Variant.MONOCHROME
    }

    private fun findDesiredChromaByTone(
        hue: Double,
        chroma: Double,
        tone: Double,
        byDecreasingTone: Boolean,
    ): Double {
        var answer = tone
        var closestToChroma = Hct.Companion.from(hue, chroma, tone)
        if (closestToChroma.chroma < chroma) {
            var chromaPeak = closestToChroma.chroma
            while (closestToChroma.chroma < chroma) {
                answer += if (byDecreasingTone) -1.0 else 1.0
                val potentialSolution = Hct.Companion.from(hue, chroma, answer)
                if (chromaPeak > potentialSolution.chroma) {
                    break
                }
                if (abs(potentialSolution.chroma - chroma) < 0.4) {
                    break
                }
                val potentialDelta = abs(potentialSolution.chroma - chroma)
                val currentDelta = abs(closestToChroma.chroma - chroma)
                if (potentialDelta < currentDelta) {
                    closestToChroma = potentialSolution
                }
                chromaPeak = max(chromaPeak, potentialSolution.chroma)
            }
        }
        return answer
    }

    // ///////////////////////////////////////////////////////////////
    // Color value calculations //
    // ///////////////////////////////////////////////////////////////
    override fun getHct(scheme: DynamicScheme, color: DynamicColor): Hct {
        // This is crucial for aesthetics: we aren't simply the taking the standard color
        // and changing its tone for contrast. Rather, we find the tone for contrast, then
        // use the specified chroma from the palette to construct a new color.
        //
        // For example, this enables colors with standard tone of T90, which has limited chroma, to
        // "recover" intended chroma as contrast increases.
        val tone = getTone(scheme, color)
        return color.palette.invoke(scheme).getHct(tone)
    }

    override fun getTone(scheme: DynamicScheme, color: DynamicColor): Double {
        val decreasingContrast = scheme.contrastLevel < 0
        val toneDeltaPair = color.toneDeltaPair?.invoke(scheme)

        // Case 1: dual foreground, pair of colors with delta constraint.
        if (toneDeltaPair != null) {
            val roleA = toneDeltaPair.roleA
            val roleB = toneDeltaPair.roleB
            val delta = toneDeltaPair.delta
            val polarity = toneDeltaPair.polarity
            val stayTogether = toneDeltaPair.stayTogether
            val aIsNearer =
                (polarity == TonePolarity.NEARER ||
                        (polarity == TonePolarity.LIGHTER && !scheme.isDark) ||
                        (polarity == TonePolarity.DARKER && !scheme.isDark))
            val nearer = if (aIsNearer) roleA else roleB
            val farther = if (aIsNearer) roleB else roleA
            val amNearer = color.name == nearer.name
            val expansionDir = if (scheme.isDark) 1 else -1
            var nTone = nearer.tone.invoke(scheme)
            var fTone = farther.tone.invoke(scheme)

            // 1st round: solve to min, each
            val background = color.background
            val nContrastCurve = nearer.contrastCurve?.invoke(scheme)
            val fContrastCurve = farther.contrastCurve?.invoke(scheme)
            if (
                background != null &&
                nearer.contrastCurve != null &&
                farther.contrastCurve != null &&
                nContrastCurve != null &&
                fContrastCurve != null
            ) {
                val bg = background.invoke(scheme)
                if (bg != null) {
                    val nContrast = nContrastCurve.get(scheme.contrastLevel)
                    val fContrast = fContrastCurve.get(scheme.contrastLevel)
                    val bgTone = bg.getTone(scheme)

                    // If a color is good enough, it is not adjusted.
                    // Initial and adjusted tones for `nearer`
                    if (Contrast.ratioOfTones(bgTone, nTone) < nContrast) {
                        nTone = DynamicColor.foregroundTone(bgTone, nContrast)
                    }
                    // Initial and adjusted tones for `farther`
                    if (Contrast.ratioOfTones(bgTone, fTone) < fContrast) {
                        fTone = DynamicColor.foregroundTone(bgTone, fContrast)
                    }
                    if (decreasingContrast) {
                        // If decreasing contrast, adjust color to the "bare minimum"
                        // that satisfies contrast.
                        nTone = DynamicColor.foregroundTone(bgTone, nContrast)
                        fTone = DynamicColor.foregroundTone(bgTone, fContrast)
                    }
                }
            }

            // If constraint is not satisfied, try another round.
            if ((fTone - nTone) * expansionDir < delta) {
                // 2nd round: expand farther to match delta.
                fTone = MathUtils.clampDouble(0.0, 100.0, nTone + delta * expansionDir)
                // If constraint is not satisfied, try another round.
                if ((fTone - nTone) * expansionDir < delta) {
                    // 3rd round: contract nearer to match delta.
                    nTone = MathUtils.clampDouble(0.0, 100.0, fTone - delta * expansionDir)
                }
            }

            // Avoids the 50-59 awkward zone.
            if (50 <= nTone && nTone < 60) {
                // If `nearer` is in the awkward zone, move it away, together with
                // `farther`.
                if (expansionDir > 0) {
                    nTone = 60.0
                    fTone = max(fTone, nTone + delta * expansionDir)
                } else {
                    nTone = 49.0
                    fTone = min(fTone, nTone + delta * expansionDir)
                }
            } else if (50 <= fTone && fTone < 60) {
                if (stayTogether) {
                    // Fixes both, to avoid two colors on opposite sides of the "awkward
                    // zone".
                    if (expansionDir > 0) {
                        nTone = 60.0
                        fTone = max(fTone, nTone + delta * expansionDir)
                    } else {
                        nTone = 49.0
                        fTone = min(fTone, nTone + delta * expansionDir)
                    }
                } else {
                    // Not required to stay together; fixes just one.
                    if (expansionDir > 0) {
                        fTone = 60.0
                    } else {
                        fTone = 49.0
                    }
                }
            }

            // Returns `nTone` if this color is `nearer`, otherwise `fTone`.
            return if (amNearer) nTone else fTone
        } else {
            // Case 2: No contrast pair; just solve for itself.
            var answer = color.tone.invoke(scheme)
            val background = color.background?.invoke(scheme)
            val contrastCurve = color.contrastCurve?.invoke(scheme)
            if (background == null || contrastCurve == null) {
                return answer // No adjustment for colors with no background.
            }
            val bgTone = background.getTone(scheme)
            val desiredRatio = contrastCurve.get(scheme.contrastLevel)
            if (Contrast.ratioOfTones(bgTone, answer) >= desiredRatio) {
                // Don't "improve" what's good enough.
            } else {
                // Rough improvement.
                answer = DynamicColor.foregroundTone(bgTone, desiredRatio)
            }
            if (decreasingContrast) {
                answer = DynamicColor.foregroundTone(bgTone, desiredRatio)
            }
            if (color.isBackground && 50 <= answer && answer < 60) {
                // Must adjust
                answer =
                    if (Contrast.ratioOfTones(49.0, bgTone) >= desiredRatio) {
                        49.0
                    } else {
                        60.0
                    }
            }
            val secondBackground = color.secondBackground?.invoke(scheme)
            if (secondBackground == null) {
                return answer
            }

            // Case 3: Adjust for dual backgrounds.
            val bgTone1 = background.getTone(scheme)
            val bgTone2 = secondBackground.getTone(scheme)
            val upper = max(bgTone1, bgTone2)
            val lower = min(bgTone1, bgTone2)
            if (
                Contrast.ratioOfTones(upper, answer) >= desiredRatio &&
                Contrast.ratioOfTones(lower, answer) >= desiredRatio
            ) {
                return answer
            }

            // The darkest light tone that satisfies the desired ratio,
            // or -1 if such ratio cannot be reached.
            val lightOption = Contrast.lighter(upper, desiredRatio)

            // The lightest dark tone that satisfies the desired ratio,
            // or -1 if such ratio cannot be reached.
            val darkOption = Contrast.darker(lower, desiredRatio)

            // Tones suitable for the foreground.
            val availables = mutableListOf<Double>()
            if (lightOption != -1.0) {
                availables.add(lightOption)
            }
            if (darkOption != -1.0) {
                availables.add(darkOption)
            }
            val prefersLight =
                DynamicColor.tonePrefersLightForeground(bgTone1) ||
                        DynamicColor.tonePrefersLightForeground(bgTone2)
            if (prefersLight) {
                return if (lightOption == -1.0) 100.0 else lightOption
            }
            return if (availables.size == 1) {
                availables[0]
            } else if (darkOption == -1.0) {
                0.0
            } else {
                darkOption
            }
        }
    }

    // ////////////////////////////////////////////////////////////////
    // Scheme Palettes //
    // ////////////////////////////////////////////////////////////////
    override fun getPrimaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette {
        return when (variant) {
            Variant.CONTENT,
            Variant.FIDELITY -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, sourceColorHct.chroma)

            Variant.FRUIT_SALAD ->
                TonalPalette.Companion.fromHueAndChroma(
                    MathUtils.sanitizeDegreesDouble(sourceColorHct.hue - 50.0),
                    48.0,
                )

            Variant.MONOCHROME -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.NEUTRAL -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 12.0)
            Variant.RAINBOW -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 48.0)
            Variant.TONAL_SPOT -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 36.0)
            Variant.EXPRESSIVE ->
                TonalPalette.Companion.fromHueAndChroma(
                    MathUtils.sanitizeDegreesDouble(sourceColorHct.hue + 240),
                    40.0,
                )

            Variant.VIBRANT -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 200.0)
        }
    }

    override fun getSecondaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette {
        return when (variant) {
            Variant.CONTENT,
            Variant.FIDELITY ->
                TonalPalette.Companion.fromHueAndChroma(
                    sourceColorHct.hue,
                    max(sourceColorHct.chroma - 32.0, sourceColorHct.chroma * 0.5),
                )

            Variant.FRUIT_SALAD ->
                TonalPalette.Companion.fromHueAndChroma(
                    MathUtils.sanitizeDegreesDouble(sourceColorHct.hue - 50.0),
                    36.0,
                )

            Variant.MONOCHROME -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.NEUTRAL -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 8.0)
            Variant.RAINBOW -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 16.0)
            Variant.TONAL_SPOT -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 16.0)
            Variant.EXPRESSIVE ->
                TonalPalette.Companion.fromHueAndChroma(
                    DynamicScheme.getRotatedHue(
                        sourceColorHct,
                        doubleArrayOf(0.0, 21.0, 51.0, 121.0, 151.0, 191.0, 271.0, 321.0, 360.0),
                        doubleArrayOf(45.0, 95.0, 45.0, 20.0, 45.0, 90.0, 45.0, 45.0, 45.0),
                    ),
                    24.0,
                )

            Variant.VIBRANT ->
                TonalPalette.Companion.fromHueAndChroma(
                    DynamicScheme.getRotatedHue(
                        sourceColorHct,
                        doubleArrayOf(0.0, 41.0, 61.0, 101.0, 131.0, 181.0, 251.0, 301.0, 360.0),
                        doubleArrayOf(18.0, 15.0, 10.0, 12.0, 15.0, 18.0, 15.0, 12.0, 12.0),
                    ),
                    24.0,
                )
        }
    }

    override fun getTertiaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette {
        return when (variant) {
            Variant.CONTENT ->
                TonalPalette.Companion.fromHct(
                    DislikeAnalyzer.fixIfDisliked(
                        TemperatureCache(sourceColorHct).getAnalogousColors(count = 3, divisions = 6)[2]
                    )
                )

            Variant.FIDELITY ->
                TonalPalette.Companion.fromHct(
                    DislikeAnalyzer.fixIfDisliked(TemperatureCache(sourceColorHct).complement)
                )

            Variant.FRUIT_SALAD -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 36.0)
            Variant.MONOCHROME -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.NEUTRAL -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 16.0)
            Variant.RAINBOW,
            Variant.TONAL_SPOT ->
                TonalPalette.Companion.fromHueAndChroma(
                    MathUtils.sanitizeDegreesDouble(sourceColorHct.hue + 60.0),
                    24.0,
                )

            Variant.EXPRESSIVE ->
                TonalPalette.Companion.fromHueAndChroma(
                    DynamicScheme.getRotatedHue(
                        sourceColorHct,
                        doubleArrayOf(0.0, 21.0, 51.0, 121.0, 151.0, 191.0, 271.0, 321.0, 360.0),
                        doubleArrayOf(120.0, 120.0, 20.0, 45.0, 20.0, 15.0, 20.0, 120.0, 120.0),
                    ),
                    32.0,
                )

            Variant.VIBRANT ->
                TonalPalette.Companion.fromHueAndChroma(
                    DynamicScheme.getRotatedHue(
                        sourceColorHct,
                        doubleArrayOf(0.0, 41.0, 61.0, 101.0, 131.0, 181.0, 251.0, 301.0, 360.0),
                        doubleArrayOf(35.0, 30.0, 20.0, 25.0, 30.0, 35.0, 30.0, 25.0, 25.0),
                    ),
                    32.0,
                )
        }
    }

    override fun getNeutralPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette {
        return when (variant) {
            Variant.CONTENT,
            Variant.FIDELITY ->
                TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, sourceColorHct.chroma / 8.0)

            Variant.FRUIT_SALAD -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 10.0)
            Variant.MONOCHROME -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.NEUTRAL -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 2.0)
            Variant.RAINBOW -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.TONAL_SPOT -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 6.0)
            Variant.EXPRESSIVE ->
                TonalPalette.Companion.fromHueAndChroma(MathUtils.sanitizeDegreesDouble(sourceColorHct.hue + 15), 8.0)

            Variant.VIBRANT -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 10.0)
        }
    }

    override fun getNeutralVariantPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette {
        return when (variant) {
            Variant.CONTENT,
            Variant.FIDELITY ->
                TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, (sourceColorHct.chroma / 8.0) + 4.0)

            Variant.FRUIT_SALAD -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 16.0)
            Variant.MONOCHROME -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.NEUTRAL -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 2.0)
            Variant.RAINBOW -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.TONAL_SPOT -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 8.0)
            Variant.EXPRESSIVE ->
                TonalPalette.Companion.fromHueAndChroma(
                    MathUtils.sanitizeDegreesDouble(sourceColorHct.hue + 15),
                    12.0,
                )

            Variant.VIBRANT -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 12.0)
        }
    }

    override fun getErrorPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette {
        return TonalPalette.Companion.fromHueAndChroma(25.0, 84.0)
    }
}
