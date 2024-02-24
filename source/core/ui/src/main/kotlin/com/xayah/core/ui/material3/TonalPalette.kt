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

import androidx.compose.ui.graphics.Color
import com.xayah.core.ui.material3.tokens.PaletteTokens

/**
 * Tonal Palette structure in Material.
 *
 * A tonal palette is comprised of 5 tonal ranges. Each tonal range includes the 13 stops, or
 * tonal swatches.
 *
 * Tonal range names are:
 * - Neutral (N)
 * - Neutral variant (NV)
 * - Primary (P)
 * - Secondary (S)
 * - Tertiary (T)
 */
internal class TonalPalette(
    // The neutral tonal range from the generated dynamic color palette.
    // Ordered from the lightest shade [neutral100] to the darkest shade [neutral0].
    val neutral100: Color,
    val neutral99: Color,
    val neutral98: Color,
    val neutral96: Color,
    val neutral95: Color,
    val neutral94: Color,
    val neutral92: Color,
    val neutral90: Color,
    val neutral87: Color,
    val neutral80: Color,
    val neutral70: Color,
    val neutral60: Color,
    val neutral50: Color,
    val neutral40: Color,
    val neutral30: Color,
    val neutral24: Color,
    val neutral22: Color,
    val neutral20: Color,
    val neutral17: Color,
    val neutral12: Color,
    val neutral10: Color,
    val neutral6: Color,
    val neutral4: Color,
    val neutral0: Color,

    // The neutral variant tonal range, sometimes called "neutral 2",  from the
    // generated dynamic color palette.
    // Ordered from the lightest shade [neutralVariant100] to the darkest shade [neutralVariant0].
    val neutralVariant100: Color,
    val neutralVariant99: Color,
    val neutralVariant98: Color,
    val neutralVariant96: Color,
    val neutralVariant95: Color,
    val neutralVariant94: Color,
    val neutralVariant92: Color,
    val neutralVariant90: Color,
    val neutralVariant87: Color,
    val neutralVariant80: Color,
    val neutralVariant70: Color,
    val neutralVariant60: Color,
    val neutralVariant50: Color,
    val neutralVariant40: Color,
    val neutralVariant30: Color,
    val neutralVariant24: Color,
    val neutralVariant22: Color,
    val neutralVariant20: Color,
    val neutralVariant17: Color,
    val neutralVariant12: Color,
    val neutralVariant10: Color,
    val neutralVariant6: Color,
    val neutralVariant4: Color,
    val neutralVariant0: Color,

    // The primary tonal range from the generated dynamic color palette.
    // Ordered from the lightest shade [primary100] to the darkest shade [primary0].
    val primary100: Color,
    val primary99: Color,
    val primary95: Color,
    val primary90: Color,
    val primary80: Color,
    val primary70: Color,
    val primary60: Color,
    val primary50: Color,
    val primary40: Color,
    val primary30: Color,
    val primary20: Color,
    val primary10: Color,
    val primary0: Color,

    // The secondary tonal range from the generated dynamic color palette.
    // Ordered from the lightest shade [secondary100] to the darkest shade [secondary0].
    val secondary100: Color,
    val secondary99: Color,
    val secondary95: Color,
    val secondary90: Color,
    val secondary80: Color,
    val secondary70: Color,
    val secondary60: Color,
    val secondary50: Color,
    val secondary40: Color,
    val secondary30: Color,
    val secondary20: Color,
    val secondary10: Color,
    val secondary0: Color,

    // The tertiary tonal range from the generated dynamic color palette.
    // Ordered from the lightest shade [tertiary100] to the darkest shade [tertiary0].
    val tertiary100: Color,
    val tertiary99: Color,
    val tertiary95: Color,
    val tertiary90: Color,
    val tertiary80: Color,
    val tertiary70: Color,
    val tertiary60: Color,
    val tertiary50: Color,
    val tertiary40: Color,
    val tertiary30: Color,
    val tertiary20: Color,
    val tertiary10: Color,
    val tertiary0: Color,

    // The error tonal range from the generated dynamic color palette.
    // Ordered from the lightest shade [error100] to the darkest shade [error0].
    val error100: Color,
    val error99: Color,
    val error95: Color,
    val error90: Color,
    val error80: Color,
    val error70: Color,
    val error60: Color,
    val error50: Color,
    val error40: Color,
    val error30: Color,
    val error20: Color,
    val error10: Color,
    val error0: Color,
)

/**
 * Baseline colors in Material.
 */
internal val BaselineTonalPalette =
    TonalPalette(
        neutral100 = PaletteTokens.Neutral100,
        neutral99 = PaletteTokens.Neutral99,
        neutral98 = PaletteTokens.Neutral98,
        neutral96 = PaletteTokens.Neutral96,
        neutral95 = PaletteTokens.Neutral95,
        neutral94 = PaletteTokens.Neutral94,
        neutral92 = PaletteTokens.Neutral92,
        neutral90 = PaletteTokens.Neutral90,
        neutral87 = PaletteTokens.Neutral87,
        neutral80 = PaletteTokens.Neutral80,
        neutral70 = PaletteTokens.Neutral70,
        neutral60 = PaletteTokens.Neutral60,
        neutral50 = PaletteTokens.Neutral50,
        neutral40 = PaletteTokens.Neutral40,
        neutral30 = PaletteTokens.Neutral30,
        neutral24 = PaletteTokens.Neutral24,
        neutral22 = PaletteTokens.Neutral22,
        neutral20 = PaletteTokens.Neutral20,
        neutral17 = PaletteTokens.Neutral17,
        neutral12 = PaletteTokens.Neutral12,
        neutral10 = PaletteTokens.Neutral10,
        neutral6 = PaletteTokens.Neutral6,
        neutral4 = PaletteTokens.Neutral4,
        neutral0 = PaletteTokens.Neutral0,
        neutralVariant100 = PaletteTokens.NeutralVariant100,
        neutralVariant99 = PaletteTokens.NeutralVariant99,
        neutralVariant95 = PaletteTokens.NeutralVariant95,
        neutralVariant90 = PaletteTokens.NeutralVariant90,
        neutralVariant80 = PaletteTokens.NeutralVariant80,
        neutralVariant70 = PaletteTokens.NeutralVariant70,
        neutralVariant60 = PaletteTokens.NeutralVariant60,
        neutralVariant50 = PaletteTokens.NeutralVariant50,
        neutralVariant40 = PaletteTokens.NeutralVariant40,
        neutralVariant30 = PaletteTokens.NeutralVariant30,
        neutralVariant20 = PaletteTokens.NeutralVariant20,
        neutralVariant10 = PaletteTokens.NeutralVariant10,
        neutralVariant0 = PaletteTokens.NeutralVariant0,
        primary100 = PaletteTokens.Primary100,
        primary99 = PaletteTokens.Primary99,
        primary95 = PaletteTokens.Primary95,
        primary90 = PaletteTokens.Primary90,
        primary80 = PaletteTokens.Primary80,
        primary70 = PaletteTokens.Primary70,
        primary60 = PaletteTokens.Primary60,
        primary50 = PaletteTokens.Primary50,
        primary40 = PaletteTokens.Primary40,
        primary30 = PaletteTokens.Primary30,
        primary20 = PaletteTokens.Primary20,
        primary10 = PaletteTokens.Primary10,
        primary0 = PaletteTokens.Primary0,
        secondary100 = PaletteTokens.Secondary100,
        secondary99 = PaletteTokens.Secondary99,
        secondary95 = PaletteTokens.Secondary95,
        secondary90 = PaletteTokens.Secondary90,
        secondary80 = PaletteTokens.Secondary80,
        secondary70 = PaletteTokens.Secondary70,
        secondary60 = PaletteTokens.Secondary60,
        secondary50 = PaletteTokens.Secondary50,
        secondary40 = PaletteTokens.Secondary40,
        secondary30 = PaletteTokens.Secondary30,
        secondary20 = PaletteTokens.Secondary20,
        secondary10 = PaletteTokens.Secondary10,
        secondary0 = PaletteTokens.Secondary0,
        tertiary100 = PaletteTokens.Tertiary100,
        tertiary99 = PaletteTokens.Tertiary99,
        tertiary95 = PaletteTokens.Tertiary95,
        tertiary90 = PaletteTokens.Tertiary90,
        tertiary80 = PaletteTokens.Tertiary80,
        tertiary70 = PaletteTokens.Tertiary70,
        tertiary60 = PaletteTokens.Tertiary60,
        tertiary50 = PaletteTokens.Tertiary50,
        tertiary40 = PaletteTokens.Tertiary40,
        tertiary30 = PaletteTokens.Tertiary30,
        tertiary20 = PaletteTokens.Tertiary20,
        tertiary10 = PaletteTokens.Tertiary10,
        tertiary0 = PaletteTokens.Tertiary0,

        // Currently unused baseline palette values
        neutralVariant98 = Color.Unspecified,
        neutralVariant96 = Color.Unspecified,
        neutralVariant94 = Color.Unspecified,
        neutralVariant92 = Color.Unspecified,
        neutralVariant87 = Color.Unspecified,
        neutralVariant24 = Color.Unspecified,
        neutralVariant22 = Color.Unspecified,
        neutralVariant17 = Color.Unspecified,
        neutralVariant12 = Color.Unspecified,
        neutralVariant6 = Color.Unspecified,
        neutralVariant4 = Color.Unspecified,

        error100 = PaletteTokens.Error100,
        error99 = PaletteTokens.Error99,
        error95 = PaletteTokens.Error95,
        error90 = PaletteTokens.Error90,
        error80 = PaletteTokens.Error80,
        error70 = PaletteTokens.Error70,
        error60 = PaletteTokens.Error60,
        error50 = PaletteTokens.Error50,
        error40 = PaletteTokens.Error40,
        error30 = PaletteTokens.Error30,
        error20 = PaletteTokens.Error20,
        error10 = PaletteTokens.Error10,
        error0 = PaletteTokens.Error0,
    )