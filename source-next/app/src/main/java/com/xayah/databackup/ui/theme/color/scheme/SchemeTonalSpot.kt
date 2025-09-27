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
package com.xayah.databackup.ui.theme.color.scheme

import com.xayah.databackup.ui.theme.color.dynamiccolor.ColorSpec
import com.xayah.databackup.ui.theme.color.dynamiccolor.ColorSpecs
import com.xayah.databackup.ui.theme.color.dynamiccolor.DynamicScheme
import com.xayah.databackup.ui.theme.color.dynamiccolor.Variant
import com.xayah.databackup.ui.theme.color.hct.Hct

/** A calm theme, sedated colors that aren't particularly chromatic. */
class SchemeTonalSpot(
    sourceColorHct: Hct,
    isDark: Boolean,
    contrastLevel: Double,
    specVersion: ColorSpec.SpecVersion = DEFAULT_SPEC_VERSION,
    platform: Platform = DEFAULT_PLATFORM,
) :
    DynamicScheme(
        sourceColorHct,
        Variant.TONAL_SPOT,
        isDark,
        contrastLevel,
        platform,
        specVersion,
        ColorSpecs.get(specVersion)
            .getPrimaryPalette(Variant.TONAL_SPOT, sourceColorHct, isDark, platform, contrastLevel),
        ColorSpecs.get(specVersion)
            .getSecondaryPalette(Variant.TONAL_SPOT, sourceColorHct, isDark, platform, contrastLevel),
        ColorSpecs.get(specVersion)
            .getTertiaryPalette(Variant.TONAL_SPOT, sourceColorHct, isDark, platform, contrastLevel),
        ColorSpecs.get(specVersion)
            .getNeutralPalette(Variant.TONAL_SPOT, sourceColorHct, isDark, platform, contrastLevel),
        ColorSpecs.get(specVersion)
            .getNeutralVariantPalette(
                Variant.TONAL_SPOT,
                sourceColorHct,
                isDark,
                platform,
                contrastLevel,
            ),
        ColorSpecs.get(specVersion)
            .getErrorPalette(Variant.TONAL_SPOT, sourceColorHct, isDark, platform, contrastLevel),
    )
