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
import com.xayah.databackup.ui.theme.color.dynamiccolor.ToneDeltaPair.DeltaConstraint
import com.xayah.databackup.ui.theme.color.hct.Hct
import com.xayah.databackup.ui.theme.color.palettes.TonalPalette
import com.xayah.databackup.ui.theme.color.utils.MathUtils
import kotlin.math.max
import kotlin.math.min

/** [ColorSpec] implementation for the 2025 spec. */
class ColorSpec2025 : ColorSpec2021() {
    // ////////////////////////////////////////////////////////////////
    // Surfaces [S] //
    // ////////////////////////////////////////////////////////////////
    override fun background(): DynamicColor {
        // Remapped to surface for 2025 spec.
        val color2025 = surface().toBuilder().setName("background").build()
        return super.background()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onBackground(): DynamicColor {
        // Remapped to onSurface for 2025 spec.
        val color2025Builder = onSurface().toBuilder().setName("on_background")
        color2025Builder.setTone { s: DynamicScheme ->
            if (s.platform == DynamicScheme.Platform.WATCH) 100.0 else onSurface().getTone(s)
        }
        return super.onBackground()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025Builder.build())
            .build()
    }

    override fun surface(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("surface")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setTone { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) {
                            4.0
                        } else {
                            if (Hct.Companion.isYellow(s.neutralPalette.hue)) {
                                99.0
                            } else if (s.variant == Variant.VIBRANT) {
                                97.0
                            } else {
                                98.0
                            }
                        }
                    } else {
                        0.0
                    }
                }
                .setIsBackground(true)
                .build()
        return super.surface()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun surfaceDim(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("surface_dim")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setTone { s: DynamicScheme ->
                    if (s.isDark) {
                        4.0
                    } else {
                        if (Hct.Companion.isYellow(s.neutralPalette.hue)) {
                            90.0
                        } else if (s.variant == Variant.VIBRANT) {
                            85.0
                        } else {
                            87.0
                        }
                    }
                }
                .setIsBackground(true)
                .setChromaMultiplier { s: DynamicScheme ->
                    if (!s.isDark) {
                        if (s.variant == Variant.NEUTRAL) {
                            return@setChromaMultiplier 2.5
                        } else if (s.variant == Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.7
                        } else if (s.variant == Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (Hct.Companion.isYellow(s.neutralPalette.hue)) 2.7 else 1.75
                        } else if (s.variant == Variant.VIBRANT) {
                            return@setChromaMultiplier 1.36
                        }
                    }
                    1.0
                }
                .build()
        return super.surfaceDim()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun surfaceBright(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("surface_bright")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setTone { s: DynamicScheme ->
                    if (s.isDark) {
                        18.0
                    } else {
                        if (Hct.Companion.isYellow(s.neutralPalette.hue)) {
                            99.0
                        } else if (s.variant == Variant.VIBRANT) {
                            97.0
                        } else {
                            98.0
                        }
                    }
                }
                .setIsBackground(true)
                .setChromaMultiplier { s: DynamicScheme ->
                    if (s.isDark) {
                        if (s.variant == Variant.NEUTRAL) {
                            return@setChromaMultiplier 2.5
                        } else if (s.variant == Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.7
                        } else if (s.variant == Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (Hct.Companion.isYellow(s.neutralPalette.hue)) 2.7 else 1.75
                        } else if (s.variant == Variant.VIBRANT) {
                            return@setChromaMultiplier 1.36
                        }
                    }
                    1.0
                }
                .build()
        return super.surfaceBright()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun surfaceContainerLowest(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("surface_container_lowest")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setTone { s: DynamicScheme -> if (s.isDark) 0.0 else 100.0 }
                .setIsBackground(true)
                .build()
        return super.surfaceContainerLowest()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun surfaceContainerLow(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("surface_container_low")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setTone { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) {
                            6.0
                        } else {
                            if (Hct.Companion.isYellow(s.neutralPalette.hue)) {
                                98.0
                            } else if (s.variant == Variant.VIBRANT) {
                                95.0
                            } else {
                                96.0
                            }
                        }
                    } else {
                        15.0
                    }
                }
                .setIsBackground(true)
                .setChromaMultiplier { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.variant == Variant.NEUTRAL) {
                            return@setChromaMultiplier 1.3
                        } else if (s.variant == Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.25
                        } else if (s.variant == Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (Hct.Companion.isYellow(s.neutralPalette.hue)) 1.3 else 1.15
                        } else if (s.variant == Variant.VIBRANT) {
                            return@setChromaMultiplier 1.08
                        }
                    }
                    1.0
                }
                .build()
        return super.surfaceContainerLow()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun surfaceContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("surface_container")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setTone { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) {
                            9.0
                        } else {
                            if (Hct.Companion.isYellow(s.neutralPalette.hue)) {
                                96.0
                            } else if (s.variant == Variant.VIBRANT) {
                                92.0
                            } else {
                                94.0
                            }
                        }
                    } else {
                        20.0
                    }
                }
                .setIsBackground(true)
                .setChromaMultiplier { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.variant == Variant.NEUTRAL) {
                            return@setChromaMultiplier 1.6
                        } else if (s.variant == Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.4
                        } else if (s.variant == Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (Hct.Companion.isYellow(s.neutralPalette.hue)) 1.6 else 1.3
                        } else if (s.variant == Variant.VIBRANT) {
                            return@setChromaMultiplier 1.15
                        }
                    }
                    1.0
                }
                .build()
        return super.surfaceContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun surfaceContainerHigh(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("surface_container_high")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setTone { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) {
                            12.0
                        } else {
                            if (Hct.Companion.isYellow(s.neutralPalette.hue)) {
                                94.0
                            } else if (s.variant == Variant.VIBRANT) {
                                90.0
                            } else {
                                92.0
                            }
                        }
                    } else {
                        25.0
                    }
                }
                .setIsBackground(true)
                .setChromaMultiplier { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.variant == Variant.NEUTRAL) {
                            return@setChromaMultiplier 1.9
                        } else if (s.variant == Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.5
                        } else if (s.variant == Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (Hct.Companion.isYellow(s.neutralPalette.hue)) 1.95 else 1.45
                        } else if (s.variant == Variant.VIBRANT) {
                            return@setChromaMultiplier 1.22
                        }
                    }
                    1.0
                }
                .build()
        return super.surfaceContainerHigh()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun surfaceContainerHighest(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("surface_container_highest")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setTone { s: DynamicScheme ->
                    if (s.isDark) {
                        15.0
                    } else {
                        if (Hct.Companion.isYellow(s.neutralPalette.hue)) {
                            92.0
                        } else if (s.variant == Variant.VIBRANT) {
                            88.0
                        } else {
                            90.0
                        }
                    }
                }
                .setIsBackground(true)
                .setChromaMultiplier { s: DynamicScheme ->
                    if (s.variant == Variant.NEUTRAL) {
                        return@setChromaMultiplier 2.2
                    } else if (s.variant == Variant.TONAL_SPOT) {
                        return@setChromaMultiplier 1.7
                    } else if (s.variant == Variant.EXPRESSIVE) {
                        return@setChromaMultiplier if (Hct.Companion.isYellow(s.neutralPalette.hue)) 2.3 else 1.6
                    } else if (s.variant == Variant.VIBRANT) {
                        return@setChromaMultiplier 1.29
                    }
                    1.0
                }
                .build()
        return super.surfaceContainerHighest()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onSurface(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_surface")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setTone { s: DynamicScheme ->
                    if (s.variant == Variant.VIBRANT) {
                        tMaxC(s.neutralPalette, 0.0, 100.0, 1.1)
                    } else {
                        DynamicColor.getInitialToneFromBackground { scheme: DynamicScheme ->
                            if (scheme.platform == DynamicScheme.Platform.PHONE) {
                                if (scheme.isDark) surfaceBright() else surfaceDim()
                            } else {
                                surfaceContainerHigh()
                            }
                        }
                            .invoke(s)
                    }
                }
                .setChromaMultiplier { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.variant == Variant.NEUTRAL) {
                            return@setChromaMultiplier 2.2
                        } else if (s.variant == Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.7
                        } else if (s.variant == Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (Hct.Companion.isYellow(s.neutralPalette.hue))
                                (if (s.isDark) 3.0 else 2.3)
                            else 1.6
                        }
                    }
                    1.0
                }
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        surfaceContainerHigh()
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.isDark) getContrastCurve(11.0) else getContrastCurve(9.0)
                }
                .build()
        return super.onSurface()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun surfaceVariant(): DynamicColor {
        // Remapped to surfaceContainerHighest for 2025 spec.
        val color2025 = surfaceContainerHighest().toBuilder().setName("surface_variant").build()
        return super.surfaceVariant()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onSurfaceVariant(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_surface_variant")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setChromaMultiplier { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.variant == Variant.NEUTRAL) {
                            return@setChromaMultiplier 2.2
                        } else if (s.variant == Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.7
                        } else if (s.variant == Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (Hct.Companion.isYellow(s.neutralPalette.hue))
                                (if (s.isDark) 3.0 else 2.3)
                            else 1.6
                        }
                    }
                    1.0
                }
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        surfaceContainerHigh()
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        (if (s.isDark) getContrastCurve(6.0) else getContrastCurve(4.5))
                    } else {
                        getContrastCurve(7.0)
                    }
                }
                .build()
        return super.onSurfaceVariant()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun inverseSurface(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("inverse_surface")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setTone { s: DynamicScheme -> if (s.isDark) 98.0 else 4.0 }
                .setIsBackground(true)
                .build()
        return super.inverseSurface()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun inverseOnSurface(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("inverse_on_surface")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setBackground { s: DynamicScheme -> inverseSurface() }
                .setContrastCurve { s: DynamicScheme -> getContrastCurve(7.0) }
                .build()
        return super.inverseOnSurface()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun outline(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("outline")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setChromaMultiplier { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.variant == Variant.NEUTRAL) {
                            return@setChromaMultiplier 2.2
                        } else if (s.variant == Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.7
                        } else if (s.variant == Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (Hct.Companion.isYellow(s.neutralPalette.hue))
                                (if (s.isDark) 3.0 else 2.3)
                            else 1.6
                        }
                    }
                    1.0
                }
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        surfaceContainerHigh()
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(3.0) else getContrastCurve(4.5)
                }
                .build()
        return super.outline()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun outlineVariant(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("outline_variant")
                .setPalette { s: DynamicScheme -> s.neutralPalette }
                .setChromaMultiplier { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.variant == Variant.NEUTRAL) {
                            return@setChromaMultiplier 2.2
                        } else if (s.variant == Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.7
                        } else if (s.variant == Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (Hct.Companion.isYellow(s.neutralPalette.hue))
                                (if (s.isDark) 3.0 else 2.3)
                            else 1.6
                        }
                    }
                    1.0
                }
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        surfaceContainerHigh()
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(1.5) else getContrastCurve(3.0)
                }
                .build()
        return super.outlineVariant()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun surfaceTint(): DynamicColor {
        // Remapped to primary for 2025 spec.
        val color2025 = primary().toBuilder().setName("surface_tint").build()
        return super.surfaceTint()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Primaries [P] //
    // ////////////////////////////////////////////////////////////////
    override fun primary(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("primary")
                .setPalette { s: DynamicScheme -> s.primaryPalette }
                .setTone { s: DynamicScheme ->
                    when {
                        s.variant == Variant.NEUTRAL -> {
                            if (s.platform == DynamicScheme.Platform.PHONE) {
                                if (s.isDark) 80.0 else 40.0
                            } else {
                                90.0
                            }
                        }

                        s.variant == Variant.TONAL_SPOT -> {
                            if (s.platform == DynamicScheme.Platform.PHONE) {
                                if (s.isDark) {
                                    80.0
                                } else {
                                    tMaxC(s.primaryPalette)
                                }
                            } else {
                                tMaxC(s.primaryPalette, 0.0, 90.0)
                            }
                        }

                        s.variant == Variant.EXPRESSIVE -> {
                            tMaxC(
                                s.primaryPalette,
                                0.0,
                                if (Hct.Companion.isYellow(s.primaryPalette.hue)) 25.0
                                else if (Hct.Companion.isCyan(s.primaryPalette.hue)) 88.0 else 98.0,
                            )
                        }

                        else -> { // VIBRANT
                            tMaxC(s.primaryPalette, 0.0, if (Hct.Companion.isCyan(s.primaryPalette.hue)) 88.0 else 98.0)
                        }
                    }
                }
                .setIsBackground(true)
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        surfaceContainerHigh()
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(4.5) else getContrastCurve(7.0)
                }
                .setToneDeltaPair { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        ToneDeltaPair(
                            primaryContainer(),
                            primary(),
                            5.0,
                            TonePolarity.RELATIVE_LIGHTER,
                            DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }
                .build()
        return super.primary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun primaryDim(): DynamicColor {
        return DynamicColor.Builder()
            .setName("primary_dim")
            .setPalette { s: DynamicScheme -> s.primaryPalette }
            .setTone { s: DynamicScheme ->
                if (s.variant == Variant.NEUTRAL) {
                    85.0
                } else if (s.variant == Variant.TONAL_SPOT) {
                    tMaxC(s.primaryPalette, 0.0, 90.0)
                } else {
                    tMaxC(s.primaryPalette)
                }
            }
            .setIsBackground(true)
            .setBackground { s: DynamicScheme -> surfaceContainerHigh() }
            .setContrastCurve { s: DynamicScheme -> getContrastCurve(4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(primaryDim(), primary(), 5.0, TonePolarity.DARKER, DeltaConstraint.FARTHER)
            }
            .build()
    }

    override fun onPrimary(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_primary")
                .setPalette { s: DynamicScheme -> s.primaryPalette }
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) primary() else primaryDim()
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(6.0) else getContrastCurve(7.0)
                }
                .build()
        return super.onPrimary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun primaryContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("primary_container")
                .setPalette { s: DynamicScheme -> s.primaryPalette }
                .setTone { s: DynamicScheme ->
                    when {
                        s.platform == DynamicScheme.Platform.WATCH -> 30.0
                        s.variant == Variant.NEUTRAL -> if (s.isDark) 30.0 else 90.0
                        s.variant == Variant.TONAL_SPOT ->
                            if (s.isDark) tMinC(s.primaryPalette, 35.0, 93.0)
                            else tMaxC(s.primaryPalette, 0.0, 90.0)

                        s.variant == Variant.EXPRESSIVE ->
                            if (s.isDark) tMaxC(s.primaryPalette, 30.0, 93.0)
                            else
                                tMaxC(s.primaryPalette, 78.0, if (Hct.Companion.isCyan(s.primaryPalette.hue)) 88.0 else 90.0)

                        else -> { // VIBRANT
                            if (s.isDark) tMinC(s.primaryPalette, 66.0, 93.0)
                            else
                                tMaxC(s.primaryPalette, 66.0, if (Hct.Companion.isCyan(s.primaryPalette.hue)) 88.0 else 93.0)
                        }
                    }
                }
                .setIsBackground(true)
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        null
                    }
                }
                .setToneDeltaPair { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.WATCH) {
                        ToneDeltaPair(
                            primaryContainer(),
                            primaryDim(),
                            10.0,
                            TonePolarity.DARKER,
                            DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE && s.contrastLevel > 0) getContrastCurve(1.5) else null
                }
                .build()
        return super.primaryContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onPrimaryContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_primary_container")
                .setPalette { s: DynamicScheme -> s.primaryPalette }
                .setBackground { s: DynamicScheme -> primaryContainer() }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(6.0) else getContrastCurve(7.0)
                }
                .build()
        return super.onPrimaryContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun inversePrimary(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("inverse_primary")
                .setPalette { s: DynamicScheme -> s.primaryPalette }
                .setTone { s: DynamicScheme -> tMaxC(s.primaryPalette) }
                .setBackground { s: DynamicScheme -> inverseSurface() }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(6.0) else getContrastCurve(7.0)
                }
                .build()
        return super.inversePrimary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Secondaries [Q] //
    // ////////////////////////////////////////////////////////////////
    override fun secondary(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("secondary")
                .setPalette { s: DynamicScheme -> s.secondaryPalette }
                .setTone { s: DynamicScheme ->
                    when {
                        s.platform == DynamicScheme.Platform.WATCH ->
                            if (s.variant == Variant.NEUTRAL) 90.0 else tMaxC(s.secondaryPalette, 0.0, 90.0)

                        s.variant == Variant.NEUTRAL ->
                            if (s.isDark) tMinC(s.secondaryPalette, 0.0, 98.0) else tMaxC(s.secondaryPalette)

                        s.variant == Variant.VIBRANT ->
                            tMaxC(s.secondaryPalette, 0.0, if (s.isDark) 90.0 else 98.0)

                        else -> { // EXPRESSIVE and TONAL_SPOT
                            if (s.isDark) 80.0 else tMaxC(s.secondaryPalette)
                        }
                    }
                }
                .setIsBackground(true)
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        surfaceContainerHigh()
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(4.5) else getContrastCurve(7.0)
                }
                .setToneDeltaPair { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        ToneDeltaPair(
                            secondaryContainer(),
                            secondary(),
                            5.0,
                            TonePolarity.RELATIVE_LIGHTER,
                            DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }
                .build()
        return super.secondary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun secondaryDim(): DynamicColor? {
        return DynamicColor.Builder()
            .setName("secondary_dim")
            .setPalette { s: DynamicScheme -> s.secondaryPalette }
            .setTone { s: DynamicScheme ->
                if (s.variant == Variant.NEUTRAL) {
                    85.0
                } else {
                    tMaxC(s.secondaryPalette, 0.0, 90.0)
                }
            }
            .setIsBackground(true)
            .setBackground { s: DynamicScheme -> surfaceContainerHigh() }
            .setContrastCurve { s: DynamicScheme -> getContrastCurve(4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(
                    secondaryDim()!!,
                    secondary(),
                    5.0,
                    TonePolarity.DARKER,
                    DeltaConstraint.FARTHER,
                )
            }
            .build()
    }

    override fun onSecondary(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_secondary")
                .setPalette { s: DynamicScheme -> s.secondaryPalette }
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) secondary() else secondaryDim()
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(6.0) else getContrastCurve(7.0)
                }
                .build()
        return super.onSecondary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun secondaryContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("secondary_container")
                .setPalette { s: DynamicScheme -> s.secondaryPalette }
                .setTone { s: DynamicScheme ->
                    when {
                        s.platform == DynamicScheme.Platform.WATCH -> 30.0
                        s.variant == Variant.VIBRANT ->
                            if (s.isDark) tMinC(s.secondaryPalette, 30.0, 40.0)
                            else tMaxC(s.secondaryPalette, 84.0, 90.0)

                        s.variant == Variant.EXPRESSIVE ->
                            if (s.isDark) 15.0 else tMaxC(s.secondaryPalette, 90.0, 95.0)

                        else -> if (s.isDark) 25.0 else 90.0
                    }
                }
                .setIsBackground(true)
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        null
                    }
                }
                .setToneDeltaPair { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.WATCH) {
                        ToneDeltaPair(
                            secondaryContainer(),
                            secondaryDim()!!,
                            10.0,
                            TonePolarity.DARKER,
                            DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE && s.contrastLevel > 0) getContrastCurve(1.5) else null
                }
                .build()
        return super.secondaryContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onSecondaryContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_secondary_container")
                .setPalette { s: DynamicScheme -> s.secondaryPalette }
                .setBackground { s: DynamicScheme -> secondaryContainer() }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(6.0) else getContrastCurve(7.0)
                }
                .build()
        return super.onSecondaryContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Tertiaries [T] //
    // ////////////////////////////////////////////////////////////////
    override fun tertiary(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("tertiary")
                .setPalette { s: DynamicScheme -> s.tertiaryPalette }
                .setTone { s: DynamicScheme ->
                    when {
                        s.platform == DynamicScheme.Platform.WATCH ->
                            if (s.variant == Variant.TONAL_SPOT) tMaxC(s.tertiaryPalette, 0.0, 90.0)
                            else tMaxC(s.tertiaryPalette)

                        s.variant == Variant.EXPRESSIVE || s.variant == Variant.VIBRANT ->
                            tMaxC(
                                s.tertiaryPalette,
                                lowerBound = 0.0,
                                upperBound =
                                    if (Hct.Companion.isCyan(s.tertiaryPalette.hue)) 88.0 else if (s.isDark) 98.0 else 100.0,
                            )

                        else -> { // NEUTRAL and TONAL_SPOT
                            if (s.isDark) tMaxC(s.tertiaryPalette, 0.0, 98.0) else tMaxC(s.tertiaryPalette)
                        }
                    }
                }
                .setIsBackground(true)
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        surfaceContainerHigh()
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(4.5) else getContrastCurve(7.0)
                }
                .setToneDeltaPair { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        ToneDeltaPair(
                            tertiaryContainer(),
                            tertiary(),
                            5.0,
                            TonePolarity.RELATIVE_LIGHTER,
                            DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }
                .build()
        return super.tertiary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun tertiaryDim(): DynamicColor? {
        return DynamicColor.Builder()
            .setName("tertiary_dim")
            .setPalette { s: DynamicScheme -> s.tertiaryPalette }
            .setTone { s: DynamicScheme ->
                if (s.variant == Variant.TONAL_SPOT) {
                    tMaxC(s.tertiaryPalette, 0.0, 90.0)
                } else {
                    tMaxC(s.tertiaryPalette)
                }
            }
            .setIsBackground(true)
            .setBackground { s: DynamicScheme -> surfaceContainerHigh() }
            .setContrastCurve { s: DynamicScheme -> getContrastCurve(4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(
                    tertiaryDim()!!,
                    tertiary(),
                    5.0,
                    TonePolarity.DARKER,
                    DeltaConstraint.FARTHER,
                )
            }
            .build()
    }

    override fun onTertiary(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_tertiary")
                .setPalette { s: DynamicScheme -> s.tertiaryPalette }
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) tertiary() else tertiaryDim()
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(6.0) else getContrastCurve(7.0)
                }
                .build()
        return super.onTertiary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun tertiaryContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("tertiary_container")
                .setPalette { s: DynamicScheme -> s.tertiaryPalette }
                .setTone { s: DynamicScheme ->
                    when {
                        s.platform == DynamicScheme.Platform.WATCH ->
                            if (s.variant == Variant.TONAL_SPOT) tMaxC(s.tertiaryPalette, 0.0, 90.0)
                            else tMaxC(s.tertiaryPalette)

                        s.variant == Variant.NEUTRAL ->
                            if (s.isDark) tMaxC(s.tertiaryPalette, 0.0, 93.0)
                            else tMaxC(s.tertiaryPalette, 0.0, 96.0)

                        s.variant == Variant.TONAL_SPOT ->
                            tMaxC(s.tertiaryPalette, 0.0, if (s.isDark) 93.0 else 100.0)

                        s.variant == Variant.EXPRESSIVE ->
                            tMaxC(
                                s.tertiaryPalette,
                                lowerBound = 75.0,
                                upperBound =
                                    if (Hct.Companion.isCyan(s.tertiaryPalette.hue)) 88.0 else if (s.isDark) 93.0 else 100.0,
                            )

                        else -> { // VIBRANT
                            if (s.isDark) tMaxC(s.tertiaryPalette, 0.0, 93.0)
                            else tMaxC(s.tertiaryPalette, 72.0, 100.0)
                        }
                    }
                }
                .setIsBackground(true)
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        null
                    }
                }
                .setToneDeltaPair { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.WATCH) {
                        ToneDeltaPair(
                            tertiaryContainer(),
                            tertiaryDim()!!,
                            10.0,
                            TonePolarity.DARKER,
                            DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE && s.contrastLevel > 0) getContrastCurve(1.5) else null
                }
                .build()
        return super.tertiaryContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onTertiaryContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_tertiary_container")
                .setPalette { s: DynamicScheme -> s.tertiaryPalette }
                .setBackground { s: DynamicScheme -> tertiaryContainer() }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(6.0) else getContrastCurve(7.0)
                }
                .build()
        return super.onTertiaryContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Errors [E] //
    // ////////////////////////////////////////////////////////////////
    override fun error(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("error")
                .setPalette { s: DynamicScheme -> s.errorPalette }
                .setTone { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) tMinC(s.errorPalette, 0.0, 98.0) else tMaxC(s.errorPalette)
                    } else {
                        tMinC(s.errorPalette)
                    }
                }
                .setIsBackground(true)
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        surfaceContainerHigh()
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(4.5) else getContrastCurve(7.0)
                }
                .setToneDeltaPair { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        ToneDeltaPair(
                            errorContainer(),
                            error(),
                            5.0,
                            TonePolarity.RELATIVE_LIGHTER,
                            DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }
                .build()
        return super.error()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun errorDim(): DynamicColor? {
        return DynamicColor.Builder()
            .setName("error_dim")
            .setPalette { s: DynamicScheme -> s.errorPalette }
            .setTone { s: DynamicScheme -> tMinC(s.errorPalette) }
            .setIsBackground(true)
            .setBackground { s: DynamicScheme -> surfaceContainerHigh() }
            .setContrastCurve { s: DynamicScheme -> getContrastCurve(4.5) }
            .setToneDeltaPair { s: DynamicScheme ->
                ToneDeltaPair(errorDim()!!, error(), 5.0, TonePolarity.DARKER, DeltaConstraint.FARTHER)
            }
            .build()
    }

    override fun onError(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_error")
                .setPalette { s: DynamicScheme -> s.errorPalette }
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) error() else errorDim()
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(6.0) else getContrastCurve(7.0)
                }
                .build()
        return super.onError()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun errorContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("error_container")
                .setPalette { s: DynamicScheme -> s.errorPalette }
                .setTone { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.WATCH) {
                        30.0
                    } else {
                        if (s.isDark) tMinC(s.errorPalette, 30.0, 93.0) else tMaxC(s.errorPalette, 0.0, 90.0)
                    }
                }
                .setIsBackground(true)
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        null
                    }
                }
                .setToneDeltaPair { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.WATCH) {
                        ToneDeltaPair(
                            errorContainer(),
                            errorDim()!!,
                            10.0,
                            TonePolarity.DARKER,
                            DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE && s.contrastLevel > 0) getContrastCurve(1.5) else null
                }
                .build()
        return super.errorContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onErrorContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_error_container")
                .setPalette { s: DynamicScheme -> s.errorPalette }
                .setBackground { s: DynamicScheme -> errorContainer() }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) getContrastCurve(4.5) else getContrastCurve(7.0)
                }
                .build()
        return super.onErrorContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Primary Fixed Colors [PF] //
    // ////////////////////////////////////////////////////////////////
    override fun primaryFixed(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("primary_fixed")
                .setPalette { s: DynamicScheme -> s.primaryPalette }
                .setTone { s: DynamicScheme ->
                    val tempS = DynamicScheme.from(s, isDark = false, contrastLevel = 0.0)
                    primaryContainer().getTone(tempS)
                }
                .setIsBackground(true)
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        null
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE && s.contrastLevel > 0) getContrastCurve(1.5) else null
                }
                .build()
        return super.primaryFixed()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun primaryFixedDim(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("primary_fixed_dim")
                .setPalette { s: DynamicScheme -> s.primaryPalette }
                .setTone { s: DynamicScheme -> primaryFixed().getTone(s) }
                .setIsBackground(true)
                .setToneDeltaPair { s: DynamicScheme ->
                    ToneDeltaPair(
                        primaryFixedDim(),
                        primaryFixed(),
                        5.0,
                        TonePolarity.DARKER,
                        DeltaConstraint.EXACT,
                    )
                }
                .build()
        return super.primaryFixedDim()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onPrimaryFixed(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_primary_fixed")
                .setPalette { s: DynamicScheme -> s.primaryPalette }
                .setBackground { s: DynamicScheme -> primaryFixedDim() }
                .setContrastCurve { s: DynamicScheme -> getContrastCurve(7.0) }
                .build()
        return super.onPrimaryFixed()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onPrimaryFixedVariant(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_primary_fixed_variant")
                .setPalette { s: DynamicScheme -> s.primaryPalette }
                .setBackground { s: DynamicScheme -> primaryFixedDim() }
                .setContrastCurve { s: DynamicScheme -> getContrastCurve(4.5) }
                .build()
        return super.onPrimaryFixedVariant()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Secondary Fixed Colors [QF] //
    // ////////////////////////////////////////////////////////////////
    override fun secondaryFixed(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("secondary_fixed")
                .setPalette { s: DynamicScheme -> s.secondaryPalette }
                .setTone { s: DynamicScheme ->
                    val tempS = DynamicScheme.from(s, isDark = false, contrastLevel = 0.0)
                    secondaryContainer().getTone(tempS)
                }
                .setIsBackground(true)
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        null
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE && s.contrastLevel > 0) getContrastCurve(1.5) else null
                }
                .build()
        return super.secondaryFixed()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun secondaryFixedDim(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("secondary_fixed_dim")
                .setPalette { s: DynamicScheme -> s.secondaryPalette }
                .setTone { s: DynamicScheme -> secondaryFixed().getTone(s) }
                .setIsBackground(true)
                .setToneDeltaPair { s: DynamicScheme ->
                    ToneDeltaPair(
                        secondaryFixedDim(),
                        secondaryFixed(),
                        5.0,
                        TonePolarity.DARKER,
                        DeltaConstraint.EXACT,
                    )
                }
                .build()
        return super.secondaryFixedDim()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onSecondaryFixed(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_secondary_fixed")
                .setPalette { s: DynamicScheme -> s.secondaryPalette }
                .setBackground { s: DynamicScheme -> secondaryFixedDim() }
                .setContrastCurve { s: DynamicScheme -> getContrastCurve(7.0) }
                .build()
        return super.onSecondaryFixed()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onSecondaryFixedVariant(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_secondary_fixed_variant")
                .setPalette { s: DynamicScheme -> s.secondaryPalette }
                .setBackground { s: DynamicScheme -> secondaryFixedDim() }
                .setContrastCurve { s: DynamicScheme -> getContrastCurve(4.5) }
                .build()
        return super.onSecondaryFixedVariant()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Tertiary Fixed Colors [TF] //
    // ////////////////////////////////////////////////////////////////
    override fun tertiaryFixed(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("tertiary_fixed")
                .setPalette { s: DynamicScheme -> s.tertiaryPalette }
                .setTone { s: DynamicScheme ->
                    val tempS = DynamicScheme.from(s, isDark = false, contrastLevel = 0.0)
                    tertiaryContainer().getTone(tempS)
                }
                .setIsBackground(true)
                .setBackground { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        null
                    }
                }
                .setContrastCurve { s: DynamicScheme ->
                    if (s.platform == DynamicScheme.Platform.PHONE && s.contrastLevel > 0) getContrastCurve(1.5) else null
                }
                .build()
        return super.tertiaryFixed()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun tertiaryFixedDim(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("tertiary_fixed_dim")
                .setPalette { s: DynamicScheme -> s.tertiaryPalette }
                .setTone { s: DynamicScheme -> tertiaryFixed().getTone(s) }
                .setIsBackground(true)
                .setToneDeltaPair { s: DynamicScheme ->
                    ToneDeltaPair(
                        tertiaryFixedDim(),
                        tertiaryFixed(),
                        5.0,
                        TonePolarity.DARKER,
                        DeltaConstraint.EXACT,
                    )
                }
                .build()
        return super.tertiaryFixedDim()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onTertiaryFixed(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_tertiary_fixed")
                .setPalette { s: DynamicScheme -> s.tertiaryPalette }
                .setBackground { s: DynamicScheme -> tertiaryFixedDim() }
                .setContrastCurve { s: DynamicScheme -> getContrastCurve(7.0) }
                .build()
        return super.onTertiaryFixed()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun onTertiaryFixedVariant(): DynamicColor {
        val color2025 =
            DynamicColor.Builder()
                .setName("on_tertiary_fixed_variant")
                .setPalette { s: DynamicScheme -> s.tertiaryPalette }
                .setBackground { s: DynamicScheme -> tertiaryFixedDim() }
                .setContrastCurve { s: DynamicScheme -> getContrastCurve(4.5) }
                .build()
        return super.onTertiaryFixedVariant()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Android-only Colors //
    // ////////////////////////////////////////////////////////////////
    override fun controlActivated(): DynamicColor {
        // Remapped to primaryContainer for 2025 spec.
        val color2025 = primaryContainer().toBuilder().setName("control_activated").build()
        return super.controlActivated()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun controlNormal(): DynamicColor {
        // Remapped to onSurfaceVariant for 2025 spec.
        val color2025 = onSurfaceVariant().toBuilder().setName("control_normal").build()
        return super.controlNormal()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    override fun textPrimaryInverse(): DynamicColor {
        // Remapped to inverseOnSurface for 2025 spec.
        val color2025 = inverseOnSurface().toBuilder().setName("text_primary_inverse").build()
        return super.textPrimaryInverse()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // ////////////////////////////////////////////////////////////////
    // Other //
    // ////////////////////////////////////////////////////////////////
    private fun findBestToneForChroma(
        hue: Double,
        chroma: Double,
        tone: Double,
        byDecreasingTone: Boolean,
    ): Double {
        var tone = tone
        var answer = tone
        var bestCandidate = Hct.Companion.from(hue, chroma, answer)
        while (bestCandidate.chroma < chroma) {
            if (tone < 0 || tone > 100) {
                break
            }
            tone += if (byDecreasingTone) -1.0 else 1.0
            val newCandidate = Hct.Companion.from(hue, chroma, tone)
            if (bestCandidate.chroma < newCandidate.chroma) {
                bestCandidate = newCandidate
                answer = tone
            }
        }
        return answer
    }

    private fun tMaxC(palette: TonalPalette): Double {
        return tMaxC(palette, 0.0, 100.0)
    }

    private fun tMaxC(palette: TonalPalette, lowerBound: Double, upperBound: Double): Double {
        return tMaxC(palette, lowerBound, upperBound, 1.0)
    }

    private fun tMaxC(
        palette: TonalPalette,
        lowerBound: Double,
        upperBound: Double,
        chromaMultiplier: Double,
    ): Double {
        val answer = findBestToneForChroma(palette.hue, palette.chroma * chromaMultiplier, 100.0, true)
        return MathUtils.clampDouble(lowerBound, upperBound, answer)
    }

    private fun tMinC(palette: TonalPalette): Double {
        return tMinC(palette, 0.0, 100.0)
    }

    private fun tMinC(palette: TonalPalette, lowerBound: Double, upperBound: Double): Double {
        val answer = findBestToneForChroma(palette.hue, palette.chroma, 0.0, false)
        return MathUtils.clampDouble(lowerBound, upperBound, answer)
    }

    private fun getContrastCurve(defaultContrast: Double): ContrastCurve {
        return when (defaultContrast) {
            1.5 -> ContrastCurve(1.5, 1.5, 3.0, 4.5)
            3.0 -> ContrastCurve(3.0, 3.0, 4.5, 7.0)
            4.5 -> ContrastCurve(4.5, 4.5, 7.0, 11.0)
            6.0 -> ContrastCurve(6.0, 6.0, 7.0, 11.0)
            7.0 -> ContrastCurve(7.0, 7.0, 11.0, 21.0)
            9.0 -> ContrastCurve(9.0, 9.0, 11.0, 21.0)
            11.0 -> ContrastCurve(11.0, 11.0, 21.0, 21.0)
            21.0 -> ContrastCurve(21.0, 21.0, 21.0, 21.0)
            else -> ContrastCurve(defaultContrast, defaultContrast, 7.0, 21.0)
        }
    }

    // /////////////////////////////////////////////////////////////////
    // Color value calculations //
    // /////////////////////////////////////////////////////////////////
    override fun getHct(scheme: DynamicScheme, color: DynamicColor): Hct {
        // This is crucial for aesthetics: we aren't simply the taking the standard color
        // and changing its tone for contrast. Rather, we find the tone for contrast, then
        // use the specified chroma from the palette to construct a new color.
        //
        // For example, this enables colors with standard tone of T90, which has limited chroma, to
        // "recover" intended chroma as contrast increases.
        val palette = color.palette.invoke(scheme)
        val tone = getTone(scheme, color)
        val hue = palette.hue
        val chromaMultiplier = color.chromaMultiplier?.invoke(scheme) ?: 1.0
        val chroma = palette.chroma * chromaMultiplier
        return Hct.Companion.from(hue, chroma, tone)
    }

    override fun getTone(scheme: DynamicScheme, color: DynamicColor): Double {
        val toneDeltaPair = color.toneDeltaPair?.invoke(scheme)

        // Case 0: tone delta pair.
        if (toneDeltaPair != null) {
            val roleA = toneDeltaPair.roleA
            val roleB = toneDeltaPair.roleB
            val polarity = toneDeltaPair.polarity
            val constraint = toneDeltaPair.constraint
            val absoluteDelta =
                if (
                    polarity == TonePolarity.DARKER ||
                    (polarity == TonePolarity.RELATIVE_LIGHTER && scheme.isDark) ||
                    (polarity == TonePolarity.RELATIVE_DARKER && !scheme.isDark)
                ) {
                    -toneDeltaPair.delta
                } else {
                    toneDeltaPair.delta
                }
            val amRoleA = color.name == roleA.name
            val selfRole = if (amRoleA) roleA else roleB
            val referenceRole = if (amRoleA) roleB else roleA
            var selfTone = selfRole.tone.invoke(scheme)
            val referenceTone = referenceRole.getTone(scheme)
            val relativeDelta = absoluteDelta * (if (amRoleA) 1 else -1)
            when (constraint) {
                DeltaConstraint.EXACT ->
                    selfTone = MathUtils.clampDouble(0.0, 100.0, referenceTone + relativeDelta)

                DeltaConstraint.NEARER ->
                    if (relativeDelta > 0) {
                        selfTone =
                            MathUtils.clampDouble(
                                0.0,
                                100.0,
                                MathUtils.clampDouble(referenceTone, referenceTone + relativeDelta, selfTone),
                            )
                    } else {
                        selfTone =
                            MathUtils.clampDouble(
                                0.0,
                                100.0,
                                MathUtils.clampDouble(referenceTone + relativeDelta, referenceTone, selfTone),
                            )
                    }

                DeltaConstraint.FARTHER ->
                    if (relativeDelta > 0) {
                        selfTone = MathUtils.clampDouble(referenceTone + relativeDelta, 100.0, selfTone)
                    } else {
                        selfTone = MathUtils.clampDouble(0.0, referenceTone + relativeDelta, selfTone)
                    }
            }
            val background = color.background?.invoke(scheme)
            val contrastCurve = color.contrastCurve?.invoke(scheme)
            if (background != null && contrastCurve != null) {
                val bgTone = background.getTone(scheme)
                val selfContrast = contrastCurve.get(scheme.contrastLevel)
                selfTone =
                    if (
                        Contrast.ratioOfTones(bgTone, selfTone) >= selfContrast && scheme.contrastLevel >= 0
                    ) {
                        selfTone
                    } else {
                        DynamicColor.foregroundTone(bgTone, selfContrast)
                    }
            }

            // This can avoid the awkward tones for background colors including the access fixed colors.
            // Accent fixed dim colors should not be adjusted.
            if (color.isBackground && !color.name.endsWith("_fixed_dim")) {
                selfTone =
                    if (selfTone >= 57) {
                        MathUtils.clampDouble(65.0, 100.0, selfTone)
                    } else {
                        MathUtils.clampDouble(0.0, 49.0, selfTone)
                    }
            }
            return selfTone
        } else {
            // Case 1: No tone delta pair; just solve for itself.
            var answer = color.tone.invoke(scheme)
            val background = color.background?.invoke(scheme)
            val contrastCurve = color.contrastCurve?.invoke(scheme)
            if (background == null || contrastCurve == null) {
                return answer // No adjustment for colors with no background.
            }
            val bgTone = background.getTone(scheme)
            val desiredRatio = contrastCurve.get(scheme.contrastLevel)

            // Recalculate the tone from desired contrast ratio if the current
            // contrast ratio is not enough or desired contrast level is decreasing
            // (<0).
            answer =
                if (Contrast.ratioOfTones(bgTone, answer) >= desiredRatio && scheme.contrastLevel >= 0) {
                    answer
                } else {
                    DynamicColor.foregroundTone(bgTone, desiredRatio)
                }

            // This can avoid the awkward tones for background colors including the access fixed colors.
            // Accent fixed dim colors should not be adjusted.
            if (color.isBackground && !color.name.endsWith("_fixed_dim")) {
                answer =
                    if (answer >= 57) {
                        MathUtils.clampDouble(65.0, 100.0, answer)
                    } else {
                        MathUtils.clampDouble(0.0, 49.0, answer)
                    }
            }
            val secondBackground = color.secondBackground?.invoke(scheme)
            if (secondBackground == null) {
                return answer
            }

            // Case 2: Adjust for dual backgrounds.
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
                return if (lightOption < 0) 100.0 else lightOption
            }
            return if (availables.size == 1) {
                availables[0]
            } else if (darkOption < 0) 0.0 else darkOption
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
            Variant.NEUTRAL ->
                TonalPalette.Companion.fromHueAndChroma(
                    sourceColorHct.hue,
                    if (platform == DynamicScheme.Platform.PHONE) (if (Hct.Companion.isBlue(sourceColorHct.hue)) 12.0 else 8.0)
                    else if (Hct.Companion.isBlue(sourceColorHct.hue)) 16.0 else 12.0,
                )

            Variant.TONAL_SPOT ->
                TonalPalette.Companion.fromHueAndChroma(
                    sourceColorHct.hue,
                    if (platform == DynamicScheme.Platform.PHONE && isDark) 26.0 else 32.0,
                )

            Variant.EXPRESSIVE ->
                TonalPalette.Companion.fromHueAndChroma(
                    sourceColorHct.hue,
                    if (platform == DynamicScheme.Platform.PHONE) if (isDark) 36.0 else 48.0 else 40.0,
                )

            Variant.VIBRANT ->
                TonalPalette.Companion.fromHueAndChroma(
                    sourceColorHct.hue,
                    if (platform == DynamicScheme.Platform.PHONE) 74.0 else 56.0,
                )

            else -> super.getPrimaryPalette(variant, sourceColorHct, isDark, platform, contrastLevel)
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
            Variant.NEUTRAL ->
                TonalPalette.Companion.fromHueAndChroma(
                    sourceColorHct.hue,
                    if (platform == DynamicScheme.Platform.PHONE) (if (Hct.Companion.isBlue(sourceColorHct.hue)) 6.0 else 4.0)
                    else if (Hct.Companion.isBlue(sourceColorHct.hue)) 10.0 else 6.0,
                )

            Variant.TONAL_SPOT -> TonalPalette.Companion.fromHueAndChroma(sourceColorHct.hue, 16.0)
            Variant.EXPRESSIVE ->
                TonalPalette.Companion.fromHueAndChroma(
                    DynamicScheme.getRotatedHue(
                        sourceColorHct,
                        doubleArrayOf(0.0, 105.0, 140.0, 204.0, 253.0, 278.0, 300.0, 333.0, 360.0),
                        doubleArrayOf(-160.0, 155.0, -100.0, 96.0, -96.0, -156.0, -165.0, -160.0),
                    ),
                    if (platform == DynamicScheme.Platform.PHONE) if (isDark) 16.0 else 24.0 else 24.0,
                )

            Variant.VIBRANT ->
                TonalPalette.Companion.fromHueAndChroma(
                    DynamicScheme.getRotatedHue(
                        sourceColorHct,
                        doubleArrayOf(0.0, 38.0, 105.0, 140.0, 333.0, 360.0),
                        doubleArrayOf(-14.0, 10.0, -14.0, 10.0, -14.0),
                    ),
                    if (platform == DynamicScheme.Platform.PHONE) 56.0 else 36.0,
                )

            else -> super.getSecondaryPalette(variant, sourceColorHct, isDark, platform, contrastLevel)
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
            Variant.NEUTRAL ->
                TonalPalette.Companion.fromHueAndChroma(
                    DynamicScheme.getRotatedHue(
                        sourceColorHct,
                        doubleArrayOf(0.0, 38.0, 105.0, 161.0, 204.0, 278.0, 333.0, 360.0),
                        doubleArrayOf(-32.0, 26.0, 10.0, -39.0, 24.0, -15.0, -32.0),
                    ),
                    if (platform == DynamicScheme.Platform.PHONE) 20.0 else 36.0,
                )

            Variant.TONAL_SPOT ->
                TonalPalette.Companion.fromHueAndChroma(
                    DynamicScheme.getRotatedHue(
                        sourceColorHct,
                        doubleArrayOf(0.0, 20.0, 71.0, 161.0, 333.0, 360.0),
                        doubleArrayOf(-40.0, 48.0, -32.0, 40.0, -32.0),
                    ),
                    if (platform == DynamicScheme.Platform.PHONE) 28.0 else 32.0,
                )

            Variant.EXPRESSIVE ->
                TonalPalette.Companion.fromHueAndChroma(
                    DynamicScheme.getRotatedHue(
                        sourceColorHct,
                        doubleArrayOf(0.0, 105.0, 140.0, 204.0, 253.0, 278.0, 300.0, 333.0, 360.0),
                        doubleArrayOf(-165.0, 160.0, -105.0, 101.0, -101.0, -160.0, -170.0, -165.0),
                    ),
                    48.0,
                )

            Variant.VIBRANT ->
                TonalPalette.Companion.fromHueAndChroma(
                    DynamicScheme.getRotatedHue(
                        sourceColorHct,
                        doubleArrayOf(0.0, 38.0, 71.0, 105.0, 140.0, 161.0, 253.0, 333.0, 360.0),
                        doubleArrayOf(-72.0, 35.0, 24.0, -24.0, 62.0, 50.0, 62.0, -72.0),
                    ),
                    56.0,
                )

            else -> super.getTertiaryPalette(variant, sourceColorHct, isDark, platform, contrastLevel)
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
            Variant.NEUTRAL ->
                TonalPalette.Companion.fromHueAndChroma(
                    sourceColorHct.hue,
                    if (platform == DynamicScheme.Platform.PHONE) 1.4 else 6.0,
                )

            Variant.TONAL_SPOT ->
                TonalPalette.Companion.fromHueAndChroma(
                    sourceColorHct.hue,
                    if (platform == DynamicScheme.Platform.PHONE) 5.0 else 10.0,
                )

            Variant.EXPRESSIVE ->
                TonalPalette.Companion.fromHueAndChroma(
                    getExpressiveNeutralHue(sourceColorHct),
                    getExpressiveNeutralChroma(sourceColorHct, isDark, platform),
                )

            Variant.VIBRANT ->
                TonalPalette.Companion.fromHueAndChroma(
                    getVibrantNeutralHue(sourceColorHct),
                    getVibrantNeutralChroma(sourceColorHct, platform),
                )

            else -> super.getNeutralPalette(variant, sourceColorHct, isDark, platform, contrastLevel)
        }
    }

    override fun getNeutralVariantPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette {
        when (variant) {
            Variant.NEUTRAL ->
                return TonalPalette.Companion.fromHueAndChroma(
                    sourceColorHct.hue,
                    (if (platform == DynamicScheme.Platform.PHONE) 1.4 else 6.0) * 2.2,
                )

            Variant.TONAL_SPOT ->
                return TonalPalette.Companion.fromHueAndChroma(
                    sourceColorHct.hue,
                    (if (platform == DynamicScheme.Platform.PHONE) 5.0 else 10.0) * 1.7,
                )

            Variant.EXPRESSIVE -> {
                val expressiveNeutralHue = getExpressiveNeutralHue(sourceColorHct)
                val expressiveNeutralChroma = getExpressiveNeutralChroma(sourceColorHct, isDark, platform)
                return TonalPalette.Companion.fromHueAndChroma(
                    expressiveNeutralHue,
                    expressiveNeutralChroma *
                            if (expressiveNeutralHue >= 105 && expressiveNeutralHue < 125) 1.6 else 2.3,
                )
            }

            Variant.VIBRANT -> {
                val vibrantNeutralHue = getVibrantNeutralHue(sourceColorHct)
                val vibrantNeutralChroma = getVibrantNeutralChroma(sourceColorHct, platform)
                return TonalPalette.Companion.fromHueAndChroma(vibrantNeutralHue, vibrantNeutralChroma * 1.29)
            }

            else ->
                return super.getNeutralVariantPalette(
                    variant,
                    sourceColorHct,
                    isDark,
                    platform,
                    contrastLevel,
                )
        }
    }

    override fun getErrorPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
        contrastLevel: Double,
    ): TonalPalette {
        val errorHue =
            DynamicScheme.getPiecewiseValue(
                sourceColorHct,
                doubleArrayOf(0.0, 3.0, 13.0, 23.0, 33.0, 43.0, 153.0, 273.0, 360.0),
                doubleArrayOf(12.0, 22.0, 32.0, 12.0, 22.0, 32.0, 22.0, 12.0),
            )
        return when (variant) {
            Variant.NEUTRAL ->
                TonalPalette.Companion.fromHueAndChroma(errorHue, if (platform == DynamicScheme.Platform.PHONE) 50.0 else 40.0)

            Variant.TONAL_SPOT ->
                TonalPalette.Companion.fromHueAndChroma(errorHue, if (platform == DynamicScheme.Platform.PHONE) 60.0 else 48.0)

            Variant.EXPRESSIVE ->
                TonalPalette.Companion.fromHueAndChroma(errorHue, if (platform == DynamicScheme.Platform.PHONE) 64.0 else 48.0)

            Variant.VIBRANT ->
                TonalPalette.Companion.fromHueAndChroma(errorHue, if (platform == DynamicScheme.Platform.PHONE) 80.0 else 60.0)

            else -> super.getErrorPalette(variant, sourceColorHct, isDark, platform, contrastLevel)
        }
    }

    private fun getExpressiveNeutralHue(sourceColorHct: Hct): Double {
        return DynamicScheme.getRotatedHue(
            sourceColorHct,
            doubleArrayOf(0.0, 71.0, 124.0, 253.0, 278.0, 300.0, 360.0),
            doubleArrayOf(10.0, 0.0, 10.0, 0.0, 10.0, 0.0),
        )
    }

    private fun getExpressiveNeutralChroma(
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: DynamicScheme.Platform,
    ): Double {
        val neutralHue = getExpressiveNeutralHue(sourceColorHct)
        return if (platform == DynamicScheme.Platform.PHONE) {
            if (isDark) if (Hct.Companion.isYellow(neutralHue)) 6.0 else 14.0 else 18.0
        } else {
            12.0
        }
    }

    private fun getVibrantNeutralHue(sourceColorHct: Hct): Double {
        return DynamicScheme.getRotatedHue(
            sourceColorHct,
            doubleArrayOf(0.0, 38.0, 105.0, 140.0, 333.0, 360.0),
            doubleArrayOf(-14.0, 10.0, -14.0, 10.0, -14.0),
        )
    }

    private fun getVibrantNeutralChroma(sourceColorHct: Hct, platform: DynamicScheme.Platform): Double {
        val neutralHue = getVibrantNeutralHue(sourceColorHct)
        return if (platform == DynamicScheme.Platform.PHONE) 28.0 else if (Hct.Companion.isBlue(neutralHue)) 28.0 else 20.0
    }
}
