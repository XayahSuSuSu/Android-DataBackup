/*
 * Copyright 2021 Google LLC
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
package com.xayah.databackup.ui.theme.color.palettes

import com.xayah.databackup.ui.theme.color.hct.Hct
import kotlin.math.max
import kotlin.math.min

/**
 * An intermediate concept between the key color for a UI theme, and a full color scheme. 5 sets of
 * tones are generated, all except one use the same hue as the key color, and all vary in chroma.
 *
 * @property a1 Tonal palette for primary accent colors.
 * @property a2 Tonal palette for secondary accent colors.
 * @property a3 Tonal palette for tertiary accent colors.
 * @property n1 Tonal palette for neutral colors.
 * @property n2 Tonal palette for neutral variant colors.
 * @property error Tonal palette for error colors.
 * @deprecated Use [com.xayah.databackup.ui.theme.color.dynamiccolor.DynamicScheme] for color scheme
 *   generation. Use [CorePalettes] for core palettes
 *   container class.
 */
@Deprecated(
    "Use dynamiccolor.DynamicScheme for color scheme generation. Use palettes.CorePalettes for core palettes container class."
)
class CorePalette private constructor(argb: Int, isContent: Boolean) {
    var a1: TonalPalette
    var a2: TonalPalette
    var a3: TonalPalette
    var n1: TonalPalette
    var n2: TonalPalette
    var error: TonalPalette

    init {
        val hct = Hct.Companion.fromInt(argb)
        val hue = hct.hue
        val chroma = hct.chroma
        if (isContent) {
            a1 = TonalPalette.fromHueAndChroma(hue, chroma)
            a2 = TonalPalette.fromHueAndChroma(hue, chroma / 3.0)
            a3 = TonalPalette.fromHueAndChroma(hue + 60.0, chroma / 2.0)
            n1 = TonalPalette.fromHueAndChroma(hue, min(chroma / 12.0, 4.0))
            n2 = TonalPalette.fromHueAndChroma(hue, min(chroma / 6.0, 8.0))
        } else {
            a1 = TonalPalette.fromHueAndChroma(hue, max(48.0, chroma))
            a2 = TonalPalette.fromHueAndChroma(hue, 16.0)
            a3 = TonalPalette.fromHueAndChroma(hue + 60.0, 24.0)
            n1 = TonalPalette.fromHueAndChroma(hue, 4.0)
            n2 = TonalPalette.fromHueAndChroma(hue, 8.0)
        }
        error = TonalPalette.fromHueAndChroma(25.0, 84.0)
    }

    companion object {
        /**
         * Create key tones from a color.
         *
         * @param argb ARGB representation of a color
         * @deprecated Use [com.xayah.databackup.ui.theme.color.dynamiccolor.DynamicScheme] for color
         *   scheme generation. Use [CorePalettes] for core
         *   palettes container class.
         */
        @Deprecated(
            "Use dynamiccolor.DynamicScheme for color scheme generation. Use palettes.CorePalettes for core palettes container class."
        )
        @JvmStatic
        fun of(argb: Int): CorePalette {
            return CorePalette(argb, false)
        }

        /**
         * Create content key tones from a color.
         *
         * @param argb ARGB representation of a color
         * @deprecated Use [com.xayah.databackup.ui.theme.color.dynamiccolor.DynamicScheme] for color
         *   scheme generation. Use [CorePalettes] for core
         *   palettes container class.
         */
        @Deprecated(
            "Use dynamiccolor.DynamicScheme for color scheme generation. Use palettes.CorePalettes for core palettes container class."
        )
        @JvmStatic
        fun contentOf(argb: Int): CorePalette {
            return CorePalette(argb, true)
        }
    }
}
