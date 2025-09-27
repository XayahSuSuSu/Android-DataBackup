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

import com.xayah.databackup.ui.theme.color.contrast.Contrast
import com.xayah.databackup.ui.theme.color.hct.Hct
import com.xayah.databackup.ui.theme.color.palettes.TonalPalette
import com.xayah.databackup.ui.theme.color.utils.MathUtils
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A color that adjusts itself based on UI state, represented by DynamicScheme.
 *
 * This color automatically adjusts to accommodate a desired contrast level, or other adjustments
 * such as differing in light mode versus dark mode, or what the theme is, or what the color that
 * produced the theme is, etc.
 *
 * Colors without backgrounds do not change tone when contrast changes. Colors with backgrounds
 * become closer to their background as contrast lowers, and further when contrast increases.
 *
 * Prefer the static constructors. They provide a much more simple interface, such as requiring just
 * a hexcode, or just a hexcode and a background.
 *
 * Ultimately, each component necessary for calculating a color, adjusting it for a desired contrast
 * level, and ensuring it has a certain lightness/tone difference from another color, is provided by
 * a function that takes a DynamicScheme and returns a value. This ensures ultimate flexibility, any
 * desired behavior of a color for any design system, but it usually unnecessary. See the default
 * constructor for more information.
 */
class DynamicColor(
    val name: String,
    val palette: (DynamicScheme) -> TonalPalette,
    val tone: (DynamicScheme) -> Double,
    val isBackground: Boolean,
    val chromaMultiplier: ((DynamicScheme) -> Double)?,
    val background: ((DynamicScheme) -> DynamicColor?)?,
    val secondBackground: ((DynamicScheme) -> DynamicColor?)?,
    val contrastCurve: ((DynamicScheme) -> ContrastCurve?)?,
    val toneDeltaPair: ((DynamicScheme) -> ToneDeltaPair?)?,
    val opacity: ((DynamicScheme) -> Double?)?,
) {
    private val hctCache = mutableMapOf<DynamicScheme, Hct>()

    /**
     * A constructor for DynamicColor.
     *
     * _Strongly_ prefer using one of the convenience constructors. This class is arguably too
     * flexible to ensure it can support any scenario. Functional arguments allow overriding without
     * risks that come with subclasses.
     *
     * For example, the default behavior of adjust tone at max contrast to be at a 7.0 ratio with its
     * background is principled and matches accessibility guidance. That does not mean it's the
     * desired approach for _every_ design system, and every color pairing, always, in every case.
     *
     * For opaque colors (colors with alpha = 100%).
     *
     * @param name The name of the dynamic color.
     * @param palette Function that provides a TonalPalette given DynamicScheme. A TonalPalette is
     *   defined by a hue and chroma, so this replaces the need to specify hue/chroma. By providing a
     *   tonal palette, when contrast adjustments are made, intended chroma can be preserved.
     * @param tone Function that provides a tone, given a DynamicScheme.
     * @param isBackground Whether this dynamic color is a background, with some other color as the
     *   foreground.
     * @param background The background of the dynamic color (as a function of a `DynamicScheme`), if
     *   it exists.
     * @param secondBackground A second background of the dynamic color (as a function of a
     *   `DynamicScheme`), if it exists.
     * @param contrastCurve A `ContrastCurve` object specifying how its contrast against its
     *   background should behave in various contrast levels options.
     * @param toneDeltaPair A `ToneDeltaPair` object specifying a tone delta constraint between two
     *   colors. One of them must be the color being constructed.
     */
    constructor(
        name: String,
        palette: (DynamicScheme) -> TonalPalette,
        tone: (DynamicScheme) -> Double,
        isBackground: Boolean,
        background: ((DynamicScheme) -> DynamicColor?)?,
        secondBackground: ((DynamicScheme) -> DynamicColor?)?,
        contrastCurve: ContrastCurve?,
        toneDeltaPair: ((DynamicScheme) -> ToneDeltaPair?)?,
    ) : this(
        name,
        palette,
        tone,
        isBackground,
        chromaMultiplier = null,
        background,
        secondBackground,
        if (contrastCurve == null) null else { _ -> contrastCurve },
        toneDeltaPair,
        opacity = null,
    )

    /**
     * A constructor for DynamicColor.
     *
     * _Strongly_ prefer using one of the convenience constructors. This class is arguably too
     * flexible to ensure it can support any scenario. Functional arguments allow overriding without
     * risks that come with subclasses.
     *
     * For example, the default behavior of adjust tone at max contrast to be at a 7.0 ratio with its
     * background is principled and matches accessibility guidance. That does not mean it's the
     * desired approach for _every_ design system, and every color pairing, always, in every case.
     *
     * For opaque colors (colors with alpha = 100%).
     *
     * @param name The name of the dynamic color.
     * @param palette Function that provides a TonalPalette given DynamicScheme. A TonalPalette is
     *   defined by a hue and chroma, so this replaces the need to specify hue/chroma. By providing a
     *   tonal palette, when contrast adjustments are made, intended chroma can be preserved.
     * @param tone Function that provides a tone, given a DynamicScheme.
     * @param isBackground Whether this dynamic color is a background, with some other color as the
     *   foreground.
     * @param background The background of the dynamic color (as a function of a `DynamicScheme`), if
     *   it exists.
     * @param secondBackground A second background of the dynamic color (as a function of a
     *   `DynamicScheme`), if it exists.
     * @param contrastCurve A `ContrastCurve` object specifying how its contrast against its
     *   background should behave in various contrast levels options.
     * @param toneDeltaPair A `ToneDeltaPair` object specifying a tone delta constraint between two
     *   colors. One of them must be the color being constructed.
     * @param opacity A function returning the opacity of a color, as a number between 0 and 1.
     */
    constructor(
        name: String,
        palette: (DynamicScheme) -> TonalPalette,
        tone: (DynamicScheme) -> Double,
        isBackground: Boolean,
        background: ((DynamicScheme) -> DynamicColor?)?,
        secondBackground: ((DynamicScheme) -> DynamicColor?)?,
        contrastCurve: ContrastCurve?,
        toneDeltaPair: ((DynamicScheme) -> ToneDeltaPair?)?,
        opacity: ((DynamicScheme) -> Double?)?,
    ) : this(
        name,
        palette,
        tone,
        isBackground,
        chromaMultiplier = null,
        background,
        secondBackground,
        if (contrastCurve == null) null else { _ -> contrastCurve },
        toneDeltaPair,
        opacity,
    )

    /**
     * Returns an ARGB integer (i.e. a hex code).
     *
     * @param scheme Defines the conditions of the user interface, for example, whether or not it is
     *   dark mode or light mode, and what the desired contrast level is.
     */
    fun getArgb(scheme: DynamicScheme): Int {
        val argb = getHct(scheme).toInt()
        val opacityPercentage = opacity?.invoke(scheme)
        return if (opacityPercentage == null) {
            argb
        } else {
            val alpha = MathUtils.clampInt(0, 255, (opacityPercentage * 255).roundToInt())
            (argb and 0x00ffffff) or (alpha shl 24)
        }
    }

    /**
     * Returns an HCT object.
     *
     * @param scheme Defines the conditions of the user interface, for example, whether or not it is
     *   dark mode or light mode, and what the desired contrast level is.
     */
    fun getHct(scheme: DynamicScheme): Hct {
        val cachedAnswer = hctCache[scheme]
        if (cachedAnswer != null) {
            return cachedAnswer
        }
        val answer = ColorSpecs.get(scheme.specVersion).getHct(scheme, this)
        // NOMUTANTS--trivial test with onerous dependency injection requirement.
        if (hctCache.size > 4) {
            hctCache.clear()
        }
        // NOMUTANTS--trivial test with onerous dependency injection requirement.
        hctCache[scheme] = answer
        return answer
    }

    /** Returns the tone in HCT, ranging from 0 to 100, of the resolved color given scheme. */
    fun getTone(scheme: DynamicScheme): Double {
        return ColorSpecs.get(scheme.specVersion).getTone(scheme, this)
    }

    fun toBuilder(): Builder {
        return Builder()
            .setName(name)
            .setPalette(palette)
            .setTone(tone)
            .setIsBackground(isBackground)
            .setChromaMultiplier(chromaMultiplier)
            .setBackground(background)
            .setSecondBackground(secondBackground)
            .setContrastCurve(contrastCurve)
            .setToneDeltaPair(toneDeltaPair)
            .setOpacity(opacity)
    }

    /** Builder for [DynamicColor]. */
    class Builder {
        private var name: String? = null
        private var palette: ((DynamicScheme) -> TonalPalette)? = null
        private var tone: ((DynamicScheme) -> Double)? = null
        private var isBackground = false
        private var chromaMultiplier: ((DynamicScheme) -> Double)? = null
        private var background: ((DynamicScheme) -> DynamicColor?)? = null
        private var secondBackground: ((DynamicScheme) -> DynamicColor?)? = null
        private var contrastCurve: ((DynamicScheme) -> ContrastCurve?)? = null
        private var toneDeltaPair: ((DynamicScheme) -> ToneDeltaPair?)? = null
        private var opacity: ((DynamicScheme) -> Double?)? = null

        fun setName(name: String) = apply { this.name = name }

        fun setPalette(palette: (DynamicScheme) -> TonalPalette) = apply { this.palette = palette }

        fun setTone(tone: (DynamicScheme) -> Double) = apply { this.tone = tone }

        fun setIsBackground(isBackground: Boolean) = apply { this.isBackground = isBackground }

        fun setChromaMultiplier(chromaMultiplier: ((DynamicScheme) -> Double)?) = apply {
            this.chromaMultiplier = chromaMultiplier
        }

        fun setBackground(background: ((DynamicScheme) -> DynamicColor?)?) = apply {
            this.background = background
        }

        fun setSecondBackground(secondBackground: ((DynamicScheme) -> DynamicColor?)?) = apply {
            this.secondBackground = secondBackground
        }

        fun setContrastCurve(contrastCurve: ((DynamicScheme) -> ContrastCurve?)?) = apply {
            this.contrastCurve = contrastCurve
        }

        fun setToneDeltaPair(toneDeltaPair: ((DynamicScheme) -> ToneDeltaPair?)?) = apply {
            this.toneDeltaPair = toneDeltaPair
        }

        fun setOpacity(opacity: ((DynamicScheme) -> Double?)?) = apply { this.opacity = opacity }

        fun extendSpecVersion(specVersion: ColorSpec.SpecVersion, extendedColor: DynamicColor): Builder {
            validateExtendedColor(specVersion, extendedColor)
            return Builder()
                .setName(name!!)
                .setIsBackground(isBackground)
                .setPalette { s ->
                    (if (s.specVersion == specVersion) extendedColor.palette else palette!!).invoke(s)
                }
                .setTone { s ->
                    (if (s.specVersion == specVersion) extendedColor.tone else tone!!).invoke(s)
                }
                .setChromaMultiplier(
                    chromaMultiplier = { s ->
                        (if (s.specVersion == specVersion) extendedColor.chromaMultiplier else chromaMultiplier)
                            ?.invoke(s) ?: 1.0
                    }
                )
                .setBackground(
                    background = { s ->
                        (if (s.specVersion == specVersion) extendedColor.background else background)?.invoke(s)
                    }
                )
                .setSecondBackground(
                    secondBackground = { s ->
                        (if (s.specVersion == specVersion) extendedColor.secondBackground else secondBackground)
                            ?.invoke(s)
                    }
                )
                .setContrastCurve(
                    contrastCurve = { s ->
                        (if (s.specVersion == specVersion) extendedColor.contrastCurve else contrastCurve)
                            ?.invoke(s)
                    }
                )
                .setToneDeltaPair(
                    toneDeltaPair = { s ->
                        (if (s.specVersion == specVersion) extendedColor.toneDeltaPair else toneDeltaPair)
                            ?.invoke(s)
                    }
                )
                .setOpacity(
                    opacity = { s ->
                        (if (s.specVersion == specVersion) extendedColor.opacity else opacity)?.invoke(s)
                    }
                )
        }

        fun build(): DynamicColor {
            if (background == null && secondBackground != null) {
                throw IllegalArgumentException(
                    "Color $name has secondBackground defined, but background is not defined."
                )
            }
            if (background == null && contrastCurve != null) {
                throw IllegalArgumentException(
                    "Color $name has contrastCurve defined, but background is not defined."
                )
            }
            if (background != null && contrastCurve == null) {
                throw IllegalArgumentException(
                    "Color $name has background defined, but contrastCurve is not defined."
                )
            }
            return DynamicColor(
                name!!,
                palette!!,
                tone ?: getInitialToneFromBackground(background),
                isBackground,
                chromaMultiplier,
                background,
                secondBackground,
                contrastCurve,
                toneDeltaPair,
                opacity,
            )
        }

        private fun validateExtendedColor(specVersion: ColorSpec.SpecVersion, extendedColor: DynamicColor) {
            require(name == extendedColor.name) {
                "Attempting to extend color $name with color ${extendedColor.name} of different name for spec version $specVersion."
            }
            require(isBackground == extendedColor.isBackground) {
                "Attempting to extend color $name as a ${if (isBackground) "background" else "foreground"} with color ${extendedColor.name} as a ${if (extendedColor.isBackground) "background" else "foreground"} for spec version $specVersion."
            }
        }
    }

    companion object {
        /**
         * A convenience constructor for DynamicColor.
         *
         * _Strongly_ prefer using one of the convenience constructors. This class is arguably too
         * flexible to ensure it can support any scenario. Functional arguments allow overriding without
         * risks that come with subclasses.
         *
         * For example, the default behavior of adjust tone at max contrast to be at a 7.0 ratio with
         * its background is principled and matches accessibility guidance. That does not mean it's the
         * desired approach for _every_ design system, and every color pairing, always, in every case.
         *
         * For opaque colors (colors with alpha = 100%).
         *
         * For colors that are not backgrounds, and do not have backgrounds.
         *
         * @param name The name of the dynamic color.
         * @param palette Function that provides a TonalPalette given DynamicScheme. A TonalPalette is
         *   defined by a hue and chroma, so this replaces the need to specify hue/chroma. By providing
         *   a tonal palette, when contrast adjustments are made, intended chroma can be preserved.
         * @param tone Function that provides a tone, given a DynamicScheme.
         */
        @JvmStatic
        fun fromPalette(
            name: String,
            palette: (DynamicScheme) -> TonalPalette,
            tone: (DynamicScheme) -> Double,
        ): DynamicColor {
            return DynamicColor(
                name,
                palette,
                tone,
                isBackground = false,
                background = null,
                secondBackground = null,
                contrastCurve = null,
                toneDeltaPair = null,
            )
        }

        /**
         * A convenience constructor for DynamicColor.
         *
         * _Strongly_ prefer using one of the convenience constructors. This class is arguably too
         * flexible to ensure it can support any scenario. Functional arguments allow overriding without
         * risks that come with subclasses.
         *
         * For example, the default behavior of adjust tone at max contrast to be at a 7.0 ratio with
         * its background is principled and matches accessibility guidance. That does not mean it's the
         * desired approach for _every_ design system, and every color pairing, always, in every case.
         *
         * For opaque colors (colors with alpha = 100%).
         *
         * For colors that do not have backgrounds.
         *
         * @param name The name of the dynamic color.
         * @param palette Function that provides a TonalPalette given DynamicScheme. A TonalPalette is
         *   defined by a hue and chroma, so this replaces the need to specify hue/chroma. By providing
         *   a tonal palette, when contrast adjustments are made, intended chroma can be preserved.
         * @param tone Function that provides a tone, given a DynamicScheme.
         * @param isBackground Whether this dynamic color is a background, with some other color as the
         *   foreground.
         */
        @JvmStatic
        fun fromPalette(
            name: String,
            palette: (DynamicScheme) -> TonalPalette,
            tone: (DynamicScheme) -> Double,
            isBackground: Boolean,
        ): DynamicColor {
            return DynamicColor(
                name,
                palette,
                tone,
                isBackground,
                background = null,
                secondBackground = null,
                contrastCurve = null,
                toneDeltaPair = null,
            )
        }

        /**
         * Create a DynamicColor from a hex code.
         *
         * Result has no background; thus no support for increasing/decreasing contrast for a11y.
         *
         * @param name The name of the dynamic color.
         * @param argb The source color from which to extract the hue and chroma.
         */
        @JvmStatic
        fun fromArgb(name: String, argb: Int): DynamicColor {
            val hct = Hct.Companion.fromInt(argb)
            val palette = TonalPalette.Companion.fromInt(argb)
            return fromPalette(name, { _ -> palette }, { _ -> hct.tone })
        }

        /**
         * Given a background tone, find a foreground tone, while ensuring they reach a contrast ratio
         * that is as close to ratio as possible.
         */
        @JvmStatic
        fun foregroundTone(bgTone: Double, ratio: Double): Double {
            val lighterTone = Contrast.lighterUnsafe(bgTone, ratio)
            val darkerTone = Contrast.darkerUnsafe(bgTone, ratio)
            val lighterRatio = Contrast.ratioOfTones(lighterTone, bgTone)
            val darkerRatio = Contrast.ratioOfTones(darkerTone, bgTone)
            val preferLighter = tonePrefersLightForeground(bgTone)
            if (preferLighter) {
                // "Neglible difference" handles an edge case where the initial contrast ratio is high
                // (ex. 13.0), and the ratio passed to the function is that high ratio, and both the lighter
                // and darker ratio fails to pass that ratio.
                //
                // This was observed with Tonal Spot's On Primary Container turning black momentarily
                // between
                // high and max contrast in light mode. PC's standard tone was T90, OPC's was T10, it was
                // light mode, and the contrast level was 0.6568521221032331.
                val negligibleDifference =
                    abs(lighterRatio - darkerRatio) < 0.1 && lighterRatio < ratio && darkerRatio < ratio
                return if (lighterRatio >= ratio || lighterRatio >= darkerRatio || negligibleDifference) {
                    lighterTone
                } else {
                    darkerTone
                }
            } else {
                return if (darkerRatio >= ratio || darkerRatio >= lighterRatio) darkerTone else lighterTone
            }
        }

        /**
         * Adjust a tone down such that white has 4.5 contrast, if the tone is reasonably close to
         * supporting it.
         */
        @JvmStatic
        fun enableLightForeground(tone: Double): Double {
            return if (tonePrefersLightForeground(tone) && !toneAllowsLightForeground(tone)) {
                49.0
            } else {
                tone
            }
        }

        /**
         * People prefer white foregrounds on ~T60-70. Observed over time, and also by Andrew Somers
         * during research for APCA.
         *
         * T60 used as to create the smallest discontinuity possible when skipping down to T49 in order
         * to ensure light foregrounds.
         *
         * Since `tertiaryContainer` in dark monochrome scheme requires a tone of 60, it should not be
         * adjusted. Therefore, 60 is excluded here.
         */
        @JvmStatic
        fun tonePrefersLightForeground(tone: Double): Boolean {
            return tone.roundToInt() < 60
        }

        /** Tones less than ~T50 always permit white at 4.5 contrast. */
        @JvmStatic
        fun toneAllowsLightForeground(tone: Double): Boolean {
            return tone.roundToInt() <= 49
        }

        @JvmStatic
        fun getInitialToneFromBackground(
            background: ((DynamicScheme) -> DynamicColor?)?
        ): (DynamicScheme) -> Double {
            if (background == null) {
                return { 50.0 }
            }
            return { s -> background(s)?.getTone(s) ?: 50.0 }
        }
    }
}
