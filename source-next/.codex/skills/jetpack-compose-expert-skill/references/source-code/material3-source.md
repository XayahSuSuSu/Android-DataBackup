# Compose Material3 Source Reference

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/MaterialTheme.kt
```kotlin
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

package androidx.compose.material3

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MotionScheme.Companion.standard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Material Theming refers to the customization of your Material Design app to better reflect your
 * product’s brand.
 *
 * Material components such as [Button] and [Checkbox] use values provided here when retrieving
 * default values.
 *
 * All values may be set by providing this component with the [colorScheme][ColorScheme],
 * [typography][Typography] and [shapes][Shapes] attributes. Use this to configure the overall theme
 * of elements within this MaterialTheme.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent MaterialTheme. This allows using a MaterialTheme at the top of
 * your application, and then separate MaterialTheme(s) for different screens / parts of your UI,
 * overriding only the parts of the theme definition that need to change.
 *
 * @sample androidx.compose.material3.samples.MaterialThemeSample
 * @param colorScheme A complete definition of the Material Color theme for this hierarchy
 * @param shapes A set of corner shapes to be used as this hierarchy's shape system
 * @param typography A set of text styles to be used as this hierarchy's typography system
 * @param content The content inheriting this theme
 */
@Composable
fun MaterialTheme(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    content: @Composable () -> Unit,
) =
    MaterialTheme(
        colorScheme = colorScheme,
        motionScheme = MaterialTheme.motionScheme,
        shapes = shapes,
        typography = typography,
        content = content,
    )

/**
 * Material Theming refers to the customization of your Material Design app to better reflect your
 * product’s brand.
 *
 * Material components such as [Button] and [Checkbox] use values provided here when retrieving
 * default values.
 *
 * All values may be set by providing this component with the [colorScheme][ColorScheme],
 * [typography][Typography] attributes. Use this to configure the overall theme of elements within
 * this MaterialTheme.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent MaterialTheme. This allows using a MaterialTheme at the top of
 * your application, and then separate MaterialTheme(s) for different screens / parts of your UI,
 * overriding only the parts of the theme definition that need to change.
 *
 * @param colorScheme A complete definition of the Material Color theme for this hierarchy
 * @param motionScheme A complete definition of the Material Motion scheme for this hierarchy
 * @param shapes A set of corner shapes to be used as this hierarchy's shape system
 * @param typography A set of text styles to be used as this hierarchy's typography system
 */
@Composable
fun MaterialTheme(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    motionScheme: MotionScheme = MaterialTheme.motionScheme,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    content: @Composable () -> Unit,
) {
    val theme =
        MaterialTheme.Values(
            colorScheme = colorScheme,
            motionScheme = motionScheme,
            shapes = shapes,
            typography = typography,
        )
    val rippleIndication = ripple()
    val selectionColors = rememberTextSelectionColors(colorScheme)
    CompositionLocalProvider(
        _localMaterialTheme provides theme,
        LocalIndication provides rippleIndication,
        LocalTextSelectionColors provides selectionColors,
    ) {
        EnsurePrecisionPointerListenersRegistered {
            ProvideTextStyle(value = typography.bodyLarge, content = content)
        }
    }
}

/**
 * Contains functions to access the current theme values provided at the call site's position in the
 * hierarchy.
 */
object MaterialTheme {

    /**
     * Retrieves the current [ColorScheme] at the call site's position in the hierarchy.
     *
     * @sample androidx.compose.material3.samples.ThemeColorSample
     */
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable get() = LocalMaterialTheme.current.colorScheme

    /**
     * Retrieves the current [Typography] at the call site's position in the hierarchy.
     *
     * @sample androidx.compose.material3.samples.ThemeTextStyleSample
     */
    val typography: Typography
        @Composable @ReadOnlyComposable get() = LocalMaterialTheme.current.typography

    /**
     * Retrieves the current [Shapes] at the call site's position in the hierarchy.
     *
     * @sample androidx.compose.material3.samples.ThemeShapeSample
     */
    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = LocalMaterialTheme.current.shapes

    /** Retrieves the current [MotionScheme] at the call site's position in the hierarchy. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val motionScheme: MotionScheme
        @Composable @ReadOnlyComposable get() = LocalMaterialTheme.current.motionScheme

    /**
     * [CompositionLocal] providing [MaterialThemeSubsystems] throughout the hierarchy. You can use
     * properties in the companion object to access specific subsystems, for example [colorScheme].
     * To provide a new value for this, use [MaterialTheme]. This API is exposed to allow retrieving
     * values from inside CompositionLocalConsumerModifierNode implementations - in most cases you
     * should use [colorScheme] and other properties directly.
     */
    val LocalMaterialTheme: CompositionLocal<Values>
        get() = _localMaterialTheme

    /**
     * A read-only `CompositionLocal` that provides the current [MotionScheme] to Material 3
     * components.
     *
     * The motion scheme is typically supplied by [MaterialTheme.motionScheme] and can be overridden
     * for specific UI subtrees by wrapping it with another [MaterialTheme].
     *
     * This API is exposed to allow retrieving motion values from inside
     * `CompositionLocalConsumerModifierNode` implementations, but in most cases it's recommended to
     * read the motion values from [MaterialTheme.motionScheme].
     */
    @Suppress("ExperimentalPropertyAnnotation")
    @ExperimentalMaterial3ExpressiveApi
    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Use [LocalMaterialTheme.current.motionScheme] instead",
    )
    val LocalMotionScheme: CompositionLocal<MotionScheme>
        get() = compositionLocalWithComputedDefaultOf {
            LocalMaterialTheme.currentValue.motionScheme
        }

    /**
     * Material 3 contains different theme subsystems to allow visual customization across a UI
     * hierarchy.
     *
     * Components use properties provided here when retrieving default values.
     *
     * @property colorScheme [ColorScheme] used by material components
     * @property typography [Typography] used by material components
     * @property shapes [Shapes] used by material components
     * @property motionScheme [MotionScheme] used by material components
     */
    @Immutable
    class Values(
        val colorScheme: ColorScheme = lightColorScheme(),
        val typography: Typography = Typography(),
        val shapes: Shapes = Shapes(),
        val motionScheme: MotionScheme = standard(),
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Values

            if (colorScheme != other.colorScheme) return false
            if (typography != other.typography) return false
            if (shapes != other.shapes) return false
            if (motionScheme != other.motionScheme) return false

            return true
        }

        override fun hashCode(): Int {
            var result = colorScheme.hashCode()
            result = 31 * result + typography.hashCode()
            result = 31 * result + shapes.hashCode()
            result = 31 * result + motionScheme.hashCode()
            return result
        }

        override fun toString(): String {
            return "Values(colorScheme=$colorScheme, " +
                "typography=$typography, shapes=$shapes, motionScheme=$motionScheme)"
        }
    }
}

/**
 * Material Expressive Theming refers to the customization of your Material Design app to better
 * reflect your product’s brand.
 *
 * Material components such as [Button] and [Checkbox] use values provided here when retrieving
 * default values.
 *
 * All values may be set by providing this component with the [colorScheme][ColorScheme],
 * [typography][Typography], [shapes][Shapes] attributes. Use this to configure the overall theme of
 * elements within this MaterialTheme.
 *
 * Any values that are not set will fall back to the defaults. To inherit the current value from the
 * theme, pass them into subsequent calls and override only the parts of the theme definition that
 * need to change.
 *
 * Alternatively, only call this function at the top of your application, and then call
 * [MaterialTheme] to specify separate MaterialTheme(s) for different screens / parts of your UI,
 * overriding only the parts of the theme definition that need to change.
 *
 * @sample androidx.compose.material3.samples.MaterialExpressiveThemeSample
 * @param colorScheme A complete definition of the Material Color theme for this hierarchy
 * @param motionScheme A complete definition of the Material motion theme for this hierarchy
 * @param shapes A set of corner shapes to be used as this hierarchy's shape system
 * @param typography A set of text styles to be used as this hierarchy's typography system
 * @param content The content inheriting this theme
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun MaterialExpressiveTheme(
    colorScheme: ColorScheme? = null,
    motionScheme: MotionScheme? = null,
    shapes: Shapes? = null,
    typography: Typography? = null,
    content: @Composable () -> Unit,
) {
    if (LocalUsingExpressiveTheme.current) {
        MaterialTheme(
            colorScheme = colorScheme ?: MaterialTheme.colorScheme,
            motionScheme = motionScheme ?: MaterialTheme.motionScheme,
            typography = typography ?: MaterialTheme.typography,
            shapes = shapes ?: MaterialTheme.shapes,
            content = content,
        )
    } else {
        CompositionLocalProvider(LocalUsingExpressiveTheme provides true) {
            MaterialTheme(
                colorScheme = colorScheme ?: expressiveLightColorScheme(),
                motionScheme = motionScheme ?: MotionScheme.expressive(),
                shapes = shapes ?: Shapes(),
                // TODO: replace with calls to Expressive typography default
                typography = typography ?: Typography(),
                content = content,
            )
        }
    }
}

internal val LocalUsingExpressiveTheme = staticCompositionLocalOf { false }

@Composable
/*@VisibleForTesting*/
internal fun rememberTextSelectionColors(colorScheme: ColorScheme): TextSelectionColors {
    val primaryColor = colorScheme.primary
    return remember(primaryColor) {
        TextSelectionColors(
            handleColor = primaryColor,
            backgroundColor = primaryColor.copy(alpha = TextSelectionBackgroundOpacity),
        )
    }
}

/*@VisibleForTesting*/
internal const val TextSelectionBackgroundOpacity = 0.4f

/** Use [MaterialTheme.LocalMaterialTheme] to access this publicly. */
@Suppress("CompositionLocalNaming")
private val _localMaterialTheme: ProvidableCompositionLocal<MaterialTheme.Values> =
    staticCompositionLocalOf {
        MaterialTheme.Values()
    }
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/ColorScheme.kt
```kotlin
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

package androidx.compose.material3

import androidx.compose.material3.tokens.ColorDarkTokens
import androidx.compose.material3.tokens.ColorLightTokens
import androidx.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.compose.material3.tokens.PaletteTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ln

/**
 * A color scheme holds all the named color parameters for a [MaterialTheme].
 *
 * Color schemes are designed to be harmonious, ensure accessible text, and distinguish UI elements
 * and surfaces from one another. There are two built-in baseline schemes, [lightColorScheme] and a
 * [darkColorScheme], that can be used as-is or customized.
 *
 * The Material color system and custom schemes provide default values for color as a starting point
 * for customization.
 *
 * To learn more about colors, see
 * [Material Design colors](https://m3.material.io/styles/color/system/overview).
 *
 * @property primary The primary color is the color displayed most frequently across your app’s
 *   screens and components.
 * @property onPrimary Color used for text and icons displayed on top of the primary color.
 * @property primaryContainer The preferred tonal color of containers.
 * @property onPrimaryContainer The color (and state variants) that should be used for content on
 *   top of [primaryContainer].
 * @property inversePrimary Color to be used as a "primary" color in places where the inverse color
 *   scheme is needed, such as the button on a SnackBar.
 * @property secondary The secondary color provides more ways to accent and distinguish your
 *   product. Secondary colors are best for:
 * - Floating action buttons
 * - Selection controls, like checkboxes and radio buttons
 * - Highlighting selected text
 * - Links and headlines
 *
 * @property onSecondary Color used for text and icons displayed on top of the secondary color.
 * @property secondaryContainer A tonal color to be used in containers.
 * @property onSecondaryContainer The color (and state variants) that should be used for content on
 *   top of [secondaryContainer].
 * @property tertiary The tertiary color that can be used to balance primary and secondary colors,
 *   or bring heightened attention to an element such as an input field.
 * @property onTertiary Color used for text and icons displayed on top of the tertiary color.
 * @property tertiaryContainer A tonal color to be used in containers.
 * @property onTertiaryContainer The color (and state variants) that should be used for content on
 *   top of [tertiaryContainer].
 * @property background The background color that appears behind scrollable content.
 * @property onBackground Color used for text and icons displayed on top of the background color.
 * @property surface The surface color that affect surfaces of components, such as cards, sheets,
 *   and menus.
 * @property onSurface Color used for text and icons displayed on top of the surface color.
 * @property surfaceVariant Another option for a color with similar uses of [surface].
 * @property onSurfaceVariant The color (and state variants) that can be used for content on top of
 *   [surface].
 * @property surfaceTint This color will be used by components that apply tonal elevation and is
 *   applied on top of [surface]. The higher the elevation the more this color is used.
 * @property inverseSurface A color that contrasts sharply with [surface]. Useful for surfaces that
 *   sit on top of other surfaces with [surface] color.
 * @property inverseOnSurface A color that contrasts well with [inverseSurface]. Useful for content
 *   that sits on top of containers that are [inverseSurface].
 * @property error The error color is used to indicate errors in components, such as invalid text in
 *   a text field.
 * @property onError Color used for text and icons displayed on top of the error color.
 * @property errorContainer The preferred tonal color of error containers.
 * @property onErrorContainer The color (and state variants) that should be used for content on top
 *   of [errorContainer].
 * @property outline Subtle color used for boundaries. Outline color role adds contrast for
 *   accessibility purposes.
 * @property outlineVariant Utility color used for boundaries for decorative elements when strong
 *   contrast is not required.
 * @property scrim Color of a scrim that obscures content.
 * @property surfaceBright A [surface] variant that is always brighter than [surface], whether in
 *   light or dark mode.
 * @property surfaceDim A [surface] variant that is always dimmer than [surface], whether in light
 *   or dark mode.
 * @property surfaceContainer A [surface] variant that affects containers of components, such as
 *   cards, sheets, and menus.
 * @property surfaceContainerHigh A [surface] variant for containers with higher emphasis than
 *   [surfaceContainer]. Use this role for content which requires more emphasis than
 *   [surfaceContainer].
 * @property surfaceContainerHighest A [surface] variant for containers with higher emphasis than
 *   [surfaceContainerHigh]. Use this role for content which requires more emphasis than
 *   [surfaceContainerHigh].
 * @property surfaceContainerLow A [surface] variant for containers with lower emphasis than
 *   [surfaceContainer]. Use this role for content which requires less emphasis than
 *   [surfaceContainer].
 * @property surfaceContainerLowest A [surface] variant for containers with lower emphasis than
 *   [surfaceContainerLow]. Use this role for content which requires less emphasis than
 *   [surfaceContainerLow].
 * @property primaryFixed A [primary] variant that maintains the same tone in light and dark themes.
 *   The fixed color role may be used instead of the equivalent container role in situations where
 *   such fixed behavior is desired.
 * @property primaryFixedDim A [primary] variant that maintains the same tone in light and dark
 *   themes. Dim roles provide a stronger, more emphasized tone relative to the equivalent fixed
 *   color.
 * @property onPrimaryFixed Color used for text and icons displayed on top of [primaryFixed] or
 *   [primaryFixedDim]. Maintains the same tone in light and dark themes.
 * @property onPrimaryFixedVariant An [onPrimaryFixed] variant which provides less emphasis. Useful
 *   when a strong contrast is not required.
 * @property secondaryFixed A [secondary] variant that maintains the same tone in light and dark
 *   themes. The fixed color role may be used instead of the equivalent container role in situations
 *   where such fixed behavior is desired.
 * @property secondaryFixedDim A [secondary] variant that maintains the same tone in light and dark
 *   themes. Dim roles provide a stronger, more emphasized tone relative to the equivalent fixed
 *   color.
 * @property onSecondaryFixed Color used for text and icons displayed on top of [secondaryFixed] or
 *   [secondaryFixedDim]. Maintains the same tone in light and dark themes.
 * @property onSecondaryFixedVariant An [onSecondaryFixed] variant which provides less emphasis.
 *   Useful when a strong contrast is not required.
 * @property tertiaryFixed A [tertiary] variant that maintains the same tone in light and dark
 *   themes. The fixed color role may be used instead of the equivalent container role in situations
 *   where such fixed behavior is desired.
 * @property tertiaryFixedDim A [tertiary] variant that maintains the same tone in light and dark
 *   themes. Dim roles provide a stronger, more emphasized tone relative to the equivalent fixed
 *   color.
 * @property onTertiaryFixed Color used for text and icons displayed on top of [tertiaryFixed] or
 *   [tertiaryFixedDim]. Maintains the same tone in light and dark themes.
 * @property onTertiaryFixedVariant An [onTertiaryFixed] variant which provides less emphasis.
 *   Useful when a strong contrast is not required.
 */
@Immutable
class ColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val onPrimaryFixed: Color,
    val onPrimaryFixedVariant: Color,
    val secondaryFixed: Color,
    val secondaryFixedDim: Color,
    val onSecondaryFixed: Color,
    val onSecondaryFixedVariant: Color,
    val tertiaryFixed: Color,
    val tertiaryFixedDim: Color,
    val onTertiaryFixed: Color,
    val onTertiaryFixedVariant: Color,
) {
    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Use constructor with additional 'fixed' container roles.",
        replaceWith =
            ReplaceWith(
                "ColorScheme(primary,\n" +
                    "onPrimary,\n" +
                    "primaryContainer,\n" +
                    "onPrimaryContainer,\n" +
                    "inversePrimary,\n" +
                    "secondary,\n" +
                    "onSecondary,\n" +
                    "secondaryContainer,\n" +
                    "onSecondaryContainer,\n" +
                    "tertiary,\n" +
                    "onTertiary,\n" +
                    "tertiaryContainer,\n" +
                    "onTertiaryContainer,\n" +
                    "background,\n" +
                    "onBackground,\n" +
                    "surface,\n" +
                    "onSurface,\n" +
                    "surfaceVariant,\n" +
                    "onSurfaceVariant,\n" +
                    "surfaceTint,\n" +
                    "inverseSurface,\n" +
                    "inverseOnSurface,\n" +
                    "error,\n" +
                    "onError,\n" +
                    "errorContainer,\n" +
                    "onErrorContainer,\n" +
                    "outline,\n" +
                    "outlineVariant,\n" +
                    "scrim,\n" +
                    "surfaceBright,\n" +
                    "surfaceDim,\n" +
                    "surfaceContainer,\n" +
                    "surfaceContainerHigh,\n" +
                    "surfaceContainerHighest,\n" +
                    "surfaceContainerLow,\n" +
                    "surfaceContainerLowest,)"
            ),
    )
    constructor(
        primary: Color,
        onPrimary: Color,
        primaryContainer: Color,
        onPrimaryContainer: Color,
        inversePrimary: Color,
        secondary: Color,
        onSecondary: Color,
        secondaryContainer: Color,
        onSecondaryContainer: Color,
        tertiary: Color,
        onTertiary: Color,
        tertiaryContainer: Color,
        onTertiaryContainer: Color,
        background: Color,
        onBackground: Color,
        surface: Color,
        onSurface: Color,
        surfaceVariant: Color,
        onSurfaceVariant: Color,
        surfaceTint: Color,
        inverseSurface: Color,
        inverseOnSurface: Color,
        error: Color,
        onError: Color,
        errorContainer: Color,
        onErrorContainer: Color,
        outline: Color,
        outlineVariant: Color,
        scrim: Color,
        surfaceBright: Color,
        surfaceDim: Color,
        surfaceContainer: Color,
        surfaceContainerHigh: Color,
        surfaceContainerHighest: Color,
        surfaceContainerLow: Color,
        surfaceContainerLowest: Color,
    ) : this(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
        primaryFixed = Color.Unspecified,
        primaryFixedDim = Color.Unspecified,
        onPrimaryFixed = Color.Unspecified,
        onPrimaryFixedVariant = Color.Unspecified,
        secondaryFixed = Color.Unspecified,
        secondaryFixedDim = Color.Unspecified,
        onSecondaryFixed = Color.Unspecified,
        onSecondaryFixedVariant = Color.Unspecified,
        tertiaryFixed = Color.Unspecified,
        tertiaryFixedDim = Color.Unspecified,
        onTertiaryFixed = Color.Unspecified,
        onTertiaryFixedVariant = Color.Unspecified,
    )

    /** Returns a copy of this ColorScheme, optionally overriding some of the values. */
    fun copy(
        primary: Color = this.primary,
        onPrimary: Color = this.onPrimary,
        primaryContainer: Color = this.primaryContainer,
        onPrimaryContainer: Color = this.onPrimaryContainer,
        inversePrimary: Color = this.inversePrimary,
        secondary: Color = this.secondary,
        onSecondary: Color = this.onSecondary,
        secondaryContainer: Color = this.secondaryContainer,
        onSecondaryContainer: Color = this.onSecondaryContainer,
        tertiary: Color = this.tertiary,
        onTertiary: Color = this.onTertiary,
        tertiaryContainer: Color = this.tertiaryContainer,
        onTertiaryContainer: Color = this.onTertiaryContainer,
        background: Color = this.background,
        onBackground: Color = this.onBackground,
        surface: Color = this.surface,
        onSurface: Color = this.onSurface,
        surfaceVariant: Color = this.surfaceVariant,
        onSurfaceVariant: Color = this.onSurfaceVariant,
        surfaceTint: Color = this.surfaceTint,
        inverseSurface: Color = this.inverseSurface,
        inverseOnSurface: Color = this.inverseOnSurface,
        error: Color = this.error,
        onError: Color = this.onError,
        errorContainer: Color = this.errorContainer,
        onErrorContainer: Color = this.onErrorContainer,
        outline: Color = this.outline,
        outlineVariant: Color = this.outlineVariant,
        scrim: Color = this.scrim,
        surfaceBright: Color = this.surfaceBright,
        surfaceDim: Color = this.surfaceDim,
        surfaceContainer: Color = this.surfaceContainer,
        surfaceContainerHigh: Color = this.surfaceContainerHigh,
        surfaceContainerHighest: Color = this.surfaceContainerHighest,
        surfaceContainerLow: Color = this.surfaceContainerLow,
        surfaceContainerLowest: Color = this.surfaceContainerLowest,
        primaryFixed: Color = this.primaryFixed,
        primaryFixedDim: Color = this.primaryFixedDim,
        onPrimaryFixed: Color = this.onPrimaryFixed,
        onPrimaryFixedVariant: Color = this.onPrimaryFixedVariant,
        secondaryFixed: Color = this.secondaryFixed,
        secondaryFixedDim: Color = this.secondaryFixedDim,
        onSecondaryFixed: Color = this.onSecondaryFixed,
        onSecondaryFixedVariant: Color = this.onSecondaryFixedVariant,
        tertiaryFixed: Color = this.tertiaryFixed,
        tertiaryFixedDim: Color = this.tertiaryFixedDim,
        onTertiaryFixed: Color = this.onTertiaryFixed,
        onTertiaryFixedVariant: Color = this.onTertiaryFixedVariant,
    ): ColorScheme =
        ColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            surfaceBright = surfaceBright,
            surfaceDim = surfaceDim,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerLowest = surfaceContainerLowest,
            primaryFixed = primaryFixed,
            primaryFixedDim = primaryFixedDim,
            onPrimaryFixed = onPrimaryFixed,
            onPrimaryFixedVariant = onPrimaryFixedVariant,
            secondaryFixed = secondaryFixed,
            secondaryFixedDim = secondaryFixedDim,
            onSecondaryFixed = onSecondaryFixed,
            onSecondaryFixedVariant = onSecondaryFixedVariant,
            tertiaryFixed = tertiaryFixed,
            tertiaryFixedDim = tertiaryFixedDim,
            onTertiaryFixed = onTertiaryFixed,
            onTertiaryFixedVariant = onTertiaryFixedVariant,
        )

    @Deprecated(
        message =
            "Maintained for binary compatibility. Use overload with additional fixed roles " +
                "instead",
        level = DeprecationLevel.HIDDEN,
    )
    fun copy(
        primary: Color = this.primary,
        onPrimary: Color = this.onPrimary,
        primaryContainer: Color = this.primaryContainer,
        onPrimaryContainer: Color = this.onPrimaryContainer,
        inversePrimary: Color = this.inversePrimary,
        secondary: Color = this.secondary,
        onSecondary: Color = this.onSecondary,
        secondaryContainer: Color = this.secondaryContainer,
        onSecondaryContainer: Color = this.onSecondaryContainer,
        tertiary: Color = this.tertiary,
        onTertiary: Color = this.onTertiary,
        tertiaryContainer: Color = this.tertiaryContainer,
        onTertiaryContainer: Color = this.onTertiaryContainer,
        background: Color = this.background,
        onBackground: Color = this.onBackground,
        surface: Color = this.surface,
        onSurface: Color = this.onSurface,
        surfaceVariant: Color = this.surfaceVariant,
        onSurfaceVariant: Color = this.onSurfaceVariant,
        surfaceTint: Color = this.surfaceTint,
        inverseSurface: Color = this.inverseSurface,
        inverseOnSurface: Color = this.inverseOnSurface,
        error: Color = this.error,
        onError: Color = this.onError,
        errorContainer: Color = this.errorContainer,
        onErrorContainer: Color = this.onErrorContainer,
        outline: Color = this.outline,
        outlineVariant: Color = this.outlineVariant,
        scrim: Color = this.scrim,
    ): ColorScheme =
        copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
        )

    @Deprecated(
        message =
            "Maintained for binary compatibility. Use overload with additional fixed roles " +
                "instead",
        level = DeprecationLevel.HIDDEN,
    )
    fun copy(
        primary: Color = this.primary,
        onPrimary: Color = this.onPrimary,
        primaryContainer: Color = this.primaryContainer,
        onPrimaryContainer: Color = this.onPrimaryContainer,
        inversePrimary: Color = this.inversePrimary,
        secondary: Color = this.secondary,
        onSecondary: Color = this.onSecondary,
        secondaryContainer: Color = this.secondaryContainer,
        onSecondaryContainer: Color = this.onSecondaryContainer,
        tertiary: Color = this.tertiary,
        onTertiary: Color = this.onTertiary,
        tertiaryContainer: Color = this.tertiaryContainer,
        onTertiaryContainer: Color = this.onTertiaryContainer,
        background: Color = this.background,
        onBackground: Color = this.onBackground,
        surface: Color = this.surface,
        onSurface: Color = this.onSurface,
        surfaceVariant: Color = this.surfaceVariant,
        onSurfaceVariant: Color = this.onSurfaceVariant,
        surfaceTint: Color = this.surfaceTint,
        inverseSurface: Color = this.inverseSurface,
        inverseOnSurface: Color = this.inverseOnSurface,
        error: Color = this.error,
        onError: Color = this.onError,
        errorContainer: Color = this.errorContainer,
        onErrorContainer: Color = this.onErrorContainer,
        outline: Color = this.outline,
        outlineVariant: Color = this.outlineVariant,
        scrim: Color = this.scrim,
        surfaceBright: Color = this.surfaceBright,
        surfaceDim: Color = this.surfaceDim,
        surfaceContainer: Color = this.surfaceContainer,
        surfaceContainerHigh: Color = this.surfaceContainerHigh,
        surfaceContainerHighest: Color = this.surfaceContainerHighest,
        surfaceContainerLow: Color = this.surfaceContainerLow,
        surfaceContainerLowest: Color = this.surfaceContainerLowest,
    ): ColorScheme =
        copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            surfaceBright = surfaceBright,
            surfaceDim = surfaceDim,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerLowest = surfaceContainerLowest,
        )

    override fun toString(): String {
        return "ColorScheme(" +
            "primary=$primary" +
            "onPrimary=$onPrimary" +
            "primaryContainer=$primaryContainer" +
            "onPrimaryContainer=$onPrimaryContainer" +
            "inversePrimary=$inversePrimary" +
            "secondary=$secondary" +
            "onSecondary=$onSecondary" +
            "secondaryContainer=$secondaryContainer" +
            "onSecondaryContainer=$onSecondaryContainer" +
            "tertiary=$tertiary" +
            "onTertiary=$onTertiary" +
            "tertiaryContainer=$tertiaryContainer" +
            "onTertiaryContainer=$onTertiaryContainer" +
            "background=$background" +
            "onBackground=$onBackground" +
            "surface=$surface" +
            "onSurface=$onSurface" +
            "surfaceVariant=$surfaceVariant" +
            "onSurfaceVariant=$onSurfaceVariant" +
            "surfaceTint=$surfaceTint" +
            "inverseSurface=$inverseSurface" +
            "inverseOnSurface=$inverseOnSurface" +
            "error=$error" +
            "onError=$onError" +
            "errorContainer=$errorContainer" +
            "onErrorContainer=$onErrorContainer" +
            "outline=$outline" +
            "outlineVariant=$outlineVariant" +
            "scrim=$scrim" +
            "surfaceBright=$surfaceBright" +
            "surfaceDim=$surfaceDim" +
            "surfaceContainer=$surfaceContainer" +
            "surfaceContainerHigh=$surfaceContainerHigh" +
            "surfaceContainerHighest=$surfaceContainerHighest" +
            "surfaceContainerLow=$surfaceContainerLow" +
            "surfaceContainerLowest=$surfaceContainerLowest" +
            "primaryFixed=$primaryFixed" +
            "primaryFixedDim=$primaryFixedDim" +
            "onPrimaryFixed=$onPrimaryContainer" +
            "onPrimaryFixedVariant=$onPrimaryFixedVariant" +
            "secondaryFixed=$secondaryFixed" +
            "secondaryFixedDim=$secondaryFixedDim" +
            "onSecondaryFixed=$onSecondaryFixed" +
            "onSecondaryFixedVariant=$onSecondaryFixedVariant" +
            "tertiaryFixed=$tertiaryFixed" +
            "tertiaryFixedDim=$tertiaryFixedDim" +
            "onTertiaryFixed=$onTertiaryFixed" +
            "onTertiaryFixedVariant=$onTertiaryFixedVariant" +
            ")"
    }

    internal var defaultButtonColorsCached: ButtonColors? = null
    internal var defaultElevatedButtonColorsCached: ButtonColors? = null
    internal var defaultFilledTonalButtonColorsCached: ButtonColors? = null
    internal var defaultOutlinedButtonColorsCached: ButtonColors? = null
    internal var defaultTextButtonColorsCached: ButtonColors? = null

    internal var defaultCardColorsCached: CardColors? = null
    internal var defaultElevatedCardColorsCached: CardColors? = null
    internal var defaultOutlinedCardColorsCached: CardColors? = null

    internal var defaultAssistChipColorsCached: ChipColors? = null
    internal var defaultElevatedAssistChipColorsCached: ChipColors? = null
    internal var defaultSuggestionChipColorsCached: ChipColors? = null
    internal var defaultElevatedSuggestionChipColorsCached: ChipColors? = null
    internal var defaultFilterChipColorsCached: SelectableChipColors? = null
    internal var defaultElevatedFilterChipColorsCached: SelectableChipColors? = null
    internal var defaultInputChipColorsCached: SelectableChipColors? = null

    internal var defaultVerticalDragHandleColorsCached: DragHandleColors? = null

    @OptIn(ExperimentalMaterial3Api::class)
    internal var defaultTopAppBarColorsCached: TopAppBarColors? = null

    internal var defaultCheckboxColorsCached: CheckboxColors? = null

    @OptIn(ExperimentalMaterial3Api::class)
    internal var defaultDatePickerColorsCached: DatePickerColors? = null

    internal var defaultIconButtonColorsCached: IconButtonColors? = null
    internal var defaultIconButtonVibrantColorsCached: IconButtonColors? = null
    internal var defaultIconToggleButtonColorsCached: IconToggleButtonColors? = null
    internal var defaultIconToggleButtonVibrantColorsCached: IconToggleButtonColors? = null
    internal var defaultFilledIconButtonColorsCached: IconButtonColors? = null
    internal var defaultFilledIconToggleButtonColorsCached: IconToggleButtonColors? = null
    internal var defaultFilledTonalIconButtonColorsCached: IconButtonColors? = null
    internal var defaultFilledTonalIconToggleButtonColorsCached: IconToggleButtonColors? = null
    internal var defaultOutlinedIconButtonColorsCached: IconButtonColors? = null
    internal var defaultOutlinedIconButtonVibrantColorsCached: IconButtonColors? = null
    internal var defaultOutlinedIconToggleButtonColorsCached: IconToggleButtonColors? = null
    internal var defaultOutlinedIconToggleButtonVibrantColorsCached: IconToggleButtonColors? = null

    internal var defaultToggleButtonColorsCached: ToggleButtonColors? = null
    internal var defaultElevatedToggleButtonColorsCached: ToggleButtonColors? = null
    internal var defaultTonalToggleButtonColorsCached: ToggleButtonColors? = null
    internal var defaultOutlinedToggleButtonColorsCached: ToggleButtonColors? = null

    internal var defaultListItemColorsCached: ListItemColors? = null
    internal var defaultSegmentedListItemColorsCached: ListItemColors? = null

    internal var defaultMenuItemColorsCached: MenuItemColors? = null
    internal var defaultMenuSelectableItemColorsCached: MenuItemColors? = null
    internal var defaultMenuSelectableItemVibrantColorsCached: MenuItemColors? = null

    internal var defaultNavigationBarItemColorsCached: NavigationBarItemColors? = null
    internal var defaultShortNavigationBarItemColorsCached: NavigationItemColors? = null

    internal var defaultNavigationRailItemColorsCached: NavigationRailItemColors? = null
    internal var defaultWideWideNavigationRailColorsCached: WideNavigationRailColors? = null
    internal var defaultWideNavigationRailItemColorsCached: NavigationItemColors? = null

    internal var defaultRadioButtonColorsCached: RadioButtonColors? = null

    internal var defaultSegmentedButtonColorsCached: SegmentedButtonColors? = null

    internal var defaultSliderColorsCached: SliderColors? = null

    internal var defaultSwitchColorsCached: SwitchColors? = null

    internal var defaultOutlinedTextFieldColorsCached: TextFieldColors? = null
    internal var defaultTextFieldColorsCached: TextFieldColors? = null

    @OptIn(ExperimentalMaterial3Api::class)
    internal var defaultTimePickerColorsCached: TimePickerColors? = null

    @OptIn(ExperimentalMaterial3Api::class)
    internal var defaultRichTooltipColorsCached: RichTooltipColors? = null

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultFloatingToolbarStandardColorsCached: FloatingToolbarColors? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultFloatingToolbarVibrantColorsCached: FloatingToolbarColors? = null

    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Use constructor with additional 'surfaceContainer' roles.",
        replaceWith =
            ReplaceWith(
                "ColorScheme(primary,\n" +
                    "onPrimary,\n" +
                    "primaryContainer,\n" +
                    "onPrimaryContainer,\n" +
                    "inversePrimary,\n" +
                    "secondary,\n" +
                    "onSecondary,\n" +
                    "secondaryContainer,\n" +
                    "onSecondaryContainer,\n" +
                    "tertiary,\n" +
                    "onTertiary,\n" +
                    "tertiaryContainer,\n" +
                    "onTertiaryContainer,\n" +
                    "background,\n" +
                    "onBackground,\n" +
                    "surface,\n" +
                    "onSurface,\n" +
                    "surfaceVariant,\n" +
                    "onSurfaceVariant,\n" +
                    "surfaceTint,\n" +
                    "inverseSurface,\n" +
                    "inverseOnSurface,\n" +
                    "error,\n" +
                    "onError,\n" +
                    "errorContainer,\n" +
                    "onErrorContainer,\n" +
                    "outline,\n" +
                    "outlineVariant,\n" +
                    "scrim,\n" +
                    "surfaceBright,\n" +
                    "surfaceDim,\n" +
                    "surfaceContainer,\n" +
                    "surfaceContainerHigh,\n" +
                    "surfaceContainerHighest,\n" +
                    "surfaceContainerLow,\n" +
                    "surfaceContainerLowest,\n" +
                    "primaryFixed,\n" +
                    "primaryFixedDim,\n" +
                    "onPrimaryFixed,\n" +
                    "onPrimaryFixedVariant,\n" +
                    "secondaryFixed,\n" +
                    "secondaryFixedDim,\n" +
                    "onSecondaryFixed,\n" +
                    "onSecondaryFixedVariant,\n" +
                    "tertiaryFixed,\n" +
                    "tertiaryFixedDim,\n" +
                    "onTertiaryFixed,\n" +
                    "onTertiaryFixedVariant" +
                    ")"
            ),
    )
    constructor(
        primary: Color,
        onPrimary: Color,
        primaryContainer: Color,
        onPrimaryContainer: Color,
        inversePrimary: Color,
        secondary: Color,
        onSecondary: Color,
        secondaryContainer: Color,
        onSecondaryContainer: Color,
        tertiary: Color,
        onTertiary: Color,
        tertiaryContainer: Color,
        onTertiaryContainer: Color,
        background: Color,
        onBackground: Color,
        surface: Color,
        onSurface: Color,
        surfaceVariant: Color,
        onSurfaceVariant: Color,
        surfaceTint: Color,
        inverseSurface: Color,
        inverseOnSurface: Color,
        error: Color,
        onError: Color,
        errorContainer: Color,
        onErrorContainer: Color,
        outline: Color,
        outlineVariant: Color,
        scrim: Color,
    ) : this(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = Color.Unspecified,
        surfaceDim = Color.Unspecified,
        surfaceContainer = Color.Unspecified,
        surfaceContainerHigh = Color.Unspecified,
        surfaceContainerHighest = Color.Unspecified,
        surfaceContainerLow = Color.Unspecified,
        surfaceContainerLowest = Color.Unspecified,
        primaryFixed = Color.Unspecified,
        primaryFixedDim = Color.Unspecified,
        onPrimaryFixed = Color.Unspecified,
        onPrimaryFixedVariant = Color.Unspecified,
        secondaryFixed = Color.Unspecified,
        secondaryFixedDim = Color.Unspecified,
        onSecondaryFixed = Color.Unspecified,
        onSecondaryFixedVariant = Color.Unspecified,
        tertiaryFixed = Color.Unspecified,
        tertiaryFixedDim = Color.Unspecified,
        onTertiaryFixed = Color.Unspecified,
        onTertiaryFixedVariant = Color.Unspecified,
    )
}

/** Returns a light Material color scheme. */
fun lightColorScheme(
    primary: Color = ColorLightTokens.Primary,
    onPrimary: Color = ColorLightTokens.OnPrimary,
    primaryContainer: Color = ColorLightTokens.PrimaryContainer,
    onPrimaryContainer: Color = ColorLightTokens.OnPrimaryContainer,
    inversePrimary: Color = ColorLightTokens.InversePrimary,
    secondary: Color = ColorLightTokens.Secondary,
    onSecondary: Color = ColorLightTokens.OnSecondary,
    secondaryContainer: Color = ColorLightTokens.SecondaryContainer,
    onSecondaryContainer: Color = ColorLightTokens.OnSecondaryContainer,
    tertiary: Color = ColorLightTokens.Tertiary,
    onTertiary: Color = ColorLightTokens.OnTertiary,
    tertiaryContainer: Color = ColorLightTokens.TertiaryContainer,
    onTertiaryContainer: Color = ColorLightTokens.OnTertiaryContainer,
    background: Color = ColorLightTokens.Background,
    onBackground: Color = ColorLightTokens.OnBackground,
    surface: Color = ColorLightTokens.Surface,
    onSurface: Color = ColorLightTokens.OnSurface,
    surfaceVariant: Color = ColorLightTokens.SurfaceVariant,
    onSurfaceVariant: Color = ColorLightTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ColorLightTokens.InverseSurface,
    inverseOnSurface: Color = ColorLightTokens.InverseOnSurface,
    error: Color = ColorLightTokens.Error,
    onError: Color = ColorLightTokens.OnError,
    errorContainer: Color = ColorLightTokens.ErrorContainer,
    onErrorContainer: Color = ColorLightTokens.OnErrorContainer,
    outline: Color = ColorLightTokens.Outline,
    outlineVariant: Color = ColorLightTokens.OutlineVariant,
    scrim: Color = ColorLightTokens.Scrim,
    surfaceBright: Color = ColorLightTokens.SurfaceBright,
    surfaceContainer: Color = ColorLightTokens.SurfaceContainer,
    surfaceContainerHigh: Color = ColorLightTokens.SurfaceContainerHigh,
    surfaceContainerHighest: Color = ColorLightTokens.SurfaceContainerHighest,
    surfaceContainerLow: Color = ColorLightTokens.SurfaceContainerLow,
    surfaceContainerLowest: Color = ColorLightTokens.SurfaceContainerLowest,
    surfaceDim: Color = ColorLightTokens.SurfaceDim,
    primaryFixed: Color = ColorLightTokens.PrimaryFixed,
    primaryFixedDim: Color = ColorLightTokens.PrimaryFixedDim,
    onPrimaryFixed: Color = ColorLightTokens.OnPrimaryFixed,
    onPrimaryFixedVariant: Color = ColorLightTokens.OnPrimaryFixedVariant,
    secondaryFixed: Color = ColorLightTokens.SecondaryFixed,
    secondaryFixedDim: Color = ColorLightTokens.SecondaryFixedDim,
    onSecondaryFixed: Color = ColorLightTokens.OnSecondaryFixed,
    onSecondaryFixedVariant: Color = ColorLightTokens.OnSecondaryFixedVariant,
    tertiaryFixed: Color = ColorLightTokens.TertiaryFixed,
    tertiaryFixedDim: Color = ColorLightTokens.TertiaryFixedDim,
    onTertiaryFixed: Color = ColorLightTokens.OnTertiaryFixed,
    onTertiaryFixedVariant: Color = ColorLightTokens.OnTertiaryFixedVariant,
): ColorScheme =
    ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceDim = surfaceDim,
        primaryFixed = primaryFixed,
        primaryFixedDim = primaryFixedDim,
        onPrimaryFixed = onPrimaryFixed,
        onPrimaryFixedVariant = onPrimaryFixedVariant,
        secondaryFixed = secondaryFixed,
        secondaryFixedDim = secondaryFixedDim,
        onSecondaryFixed = onSecondaryFixed,
        onSecondaryFixedVariant = onSecondaryFixedVariant,
        tertiaryFixed = tertiaryFixed,
        tertiaryFixedDim = tertiaryFixedDim,
        onTertiaryFixed = onTertiaryFixed,
        onTertiaryFixedVariant = onTertiaryFixedVariant,
    )

/** Returns a dark Material color scheme. */
fun darkColorScheme(
    primary: Color = ColorDarkTokens.Primary,
    onPrimary: Color = ColorDarkTokens.OnPrimary,
    primaryContainer: Color = ColorDarkTokens.PrimaryContainer,
    onPrimaryContainer: Color = ColorDarkTokens.OnPrimaryContainer,
    inversePrimary: Color = ColorDarkTokens.InversePrimary,
    secondary: Color = ColorDarkTokens.Secondary,
    onSecondary: Color = ColorDarkTokens.OnSecondary,
    secondaryContainer: Color = ColorDarkTokens.SecondaryContainer,
    onSecondaryContainer: Color = ColorDarkTokens.OnSecondaryContainer,
    tertiary: Color = ColorDarkTokens.Tertiary,
    onTertiary: Color = ColorDarkTokens.OnTertiary,
    tertiaryContainer: Color = ColorDarkTokens.TertiaryContainer,
    onTertiaryContainer: Color = ColorDarkTokens.OnTertiaryContainer,
    background: Color = ColorDarkTokens.Background,
    onBackground: Color = ColorDarkTokens.OnBackground,
    surface: Color = ColorDarkTokens.Surface,
    onSurface: Color = ColorDarkTokens.OnSurface,
    surfaceVariant: Color = ColorDarkTokens.SurfaceVariant,
    onSurfaceVariant: Color = ColorDarkTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ColorDarkTokens.InverseSurface,
    inverseOnSurface: Color = ColorDarkTokens.InverseOnSurface,
    error: Color = ColorDarkTokens.Error,
    onError: Color = ColorDarkTokens.OnError,
    errorContainer: Color = ColorDarkTokens.ErrorContainer,
    onErrorContainer: Color = ColorDarkTokens.OnErrorContainer,
    outline: Color = ColorDarkTokens.Outline,
    outlineVariant: Color = ColorDarkTokens.OutlineVariant,
    scrim: Color = ColorDarkTokens.Scrim,
    surfaceBright: Color = ColorDarkTokens.SurfaceBright,
    surfaceContainer: Color = ColorDarkTokens.SurfaceContainer,
    surfaceContainerHigh: Color = ColorDarkTokens.SurfaceContainerHigh,
    surfaceContainerHighest: Color = ColorDarkTokens.SurfaceContainerHighest,
    surfaceContainerLow: Color = ColorDarkTokens.SurfaceContainerLow,
    surfaceContainerLowest: Color = ColorDarkTokens.SurfaceContainerLowest,
    surfaceDim: Color = ColorDarkTokens.SurfaceDim,
    primaryFixed: Color = ColorDarkTokens.PrimaryFixed,
    primaryFixedDim: Color = ColorDarkTokens.PrimaryFixedDim,
    onPrimaryFixed: Color = ColorDarkTokens.OnPrimaryFixed,
    onPrimaryFixedVariant: Color = ColorDarkTokens.OnPrimaryFixedVariant,
    secondaryFixed: Color = ColorDarkTokens.SecondaryFixed,
    secondaryFixedDim: Color = ColorDarkTokens.SecondaryFixedDim,
    onSecondaryFixed: Color = ColorDarkTokens.OnSecondaryFixed,
    onSecondaryFixedVariant: Color = ColorDarkTokens.OnSecondaryFixedVariant,
    tertiaryFixed: Color = ColorDarkTokens.TertiaryFixed,
    tertiaryFixedDim: Color = ColorDarkTokens.TertiaryFixedDim,
    onTertiaryFixed: Color = ColorDarkTokens.OnTertiaryFixed,
    onTertiaryFixedVariant: Color = ColorDarkTokens.OnTertiaryFixedVariant,
): ColorScheme =
    ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceDim = surfaceDim,
        primaryFixed = primaryFixed,
        primaryFixedDim = primaryFixedDim,
        onPrimaryFixed = onPrimaryFixed,
        onPrimaryFixedVariant = onPrimaryFixedVariant,
        secondaryFixed = secondaryFixed,
        secondaryFixedDim = secondaryFixedDim,
        onSecondaryFixed = onSecondaryFixed,
        onSecondaryFixedVariant = onSecondaryFixedVariant,
        tertiaryFixed = tertiaryFixed,
        tertiaryFixedDim = tertiaryFixedDim,
        onTertiaryFixed = onTertiaryFixed,
        onTertiaryFixedVariant = onTertiaryFixedVariant,
    )

/**
 * The Material color system contains pairs of colors that are typically used for the background and
 * content color inside a component. For example, a [Button] typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [ColorScheme], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [ColorScheme.primary], this will return [ColorScheme.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return
 * [Color.Unspecified].
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 *   the theme's [ColorScheme], then returns [Color.Unspecified].
 * @see contentColorFor
 */
@Stable
fun ColorScheme.contentColorFor(backgroundColor: Color): Color =
    when (backgroundColor) {
        primary -> onPrimary
        secondary -> onSecondary
        tertiary -> onTertiary
        background -> onBackground
        error -> onError
        primaryContainer -> onPrimaryContainer
        secondaryContainer -> onSecondaryContainer
        tertiaryContainer -> onTertiaryContainer
        errorContainer -> onErrorContainer
        inverseSurface -> inverseOnSurface
        surface -> onSurface
        surfaceVariant -> onSurfaceVariant
        surfaceBright -> onSurface
        surfaceContainer -> onSurface
        surfaceContainerHigh -> onSurface
        surfaceContainerHighest -> onSurface
        surfaceContainerLow -> onSurface
        surfaceContainerLowest -> onSurface
        surfaceDim -> onSurface
        primaryFixed -> onPrimaryFixed
        primaryFixedDim -> onPrimaryFixed
        secondaryFixed -> onSecondaryFixed
        secondaryFixedDim -> onSecondaryFixed
        tertiaryFixed -> onTertiaryFixed
        tertiaryFixedDim -> onTertiaryFixed
        else -> Color.Unspecified
    }

/**
 * The Material color system contains pairs of colors that are typically used for the background and
 * content color inside a component. For example, a [Button] typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [ColorScheme], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [ColorScheme.primary], this will return [ColorScheme.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return the current
 * value of [LocalContentColor] as a best-effort color.
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 *   the theme's [ColorScheme], then returns the current value of [LocalContentColor].
 * @see ColorScheme.contentColorFor
 */
@Composable
@ReadOnlyComposable
fun contentColorFor(backgroundColor: Color) =
    MaterialTheme.colorScheme.contentColorFor(backgroundColor).takeOrElse {
        LocalContentColor.current
    }

/**
 * Computes the surface tonal color at different elevation levels e.g. surface1 through surface5.
 *
 * @param elevation Elevation value used to compute alpha of the color overlay layer.
 * @return the [ColorScheme.surface] color with an alpha of the [ColorScheme.surfaceTint] color
 *   overlaid on top of it.
 */
@Stable
fun ColorScheme.surfaceColorAtElevation(elevation: Dp): Color {
    if (elevation == 0.dp) return surface
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return surfaceTint.copy(alpha = alpha).compositeOver(surface)
}

/**
 * Returns a light Material color scheme.
 *
 * The default color scheme for [MaterialExpressiveTheme]. For dark mode, use [darkColorScheme].
 *
 * Example of MaterialExpressiveTheme toggling expressiveLightColorScheme and darkTheme.
 *
 * @sample androidx.compose.material3.samples.MaterialExpressiveThemeColorSchemeSample
 */
@ExperimentalMaterial3ExpressiveApi
fun expressiveLightColorScheme() =
    lightColorScheme(
        // TODO: Replace palette references with color token references when available.
        onPrimaryContainer = PaletteTokens.Primary30,
        onSecondaryContainer = PaletteTokens.Secondary30,
        onTertiaryContainer = PaletteTokens.Tertiary30,
        onErrorContainer = PaletteTokens.Error30,
    )

@Deprecated(
    message =
        "Maintained for binary compatibility. Use overload with additional Fixed roles instead",
    level = DeprecationLevel.HIDDEN,
)
/** Returns a light Material color scheme. */
fun lightColorScheme(
    primary: Color = ColorLightTokens.Primary,
    onPrimary: Color = ColorLightTokens.OnPrimary,
    primaryContainer: Color = ColorLightTokens.PrimaryContainer,
    onPrimaryContainer: Color = ColorLightTokens.OnPrimaryContainer,
    inversePrimary: Color = ColorLightTokens.InversePrimary,
    secondary: Color = ColorLightTokens.Secondary,
    onSecondary: Color = ColorLightTokens.OnSecondary,
    secondaryContainer: Color = ColorLightTokens.SecondaryContainer,
    onSecondaryContainer: Color = ColorLightTokens.OnSecondaryContainer,
    tertiary: Color = ColorLightTokens.Tertiary,
    onTertiary: Color = ColorLightTokens.OnTertiary,
    tertiaryContainer: Color = ColorLightTokens.TertiaryContainer,
    onTertiaryContainer: Color = ColorLightTokens.OnTertiaryContainer,
    background: Color = ColorLightTokens.Background,
    onBackground: Color = ColorLightTokens.OnBackground,
    surface: Color = ColorLightTokens.Surface,
    onSurface: Color = ColorLightTokens.OnSurface,
    surfaceVariant: Color = ColorLightTokens.SurfaceVariant,
    onSurfaceVariant: Color = ColorLightTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ColorLightTokens.InverseSurface,
    inverseOnSurface: Color = ColorLightTokens.InverseOnSurface,
    error: Color = ColorLightTokens.Error,
    onError: Color = ColorLightTokens.OnError,
    errorContainer: Color = ColorLightTokens.ErrorContainer,
    onErrorContainer: Color = ColorLightTokens.OnErrorContainer,
    outline: Color = ColorLightTokens.Outline,
    outlineVariant: Color = ColorLightTokens.OutlineVariant,
    scrim: Color = ColorLightTokens.Scrim,
    surfaceBright: Color = ColorLightTokens.SurfaceBright,
    surfaceContainer: Color = ColorLightTokens.SurfaceContainer,
    surfaceContainerHigh: Color = ColorLightTokens.SurfaceContainerHigh,
    surfaceContainerHighest: Color = ColorLightTokens.SurfaceContainerHighest,
    surfaceContainerLow: Color = ColorLightTokens.SurfaceContainerLow,
    surfaceContainerLowest: Color = ColorLightTokens.SurfaceContainerLowest,
    surfaceDim: Color = ColorLightTokens.SurfaceDim,
): ColorScheme =
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceDim = surfaceDim,
    )

@Deprecated(
    message =
        "Maintained for binary compatibility. Use overload with additional surface roles instead",
    level = DeprecationLevel.HIDDEN,
)
fun lightColorScheme(
    primary: Color = ColorLightTokens.Primary,
    onPrimary: Color = ColorLightTokens.OnPrimary,
    primaryContainer: Color = ColorLightTokens.PrimaryContainer,
    onPrimaryContainer: Color = ColorLightTokens.OnPrimaryContainer,
    inversePrimary: Color = ColorLightTokens.InversePrimary,
    secondary: Color = ColorLightTokens.Secondary,
    onSecondary: Color = ColorLightTokens.OnSecondary,
    secondaryContainer: Color = ColorLightTokens.SecondaryContainer,
    onSecondaryContainer: Color = ColorLightTokens.OnSecondaryContainer,
    tertiary: Color = ColorLightTokens.Tertiary,
    onTertiary: Color = ColorLightTokens.OnTertiary,
    tertiaryContainer: Color = ColorLightTokens.TertiaryContainer,
    onTertiaryContainer: Color = ColorLightTokens.OnTertiaryContainer,
    background: Color = ColorLightTokens.Background,
    onBackground: Color = ColorLightTokens.OnBackground,
    surface: Color = ColorLightTokens.Surface,
    onSurface: Color = ColorLightTokens.OnSurface,
    surfaceVariant: Color = ColorLightTokens.SurfaceVariant,
    onSurfaceVariant: Color = ColorLightTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ColorLightTokens.InverseSurface,
    inverseOnSurface: Color = ColorLightTokens.InverseOnSurface,
    error: Color = ColorLightTokens.Error,
    onError: Color = ColorLightTokens.OnError,
    errorContainer: Color = ColorLightTokens.ErrorContainer,
    onErrorContainer: Color = ColorLightTokens.OnErrorContainer,
    outline: Color = ColorLightTokens.Outline,
    outlineVariant: Color = ColorLightTokens.OutlineVariant,
    scrim: Color = ColorLightTokens.Scrim,
): ColorScheme =
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
    )

/** Returns a dark Material color scheme. */
@Deprecated(
    message =
        "Maintained for binary compatibility. Use overload with additional surface roles instead",
    level = DeprecationLevel.HIDDEN,
)
fun darkColorScheme(
    primary: Color = ColorDarkTokens.Primary,
    onPrimary: Color = ColorDarkTokens.OnPrimary,
    primaryContainer: Color = ColorDarkTokens.PrimaryContainer,
    onPrimaryContainer: Color = ColorDarkTokens.OnPrimaryContainer,
    inversePrimary: Color = ColorDarkTokens.InversePrimary,
    secondary: Color = ColorDarkTokens.Secondary,
    onSecondary: Color = ColorDarkTokens.OnSecondary,
    secondaryContainer: Color = ColorDarkTokens.SecondaryContainer,
    onSecondaryContainer: Color = ColorDarkTokens.OnSecondaryContainer,
    tertiary: Color = ColorDarkTokens.Tertiary,
    onTertiary: Color = ColorDarkTokens.OnTertiary,
    tertiaryContainer: Color = ColorDarkTokens.TertiaryContainer,
    onTertiaryContainer: Color = ColorDarkTokens.OnTertiaryContainer,
    background: Color = ColorDarkTokens.Background,
    onBackground: Color = ColorDarkTokens.OnBackground,
    surface: Color = ColorDarkTokens.Surface,
    onSurface: Color = ColorDarkTokens.OnSurface,
    surfaceVariant: Color = ColorDarkTokens.SurfaceVariant,
    onSurfaceVariant: Color = ColorDarkTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ColorDarkTokens.InverseSurface,
    inverseOnSurface: Color = ColorDarkTokens.InverseOnSurface,
    error: Color = ColorDarkTokens.Error,
    onError: Color = ColorDarkTokens.OnError,
    errorContainer: Color = ColorDarkTokens.ErrorContainer,
    onErrorContainer: Color = ColorDarkTokens.OnErrorContainer,
    outline: Color = ColorDarkTokens.Outline,
    outlineVariant: Color = ColorDarkTokens.OutlineVariant,
    scrim: Color = ColorDarkTokens.Scrim,
    surfaceBright: Color = ColorDarkTokens.SurfaceBright,
    surfaceContainer: Color = ColorDarkTokens.SurfaceContainer,
    surfaceContainerHigh: Color = ColorDarkTokens.SurfaceContainerHigh,
    surfaceContainerHighest: Color = ColorDarkTokens.SurfaceContainerHighest,
    surfaceContainerLow: Color = ColorDarkTokens.SurfaceContainerLow,
    surfaceContainerLowest: Color = ColorDarkTokens.SurfaceContainerLowest,
    surfaceDim: Color = ColorDarkTokens.SurfaceDim,
): ColorScheme =
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceDim = surfaceDim,
    )

@Deprecated(
    message =
        "Maintained for binary compatibility. Use overload with additional surface roles instead",
    level = DeprecationLevel.HIDDEN,
)
fun darkColorScheme(
    primary: Color = ColorDarkTokens.Primary,
    onPrimary: Color = ColorDarkTokens.OnPrimary,
    primaryContainer: Color = ColorDarkTokens.PrimaryContainer,
    onPrimaryContainer: Color = ColorDarkTokens.OnPrimaryContainer,
    inversePrimary: Color = ColorDarkTokens.InversePrimary,
    secondary: Color = ColorDarkTokens.Secondary,
    onSecondary: Color = ColorDarkTokens.OnSecondary,
    secondaryContainer: Color = ColorDarkTokens.SecondaryContainer,
    onSecondaryContainer: Color = ColorDarkTokens.OnSecondaryContainer,
    tertiary: Color = ColorDarkTokens.Tertiary,
    onTertiary: Color = ColorDarkTokens.OnTertiary,
    tertiaryContainer: Color = ColorDarkTokens.TertiaryContainer,
    onTertiaryContainer: Color = ColorDarkTokens.OnTertiaryContainer,
    background: Color = ColorDarkTokens.Background,
    onBackground: Color = ColorDarkTokens.OnBackground,
    surface: Color = ColorDarkTokens.Surface,
    onSurface: Color = ColorDarkTokens.OnSurface,
    surfaceVariant: Color = ColorDarkTokens.SurfaceVariant,
    onSurfaceVariant: Color = ColorDarkTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ColorDarkTokens.InverseSurface,
    inverseOnSurface: Color = ColorDarkTokens.InverseOnSurface,
    error: Color = ColorDarkTokens.Error,
    onError: Color = ColorDarkTokens.OnError,
    errorContainer: Color = ColorDarkTokens.ErrorContainer,
    onErrorContainer: Color = ColorDarkTokens.OnErrorContainer,
    outline: Color = ColorDarkTokens.Outline,
    outlineVariant: Color = ColorDarkTokens.OutlineVariant,
    scrim: Color = ColorDarkTokens.Scrim,
): ColorScheme =
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
    )

/**
 * Helper function for component color tokens. Here is an example on how to use component color
 * tokens: ``MaterialTheme.colorScheme.fromToken(ExtendedFabBranded.BrandedContainerColor)``
 */
@Stable
internal fun ColorScheme.fromToken(value: ColorSchemeKeyTokens): Color {
    return when (value) {
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
        ColorSchemeKeyTokens.SurfaceBright -> surfaceBright
        ColorSchemeKeyTokens.SurfaceContainer -> surfaceContainer
        ColorSchemeKeyTokens.SurfaceContainerHigh -> surfaceContainerHigh
        ColorSchemeKeyTokens.SurfaceContainerHighest -> surfaceContainerHighest
        ColorSchemeKeyTokens.SurfaceContainerLow -> surfaceContainerLow
        ColorSchemeKeyTokens.SurfaceContainerLowest -> surfaceContainerLowest
        ColorSchemeKeyTokens.SurfaceDim -> surfaceDim
        ColorSchemeKeyTokens.Tertiary -> tertiary
        ColorSchemeKeyTokens.TertiaryContainer -> tertiaryContainer
        ColorSchemeKeyTokens.PrimaryFixed -> primaryFixed
        ColorSchemeKeyTokens.PrimaryFixedDim -> primaryFixedDim
        ColorSchemeKeyTokens.OnPrimaryFixed -> onPrimaryFixed
        ColorSchemeKeyTokens.OnPrimaryFixedVariant -> onPrimaryFixedVariant
        ColorSchemeKeyTokens.SecondaryFixed -> secondaryFixed
        ColorSchemeKeyTokens.SecondaryFixedDim -> secondaryFixedDim
        ColorSchemeKeyTokens.OnSecondaryFixed -> onSecondaryFixed
        ColorSchemeKeyTokens.OnSecondaryFixedVariant -> onSecondaryFixedVariant
        ColorSchemeKeyTokens.TertiaryFixed -> tertiaryFixed
        ColorSchemeKeyTokens.TertiaryFixedDim -> tertiaryFixedDim
        ColorSchemeKeyTokens.OnTertiaryFixed -> onTertiaryFixed
        ColorSchemeKeyTokens.OnTertiaryFixedVariant -> onTertiaryFixedVariant
    }
}

/**
 * A low level of alpha used to represent disabled components, such as text in a disabled Button.
 */
internal const val DisabledAlpha = 0.38f

/**
 * Converts a color token key to the local color scheme provided by the theme The color is
 * subscribed to [MaterialTheme.colorScheme] changes.
 */
internal val ColorSchemeKeyTokens.value: Color
    @ReadOnlyComposable @Composable get() = MaterialTheme.colorScheme.fromToken(this)

/**
 * Returns [ColorScheme.surfaceColorAtElevation] with the provided elevation if
 * [LocalTonalElevationEnabled] is set to true, and the provided background color matches
 * [ColorScheme.surface]. Otherwise, the provided color is returned unchanged.
 *
 * @param backgroundColor The background color to compare to [ColorScheme.surface]
 * @param elevation The elevation provided to [ColorScheme.surfaceColorAtElevation] if
 *   [backgroundColor] matches surface.
 * @return [ColorScheme.surfaceColorAtElevation] at [elevation] if [backgroundColor] ==
 *   [ColorScheme.surface] and [LocalTonalElevationEnabled] is set to true. Else [backgroundColor]
 */
@Composable
@ReadOnlyComposable
internal fun ColorScheme.applyTonalElevation(backgroundColor: Color, elevation: Dp): Color {
    val tonalElevationEnabled = LocalTonalElevationEnabled.current
    return if (backgroundColor == surface && tonalElevationEnabled) {
        surfaceColorAtElevation(elevation)
    } else {
        backgroundColor
    }
}

/**
 * Composition Local used to check if [ColorScheme.applyTonalElevation] will be applied down the
 * tree.
 *
 * Setting this value to false will cause all subsequent surfaces down the tree to not apply
 * tonalElevation.
 */
val LocalTonalElevationEnabled = staticCompositionLocalOf { true }
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Typography.kt
```kotlin
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

package androidx.compose.material3

import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.material3.tokens.TypographyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

/**
 * The Material Design type scale includes a range of contrasting styles that support the needs of
 * your product and its content.
 *
 * Use typography to make writing legible and beautiful. Material's default type scale includes
 * contrasting and flexible styles to support a wide range of use cases.
 *
 * The type scale is a combination of thirteen styles that are supported by the type system. It
 * contains reusable categories of text, each with an intended application and meaning.
 *
 * The emphasized versions of the baseline styles add dynamism and personality to the baseline
 * styles. It can be used to further stylize select pieces of text. The emphasized states have
 * pragmatic uses, such as creating clearer division of content and drawing users' eyes to relevant
 * material.
 *
 * To learn more about typography, see
 * [Material Design typography](https://m3.material.io/styles/typography/overview).
 *
 * @property displayLarge displayLarge is the largest display text.
 * @property displayMedium displayMedium is the second largest display text.
 * @property displaySmall displaySmall is the smallest display text.
 * @property headlineLarge headlineLarge is the largest headline, reserved for short, important text
 *   or numerals. For headlines, you can choose an expressive font, such as a display, handwritten,
 *   or script style. These unconventional font designs have details and intricacy that help attract
 *   the eye.
 * @property headlineMedium headlineMedium is the second largest headline, reserved for short,
 *   important text or numerals. For headlines, you can choose an expressive font, such as a
 *   display, handwritten, or script style. These unconventional font designs have details and
 *   intricacy that help attract the eye.
 * @property headlineSmall headlineSmall is the smallest headline, reserved for short, important
 *   text or numerals. For headlines, you can choose an expressive font, such as a display,
 *   handwritten, or script style. These unconventional font designs have details and intricacy that
 *   help attract the eye.
 * @property titleLarge titleLarge is the largest title, and is typically reserved for
 *   medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
 *   subtitles.
 * @property titleMedium titleMedium is the second largest title, and is typically reserved for
 *   medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
 *   subtitles.
 * @property titleSmall titleSmall is the smallest title, and is typically reserved for
 *   medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
 *   subtitles.
 * @property bodyLarge bodyLarge is the largest body, and is typically used for long-form writing as
 *   it works well for small text sizes. For longer sections of text, a serif or sans serif typeface
 *   is recommended.
 * @property bodyMedium bodyMedium is the second largest body, and is typically used for long-form
 *   writing as it works well for small text sizes. For longer sections of text, a serif or sans
 *   serif typeface is recommended.
 * @property bodySmall bodySmall is the smallest body, and is typically used for long-form writing
 *   as it works well for small text sizes. For longer sections of text, a serif or sans serif
 *   typeface is recommended.
 * @property labelLarge labelLarge text is a call to action used in different types of buttons (such
 *   as text, outlined and contained buttons) and in tabs, dialogs, and cards. Button text is
 *   typically sans serif, using all caps text.
 * @property labelMedium labelMedium is one of the smallest font sizes. It is used sparingly to
 *   annotate imagery or to introduce a headline.
 * @property labelSmall labelSmall is one of the smallest font sizes. It is used sparingly to
 *   annotate imagery or to introduce a headline.
 * @property displayLargeEmphasized an emphasized version of [displayLarge].
 * @property displayMediumEmphasized an emphasized version of [displayMedium].
 * @property displaySmallEmphasized an emphasized version of [displaySmall].
 * @property headlineLargeEmphasized an emphasized version of [headlineLarge].
 * @property headlineMediumEmphasized an emphasized version of [headlineMedium].
 * @property headlineSmallEmphasized an emphasized version of [headlineSmall].
 * @property titleLargeEmphasized an emphasized version of [titleLarge].
 * @property titleMediumEmphasized an emphasized version of [titleMedium].
 * @property titleSmallEmphasized an emphasized version of [titleSmall].
 * @property bodyLargeEmphasized an emphasized version of [bodyLarge].
 * @property bodyMediumEmphasized an emphasized version of [bodyMedium].
 * @property bodySmallEmphasized an emphasized version of [bodySmall].
 * @property labelLargeEmphasized an emphasized version of [labelLarge].
 * @property labelMediumEmphasized an emphasized version of [labelMedium].
 * @property labelSmallEmphasized an emphasized version of [labelSmall].
 */
@Immutable
class Typography
@ExperimentalMaterial3ExpressiveApi
constructor(
    val displayLarge: TextStyle = TypographyTokens.DisplayLarge,
    val displayMedium: TextStyle = TypographyTokens.DisplayMedium,
    val displaySmall: TextStyle = TypographyTokens.DisplaySmall,
    val headlineLarge: TextStyle = TypographyTokens.HeadlineLarge,
    val headlineMedium: TextStyle = TypographyTokens.HeadlineMedium,
    val headlineSmall: TextStyle = TypographyTokens.HeadlineSmall,
    val titleLarge: TextStyle = TypographyTokens.TitleLarge,
    val titleMedium: TextStyle = TypographyTokens.TitleMedium,
    val titleSmall: TextStyle = TypographyTokens.TitleSmall,
    val bodyLarge: TextStyle = TypographyTokens.BodyLarge,
    val bodyMedium: TextStyle = TypographyTokens.BodyMedium,
    val bodySmall: TextStyle = TypographyTokens.BodySmall,
    val labelLarge: TextStyle = TypographyTokens.LabelLarge,
    val labelMedium: TextStyle = TypographyTokens.LabelMedium,
    val labelSmall: TextStyle = TypographyTokens.LabelSmall,
    displayLargeEmphasized: TextStyle = TypographyTokens.DisplayLargeEmphasized,
    displayMediumEmphasized: TextStyle = TypographyTokens.DisplayMediumEmphasized,
    displaySmallEmphasized: TextStyle = TypographyTokens.DisplaySmallEmphasized,
    headlineLargeEmphasized: TextStyle = TypographyTokens.HeadlineLargeEmphasized,
    headlineMediumEmphasized: TextStyle = TypographyTokens.HeadlineMediumEmphasized,
    headlineSmallEmphasized: TextStyle = TypographyTokens.HeadlineSmallEmphasized,
    titleLargeEmphasized: TextStyle = TypographyTokens.TitleLargeEmphasized,
    titleMediumEmphasized: TextStyle = TypographyTokens.TitleMediumEmphasized,
    titleSmallEmphasized: TextStyle = TypographyTokens.TitleSmallEmphasized,
    bodyLargeEmphasized: TextStyle = TypographyTokens.BodyLargeEmphasized,
    bodyMediumEmphasized: TextStyle = TypographyTokens.BodyMediumEmphasized,
    bodySmallEmphasized: TextStyle = TypographyTokens.BodySmallEmphasized,
    labelLargeEmphasized: TextStyle = TypographyTokens.LabelLargeEmphasized,
    labelMediumEmphasized: TextStyle = TypographyTokens.LabelMediumEmphasized,
    labelSmallEmphasized: TextStyle = TypographyTokens.LabelSmallEmphasized,
) {
    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [displayLarge]. */
    val displayLargeEmphasized = displayLargeEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [displayMedium]. */
    val displayMediumEmphasized = displayMediumEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [displaySmall]. */
    val displaySmallEmphasized = displaySmallEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [headlineLarge]. */
    val headlineLargeEmphasized = headlineLargeEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [headlineMedium]. */
    val headlineMediumEmphasized = headlineMediumEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [headlineSmall]. */
    val headlineSmallEmphasized = headlineSmallEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [titleLarge]. */
    val titleLargeEmphasized = titleLargeEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [titleMedium]. */
    val titleMediumEmphasized = titleMediumEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [titleSmall]. */
    val titleSmallEmphasized = titleSmallEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [bodyLarge]. */
    val bodyLargeEmphasized = bodyLargeEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [bodyMedium]. */
    val bodyMediumEmphasized = bodyMediumEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [bodySmall]. */
    val bodySmallEmphasized = bodySmallEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [labelLarge]. */
    val labelLargeEmphasized = labelLargeEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [labelMedium]. */
    val labelMediumEmphasized = labelMediumEmphasized

    @ExperimentalMaterial3ExpressiveApi
    /** an emphasized version of [labelSmall]. */
    val labelSmallEmphasized = labelSmallEmphasized

    /**
     * The Material Design type scale includes a range of contrasting styles that support the needs
     * of your product and its content.
     *
     * Use typography to make writing legible and beautiful. Material's default type scale includes
     * contrasting and flexible styles to support a wide range of use cases.
     *
     * The type scale is a combination of thirteen styles that are supported by the type system. It
     * contains reusable categories of text, each with an intended application and meaning.
     *
     * To learn more about typography, see
     * [Material Design typography](https://m3.material.io/styles/typography/overview).
     *
     * @param displayLarge displayLarge is the largest display text.
     * @param displayMedium displayMedium is the second largest display text.
     * @param displaySmall displaySmall is the smallest display text.
     * @param headlineLarge headlineLarge is the largest headline, reserved for short, important
     *   text or numerals. For headlines, you can choose an expressive font, such as a display,
     *   handwritten, or script style. These unconventional font designs have details and intricacy
     *   that help attract the eye.
     * @param headlineMedium headlineMedium is the second largest headline, reserved for short,
     *   important text or numerals. For headlines, you can choose an expressive font, such as a
     *   display, handwritten, or script style. These unconventional font designs have details and
     *   intricacy that help attract the eye.
     * @param headlineSmall headlineSmall is the smallest headline, reserved for short, important
     *   text or numerals. For headlines, you can choose an expressive font, such as a display,
     *   handwritten, or script style. These unconventional font designs have details and intricacy
     *   that help attract the eye.
     * @param titleLarge titleLarge is the largest title, and is typically reserved for
     *   medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
     *   subtitles.
     * @param titleMedium titleMedium is the second largest title, and is typically reserved for
     *   medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
     *   subtitles.
     * @param titleSmall titleSmall is the smallest title, and is typically reserved for
     *   medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
     *   subtitles.
     * @param bodyLarge bodyLarge is the largest body, and is typically used for long-form writing
     *   as it works well for small text sizes. For longer sections of text, a serif or sans serif
     *   typeface is recommended.
     * @param bodyMedium bodyMedium is the second largest body, and is typically used for long-form
     *   writing as it works well for small text sizes. For longer sections of text, a serif or sans
     *   serif typeface is recommended.
     * @param bodySmall bodySmall is the smallest body, and is typically used for long-form writing
     *   as it works well for small text sizes. For longer sections of text, a serif or sans serif
     *   typeface is recommended.
     * @param labelLarge labelLarge text is a call to action used in different types of buttons
     *   (such as text, outlined and contained buttons) and in tabs, dialogs, and cards. Button text
     *   is typically sans serif, using all caps text.
     * @param labelMedium labelMedium is one of the smallest font sizes. It is used sparingly to
     *   annotate imagery or to introduce a headline.
     * @param labelSmall labelSmall is one of the smallest font sizes. It is used sparingly to
     *   annotate imagery or to introduce a headline.
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    constructor(
        displayLarge: TextStyle = TypographyTokens.DisplayLarge,
        displayMedium: TextStyle = TypographyTokens.DisplayMedium,
        displaySmall: TextStyle = TypographyTokens.DisplaySmall,
        headlineLarge: TextStyle = TypographyTokens.HeadlineLarge,
        headlineMedium: TextStyle = TypographyTokens.HeadlineMedium,
        headlineSmall: TextStyle = TypographyTokens.HeadlineSmall,
        titleLarge: TextStyle = TypographyTokens.TitleLarge,
        titleMedium: TextStyle = TypographyTokens.TitleMedium,
        titleSmall: TextStyle = TypographyTokens.TitleSmall,
        bodyLarge: TextStyle = TypographyTokens.BodyLarge,
        bodyMedium: TextStyle = TypographyTokens.BodyMedium,
        bodySmall: TextStyle = TypographyTokens.BodySmall,
        labelLarge: TextStyle = TypographyTokens.LabelLarge,
        labelMedium: TextStyle = TypographyTokens.LabelMedium,
        labelSmall: TextStyle = TypographyTokens.LabelSmall,
    ) : this(
        displayLarge = displayLarge,
        displayMedium = displayMedium,
        displaySmall = displaySmall,
        headlineLarge = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall = headlineSmall,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
        displayLargeEmphasized = displayLarge,
        displayMediumEmphasized = displayMedium,
        displaySmallEmphasized = displaySmall,
        headlineLargeEmphasized = headlineLarge,
        headlineMediumEmphasized = headlineMedium,
        headlineSmallEmphasized = headlineSmall,
        titleLargeEmphasized = titleLarge,
        titleMediumEmphasized = titleMedium,
        titleSmallEmphasized = titleSmall,
        bodyLargeEmphasized = bodyLarge,
        bodyMediumEmphasized = bodyMedium,
        bodySmallEmphasized = bodySmall,
        labelLargeEmphasized = labelLarge,
        labelMediumEmphasized = labelMedium,
        labelSmallEmphasized = labelSmall,
    )

    /** Returns a copy of this Typography, optionally overriding some of the values. */
    @ExperimentalMaterial3ExpressiveApi
    fun copy(
        displayLarge: TextStyle = this.displayLarge,
        displayMedium: TextStyle = this.displayMedium,
        displaySmall: TextStyle = this.displaySmall,
        headlineLarge: TextStyle = this.headlineLarge,
        headlineMedium: TextStyle = this.headlineMedium,
        headlineSmall: TextStyle = this.headlineSmall,
        titleLarge: TextStyle = this.titleLarge,
        titleMedium: TextStyle = this.titleMedium,
        titleSmall: TextStyle = this.titleSmall,
        bodyLarge: TextStyle = this.bodyLarge,
        bodyMedium: TextStyle = this.bodyMedium,
        bodySmall: TextStyle = this.bodySmall,
        labelLarge: TextStyle = this.labelLarge,
        labelMedium: TextStyle = this.labelMedium,
        labelSmall: TextStyle = this.labelSmall,
        displayLargeEmphasized: TextStyle = this.displayLargeEmphasized,
        displayMediumEmphasized: TextStyle = this.displayMediumEmphasized,
        displaySmallEmphasized: TextStyle = this.displaySmallEmphasized,
        headlineLargeEmphasized: TextStyle = this.headlineLargeEmphasized,
        headlineMediumEmphasized: TextStyle = this.headlineMediumEmphasized,
        headlineSmallEmphasized: TextStyle = this.headlineSmallEmphasized,
        titleLargeEmphasized: TextStyle = this.titleLargeEmphasized,
        titleMediumEmphasized: TextStyle = this.titleMediumEmphasized,
        titleSmallEmphasized: TextStyle = this.titleSmallEmphasized,
        bodyLargeEmphasized: TextStyle = this.bodyLargeEmphasized,
        bodyMediumEmphasized: TextStyle = this.bodyMediumEmphasized,
        bodySmallEmphasized: TextStyle = this.bodySmallEmphasized,
        labelLargeEmphasized: TextStyle = this.labelLargeEmphasized,
        labelMediumEmphasized: TextStyle = this.labelMediumEmphasized,
        labelSmallEmphasized: TextStyle = this.labelSmallEmphasized,
    ): Typography =
        Typography(
            displayLarge = displayLarge,
            displayMedium = displayMedium,
            displaySmall = displaySmall,
            headlineLarge = headlineLarge,
            headlineMedium = headlineMedium,
            headlineSmall = headlineSmall,
            titleLarge = titleLarge,
            titleMedium = titleMedium,
            titleSmall = titleSmall,
            bodyLarge = bodyLarge,
            bodyMedium = bodyMedium,
            bodySmall = bodySmall,
            labelLarge = labelLarge,
            labelMedium = labelMedium,
            labelSmall = labelSmall,
            displayLargeEmphasized = displayLargeEmphasized,
            displayMediumEmphasized = displayMediumEmphasized,
            displaySmallEmphasized = displaySmallEmphasized,
            headlineLargeEmphasized = headlineLargeEmphasized,
            headlineMediumEmphasized = headlineMediumEmphasized,
            headlineSmallEmphasized = headlineSmallEmphasized,
            titleLargeEmphasized = titleLargeEmphasized,
            titleMediumEmphasized = titleMediumEmphasized,
            titleSmallEmphasized = titleSmallEmphasized,
            bodyLargeEmphasized = bodyLargeEmphasized,
            bodyMediumEmphasized = bodyMediumEmphasized,
            bodySmallEmphasized = bodySmallEmphasized,
            labelLargeEmphasized = labelLargeEmphasized,
            labelMediumEmphasized = labelMediumEmphasized,
            labelSmallEmphasized = labelSmallEmphasized,
        )

    /** Returns a copy of this Typography, optionally overriding some of the values. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun copy(
        displayLarge: TextStyle = this.displayLarge,
        displayMedium: TextStyle = this.displayMedium,
        displaySmall: TextStyle = this.displaySmall,
        headlineLarge: TextStyle = this.headlineLarge,
        headlineMedium: TextStyle = this.headlineMedium,
        headlineSmall: TextStyle = this.headlineSmall,
        titleLarge: TextStyle = this.titleLarge,
        titleMedium: TextStyle = this.titleMedium,
        titleSmall: TextStyle = this.titleSmall,
        bodyLarge: TextStyle = this.bodyLarge,
        bodyMedium: TextStyle = this.bodyMedium,
        bodySmall: TextStyle = this.bodySmall,
        labelLarge: TextStyle = this.labelLarge,
        labelMedium: TextStyle = this.labelMedium,
        labelSmall: TextStyle = this.labelSmall,
    ): Typography =
        copy(
            displayLarge = displayLarge,
            displayMedium = displayMedium,
            displaySmall = displaySmall,
            headlineLarge = headlineLarge,
            headlineMedium = headlineMedium,
            headlineSmall = headlineSmall,
            titleLarge = titleLarge,
            titleMedium = titleMedium,
            titleSmall = titleSmall,
            bodyLarge = bodyLarge,
            bodyMedium = bodyMedium,
            bodySmall = bodySmall,
            labelLarge = labelLarge,
            labelMedium = labelMedium,
            labelSmall = labelSmall,
            displayLargeEmphasized = this.displayLargeEmphasized,
            displayMediumEmphasized = this.displayMediumEmphasized,
            displaySmallEmphasized = this.displaySmallEmphasized,
            headlineLargeEmphasized = this.headlineLargeEmphasized,
            headlineMediumEmphasized = this.headlineMediumEmphasized,
            headlineSmallEmphasized = this.headlineSmallEmphasized,
            titleLargeEmphasized = this.titleLargeEmphasized,
            titleMediumEmphasized = this.titleMediumEmphasized,
            titleSmallEmphasized = this.titleSmallEmphasized,
            bodyLargeEmphasized = this.bodyLargeEmphasized,
            bodyMediumEmphasized = this.bodyMediumEmphasized,
            bodySmallEmphasized = this.bodySmallEmphasized,
            labelLargeEmphasized = this.labelLargeEmphasized,
            labelMediumEmphasized = this.labelMediumEmphasized,
            labelSmallEmphasized = this.labelSmallEmphasized,
        )

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Typography) return false

        if (displayLarge != other.displayLarge) return false
        if (displayMedium != other.displayMedium) return false
        if (displaySmall != other.displaySmall) return false
        if (headlineLarge != other.headlineLarge) return false
        if (headlineMedium != other.headlineMedium) return false
        if (headlineSmall != other.headlineSmall) return false
        if (titleLarge != other.titleLarge) return false
        if (titleMedium != other.titleMedium) return false
        if (titleSmall != other.titleSmall) return false
        if (bodyLarge != other.bodyLarge) return false
        if (bodyMedium != other.bodyMedium) return false
        if (bodySmall != other.bodySmall) return false
        if (labelLarge != other.labelLarge) return false
        if (labelMedium != other.labelMedium) return false
        if (labelSmall != other.labelSmall) return false
        if (displayLargeEmphasized != other.displayLargeEmphasized) return false
        if (displayMediumEmphasized != other.displayMediumEmphasized) return false
        if (displaySmallEmphasized != other.displaySmallEmphasized) return false
        if (headlineLargeEmphasized != other.headlineLargeEmphasized) return false
        if (headlineMediumEmphasized != other.headlineMediumEmphasized) return false
        if (headlineSmallEmphasized != other.headlineSmallEmphasized) return false
        if (titleLargeEmphasized != other.titleLargeEmphasized) return false
        if (titleMediumEmphasized != other.titleMediumEmphasized) return false
        if (titleSmallEmphasized != other.titleSmallEmphasized) return false
        if (bodyLargeEmphasized != other.bodyLargeEmphasized) return false
        if (bodyMediumEmphasized != other.bodyMediumEmphasized) return false
        if (bodySmallEmphasized != other.bodySmallEmphasized) return false
        if (labelLargeEmphasized != other.labelLargeEmphasized) return false
        if (labelMediumEmphasized != other.labelMediumEmphasized) return false
        if (labelSmallEmphasized != other.labelSmallEmphasized) return false
        return true
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun hashCode(): Int {
        var result = displayLarge.hashCode()
        result = 31 * result + displayMedium.hashCode()
        result = 31 * result + displaySmall.hashCode()
        result = 31 * result + headlineLarge.hashCode()
        result = 31 * result + headlineMedium.hashCode()
        result = 31 * result + headlineSmall.hashCode()
        result = 31 * result + titleLarge.hashCode()
        result = 31 * result + titleMedium.hashCode()
        result = 31 * result + titleSmall.hashCode()
        result = 31 * result + bodyLarge.hashCode()
        result = 31 * result + bodyMedium.hashCode()
        result = 31 * result + bodySmall.hashCode()
        result = 31 * result + labelLarge.hashCode()
        result = 31 * result + labelMedium.hashCode()
        result = 31 * result + labelSmall.hashCode()
        result = 31 * result + displayLargeEmphasized.hashCode()
        result = 31 * result + displayMediumEmphasized.hashCode()
        result = 31 * result + displaySmallEmphasized.hashCode()
        result = 31 * result + headlineLargeEmphasized.hashCode()
        result = 31 * result + headlineMediumEmphasized.hashCode()
        result = 31 * result + headlineSmallEmphasized.hashCode()
        result = 31 * result + titleLargeEmphasized.hashCode()
        result = 31 * result + titleMediumEmphasized.hashCode()
        result = 31 * result + titleSmallEmphasized.hashCode()
        result = 31 * result + bodyLargeEmphasized.hashCode()
        result = 31 * result + bodyMediumEmphasized.hashCode()
        result = 31 * result + bodySmallEmphasized.hashCode()
        result = 31 * result + labelLargeEmphasized.hashCode()
        result = 31 * result + labelMediumEmphasized.hashCode()
        result = 31 * result + labelSmallEmphasized.hashCode()
        return result
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun toString(): String {
        return "Typography(displayLarge=$displayLarge, displayMedium=$displayMedium," +
            "displaySmall=$displaySmall, " +
            "headlineLarge=$headlineLarge, headlineMedium=$headlineMedium," +
            " headlineSmall=$headlineSmall, " +
            "titleLarge=$titleLarge, titleMedium=$titleMedium, titleSmall=$titleSmall, " +
            "bodyLarge=$bodyLarge, bodyMedium=$bodyMedium, bodySmall=$bodySmall, " +
            "labelLarge=$labelLarge, labelMedium=$labelMedium, labelSmall=$labelSmall, " +
            "displayLargeEmphasized=$displayLargeEmphasized, " +
            "displayMediumEmphasized=$displayMediumEmphasized, " +
            "displaySmallEmphasized=$displaySmallEmphasized, " +
            "headlineLargeEmphasized=$headlineLargeEmphasized, " +
            "headlineMediumEmphasized=$headlineMediumEmphasized, " +
            "headlineSmallEmphasized=$headlineSmallEmphasized, " +
            "titleLargeEmphasized=$titleLargeEmphasized, " +
            "titleMediumEmphasized=$titleMediumEmphasized, " +
            "titleSmallEmphasized=$titleSmallEmphasized, " +
            "bodyLargeEmphasized=$bodyLargeEmphasized, " +
            "bodyMediumEmphasized=$bodyMediumEmphasized, " +
            "bodySmallEmphasized=$bodySmallEmphasized, " +
            "labelLargeEmphasized=$labelLargeEmphasized, " +
            "labelMediumEmphasized=$labelMediumEmphasized, " +
            "labelSmallEmphasized=$labelSmallEmphasized)"
    }
}

/** Helper function for component typography tokens. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun Typography.fromToken(value: TypographyKeyTokens): TextStyle {
    return when (value) {
        TypographyKeyTokens.DisplayLarge -> displayLarge
        TypographyKeyTokens.DisplayMedium -> displayMedium
        TypographyKeyTokens.DisplaySmall -> displaySmall
        TypographyKeyTokens.HeadlineLarge -> headlineLarge
        TypographyKeyTokens.HeadlineMedium -> headlineMedium
        TypographyKeyTokens.HeadlineSmall -> headlineSmall
        TypographyKeyTokens.TitleLarge -> titleLarge
        TypographyKeyTokens.TitleMedium -> titleMedium
        TypographyKeyTokens.TitleSmall -> titleSmall
        TypographyKeyTokens.BodyLarge -> bodyLarge
        TypographyKeyTokens.BodyMedium -> bodyMedium
        TypographyKeyTokens.BodySmall -> bodySmall
        TypographyKeyTokens.LabelLarge -> labelLarge
        TypographyKeyTokens.LabelMedium -> labelMedium
        TypographyKeyTokens.LabelSmall -> labelSmall
        TypographyKeyTokens.DisplayLargeEmphasized -> displayLargeEmphasized
        TypographyKeyTokens.DisplayMediumEmphasized -> displayMediumEmphasized
        TypographyKeyTokens.DisplaySmallEmphasized -> displaySmallEmphasized
        TypographyKeyTokens.HeadlineLargeEmphasized -> headlineLargeEmphasized
        TypographyKeyTokens.HeadlineMediumEmphasized -> headlineMediumEmphasized
        TypographyKeyTokens.HeadlineSmallEmphasized -> headlineSmallEmphasized
        TypographyKeyTokens.TitleLargeEmphasized -> titleLargeEmphasized
        TypographyKeyTokens.TitleMediumEmphasized -> titleMediumEmphasized
        TypographyKeyTokens.TitleSmallEmphasized -> titleSmallEmphasized
        TypographyKeyTokens.BodyLargeEmphasized -> bodyLargeEmphasized
        TypographyKeyTokens.BodyMediumEmphasized -> bodyMediumEmphasized
        TypographyKeyTokens.BodySmallEmphasized -> bodySmallEmphasized
        TypographyKeyTokens.LabelLargeEmphasized -> labelLargeEmphasized
        TypographyKeyTokens.LabelMediumEmphasized -> labelMediumEmphasized
        TypographyKeyTokens.LabelSmallEmphasized -> labelSmallEmphasized
    }
}

internal val TypographyKeyTokens.value: TextStyle
    @Composable @ReadOnlyComposable get() = MaterialTheme.typography.fromToken(this)

internal val LocalTypography = staticCompositionLocalOf { Typography() }
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Shapes.kt
```kotlin
/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.tokens.ShapeKeyTokens
import androidx.compose.material3.tokens.ShapeTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

/**
 * Material surfaces can be displayed in different shapes. Shapes direct attention, identify
 * components, communicate state, and express brand.
 *
 * The shape scale defines the style of container corners, offering a range of roundedness from
 * square to fully circular.
 *
 * There are different sizes of shapes:
 * - Extra Small
 * - Small
 * - Medium
 * - Large, Large Increased
 * - Extra Large, Extra Large Increased
 * - Extra Extra Large
 *
 * You can customize the shape system for all components in the [MaterialTheme] or you can do it on
 * a per component basis.
 *
 * You can change the shape that a component has by overriding the shape parameter for that
 * component. For example, by default, buttons use the shape style “full.” If your product requires
 * a smaller amount of roundedness, you can override the shape parameter with a different shape
 * value like [MaterialTheme.shapes.small].
 *
 * To learn more about shapes, see
 * [Material Design shapes](https://m3.material.io/styles/shape/overview).
 *
 * @param extraSmall A shape style with 4 same-sized corners whose size are bigger than
 *   [RectangleShape] and smaller than [Shapes.small]. By default autocomplete menu, select menu,
 *   snackbars, standard menu, and text fields use this shape.
 * @param small A shape style with 4 same-sized corners whose size are bigger than
 *   [Shapes.extraSmall] and smaller than [Shapes.medium]. By default chips use this shape.
 * @param medium A shape style with 4 same-sized corners whose size are bigger than [Shapes.small]
 *   and smaller than [Shapes.large]. By default cards and small FABs use this shape.
 * @param large A shape style with 4 same-sized corners whose size are bigger than [Shapes.medium]
 *   and smaller than [Shapes.extraLarge]. By default extended FABs, FABs, and navigation drawers
 *   use this shape.
 * @param extraLarge A shape style with 4 same-sized corners whose size are bigger than
 *   [Shapes.large] and smaller than [CircleShape]. By default large FABs use this shape.
 * @param largeIncreased A shape style with 4 same-sized corners whose size are bigger than
 *   [Shapes.medium] and smaller than [Shapes.extraLarge]. Slightly larger variant to
 *   [Shapes.large].
 * @param extraLargeIncreased A shape style with 4 same-sized corners whose size are bigger than
 *   [Shapes.large] and smaller than [Shapes.extraExtraLarge]. Slightly larger variant to
 *   [Shapes.extraLarge].
 * @param extraExtraLarge A shape style with 4 same-sized corners whose size are bigger than
 *   [Shapes.extraLarge] and smaller than [CircleShape].
 */
// TODO: Update new shape descriptions to list what components leverage them by default.
// TODO(b/368578382): Update 'increased' variant kdocs to reference design documentation.
@Immutable
class Shapes
@ExperimentalMaterial3ExpressiveApi
constructor(
    // Shapes None and Full are omitted as None is a RectangleShape and Full is a CircleShape.
    val extraSmall: CornerBasedShape = ShapeDefaults.ExtraSmall,
    val small: CornerBasedShape = ShapeDefaults.Small,
    val medium: CornerBasedShape = ShapeDefaults.Medium,
    val large: CornerBasedShape = ShapeDefaults.Large,
    val extraLarge: CornerBasedShape = ShapeDefaults.ExtraLarge,
    largeIncreased: CornerBasedShape = ShapeDefaults.LargeIncreased,
    extraLargeIncreased: CornerBasedShape = ShapeDefaults.ExtraLargeIncreased,
    extraExtraLarge: CornerBasedShape = ShapeDefaults.ExtraExtraLarge,
) {
    @ExperimentalMaterial3ExpressiveApi
    /**
     * A shape style with 4 same-sized corners whose size are bigger than [Shapes.medium] and
     * smaller than [Shapes.extraLarge]. Slightly larger variant to [Shapes.large].
     */
    val largeIncreased = largeIncreased

    @ExperimentalMaterial3ExpressiveApi
    /**
     * A shape style with 4 same-sized corners whose size are bigger than [Shapes.large] and smaller
     * than [Shapes.extraExtraLarge]. Slightly larger variant to [Shapes.extraLarge].
     */
    val extraLargeIncreased = extraLargeIncreased

    @ExperimentalMaterial3ExpressiveApi
    /**
     * A shape style with 4 same-sized corners whose size are bigger than [Shapes.extraLarge] and
     * smaller than [CircleShape].
     */
    val extraExtraLarge = extraExtraLarge

    /**
     * Material surfaces can be displayed in different shapes. Shapes direct attention, identify
     * components, communicate state, and express brand.
     *
     * The shape scale defines the style of container corners, offering a range of roundedness from
     * square to fully circular.
     *
     * There are different sizes of shapes:
     * - Extra Small
     * - Small
     * - Medium
     * - Large, Large Increased
     * - Extra Large, Extra Large Increased
     * - Extra Extra Large
     *
     * You can customize the shape system for all components in the [MaterialTheme] or you can do it
     * on a per component basis.
     *
     * You can change the shape that a component has by overriding the shape parameter for that
     * component. For example, by default, buttons use the shape style “full.” If your product
     * requires a smaller amount of roundedness, you can override the shape parameter with a
     * different shape value like [MaterialTheme.shapes.small].
     *
     * To learn more about shapes, see
     * [Material Design shapes](https://m3.material.io/styles/shape/overview).
     *
     * @param extraSmall A shape style with 4 same-sized corners whose size are bigger than
     *   [RectangleShape] and smaller than [Shapes.small]. By default autocomplete menu, select
     *   menu, snackbars, standard menu, and text fields use this shape.
     * @param small A shape style with 4 same-sized corners whose size are bigger than
     *   [Shapes.extraSmall] and smaller than [Shapes.medium]. By default chips use this shape.
     * @param medium A shape style with 4 same-sized corners whose size are bigger than
     *   [Shapes.small] and smaller than [Shapes.large]. By default cards and small FABs use this
     *   shape.
     * @param large A shape style with 4 same-sized corners whose size are bigger than
     *   [Shapes.medium] and smaller than [Shapes.extraLarge]. By default extended FABs, FABs, and
     *   navigation drawers use this shape.
     * @param extraLarge A shape style with 4 same-sized corners whose size are bigger than
     *   [Shapes.large] and smaller than [CircleShape]. By default large FABs use this shape.
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    constructor(
        extraSmall: CornerBasedShape = ShapeDefaults.ExtraSmall,
        small: CornerBasedShape = ShapeDefaults.Small,
        medium: CornerBasedShape = ShapeDefaults.Medium,
        large: CornerBasedShape = ShapeDefaults.Large,
        extraLarge: CornerBasedShape = ShapeDefaults.ExtraLarge,
    ) : this(
        extraSmall = extraSmall,
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge,
        largeIncreased = ShapeDefaults.LargeIncreased,
        extraLargeIncreased = ShapeDefaults.ExtraLargeIncreased,
        extraExtraLarge = ShapeDefaults.ExtraExtraLarge,
    )

    /** Returns a copy of this Shapes, optionally overriding some of the values. */
    @ExperimentalMaterial3ExpressiveApi
    fun copy(
        extraSmall: CornerBasedShape = this.extraSmall,
        small: CornerBasedShape = this.small,
        medium: CornerBasedShape = this.medium,
        large: CornerBasedShape = this.large,
        extraLarge: CornerBasedShape = this.extraLarge,
        largeIncreased: CornerBasedShape = this.largeIncreased,
        extraLargeIncreased: CornerBasedShape = this.extraLargeIncreased,
        extraExtraLarge: CornerBasedShape = this.extraExtraLarge,
    ): Shapes =
        Shapes(
            extraSmall = extraSmall,
            small = small,
            medium = medium,
            large = large,
            extraLarge = extraLarge,
            largeIncreased = largeIncreased,
            extraLargeIncreased = extraLargeIncreased,
            extraExtraLarge = extraExtraLarge,
        )

    /** Returns a copy of this Shapes, optionally overriding some of the values. */
    fun copy(
        extraSmall: CornerBasedShape = this.extraSmall,
        small: CornerBasedShape = this.small,
        medium: CornerBasedShape = this.medium,
        large: CornerBasedShape = this.large,
        extraLarge: CornerBasedShape = this.extraLarge,
    ): Shapes =
        Shapes(
            extraSmall = extraSmall,
            small = small,
            medium = medium,
            large = large,
            extraLarge = extraLarge,
        )

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shapes) return false
        if (extraSmall != other.extraSmall) return false
        if (small != other.small) return false
        if (medium != other.medium) return false
        if (large != other.large) return false
        if (extraLarge != other.extraLarge) return false
        if (largeIncreased != other.largeIncreased) return false
        if (extraLargeIncreased != other.extraLargeIncreased) return false
        if (extraExtraLarge != other.extraExtraLarge) return false
        return true
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun hashCode(): Int {
        var result = extraSmall.hashCode()
        result = 31 * result + small.hashCode()
        result = 31 * result + medium.hashCode()
        result = 31 * result + large.hashCode()
        result = 31 * result + extraLarge.hashCode()
        result = 31 * result + largeIncreased.hashCode()
        result = 31 * result + extraLargeIncreased.hashCode()
        result = 31 * result + extraExtraLarge.hashCode()
        return result
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun toString(): String {
        return "Shapes(" +
            "extraSmall=$extraSmall, " +
            "small=$small, " +
            "medium=$medium, " +
            "large=$large, " +
            "largeIncreased=$largeIncreased, " +
            "extraLarge=$extraLarge, " +
            "extralargeIncreased=$extraLargeIncreased, " +
            "extraExtraLarge=$extraExtraLarge)"
    }

    /** Cached shapes used in components */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultButtonShapesCached: ButtonShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultToggleButtonShapesCached: ToggleButtonShapes? = null
    internal var defaultVerticalDragHandleShapesCached: DragHandleShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultIconToggleButtonShapesCached: IconToggleButtonShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultIconButtonShapesCached: IconButtonShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultListItemShapesCached: ListItemShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultMenuStandaloneItemShapesCached: MenuItemShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultMenuLeadingItemShapesCached: MenuItemShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultMenuMiddleItemShapesCached: MenuItemShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultMenuTrailingItemShapesCached: MenuItemShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultMenuStandaloneGroupShapesCached: MenuGroupShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultMenuLeadingGroupShapesCached: MenuGroupShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultMenuMiddleGroupShapesCached: MenuGroupShapes? = null
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal var defaultMenuTrailingGroupShapesCached: MenuGroupShapes? = null
}

/** Contains the default values used by [Shapes] */
object ShapeDefaults {
    /** Extra small sized corner shape */
    val ExtraSmall: CornerBasedShape = ShapeTokens.CornerExtraSmall

    /** Small sized corner shape */
    val Small: CornerBasedShape = ShapeTokens.CornerSmall

    /** Medium sized corner shape */
    val Medium: CornerBasedShape = ShapeTokens.CornerMedium

    /** Large sized corner shape */
    val Large: CornerBasedShape = ShapeTokens.CornerLarge

    @ExperimentalMaterial3ExpressiveApi
    /** Large sized corner shape, slightly larger than [Large] */
    val LargeIncreased: CornerBasedShape = ShapeTokens.CornerLargeIncreased

    /** Extra large sized corner shape */
    val ExtraLarge: CornerBasedShape = ShapeTokens.CornerExtraLarge

    @ExperimentalMaterial3ExpressiveApi
    /** Extra large sized corner shape, slightly larger than [ExtraLarge] */
    val ExtraLargeIncreased: CornerBasedShape = ShapeTokens.CornerExtraLargeIncreased

    @ExperimentalMaterial3ExpressiveApi
    /** An extra extra large (XXL) sized corner shape */
    val ExtraExtraLarge: CornerBasedShape = ShapeTokens.CornerExtraExtraLarge

    // TODO(b/368578382): Update 'increased' variant kdocs to reference design documentation.
    /** A non-rounded corner size */
    internal val CornerNone: CornerSize = ShapeTokens.CornerValueNone

    /** An extra small rounded corner size */
    internal val CornerExtraSmall: CornerSize = ShapeTokens.CornerValueExtraSmall

    /** A small rounded corner size */
    internal val CornerSmall: CornerSize = ShapeTokens.CornerValueSmall

    /** A medium rounded corner size */
    internal val CornerMedium: CornerSize = ShapeTokens.CornerValueMedium

    /** A large rounded corner size */
    internal val CornerLarge: CornerSize = ShapeTokens.CornerValueLarge

    /** A large rounded corner size, slightly larger than [CornerLarge] */
    internal val CornerLargeIncreased: CornerSize = ShapeTokens.CornerValueLargeIncreased

    /** An extra large rounded corner size */
    internal val CornerExtraLarge: CornerSize = ShapeTokens.CornerValueExtraLarge

    /** An extra large rounded corner size, slightly larger than [CornerExtraLarge] */
    internal val CornerExtraLargeIncreased: CornerSize = ShapeTokens.CornerValueExtraLargeIncreased

    /** An extra extra large (XXL) rounded corner size */
    internal val CornerExtraExtraLarge: CornerSize = ShapeTokens.CornerValueExtraExtraLarge

    /** A fully rounded corner size */
    internal val CornerFull: CornerSize = CornerSize(100)
}

/** Helper function for component shape tokens. Used to grab the top values of a shape parameter. */
internal fun CornerBasedShape.top(
    bottomSize: CornerSize = ShapeDefaults.CornerNone
): CornerBasedShape {
    return copy(bottomStart = bottomSize, bottomEnd = bottomSize)
}

/**
 * Helper function for component shape tokens. Used to grab the bottom values of a shape parameter.
 */
internal fun CornerBasedShape.bottom(
    topSize: CornerSize = ShapeDefaults.CornerNone
): CornerBasedShape {
    return copy(topStart = topSize, topEnd = topSize)
}

/**
 * Helper function for component shape tokens. Used to grab the start values of a shape parameter.
 */
internal fun CornerBasedShape.start(
    endSize: CornerSize = ShapeDefaults.CornerNone
): CornerBasedShape {
    return copy(topEnd = endSize, bottomEnd = endSize)
}

/** Helper function for component shape tokens. Used to grab the end values of a shape parameter. */
internal fun CornerBasedShape.end(
    startSize: CornerSize = ShapeDefaults.CornerNone
): CornerBasedShape {
    return copy(topStart = startSize, bottomStart = startSize)
}

/**
 * Helper function for component shape tokens. Here is an example on how to use component color
 * tokens: ``MaterialTheme.shapes.fromToken(FabPrimarySmallTokens.ContainerShape)``
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun Shapes.fromToken(value: ShapeKeyTokens): Shape {
    return when (value) {
        ShapeKeyTokens.CornerExtraLarge -> extraLarge
        ShapeKeyTokens.CornerExtraLargeIncreased -> extraLargeIncreased
        ShapeKeyTokens.CornerExtraExtraLarge -> extraExtraLarge
        ShapeKeyTokens.CornerExtraLargeTop -> extraLarge.top()
        ShapeKeyTokens.CornerExtraSmall -> extraSmall
        ShapeKeyTokens.CornerExtraSmallTop -> extraSmall.top()
        ShapeKeyTokens.CornerFull -> CircleShape
        ShapeKeyTokens.CornerLarge -> large
        ShapeKeyTokens.CornerLargeIncreased -> largeIncreased
        ShapeKeyTokens.CornerLargeEnd -> large.end()
        ShapeKeyTokens.CornerLargeTop -> large.top()
        ShapeKeyTokens.CornerMedium -> medium
        ShapeKeyTokens.CornerNone -> RectangleShape
        ShapeKeyTokens.CornerSmall -> small
        ShapeKeyTokens.CornerLargeStart -> large.start()
    }
}

/**
 * Converts a shape token key to the local shape provided by the theme The color is subscribed to
 * [LocalShapes] changes
 */
internal val ShapeKeyTokens.value: Shape
    @Composable @ReadOnlyComposable get() = MaterialTheme.shapes.fromToken(this)

/** CompositionLocal used to specify the default shapes for the surfaces. */
internal val LocalShapes = staticCompositionLocalOf { Shapes() }
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Button.kt
```kotlin
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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.animateElevation
import androidx.compose.material3.internal.rememberAnimatedShape
import androidx.compose.material3.tokens.BaselineButtonTokens
import androidx.compose.material3.tokens.ButtonLargeTokens
import androidx.compose.material3.tokens.ButtonMediumTokens
import androidx.compose.material3.tokens.ButtonSmallTokens
import androidx.compose.material3.tokens.ButtonXLargeTokens
import androidx.compose.material3.tokens.ButtonXSmallTokens
import androidx.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.compose.material3.tokens.ElevatedButtonTokens
import androidx.compose.material3.tokens.FilledButtonTokens
import androidx.compose.material3.tokens.FilledTonalButtonTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.OutlinedButtonTokens
import androidx.compose.material3.tokens.TextButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * [Material Design button](https://m3.material.io/components/buttons/overview)
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post.
 *
 * ![Filled button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-button.png)
 *
 * Filled buttons are high-emphasis buttons. Filled buttons have the most visual impact after the
 * [FloatingActionButton], and should be used for important, final actions that complete a flow,
 * like "Save", "Join now", or "Confirm".
 *
 * @sample androidx.compose.material3.samples.ButtonSample
 * @sample androidx.compose.material3.samples.ButtonWithIconSample
 *
 * Button that uses a square shape instead of the default round shape:
 *
 * @sample androidx.compose.material3.samples.SquareButtonSample
 *
 * Button that utilizes the small design content padding:
 *
 * @sample androidx.compose.material3.samples.SmallButtonSample
 *
 * [Button] uses the small design spec as default. For a [Button] that uses the design for extra
 * small, medium, large, or extra large buttons:
 *
 * @sample androidx.compose.material3.samples.XSmallButtonWithIconSample
 * @sample androidx.compose.material3.samples.MediumButtonWithIconSample
 * @sample androidx.compose.material3.samples.LargeButtonWithIconSample
 * @sample androidx.compose.material3.samples.XLargeButtonWithIconSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [ElevatedButton] for an [FilledTonalButton] with a shadow.
 * - See [TextButton] for a low-emphasis button with no border.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this button's container, border (when [border] is not null),
 *   and shadow (when using [elevation])
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.buttonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. See
 *   [ButtonElevation.shadowElevation].
 * @param border the border to draw around the container of this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text, icon or image.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val containerColor = colors.containerColor(enabled)
    val contentColor = colors.contentColor(enabled)
    val shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        border = border,
        interactionSource = interactionSource,
    ) {
        ProvideContentColorTextStyle(
            contentColor = contentColor,
            textStyle = MaterialTheme.typography.labelLarge,
        ) {
            Row(
                Modifier.defaultMinSize(
                        minWidth = ButtonDefaults.MinWidth,
                        minHeight = ButtonDefaults.MinHeight,
                    )
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

// TODO add link to image of pressed button
/**
 * [Material Design button](https://m3.material.io/components/buttons/overview)
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post. It also morphs between the shapes provided in [shapes] depending on the state of the
 * interaction with the button as long as the shapes provided our [CornerBasedShape]s. If a shape in
 * [shapes] isn't a [CornerBasedShape], then button will change between the [ButtonShapes] according
 * to user interaction.
 *
 * ![Filled button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-button.png)
 *
 * Filled buttons are high-emphasis buttons. Filled buttons have the most visual impact after the
 * [FloatingActionButton], and should be used for important, final actions that complete a flow,
 * like "Save", "Join now", or "Confirm".
 *
 * @sample androidx.compose.material3.samples.ButtonWithAnimatedShapeSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [ElevatedButton] for an [FilledTonalButton] with a shadow.
 * - See [TextButton] for a low-emphasis button with no border.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param shapes the [ButtonShapes] that this button with morph between depending on the user's
 *   interaction with the button.
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.buttonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. See
 *   [ButtonElevation.shadowElevation].
 * @param border the border to draw around the container of this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text, icon or image.
 */
@Composable
@ExperimentalMaterial3ExpressiveApi
fun Button(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // TODO Load the motionScheme tokens from the component tokens file
    // MotionSchemeKeyTokens.DefaultEffects is intentional here to prevent
    // any bounce in this component.
    val defaultAnimationSpec = MotionSchemeKeyTokens.DefaultEffects.value<Float>()
    val pressed by interactionSource.collectIsPressedAsState()
    val containerColor = colors.containerColor(enabled)
    val contentColor = colors.contentColor(enabled)
    val shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp
    val buttonShape = shapeByInteraction(shapes, pressed, defaultAnimationSpec)

    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        enabled = enabled,
        shape = buttonShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        border = border,
        interactionSource = interactionSource,
    ) {
        ProvideContentColorTextStyle(
            contentColor = contentColor,
            textStyle = MaterialTheme.typography.labelLarge,
        ) {
            Row(
                Modifier.defaultMinSize(
                        minWidth = ButtonDefaults.MinWidth,
                        minHeight = ButtonDefaults.MinHeight,
                    )
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/**
 * [Material Design elevated button](https://m3.material.io/components/buttons/overview)
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post.
 *
 * ![Elevated button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-button.png)
 *
 * Elevated buttons are high-emphasis buttons that are essentially [FilledTonalButton]s with a
 * shadow. To prevent shadow creep, only use them when absolutely necessary, such as when the button
 * requires visual separation from patterned container.
 *
 * @sample androidx.compose.material3.samples.ElevatedButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [TextButton] for a low-emphasis button with no border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this button's container, border (when [border] is not null),
 *   and shadow (when using [elevation])
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.elevatedButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay. See [ButtonDefaults.elevatedButtonElevation].
 * @param border the border to draw around the container of this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text, icon or image.
 */
@Composable
fun ElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.elevatedShape,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) =
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

// TODO add link to image of pressed elevated button
/**
 * [Material Design elevated button](https://m3.material.io/components/buttons/overview)
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post. It also morphs between the shapes provided in [shapes] depending on the state of the
 * interaction with the button as long as the shapes provided our [CornerBasedShape]s. If a shape in
 * [shapes] isn't a [CornerBasedShape], then button will change between the [ButtonShapes] according
 * to user interaction.
 *
 * ![Elevated button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-button.png)
 *
 * Elevated buttons are high-emphasis buttons that are essentially [FilledTonalButton]s with a
 * shadow. To prevent shadow creep, only use them when absolutely necessary, such as when the button
 * requires visual separation from patterned container.
 *
 * @sample androidx.compose.material3.samples.ElevatedButtonWithAnimatedShapeSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [TextButton] for a low-emphasis button with no border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param shapes the [ButtonShapes] that this button with morph between depending on the user's
 *   interaction with the button.
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.elevatedButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay. See [ButtonDefaults.elevatedButtonElevation].
 * @param border the border to draw around the container of this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text, icon or image.
 */
@Composable
@ExperimentalMaterial3ExpressiveApi
fun ElevatedButton(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) =
    Button(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

/**
 * [Material Design filled tonal button](https://m3.material.io/components/buttons/overview)
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post.
 *
 * ![Filled tonal button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-tonal-button.png)
 *
 * Filled tonal buttons are medium-emphasis buttons that is an alternative middle ground between
 * default [Button]s (filled) and [OutlinedButton]s. They can be used in contexts where
 * lower-priority button requires slightly more emphasis than an outline would give, such as "Next"
 * in an onboarding flow. Tonal buttons use the secondary color mapping.
 *
 * @sample androidx.compose.material3.samples.FilledTonalButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [ElevatedButton] for a [FilledTonalButton] with a shadow.
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [TextButton] for a low-emphasis button with no border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this button's container, border (when [border] is not null),
 *   and shadow (when using [elevation])
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.filledTonalButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay.
 * @param border the border to draw around the container of this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text, icon or image.
 */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) =
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

// TODO add link to image of pressed filled tonal button
/**
 * [Material Design filled tonal button](https://m3.material.io/components/buttons/overview)
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post. It also morphs between the shapes provided in [shapes] depending on the state of the
 * interaction with the button as long as the shapes provided our [CornerBasedShape]s. If a shape in
 * [shapes] isn't a [CornerBasedShape], then button will change between the [ButtonShapes] according
 * to user interaction.
 *
 * ![Filled tonal button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-tonal-button.png)
 *
 * Filled tonal buttons are medium-emphasis buttons that is an alternative middle ground between
 * default [Button]s (filled) and [OutlinedButton]s. They can be used in contexts where
 * lower-priority button requires slightly more emphasis than an outline would give, such as "Next"
 * in an onboarding flow. Tonal buttons use the secondary color mapping.
 *
 * @sample androidx.compose.material3.samples.FilledTonalButtonWithAnimatedShapeSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [ElevatedButton] for a [FilledTonalButton] with a shadow.
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [TextButton] for a low-emphasis button with no border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param shapes the [ButtonShapes] that this button with morph between depending on the user's
 *   interaction with the button.
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.filledTonalButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay.
 * @param border the border to draw around the container of this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text, icon or image.
 */
@Composable
@ExperimentalMaterial3ExpressiveApi
fun FilledTonalButton(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) =
    Button(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

/**
 * [Material Design outlined button](https://m3.material.io/components/buttons/overview)
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post.
 *
 * ![Outlined button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-button.png)
 *
 * Outlined buttons are medium-emphasis buttons. They contain actions that are important, but are
 * not the primary action in an app. Outlined buttons pair well with [Button]s to indicate an
 * alternative, secondary action.
 *
 * @sample androidx.compose.material3.samples.OutlinedButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [TextButton] for a low-emphasis button with no border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this button's container, border (when [border] is not null),
 *   and shadow (when using [elevation]).
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.outlinedButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay.
 * @param border the border to draw around the container of this button. Pass `null` for no border.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text, icon or image.
 */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) =
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

// TODO add link to image of pressed outlined button
/**
 * [Material Design outlined button](https://m3.material.io/components/buttons/overview)
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post. It also morphs between the shapes provided in [shapes] depending on the state of the
 * interaction with the button as long as the shapes provided our [CornerBasedShape]s. If a shape in
 * [shapes] isn't a [CornerBasedShape], then button will change between the [ButtonShapes] according
 * to user interaction.
 *
 * ![Outlined button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-button.png)
 *
 * Outlined buttons are medium-emphasis buttons. They contain actions that are important, but are
 * not the primary action in an app. Outlined buttons pair well with [Button]s to indicate an
 * alternative, secondary action.
 *
 * @sample androidx.compose.material3.samples.OutlinedButtonWithAnimatedShapeSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [TextButton] for a low-emphasis button with no border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param shapes the [ButtonShapes] that this button with morph between depending on the user's
 *   interaction with the button.
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.outlinedButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay.
 * @param border the border to draw around the container of this button. Pass `null` for no border.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text, icon or image.
 */
@Composable
@ExperimentalMaterial3ExpressiveApi
fun OutlinedButton(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) =
    Button(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

/**
 * [Material Design text button](https://m3.material.io/components/buttons/overview)
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post.
 *
 * ![Text button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/text-button.png)
 *
 * Text buttons are typically used for less-pronounced actions, including those located in dialogs
 * and cards. In cards, text buttons help maintain an emphasis on card content. Text buttons are
 * used for the lowest priority actions, especially when presenting multiple options.
 *
 * @sample androidx.compose.material3.samples.TextButtonWithAnimatedShapeSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [ElevatedButton] for a [FilledTonalButton] with a shadow.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this button's container, border (when [border] is not null),
 *   and shadow (when using [elevation])
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.textButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay. A TextButton typically has no elevation, and the default value is `null`. See
 *   [ElevatedButton] for a button with elevation.
 * @param border the border to draw around the container of this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text.
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) =
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

// TODO add link to image of pressed text button
/**
 * [Material Design text button](https://m3.material.io/components/buttons/overview)
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post. It also morphs between the shapes provided in [shapes] depending on the state of the
 * interaction with the button as long as the shapes provided our [CornerBasedShape]s. If a shape in
 * [shapes] isn't a [CornerBasedShape], then button will change between the [ButtonShapes] according
 * to user interaction.
 *
 * ![Text button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/text-button.png)
 *
 * Text buttons are typically used for less-pronounced actions, including those located in dialogs
 * and cards. In cards, text buttons help maintain an emphasis on card content. Text buttons are
 * used for the lowest priority actions, especially when presenting multiple options.
 *
 * @sample androidx.compose.material3.samples.TextButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [ElevatedButton] for a [FilledTonalButton] with a shadow.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param shapes the [ButtonShapes] that this button with morph between depending on the user's
 *   interaction with the button.
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.textButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay. A TextButton typically has no elevation, and the default value is `null`. See
 *   [ElevatedButton] for a button with elevation.
 * @param border the border to draw around the container of this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun TextButton(
    onClick: () -> Unit,
    shapes: ButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) =
    Button(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

// TODO(b/201341237): Use token values for 0 elevation?
// TODO(b/201341237): Use token values for null border?
// TODO(b/201341237): Use token values for no color (transparent)?
/**
 * Contains the default values used by all 5 button types.
 *
 * Default values that apply to all buttons types are [MinWidth], [MinHeight], [IconSize], and
 * [IconSpacing].
 *
 * A default value that applies only to [Button], [ElevatedButton], [FilledTonalButton], and
 * [OutlinedButton] is [ContentPadding].
 *
 * Default values that apply only to [Button] are [buttonColors] and [buttonElevation]. Default
 * values that apply only to [ElevatedButton] are [elevatedButtonColors] and
 * [elevatedButtonElevation]. Default values that apply only to [FilledTonalButton] are
 * [filledTonalButtonColors] and [filledTonalButtonElevation]. A default value that applies only to
 * [OutlinedButton] is [outlinedButtonColors]. Default values that apply only to [TextButton] is
 * [textButtonColors].
 */
object ButtonDefaults {

    private val ButtonLeadingSpace = BaselineButtonTokens.LeadingSpace
    private val ButtonTrailingSpace = BaselineButtonTokens.TrailingSpace
    private val ButtonWithIconStartpadding = 16.dp
    private val SmallStartPadding = ButtonSmallTokens.LeadingSpace
    private val SmallEndPadding = ButtonSmallTokens.TrailingSpace
    private val ButtonVerticalPadding = 8.dp

    /**
     * The default content padding used by [Button], [ElevatedButton], [FilledTonalButton],
     * [OutlinedButton], and [TextButton] buttons.
     * - See [ButtonWithIconContentPadding] for content padding used by [Button] that contains
     *   [Icon].
     */
    val ContentPadding =
        PaddingValues(
            start = ButtonLeadingSpace,
            top = ButtonVerticalPadding,
            end = ButtonTrailingSpace,
            bottom = ButtonVerticalPadding,
        )

    /** The default content padding used by [Button] that contains an [Icon]. */
    val ButtonWithIconContentPadding =
        PaddingValues(
            start = ButtonWithIconStartpadding,
            top = ButtonVerticalPadding,
            end = ButtonTrailingSpace,
            bottom = ButtonVerticalPadding,
        )

    /** The default content padding used for small [Button] */
    @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
    @ExperimentalMaterial3ExpressiveApi
    val SmallButtonContentPadding
        get() =
            PaddingValues(
                start = SmallStartPadding,
                top = ButtonVerticalPadding,
                end = SmallEndPadding,
                bottom = ButtonVerticalPadding,
            )

    /** The default content padding used for small [Button] */
    @ExperimentalMaterial3ExpressiveApi
    val SmallContentPadding
        get() =
            PaddingValues(
                start = SmallStartPadding,
                top = SmallVerticalPadding,
                end = SmallEndPadding,
                bottom = SmallVerticalPadding,
            )

    private fun getSmallContentPadding(hasStartIcon: Boolean, hasEndIcon: Boolean) =
        PaddingValues(
            start = if (hasStartIcon) IconSmallHorizontalPadding else SmallStartPadding,
            top = SmallVerticalPadding,
            end = if (hasEndIcon) IconSmallHorizontalPadding else SmallEndPadding,
            bottom = SmallVerticalPadding,
        )

    /** Default content padding for an extra small button. */
    @ExperimentalMaterial3ExpressiveApi
    val ExtraSmallContentPadding
        get() =
            PaddingValues(
                // TODO update with the value from ButtonXSmallTokens.kt once it's been corrected
                start = 12.dp,
                end = 12.dp,
                top = 6.dp,
                bottom = 6.dp,
            )

    /** Default content padding for a medium button. */
    @ExperimentalMaterial3ExpressiveApi
    val MediumContentPadding
        get() =
            PaddingValues(
                start = MediumLeadingPadding,
                top = MediumVerticalPadding,
                end = MediumTrailingPadding,
                bottom = MediumVerticalPadding,
            )

    private fun getMediumContentPadding(hasLeadingIcon: Boolean, hasTrailingIcon: Boolean) =
        PaddingValues(
            start = if (hasLeadingIcon) IconMediumLeadingPadding else MediumLeadingPadding,
            top = MediumVerticalPadding,
            end = if (hasTrailingIcon) IconMediumTrailingPadding else MediumTrailingPadding,
            bottom = MediumVerticalPadding,
        )

    /** Default content padding for a large button. */
    @ExperimentalMaterial3ExpressiveApi
    val LargeContentPadding
        get() =
            PaddingValues(
                start = LargeLeadingPadding,
                top = LargeVerticalPadding,
                end = LargeTrailingPadding,
                bottom = LargeVerticalPadding,
            )

    private fun getLargeContentPadding(hasLeadingIcon: Boolean, hasTrailingIcon: Boolean) =
        PaddingValues(
            start = if (hasLeadingIcon) IconLargeLeadingPadding else LargeLeadingPadding,
            top = LargeVerticalPadding,
            end = if (hasTrailingIcon) IconLargeTrailingPadding else LargeTrailingPadding,
            bottom = LargeVerticalPadding,
        )

    /** Default content padding for an extra large button. */
    @ExperimentalMaterial3ExpressiveApi
    val ExtraLargeContentPadding
        get() =
            PaddingValues(
                start = ButtonXLargeTokens.LeadingSpace,
                end = ButtonXLargeTokens.TrailingSpace,
                top = 48.dp,
                bottom = 48.dp,
            )

    private val TextButtonHorizontalPadding = 12.dp

    /**
     * The default content padding used by [TextButton].
     * - See [TextButtonWithIconContentPadding] for content padding used by [TextButton] that
     *   contains [Icon].
     *
     * Note: it's recommended to use [ContentPadding] instead for a more consistent look between all
     * buttons variants.
     */
    val TextButtonContentPadding =
        PaddingValues(
            start = TextButtonHorizontalPadding,
            top = ContentPadding.calculateTopPadding(),
            end = TextButtonHorizontalPadding,
            bottom = ContentPadding.calculateBottomPadding(),
        )

    private val TextButtonWithIconHorizontalEndPadding = 16.dp

    /**
     * The default content padding used by [TextButton] that contains an [Icon].
     *
     * Note: it's recommended to use [ButtonWithIconContentPadding] instead for a more consistent
     * look between all buttons variants.
     */
    val TextButtonWithIconContentPadding =
        PaddingValues(
            start = TextButtonHorizontalPadding,
            top = ContentPadding.calculateTopPadding(),
            end = TextButtonWithIconHorizontalEndPadding,
            bottom = ContentPadding.calculateBottomPadding(),
        )

    /**
     * The default min width applied for small buttons. Note that you can override it by applying
     * Modifier.widthIn directly on the button composable.
     */
    val MinWidth = 58.dp

    /**
     * The default min height applied for small buttons. Note that you can override it by applying
     * Modifier.heightIn directly on the button composable.
     */
    val MinHeight =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            36.dp
        } else {
            ButtonSmallTokens.ContainerHeight
        }

    /** The default height for a extra small button container. */
    @ExperimentalMaterial3ExpressiveApi
    val ExtraSmallContainerHeight = ButtonXSmallTokens.ContainerHeight

    /** The default height for a medium button container. */
    @ExperimentalMaterial3ExpressiveApi
    val MediumContainerHeight =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            46.dp
        } else {
            ButtonMediumTokens.ContainerHeight
        }

    /** The default height for a large button container. */
    @ExperimentalMaterial3ExpressiveApi
    val LargeContainerHeight =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            54.dp
        } else {
            ButtonLargeTokens.ContainerHeight
        }

    /** The default height for a extra large button container. */
    @ExperimentalMaterial3ExpressiveApi
    val ExtraLargeContainerHeight = ButtonXLargeTokens.ContainerHeight

    /** The default size of the icon when used inside a small button. */
    // TODO update with the correct value in BaselineButtonTokens when available
    val IconSize = 18.dp

    /** The default size of the icon used inside an extra small button. */
    @ExperimentalMaterial3ExpressiveApi val ExtraSmallIconSize = ButtonXSmallTokens.IconSize

    /** The expressive size of the icon used inside a small button. */
    @ExperimentalMaterial3ExpressiveApi val SmallIconSize = ButtonSmallTokens.IconSize

    /** The default size of the icon used inside a medium button. */
    @ExperimentalMaterial3ExpressiveApi val MediumIconSize = ButtonMediumTokens.IconSize

    /** The default size of the icon used inside a large button. */
    @ExperimentalMaterial3ExpressiveApi
    val LargeIconSize =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            24.dp
        } else {
            ButtonLargeTokens.IconSize
        }

    /** The default size of the icon used inside an extra large button. */
    @ExperimentalMaterial3ExpressiveApi val ExtraLargeIconSize = ButtonXLargeTokens.IconSize

    /**
     * The default size of the spacing between an icon and a text when they used inside a small
     * button.
     */
    val IconSpacing = ButtonSmallTokens.IconLabelSpace

    /**
     * The default spacing between an icon and a text when they used inside any extra small button.
     */
    // TODO use the value from ButtonXSmallTokens.kt once it's been corrected
    @ExperimentalMaterial3ExpressiveApi val ExtraSmallIconSpacing = 4.dp

    /** The default spacing between an icon and a text when they're inside any medium button. */
    @ExperimentalMaterial3ExpressiveApi val MediumIconSpacing = ButtonMediumTokens.IconLabelSpace

    /** The default spacing between an icon and a text when they're inside any large button. */
    @ExperimentalMaterial3ExpressiveApi
    val LargeIconSpacing =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            8.dp
        } else {
            ButtonLargeTokens.IconLabelSpace
        }

    /**
     * The default spacing between an icon and a text when they used inside any extra large button.
     */
    @ExperimentalMaterial3ExpressiveApi
    val ExtraLargeIconSpacing = ButtonXLargeTokens.IconLabelSpace

    /** Square shape for default buttons. */
    @ExperimentalMaterial3ExpressiveApi
    val squareShape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeSquare.value

    /** Pressed shape for default buttons. */
    @ExperimentalMaterial3ExpressiveApi
    val pressedShape: Shape
        @Composable get() = ButtonSmallTokens.PressedContainerShape.value

    /** Pressed shape for extra small buttons. */
    @ExperimentalMaterial3ExpressiveApi
    val extraSmallPressedShape: Shape
        @Composable get() = ButtonXSmallTokens.PressedContainerShape.value

    /** Pressed shape for medium buttons. */
    @ExperimentalMaterial3ExpressiveApi
    val mediumPressedShape: Shape
        @Composable get() = ButtonMediumTokens.PressedContainerShape.value

    /** Pressed shape for large buttons. */
    @ExperimentalMaterial3ExpressiveApi
    val largePressedShape: Shape
        @Composable get() = ButtonLargeTokens.PressedContainerShape.value

    /** Pressed shape for extra large buttons. */
    @ExperimentalMaterial3ExpressiveApi
    val extraLargePressedShape: Shape
        @Composable get() = ButtonXLargeTokens.PressedContainerShape.value

    /** Default shape for a button. */
    val shape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /** Default shape for an elevated button. */
    val elevatedShape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /** Default shape for a filled tonal button. */
    val filledTonalShape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /** Default shape for an outlined button. */
    val outlinedShape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /** Default shape for a text button. */
    val textShape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /**
     * Creates a [ButtonShapes] that represents the default shape and pressed shape used in a
     * button.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun shapes() = MaterialTheme.shapes.defaultButtonShapes

    /**
     * Creates a [ButtonShapes] that represents the default shape and pressedShape used in a
     * [Button] and its variants.
     *
     * @param shape the unchecked shape for [ButtonShapes]
     * @param pressedShape the unchecked shape for [ButtonShapes]
     */
    @Composable
    @ExperimentalMaterial3ExpressiveApi
    fun shapes(shape: Shape? = null, pressedShape: Shape? = null): ButtonShapes =
        MaterialTheme.shapes.defaultButtonShapes.copy(shape = shape, pressedShape = pressedShape)

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val Shapes.defaultButtonShapes: ButtonShapes
        get() {
            return defaultButtonShapesCached
                ?: ButtonShapes(
                        shape = fromToken(ButtonSmallTokens.ContainerShapeRound),
                        pressedShape = fromToken(ButtonSmallTokens.PressedContainerShape),
                    )
                    .also { defaultButtonShapesCached = it }
        }

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in a
     * [Button].
     */
    @Composable fun buttonColors() = MaterialTheme.colorScheme.defaultButtonColors

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in a
     * [Button].
     *
     * @param containerColor the container color of this [Button] when enabled.
     * @param contentColor the content color of this [Button] when enabled.
     * @param disabledContainerColor the container color of this [Button] when not enabled.
     * @param disabledContentColor the content color of this [Button] when not enabled.
     */
    @Composable
    fun buttonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    internal val ColorScheme.defaultButtonColors: ButtonColors
        get() {
            return defaultButtonColorsCached
                ?: ButtonColors(
                        containerColor = fromToken(FilledButtonTokens.ContainerColor),
                        contentColor = fromToken(FilledButtonTokens.LabelTextColor),
                        disabledContainerColor =
                            fromToken(FilledButtonTokens.DisabledContainerColor)
                                .copy(alpha = FilledButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(FilledButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = FilledButtonTokens.DisabledLabelTextOpacity),
                    )
                    .also { defaultButtonColorsCached = it }
        }

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [ElevatedButton].
     */
    @Composable fun elevatedButtonColors() = MaterialTheme.colorScheme.defaultElevatedButtonColors

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [ElevatedButton].
     *
     * @param containerColor the container color of this [ElevatedButton] when enabled
     * @param contentColor the content color of this [ElevatedButton] when enabled
     * @param disabledContainerColor the container color of this [ElevatedButton] when not enabled
     * @param disabledContentColor the content color of this [ElevatedButton] when not enabled
     */
    @Composable
    fun elevatedButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultElevatedButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    internal val ColorScheme.defaultElevatedButtonColors: ButtonColors
        get() {
            return defaultElevatedButtonColorsCached
                ?: ButtonColors(
                        containerColor = fromToken(ElevatedButtonTokens.ContainerColor),
                        contentColor = fromToken(ElevatedButtonTokens.LabelTextColor),
                        disabledContainerColor =
                            fromToken(ElevatedButtonTokens.DisabledContainerColor)
                                .copy(alpha = ElevatedButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(ElevatedButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = ElevatedButtonTokens.DisabledLabelTextOpacity),
                    )
                    .also { defaultElevatedButtonColorsCached = it }
        }

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [FilledTonalButton].
     */
    @Composable
    fun filledTonalButtonColors() = MaterialTheme.colorScheme.defaultFilledTonalButtonColors

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [FilledTonalButton].
     *
     * @param containerColor the container color of this [FilledTonalButton] when enabled
     * @param contentColor the content color of this [FilledTonalButton] when enabled
     * @param disabledContainerColor the container color of this [FilledTonalButton] when not
     *   enabled
     * @param disabledContentColor the content color of this [FilledTonalButton] when not enabled
     */
    @Composable
    fun filledTonalButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    internal val ColorScheme.defaultFilledTonalButtonColors: ButtonColors
        get() {
            return defaultFilledTonalButtonColorsCached
                ?: ButtonColors(
                        containerColor = fromToken(FilledTonalButtonTokens.ContainerColor),
                        contentColor = fromToken(FilledTonalButtonTokens.LabelTextColor),
                        disabledContainerColor =
                            fromToken(FilledTonalButtonTokens.DisabledContainerColor)
                                .copy(alpha = FilledTonalButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(FilledTonalButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = FilledTonalButtonTokens.DisabledLabelTextOpacity),
                    )
                    .also { defaultFilledTonalButtonColorsCached = it }
        }

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [OutlinedButton].
     */
    @Composable fun outlinedButtonColors() = MaterialTheme.colorScheme.defaultOutlinedButtonColors

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [OutlinedButton].
     *
     * @param containerColor the container color of this [OutlinedButton] when enabled
     * @param contentColor the content color of this [OutlinedButton] when enabled
     * @param disabledContainerColor the container color of this [OutlinedButton] when not enabled
     * @param disabledContentColor the content color of this [OutlinedButton] when not enabled
     */
    @Composable
    fun outlinedButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    internal val ColorScheme.defaultOutlinedButtonColors: ButtonColors
        get() {
            return defaultOutlinedButtonColorsCached
                ?: ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(OutlinedButtonTokens.LabelTextColor),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(OutlinedButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = OutlinedButtonTokens.DisabledLabelTextOpacity),
                    )
                    .also { defaultOutlinedButtonColorsCached = it }
        }

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in a
     * [TextButton].
     */
    @Composable fun textButtonColors() = MaterialTheme.colorScheme.defaultTextButtonColors

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in a
     * [TextButton].
     *
     * @param containerColor the container color of this [TextButton] when enabled
     * @param contentColor the content color of this [TextButton] when enabled
     * @param disabledContainerColor the container color of this [TextButton] when not enabled
     * @param disabledContentColor the content color of this [TextButton] when not enabled
     */
    @Composable
    fun textButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultTextButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    internal val ColorScheme.defaultTextButtonColors: ButtonColors
        get() {
            return defaultTextButtonColorsCached
                ?: ButtonColors(
                        containerColor = Color.Transparent,
                        // TODO replace with the token value once it's corrected
                        contentColor = fromToken(ColorSchemeKeyTokens.Primary),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(TextButtonTokens.DisabledLabelColor)
                                .copy(alpha = TextButtonTokens.DisabledLabelOpacity),
                    )
                    .also { defaultTextButtonColorsCached = it }
        }

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [Button].
     *
     * @param defaultElevation the elevation used when the [Button] is enabled, and has no other
     *   [Interaction]s.
     * @param pressedElevation the elevation used when this [Button] is enabled and pressed.
     * @param focusedElevation the elevation used when the [Button] is enabled and focused.
     * @param hoveredElevation the elevation used when the [Button] is enabled and hovered.
     * @param disabledElevation the elevation used when the [Button] is not enabled.
     */
    @Composable
    fun buttonElevation(
        defaultElevation: Dp = FilledButtonTokens.ContainerElevation,
        pressedElevation: Dp = FilledButtonTokens.PressedContainerElevation,
        focusedElevation: Dp = FilledButtonTokens.FocusedContainerElevation,
        hoveredElevation: Dp = FilledButtonTokens.HoveredContainerElevation,
        disabledElevation: Dp = FilledButtonTokens.DisabledContainerElevation,
    ): ButtonElevation =
        ButtonElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            disabledElevation = disabledElevation,
        )

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [ElevatedButton].
     *
     * @param defaultElevation the elevation used when the [ElevatedButton] is enabled, and has no
     *   other [Interaction]s.
     * @param pressedElevation the elevation used when this [ElevatedButton] is enabled and pressed.
     * @param focusedElevation the elevation used when the [ElevatedButton] is enabled and focused.
     * @param hoveredElevation the elevation used when the [ElevatedButton] is enabled and hovered.
     * @param disabledElevation the elevation used when the [ElevatedButton] is not enabled.
     */
    @Composable
    fun elevatedButtonElevation(
        defaultElevation: Dp = ElevatedButtonTokens.ContainerElevation,
        pressedElevation: Dp = ElevatedButtonTokens.PressedContainerElevation,
        focusedElevation: Dp = ElevatedButtonTokens.FocusedContainerElevation,
        hoveredElevation: Dp = ElevatedButtonTokens.HoveredContainerElevation,
        disabledElevation: Dp = ElevatedButtonTokens.DisabledContainerElevation,
    ): ButtonElevation =
        ButtonElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            disabledElevation = disabledElevation,
        )

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [FilledTonalButton].
     *
     * @param defaultElevation the elevation used when the [FilledTonalButton] is enabled, and has
     *   no other [Interaction]s.
     * @param pressedElevation the elevation used when this [FilledTonalButton] is enabled and
     *   pressed.
     * @param focusedElevation the elevation used when the [FilledTonalButton] is enabled and
     *   focused.
     * @param hoveredElevation the elevation used when the [FilledTonalButton] is enabled and
     *   hovered.
     * @param disabledElevation the elevation used when the [FilledTonalButton] is not enabled.
     */
    @Composable
    fun filledTonalButtonElevation(
        defaultElevation: Dp = FilledTonalButtonTokens.ContainerElevation,
        pressedElevation: Dp = FilledTonalButtonTokens.PressedContainerElevation,
        focusedElevation: Dp = FilledTonalButtonTokens.FocusContainerElevation,
        hoveredElevation: Dp = FilledTonalButtonTokens.HoverContainerElevation,
        disabledElevation: Dp = 0.dp,
    ): ButtonElevation =
        ButtonElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            disabledElevation = disabledElevation,
        )

    /** The default [BorderStroke] used by [OutlinedButton]. */
    val outlinedButtonBorder: BorderStroke
        @Composable
        @Deprecated(
            message =
                "Please use the version that takes an `enabled` param to get the " +
                    "`BorderStroke` with the correct opacity",
            replaceWith = ReplaceWith("outlinedButtonBorder(enabled)"),
        )
        get() =
            BorderStroke(
                width = ButtonSmallTokens.OutlinedOutlineWidth,
                color = OutlinedButtonTokens.OutlineColor.value,
            )

    /**
     * The default [BorderStroke] used by [OutlinedButton].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    fun outlinedButtonBorder(enabled: Boolean = true): BorderStroke =
        BorderStroke(
            width = ButtonSmallTokens.OutlinedOutlineWidth,
            color =
                if (enabled) {
                    OutlinedButtonTokens.OutlineColor.value
                } else {
                    OutlinedButtonTokens.OutlineColor.value.copy(
                        alpha = OutlinedButtonTokens.DisabledContainerOpacity
                    )
                },
        )

    /**
     * Recommended [ButtonShapes] for a provided button height.
     *
     * @param buttonHeight The height of the button
     */
    @Composable
    @ExperimentalMaterial3ExpressiveApi
    fun shapesFor(buttonHeight: Dp): ButtonShapes {
        val xSmallHeight = ExtraSmallContainerHeight
        val smallHeight = MinHeight
        val mediumHeight = MediumContainerHeight
        val largeHeight = LargeContainerHeight
        val xLargeHeight = ExtraLargeContainerHeight
        return when {
            buttonHeight <= (xSmallHeight + smallHeight) / 2 ->
                shapes(shape = shape, pressedShape = extraSmallPressedShape)
            buttonHeight <= (smallHeight + mediumHeight) / 2 -> shapes()
            buttonHeight <= (mediumHeight + largeHeight) / 2 ->
                shapes(shape = shape, pressedShape = mediumPressedShape)
            buttonHeight <= (largeHeight + xLargeHeight) / 2 ->
                shapes(shape = shape, pressedShape = largePressedShape)
            else -> shapes(shape = shape, pressedShape = extraLargePressedShape)
        }
    }

    /**
     * Recommended [PaddingValues] for a provided button height.
     *
     * @param buttonHeight The height of the button
     * @param hasStartIcon Whether the button has a leading icon
     * @param hasEndIcon Whether the button has a trailing icon
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun contentPaddingFor(
        buttonHeight: Dp,
        hasStartIcon: Boolean = false,
        hasEndIcon: Boolean = false,
    ): PaddingValues {
        val smallHeight = MinHeight
        val mediumHeight = MediumContainerHeight
        val largeHeight = LargeContainerHeight
        val xLargeHeight = ExtraLargeContainerHeight
        return when {
            buttonHeight < smallHeight -> ExtraSmallContentPadding
            buttonHeight < mediumHeight -> getSmallContentPadding(hasStartIcon, hasEndIcon)
            buttonHeight < largeHeight -> getMediumContentPadding(hasStartIcon, hasEndIcon)
            buttonHeight < xLargeHeight -> getLargeContentPadding(hasStartIcon, hasEndIcon)
            else -> ExtraLargeContentPadding
        }
    }

    /**
     * Recommended [PaddingValues] for a provided button height.
     *
     * @param buttonHeight The height of the button
     */
    @Deprecated(
        message = "Deprecated in favor of function with hasLeadingIcon and hasTrailingIcon params",
        level = DeprecationLevel.HIDDEN,
    )
    @ExperimentalMaterial3ExpressiveApi
    fun contentPaddingFor(buttonHeight: Dp): PaddingValues {
        val smallHeight = MinHeight
        val mediumHeight = MediumContainerHeight
        val largeHeight = LargeContainerHeight
        val xLargeHeight = ExtraLargeContainerHeight
        return when {
            buttonHeight < smallHeight -> ExtraSmallContentPadding
            buttonHeight < mediumHeight -> SmallContentPadding
            buttonHeight < largeHeight -> MediumContentPadding
            buttonHeight < xLargeHeight -> LargeContentPadding
            else -> ExtraLargeContentPadding
        }
    }

    /**
     * Recommended Icon size for a provided button height.
     *
     * @param buttonHeight The height of the button
     */
    @ExperimentalMaterial3ExpressiveApi
    fun iconSizeFor(buttonHeight: Dp): Dp {
        val smallHeight = MinHeight
        val mediumHeight = MediumContainerHeight
        val largeHeight = LargeContainerHeight
        val xLargeHeight = ExtraLargeContainerHeight
        return when {
            buttonHeight < smallHeight -> ExtraSmallIconSize
            buttonHeight < mediumHeight -> SmallIconSize
            buttonHeight < largeHeight -> MediumIconSize
            buttonHeight < xLargeHeight -> LargeIconSize
            else -> ExtraLargeIconSize
        }
    }

    /**
     * Recommended spacing after an [Icon] for a provided button height.
     *
     * @param buttonHeight The height of the button
     */
    @ExperimentalMaterial3ExpressiveApi
    fun iconSpacingFor(buttonHeight: Dp): Dp {
        val smallHeight = MinHeight
        val mediumHeight = MediumContainerHeight
        val largeHeight = LargeContainerHeight
        val xLargeHeight = ExtraLargeContainerHeight
        return when {
            buttonHeight < smallHeight -> ExtraSmallIconSpacing
            buttonHeight < mediumHeight -> IconSpacing
            buttonHeight < largeHeight -> MediumIconSpacing
            buttonHeight < xLargeHeight -> LargeIconSpacing
            else -> ExtraLargeIconSpacing
        }
    }

    /**
     * Recommended [TextStyle] for a [Text] provided a button height.
     *
     * @param buttonHeight The height of the button
     */
    @Composable
    @ExperimentalMaterial3ExpressiveApi
    fun textStyleFor(buttonHeight: Dp): TextStyle {
        val mediumHeight = MediumContainerHeight
        val largeHeight = LargeContainerHeight
        val xLargeHeight = ExtraLargeContainerHeight
        return when {
            buttonHeight < mediumHeight -> MaterialTheme.typography.labelLarge
            buttonHeight < largeHeight ->
                if (shouldUsePrecisionPointerComponentSizing.value) {
                    MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, lineHeight = 22.sp)
                } else {
                    MaterialTheme.typography.titleMedium
                }
            buttonHeight < xLargeHeight ->
                if (shouldUsePrecisionPointerComponentSizing.value) {
                    MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                    )
                } else {
                    MaterialTheme.typography.headlineSmall
                }
            else -> MaterialTheme.typography.headlineLarge
        }
    }

    private val SmallVerticalPadding =
        if (shouldUsePrecisionPointerComponentSizing.value) 8.dp else 10.dp
    private val IconSmallHorizontalPadding =
        if (shouldUsePrecisionPointerComponentSizing.value) 12.dp else SmallStartPadding
    private val MediumLeadingPadding = ButtonMediumTokens.LeadingSpace
    private val MediumTrailingPadding = ButtonMediumTokens.TrailingSpace
    private val MediumVerticalPadding =
        if (shouldUsePrecisionPointerComponentSizing.value) 12.dp else 16.dp
    private val IconMediumLeadingPadding =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            20.dp
        } else {
            ButtonMediumTokens.LeadingSpace
        }
    private val IconMediumTrailingPadding =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            20.dp
        } else {
            ButtonMediumTokens.TrailingSpace
        }
    private val LargeVerticalPadding =
        if (shouldUsePrecisionPointerComponentSizing.value) 14.dp else 32.dp
    private val LargeLeadingPadding =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            32.dp
        } else {
            ButtonLargeTokens.LeadingSpace
        }
    private val LargeTrailingPadding =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            32.dp
        } else {
            ButtonLargeTokens.TrailingSpace
        }
    private val IconLargeLeadingPadding =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            28.dp
        } else {
            ButtonLargeTokens.LeadingSpace
        }
    private val IconLargeTrailingPadding =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            28.dp
        } else {
            ButtonLargeTokens.TrailingSpace
        }
}

/**
 * Represents the elevation for a button in different states.
 * - See [ButtonDefaults.buttonElevation] for the default elevation used in a [Button].
 * - See [ButtonDefaults.elevatedButtonElevation] for the default elevation used in a
 *   [ElevatedButton].
 */
@Stable
class ButtonElevation
internal constructor(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val disabledElevation: Dp,
) {
    /**
     * Represents the shadow elevation used in a button, depending on its [enabled] state and
     * [interactionSource].
     *
     * Shadow elevation is used to apply a shadow around the button to give it higher emphasis.
     *
     * @param enabled whether the button is enabled
     * @param interactionSource the [InteractionSource] for this button
     */
    @Composable
    internal fun shadowElevation(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> {
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    private fun animateElevation(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }
                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }
                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }
                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target =
            if (!enabled) {
                disabledElevation
            } else {
                when (interaction) {
                    is PressInteraction.Press -> pressedElevation
                    is HoverInteraction.Enter -> hoveredElevation
                    is FocusInteraction.Focus -> focusedElevation
                    else -> defaultElevation
                }
            }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        LaunchedEffect(target) {
            if (animatable.targetValue != target) {
                if (!enabled) {
                    // No transition when moving to a disabled state
                    animatable.snapTo(target)
                } else {
                    val lastInteraction =
                        when (animatable.targetValue) {
                            pressedElevation -> PressInteraction.Press(Offset.Zero)
                            hoveredElevation -> HoverInteraction.Enter()
                            focusedElevation -> FocusInteraction.Focus()
                            else -> null
                        }
                    animatable.animateElevation(
                        from = lastInteraction,
                        to = interaction,
                        target = target,
                    )
                }
            }
        }

        return animatable.asState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ButtonElevation) return false

        if (defaultElevation != other.defaultElevation) return false
        if (pressedElevation != other.pressedElevation) return false
        if (focusedElevation != other.focusedElevation) return false
        if (hoveredElevation != other.hoveredElevation) return false
        if (disabledElevation != other.disabledElevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultElevation.hashCode()
        result = 31 * result + pressedElevation.hashCode()
        result = 31 * result + focusedElevation.hashCode()
        result = 31 * result + hoveredElevation.hashCode()
        result = 31 * result + disabledElevation.hashCode()
        return result
    }
}

/**
 * Represents the container and content colors used in a button in different states.
 *
 * @param containerColor the container color of this [Button] when enabled.
 * @param contentColor the content color of this [Button] when enabled.
 * @param disabledContainerColor the container color of this [Button] when not enabled.
 * @param disabledContentColor the content color of this [Button] when not enabled.
 *     @constructor create an instance with arbitrary colors.
 * - See [ButtonDefaults.buttonColors] for the default colors used in a [Button].
 * - See [ButtonDefaults.elevatedButtonColors] for the default colors used in a [ElevatedButton].
 * - See [ButtonDefaults.textButtonColors] for the default colors used in a [TextButton].
 */
@Immutable
class ButtonColors
constructor(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
) {
    /**
     * Returns a copy of this ButtonColors, optionally overriding some of the values. This uses the
     * Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledContentColor: Color = this.disabledContentColor,
    ) =
        ButtonColors(
            containerColor.takeOrElse { this.containerColor },
            contentColor.takeOrElse { this.contentColor },
            disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledContentColor.takeOrElse { this.disabledContentColor },
        )

    /**
     * Represents the container color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Stable
    internal fun containerColor(enabled: Boolean): Color =
        if (enabled) containerColor else disabledContainerColor

    /**
     * Represents the content color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Stable
    internal fun contentColor(enabled: Boolean): Color =
        if (enabled) contentColor else disabledContentColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ButtonColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }
}

/**
 * The shapes that will be used in buttons. Button will morph between these shapes depending on the
 * interaction of the button, assuming all of the shapes are [CornerBasedShape]s.
 *
 * @property shape is the active shape.
 * @property pressedShape is the pressed shape.
 */
@ExperimentalMaterial3ExpressiveApi
@Immutable
class ButtonShapes(val shape: Shape, val pressedShape: Shape) {
    /** Returns a copy of this ButtonShapes, optionally overriding some of the values. */
    fun copy(shape: Shape? = this.shape, pressedShape: Shape? = this.pressedShape) =
        ButtonShapes(
            shape = shape.takeOrElse { this.shape },
            pressedShape = pressedShape.takeOrElse { this.pressedShape },
        )

    internal fun Shape?.takeOrElse(block: () -> Shape): Shape = this ?: block()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ButtonShapes) return false

        if (shape != other.shape) return false
        if (pressedShape != other.pressedShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + pressedShape.hashCode()

        return result
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val ButtonShapes.hasRoundedCornerShapes: Boolean
    get() = shape is RoundedCornerShape && pressedShape is RoundedCornerShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val ButtonShapes.hasCornerBasedShapes: Boolean
    get() = shape is CornerBasedShape && pressedShape is CornerBasedShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun shapeByInteraction(
    shapes: ButtonShapes,
    pressed: Boolean,
    animationSpec: FiniteAnimationSpec<Float>,
): Shape {
    val shape =
        if (pressed) {
            shapes.pressedShape
        } else {
            shapes.shape
        }
    if (shapes.hasRoundedCornerShapes)
        return key(shapes) { rememberAnimatedShape(shape as RoundedCornerShape, animationSpec) }
    else if (shapes.hasCornerBasedShapes)
        return key(shapes) { rememberAnimatedShape(shape as CornerBasedShape, animationSpec) }

    return shape
}
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Card.kt
```kotlin
/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.internal.animateElevation
import androidx.compose.material3.tokens.ElevatedCardTokens
import androidx.compose.material3.tokens.FilledCardTokens
import androidx.compose.material3.tokens.OutlinedCardTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp

/**
 * [Material Design filled card](https://m3.material.io/components/cards/overview)
 *
 * Cards contain contain content and actions that relate information about a subject. Filled cards
 * provide subtle separation from the background. This has less emphasis than elevated or outlined
 * cards.
 *
 * This Card does not handle input events - see the other Card overloads if you want a clickable or
 * selectable Card.
 *
 * ![Filled card
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-card.png)
 *
 * Card sample:
 *
 * @sample androidx.compose.material3.samples.CardSample
 * @param modifier the [Modifier] to be applied to this card
 * @param shape defines the shape of this card's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [CardColors] that will be used to resolve the colors used for this card in
 *   different states. See [CardDefaults.cardColors].
 * @param elevation [CardElevation] used to resolve the elevation for this card in different states.
 *   This controls the size of the shadow below the card. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   also: [Surface].
 * @param border the border to draw around the container of this card
 * @param content The content displayed on the card
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.containerColor(enabled = true),
        contentColor = colors.contentColor(enabled = true),
        shadowElevation = elevation.shadowElevation(enabled = true, interactionSource = null).value,
        border = border,
    ) {
        Column(content = content)
    }
}

/**
 * [Material Design filled card](https://m3.material.io/components/cards/overview)
 *
 * Cards contain contain content and actions that relate information about a subject. Filled cards
 * provide subtle separation from the background. This has less emphasis than elevated or outlined
 * cards.
 *
 * This Card handles click events, calling its [onClick] lambda.
 *
 * ![Filled card
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-card.png)
 *
 * Clickable card sample:
 *
 * @sample androidx.compose.material3.samples.ClickableCardSample
 * @param onClick called when this card is clicked
 * @param modifier the [Modifier] to be applied to this card
 * @param enabled controls the enabled state of this card. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this card's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [CardColors] that will be used to resolve the color(s) used for this card in
 *   different states. See [CardDefaults.cardColors].
 * @param elevation [CardElevation] used to resolve the elevation for this card in different states.
 *   This controls the size of the shadow below the card. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   also: [Surface].
 * @param border the border to draw around the container of this card
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content The content displayed on the card
 */
@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = colors.containerColor(enabled),
        contentColor = colors.contentColor(enabled),
        shadowElevation = elevation.shadowElevation(enabled, interactionSource).value,
        border = border,
        interactionSource = interactionSource,
    ) {
        Column(content = content)
    }
}

/**
 * [Material Design elevated card](https://m3.material.io/components/cards/overview)
 *
 * Elevated cards contain content and actions that relate information about a subject. They have a
 * drop shadow, providing more separation from the background than filled cards, but less than
 * outlined cards.
 *
 * This ElevatedCard does not handle input events - see the other ElevatedCard overloads if you want
 * a clickable or selectable ElevatedCard.
 *
 * ![Elevated card
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-card.png)
 *
 * Elevated card sample:
 *
 * @sample androidx.compose.material3.samples.ElevatedCardSample
 * @param modifier the [Modifier] to be applied to this card
 * @param shape defines the shape of this card's container and shadow (when using [elevation])
 * @param colors [CardColors] that will be used to resolve the color(s) used for this card in
 *   different states. See [CardDefaults.elevatedCardElevation].
 * @param elevation [CardElevation] used to resolve the elevation for this card in different states.
 *   This controls the size of the shadow below the card. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   also: [Surface].
 * @param content The content displayed on the card
 */
@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    content: @Composable ColumnScope.() -> Unit,
) =
    Card(
        modifier = modifier,
        shape = shape,
        border = null,
        elevation = elevation,
        colors = colors,
        content = content,
    )

/**
 * [Material Design elevated card](https://m3.material.io/components/cards/overview)
 *
 * Elevated cards contain content and actions that relate information about a subject. They have a
 * drop shadow, providing more separation from the background than filled cards, but less than
 * outlined cards.
 *
 * This ElevatedCard handles click events, calling its [onClick] lambda.
 *
 * ![Elevated card
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-card.png)
 *
 * Clickable elevated card sample:
 *
 * @sample androidx.compose.material3.samples.ClickableElevatedCardSample
 * @param onClick called when this card is clicked
 * @param modifier the [Modifier] to be applied to this card
 * @param enabled controls the enabled state of this card. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this card's container and shadow (when using [elevation])
 * @param colors [CardColors] that will be used to resolve the color(s) used for this card in
 *   different states. See [CardDefaults.elevatedCardElevation].
 * @param elevation [CardElevation] used to resolve the elevation for this card in different states.
 *   This controls the size of the shadow below the card. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content The content displayed on the card
 */
@Composable
fun ElevatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) =
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = null,
        interactionSource = interactionSource,
        content = content,
    )

/**
 * [Material Design outlined card](https://m3.material.io/components/cards/overview)
 *
 * Outlined cards contain content and actions that relate information about a subject. They have a
 * visual boundary around the container. This can provide greater emphasis than the other types.
 *
 * This OutlinedCard does not handle input events - see the other OutlinedCard overloads if you want
 * a clickable or selectable OutlinedCard.
 *
 * ![Outlined card
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-card.png)
 *
 * Outlined card sample:
 *
 * @sample androidx.compose.material3.samples.OutlinedCardSample
 * @param modifier the [Modifier] to be applied to this card
 * @param shape defines the shape of this card's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [CardColors] that will be used to resolve the color(s) used for this card in
 *   different states. See [CardDefaults.outlinedCardColors].
 * @param elevation [CardElevation] used to resolve the elevation for this card in different states.
 *   This controls the size of the shadow below the card. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   also: [Surface].
 * @param border the border to draw around the container of this card
 * @param content The content displayed on the card
 */
@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.outlinedShape,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = CardDefaults.outlinedCardBorder(),
    content: @Composable ColumnScope.() -> Unit,
) =
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content,
    )

/**
 * [Material Design outlined card](https://m3.material.io/components/cards/overview)
 *
 * Outlined cards contain content and actions that relate information about a subject. They have a
 * visual boundary around the container. This can provide greater emphasis than the other types.
 *
 * This OutlinedCard handles click events, calling its [onClick] lambda.
 *
 * ![Outlined card
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-card.png)
 *
 * Clickable outlined card sample:
 *
 * @sample androidx.compose.material3.samples.ClickableOutlinedCardSample
 * @param onClick called when this card is clicked
 * @param modifier the [Modifier] to be applied to this card
 * @param enabled controls the enabled state of this card. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this card's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [CardColors] that will be used to resolve the color(s) used for this card in
 *   different states. See [CardDefaults.outlinedCardColors].
 * @param elevation [CardElevation] used to resolve the elevation for this card in different states.
 *   This controls the size of the shadow below the card. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   also: [Surface].
 * @param border the border to draw around the container of this card
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content The content displayed on the card
 */
@Composable
fun OutlinedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.outlinedShape,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = CardDefaults.outlinedCardBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) =
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource,
        content = content,
    )

/** Contains the default values used by all card types. */
object CardDefaults {
    // shape Defaults
    /** Default shape for a card. */
    val shape: Shape
        @Composable get() = FilledCardTokens.ContainerShape.value

    /** Default shape for an elevated card. */
    val elevatedShape: Shape
        @Composable get() = ElevatedCardTokens.ContainerShape.value

    /** Default shape for an outlined card. */
    val outlinedShape: Shape
        @Composable get() = OutlinedCardTokens.ContainerShape.value

    /**
     * Creates a [CardElevation] that will animate between the provided values according to the
     * Material specification for a [Card].
     *
     * @param defaultElevation the elevation used when the [Card] is has no other [Interaction]s.
     * @param pressedElevation the elevation used when the [Card] is pressed.
     * @param focusedElevation the elevation used when the [Card] is focused.
     * @param hoveredElevation the elevation used when the [Card] is hovered.
     * @param draggedElevation the elevation used when the [Card] is dragged.
     * @param disabledElevation the elevation used when the [Card] is disabled.
     */
    @Composable
    fun cardElevation(
        defaultElevation: Dp = FilledCardTokens.ContainerElevation,
        pressedElevation: Dp = FilledCardTokens.PressedContainerElevation,
        focusedElevation: Dp = FilledCardTokens.FocusContainerElevation,
        hoveredElevation: Dp = FilledCardTokens.HoverContainerElevation,
        draggedElevation: Dp = FilledCardTokens.DraggedContainerElevation,
        disabledElevation: Dp = FilledCardTokens.DisabledContainerElevation,
    ): CardElevation =
        CardElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation,
        )

    /**
     * Creates a [CardElevation] that will animate between the provided values according to the
     * Material specification for an [ElevatedCard].
     *
     * @param defaultElevation the elevation used when the [ElevatedCard] is has no other
     *   [Interaction]s.
     * @param pressedElevation the elevation used when the [ElevatedCard] is pressed.
     * @param focusedElevation the elevation used when the [ElevatedCard] is focused.
     * @param hoveredElevation the elevation used when the [ElevatedCard] is hovered.
     * @param draggedElevation the elevation used when the [ElevatedCard] is dragged.
     * @param disabledElevation the elevation used when the [Card] is disabled.
     */
    @Composable
    fun elevatedCardElevation(
        defaultElevation: Dp = ElevatedCardTokens.ContainerElevation,
        pressedElevation: Dp = ElevatedCardTokens.PressedContainerElevation,
        focusedElevation: Dp = ElevatedCardTokens.FocusContainerElevation,
        hoveredElevation: Dp = ElevatedCardTokens.HoverContainerElevation,
        draggedElevation: Dp = ElevatedCardTokens.DraggedContainerElevation,
        disabledElevation: Dp = ElevatedCardTokens.DisabledContainerElevation,
    ): CardElevation =
        CardElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation,
        )

    /**
     * Creates a [CardElevation] that will animate between the provided values according to the
     * Material specification for an [OutlinedCard].
     *
     * @param defaultElevation the elevation used when the [OutlinedCard] is has no other
     *   [Interaction]s.
     * @param pressedElevation the elevation used when the [OutlinedCard] is pressed.
     * @param focusedElevation the elevation used when the [OutlinedCard] is focused.
     * @param hoveredElevation the elevation used when the [OutlinedCard] is hovered.
     * @param draggedElevation the elevation used when the [OutlinedCard] is dragged.
     */
    @Composable
    fun outlinedCardElevation(
        defaultElevation: Dp = OutlinedCardTokens.ContainerElevation,
        pressedElevation: Dp = defaultElevation,
        focusedElevation: Dp = defaultElevation,
        hoveredElevation: Dp = defaultElevation,
        draggedElevation: Dp = OutlinedCardTokens.DraggedContainerElevation,
        disabledElevation: Dp = OutlinedCardTokens.DisabledContainerElevation,
    ): CardElevation =
        CardElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation,
        )

    /**
     * Creates a [CardColors] that represents the default container and content colors used in a
     * [Card].
     */
    @Composable fun cardColors() = MaterialTheme.colorScheme.defaultCardColors

    /**
     * Creates a [CardColors] that represents the default container and content colors used in a
     * [Card].
     *
     * @param containerColor the container color of this [Card] when enabled.
     * @param contentColor the content color of this [Card] when enabled.
     * @param disabledContainerColor the container color of this [Card] when not enabled.
     * @param disabledContentColor the content color of this [Card] when not enabled.
     */
    @Composable
    fun cardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = contentColor.copy(DisabledAlpha),
    ): CardColors =
        MaterialTheme.colorScheme.defaultCardColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    internal val ColorScheme.defaultCardColors: CardColors
        get() {
            return defaultCardColorsCached
                ?: CardColors(
                        containerColor = fromToken(FilledCardTokens.ContainerColor),
                        contentColor = contentColorFor(fromToken(FilledCardTokens.ContainerColor)),
                        disabledContainerColor =
                            fromToken(FilledCardTokens.DisabledContainerColor)
                                .copy(alpha = FilledCardTokens.DisabledContainerOpacity)
                                .compositeOver(fromToken(FilledCardTokens.ContainerColor)),
                        disabledContentColor =
                            contentColorFor(fromToken(FilledCardTokens.ContainerColor))
                                .copy(DisabledAlpha),
                    )
                    .also { defaultCardColorsCached = it }
        }

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [ElevatedCard].
     */
    @Composable fun elevatedCardColors() = MaterialTheme.colorScheme.defaultElevatedCardColors

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [ElevatedCard].
     *
     * @param containerColor the container color of this [ElevatedCard] when enabled.
     * @param contentColor the content color of this [ElevatedCard] when enabled.
     * @param disabledContainerColor the container color of this [ElevatedCard] when not enabled.
     * @param disabledContentColor the content color of this [ElevatedCard] when not enabled.
     */
    @Composable
    fun elevatedCardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = contentColor.copy(DisabledAlpha),
    ): CardColors =
        MaterialTheme.colorScheme.defaultElevatedCardColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    internal val ColorScheme.defaultElevatedCardColors: CardColors
        get() {
            return defaultElevatedCardColorsCached
                ?: CardColors(
                        containerColor = fromToken(ElevatedCardTokens.ContainerColor),
                        contentColor =
                            contentColorFor(fromToken(ElevatedCardTokens.ContainerColor)),
                        disabledContainerColor =
                            fromToken(ElevatedCardTokens.DisabledContainerColor)
                                .copy(alpha = ElevatedCardTokens.DisabledContainerOpacity)
                                .compositeOver(
                                    fromToken(ElevatedCardTokens.DisabledContainerColor)
                                ),
                        disabledContentColor =
                            contentColorFor(fromToken(ElevatedCardTokens.ContainerColor))
                                .copy(DisabledAlpha),
                    )
                    .also { defaultElevatedCardColorsCached = it }
        }

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [OutlinedCard].
     */
    @Composable fun outlinedCardColors() = MaterialTheme.colorScheme.defaultOutlinedCardColors

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [OutlinedCard].
     *
     * @param containerColor the container color of this [OutlinedCard] when enabled.
     * @param contentColor the content color of this [OutlinedCard] when enabled.
     * @param disabledContainerColor the container color of this [OutlinedCard] when not enabled.
     * @param disabledContentColor the content color of this [OutlinedCard] when not enabled.
     */
    @Composable
    fun outlinedCardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = contentColorFor(containerColor).copy(DisabledAlpha),
    ): CardColors =
        MaterialTheme.colorScheme.defaultOutlinedCardColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    internal val ColorScheme.defaultOutlinedCardColors: CardColors
        get() {
            return defaultOutlinedCardColorsCached
                ?: CardColors(
                        containerColor = fromToken(OutlinedCardTokens.ContainerColor),
                        contentColor =
                            contentColorFor(fromToken(OutlinedCardTokens.ContainerColor)),
                        disabledContainerColor = fromToken(OutlinedCardTokens.ContainerColor),
                        disabledContentColor =
                            contentColorFor(fromToken(OutlinedCardTokens.ContainerColor))
                                .copy(DisabledAlpha),
                    )
                    .also { defaultOutlinedCardColorsCached = it }
        }

    /**
     * Creates a [BorderStroke] that represents the default border used in [OutlinedCard].
     *
     * @param enabled whether the card is enabled
     */
    @Composable
    fun outlinedCardBorder(enabled: Boolean = true): BorderStroke {
        val color =
            if (enabled) {
                OutlinedCardTokens.OutlineColor.value
            } else {
                OutlinedCardTokens.DisabledOutlineColor.value
                    .copy(alpha = OutlinedCardTokens.DisabledOutlineOpacity)
                    .compositeOver(ElevatedCardTokens.ContainerColor.value)
            }
        return remember(color) { BorderStroke(OutlinedCardTokens.OutlineWidth, color) }
    }
}

/**
 * Represents the elevation for a card in different states.
 * - See [CardDefaults.cardElevation] for the default elevation used in a [Card].
 * - See [CardDefaults.elevatedCardElevation] for the default elevation used in an [ElevatedCard].
 * - See [CardDefaults.outlinedCardElevation] for the default elevation used in an [OutlinedCard].
 */
@Immutable
class CardElevation
internal constructor(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val draggedElevation: Dp,
    private val disabledElevation: Dp,
) {
    /**
     * Represents the shadow elevation used in a card, depending on its [enabled] state and
     * [interactionSource].
     *
     * Shadow elevation is used to apply a shadow around the card to give it higher emphasis.
     *
     * @param enabled whether the card is enabled
     * @param interactionSource the [InteractionSource] for this card
     */
    @Composable
    internal fun shadowElevation(
        enabled: Boolean,
        interactionSource: InteractionSource?,
    ): State<Dp> {
        if (interactionSource == null) {
            return remember { mutableStateOf(defaultElevation) }
        }
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    private fun animateElevation(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }
                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }
                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }
                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                    is DragInteraction.Start -> {
                        interactions.add(interaction)
                    }
                    is DragInteraction.Stop -> {
                        interactions.remove(interaction.start)
                    }
                    is DragInteraction.Cancel -> {
                        interactions.remove(interaction.start)
                    }
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target =
            if (!enabled) {
                disabledElevation
            } else {
                when (interaction) {
                    is PressInteraction.Press -> pressedElevation
                    is HoverInteraction.Enter -> hoveredElevation
                    is FocusInteraction.Focus -> focusedElevation
                    is DragInteraction.Start -> draggedElevation
                    else -> defaultElevation
                }
            }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        LaunchedEffect(target) {
            if (animatable.targetValue != target) {
                if (!enabled) {
                    // No transition when moving to a disabled state.
                    animatable.snapTo(target)
                } else {
                    val lastInteraction =
                        when (animatable.targetValue) {
                            pressedElevation -> PressInteraction.Press(Offset.Zero)
                            hoveredElevation -> HoverInteraction.Enter()
                            focusedElevation -> FocusInteraction.Focus()
                            draggedElevation -> DragInteraction.Start()
                            else -> null
                        }
                    animatable.animateElevation(
                        from = lastInteraction,
                        to = interaction,
                        target = target,
                    )
                }
            }
        }

        return animatable.asState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CardElevation) return false

        if (defaultElevation != other.defaultElevation) return false
        if (pressedElevation != other.pressedElevation) return false
        if (focusedElevation != other.focusedElevation) return false
        if (hoveredElevation != other.hoveredElevation) return false
        if (disabledElevation != other.disabledElevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultElevation.hashCode()
        result = 31 * result + pressedElevation.hashCode()
        result = 31 * result + focusedElevation.hashCode()
        result = 31 * result + hoveredElevation.hashCode()
        result = 31 * result + disabledElevation.hashCode()
        return result
    }
}

/**
 * Represents the container and content colors used in a card in different states.
 *
 * @param containerColor the container color of this [Card] when enabled.
 * @param contentColor the content color of this [Card] when enabled.
 * @param disabledContainerColor the container color of this [Card] when not enabled.
 * @param disabledContentColor the content color of this [Card] when not enabled.
 * @constructor create an instance with arbitrary colors.
 * - See [CardDefaults.cardColors] for the default colors used in a [Card].
 * - See [CardDefaults.elevatedCardColors] for the default colors used in a [ElevatedCard].
 * - See [CardDefaults.outlinedCardColors] for the default colors used in a [OutlinedCard].
 */
@Immutable
class CardColors
constructor(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
) {
    /**
     * Returns a copy of this CardColors, optionally overriding some of the values. This uses the
     * Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledContentColor: Color = this.disabledContentColor,
    ) =
        CardColors(
            containerColor.takeOrElse { this.containerColor },
            contentColor.takeOrElse { this.contentColor },
            disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledContentColor.takeOrElse { this.disabledContentColor },
        )

    /**
     * Represents the container color for this card, depending on [enabled].
     *
     * @param enabled whether the card is enabled
     */
    @Stable
    internal fun containerColor(enabled: Boolean): Color =
        if (enabled) containerColor else disabledContainerColor

    /**
     * Represents the content color for this card, depending on [enabled].
     *
     * @param enabled whether the card is enabled
     */
    @Stable
    internal fun contentColor(enabled: Boolean) =
        if (enabled) contentColor else disabledContentColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CardColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }
}
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Scaffold.kt
```kotlin
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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.material3.internal.MutableWindowInsets
import androidx.compose.material3.internal.systemBarsForVisualComponents
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

/**
 * [Material Design layout](https://m3.material.io/foundations/layout/understanding-layout/)
 *
 * Scaffold implements the basic Material Design visual layout structure.
 *
 * This component provides API to put together several Material components to construct your screen,
 * by ensuring proper layout strategy for them and collecting necessary data so these components
 * will work together correctly.
 *
 * Simple example of a Scaffold with [TopAppBar] and [FloatingActionButton]:
 *
 * @sample androidx.compose.material3.samples.SimpleScaffoldWithTopBar
 *
 * To show a [Snackbar], use [SnackbarHostState.showSnackbar].
 *
 * @sample androidx.compose.material3.samples.ScaffoldWithSimpleSnackbar
 * @param modifier the [Modifier] to be applied to this scaffold
 * @param topBar top app bar of the screen, typically a [TopAppBar]
 * @param bottomBar bottom bar of the screen, typically a [NavigationBar]
 * @param snackbarHost component to host [Snackbar]s that are pushed to be shown via
 *   [SnackbarHostState.showSnackbar], typically a [SnackbarHost]
 * @param floatingActionButton Main action button of the screen, typically a [FloatingActionButton]
 * @param floatingActionButtonPosition position of the FAB on the screen. See [FabPosition].
 * @param containerColor the color used for the background of this scaffold. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this scaffold. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param contentWindowInsets window insets to be passed to [content] slot via [PaddingValues]
 *   params. Scaffold will take the insets into account from the top/bottom only if the [topBar]/
 *   [bottomBar] are not present, as the scaffold expect [topBar]/[bottomBar] to handle insets
 *   instead. Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a
 *   parent layout will be excluded from [contentWindowInsets].
 * @param content content of the screen. The lambda receives a [PaddingValues] that should be
 *   applied to the content root via [Modifier.padding] and [Modifier.consumeWindowInsets] to
 *   properly offset top and bottom bars. If using [Modifier.verticalScroll], apply this modifier to
 *   the child of the scroll, and not on the scroll itself.
 */
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    val safeInsets = remember(contentWindowInsets) { MutableWindowInsets(contentWindowInsets) }
    Surface(
        modifier =
            modifier.onConsumedWindowInsetsChanged { consumedWindowInsets ->
                // Exclude currently consumed window insets from user provided contentWindowInsets
                safeInsets.insets = contentWindowInsets.exclude(consumedWindowInsets)
            },
        color = containerColor,
        contentColor = contentColor,
    ) {
        ScaffoldLayout(
            fabPosition = floatingActionButtonPosition,
            topBar = topBar,
            bottomBar = bottomBar,
            content = content,
            snackbar = snackbarHost,
            contentWindowInsets = safeInsets,
            fab = floatingActionButton,
        )
    }
}

/**
 * Layout for a [Scaffold]'s content.
 *
 * @param fabPosition [FabPosition] for the FAB (if present)
 * @param topBar the content to place at the top of the [Scaffold], typically a [SmallTopAppBar]
 * @param content the main 'body' of the [Scaffold]
 * @param snackbar the [Snackbar] displayed on top of the [content]
 * @param fab the [FloatingActionButton] displayed on top of the [content], below the [snackbar] and
 *   above the [bottomBar]
 * @param bottomBar the content to place at the bottom of the [Scaffold], on top of the [content],
 *   typically a [NavigationBar].
 */
@Composable
private fun ScaffoldLayout(
    fabPosition: FabPosition,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
    snackbar: @Composable () -> Unit,
    fab: @Composable () -> Unit,
    contentWindowInsets: WindowInsets,
    bottomBar: @Composable () -> Unit,
) {
    // Create the backing value for the content padding
    // These values will be updated during measurement, but before subcomposing the body content
    // Remembering and updating a single PaddingValues avoids needing to recompose when the values
    // change
    val contentPadding = remember {
        object : PaddingValues {
            var paddingHolder by mutableStateOf(PaddingValues(0.dp))

            override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
                paddingHolder.calculateLeftPadding(layoutDirection)

            override fun calculateTopPadding(): Dp = paddingHolder.calculateTopPadding()

            override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
                paddingHolder.calculateRightPadding(layoutDirection)

            override fun calculateBottomPadding(): Dp = paddingHolder.calculateBottomPadding()
        }
    }

    val topBarContent: @Composable () -> Unit = remember(topBar) { { Box { topBar() } } }
    val snackbarContent: @Composable () -> Unit = remember(snackbar) { { Box { snackbar() } } }
    val fabContent: @Composable () -> Unit = remember(fab) { { Box { fab() } } }
    val bodyContent: @Composable () -> Unit =
        remember(content, contentPadding) { { Box { content(contentPadding) } } }
    val bottomBarContent: @Composable () -> Unit = remember(bottomBar) { { Box { bottomBar() } } }
    SubcomposeLayout { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // respect only bottom and horizontal for snackbar and fab
        val leftInset = contentWindowInsets.getLeft(this@SubcomposeLayout, layoutDirection)
        val rightInset = contentWindowInsets.getRight(this@SubcomposeLayout, layoutDirection)
        val bottomInset = contentWindowInsets.getBottom(this@SubcomposeLayout)

        val topBarPlaceable =
            subcompose(ScaffoldLayoutContent.TopBar, topBarContent)
                .first()
                .measure(looseConstraints)

        val snackbarPlaceable =
            subcompose(ScaffoldLayoutContent.Snackbar, snackbarContent)
                .first()
                .measure(looseConstraints.offset(-leftInset - rightInset, -bottomInset))

        val fabPlaceable =
            subcompose(ScaffoldLayoutContent.Fab, fabContent)
                .first()
                .measure(looseConstraints.offset(-leftInset - rightInset, -bottomInset))

        val isFabEmpty = fabPlaceable.width == 0 && fabPlaceable.height == 0
        val fabPlacement =
            if (!isFabEmpty) {
                val fabWidth = fabPlaceable.width
                val fabHeight = fabPlaceable.height
                // FAB distance from the left of the layout, taking into account LTR / RTL
                val fabLeftOffset =
                    when (fabPosition) {
                        FabPosition.Start -> {
                            if (layoutDirection == LayoutDirection.Ltr) {
                                FabSpacing.roundToPx() + leftInset
                            } else {
                                layoutWidth - FabSpacing.roundToPx() - fabWidth - rightInset
                            }
                        }
                        FabPosition.End,
                        FabPosition.EndOverlay -> {
                            if (layoutDirection == LayoutDirection.Ltr) {
                                layoutWidth - FabSpacing.roundToPx() - fabWidth - rightInset
                            } else {
                                FabSpacing.roundToPx() + leftInset
                            }
                        }
                        else -> (layoutWidth - fabWidth + leftInset - rightInset) / 2
                    }

                FabPlacement(left = fabLeftOffset, width = fabWidth, height = fabHeight)
            } else {
                null
            }

        val bottomBarPlaceable =
            subcompose(ScaffoldLayoutContent.BottomBar, bottomBarContent)
                .first()
                .measure(looseConstraints)

        val isBottomBarEmpty = bottomBarPlaceable.width == 0 && bottomBarPlaceable.height == 0

        val fabOffsetFromBottom =
            fabPlacement?.let {
                if (isBottomBarEmpty || fabPosition == FabPosition.EndOverlay) {
                    it.height +
                        FabSpacing.roundToPx() +
                        contentWindowInsets.getBottom(this@SubcomposeLayout)
                } else {
                    // Total height is the bottom bar height + the FAB height + the padding
                    // between the FAB and bottom bar
                    bottomBarPlaceable.height + it.height + FabSpacing.roundToPx()
                }
            }

        val snackbarHeight = snackbarPlaceable.height
        val snackbarOffsetFromBottom =
            if (snackbarHeight != 0) {
                snackbarHeight +
                    (fabOffsetFromBottom
                        ?: bottomBarPlaceable.height.takeIf { !isBottomBarEmpty }
                        ?: contentWindowInsets.getBottom(this@SubcomposeLayout))
            } else {
                0
            }

        // Update the backing state for the content padding before subcomposing the body
        val insets = contentWindowInsets.asPaddingValues(this)
        contentPadding.paddingHolder =
            PaddingValues(
                top =
                    if (topBarPlaceable.width == 0 && topBarPlaceable.height == 0) {
                        insets.calculateTopPadding()
                    } else {
                        topBarPlaceable.height.toDp()
                    },
                bottom =
                    if (isBottomBarEmpty) {
                        insets.calculateBottomPadding()
                    } else {
                        bottomBarPlaceable.height.toDp()
                    },
                start = insets.calculateStartPadding(layoutDirection),
                end = insets.calculateEndPadding(layoutDirection),
            )

        val bodyContentPlaceable =
            subcompose(ScaffoldLayoutContent.MainContent, bodyContent)
                .first()
                .measure(looseConstraints)

        layout(layoutWidth, layoutHeight) {
            // Placing to control drawing order to match default elevation of each placeable
            bodyContentPlaceable.place(0, 0)
            topBarPlaceable.place(0, 0)
            snackbarPlaceable.place(
                (layoutWidth - snackbarPlaceable.width +
                    contentWindowInsets.getLeft(this@SubcomposeLayout, layoutDirection) -
                    contentWindowInsets.getRight(this@SubcomposeLayout, layoutDirection)) / 2,
                layoutHeight - snackbarOffsetFromBottom,
            )
            // The bottom bar is always at the bottom of the layout
            bottomBarPlaceable.place(0, layoutHeight - (bottomBarPlaceable.height))
            // Explicitly not using placeRelative here as `leftOffset` already accounts for RTL
            fabPlacement?.let { placement ->
                fabPlaceable.place(placement.left, layoutHeight - fabOffsetFromBottom!!)
            }
        }
    }
}

/** Object containing various default values for [Scaffold] component. */
object ScaffoldDefaults {
    /** Default insets to be used and consumed by the scaffold content slot */
    val contentWindowInsets: WindowInsets
        @Composable get() = WindowInsets.systemBarsForVisualComponents
}

/** The possible positions for a [FloatingActionButton] attached to a [Scaffold]. */
@kotlin.jvm.JvmInline
value class FabPosition internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * Position FAB at the bottom of the screen at the start, above the [NavigationBar] (if it
         * exists)
         */
        val Start = FabPosition(0)

        /**
         * Position FAB at the bottom of the screen in the center, above the [NavigationBar] (if it
         * exists)
         */
        val Center = FabPosition(1)

        /**
         * Position FAB at the bottom of the screen at the end, above the [NavigationBar] (if it
         * exists)
         */
        val End = FabPosition(2)

        /**
         * Position FAB at the bottom of the screen at the end, overlaying the [NavigationBar] (if
         * it exists)
         */
        val EndOverlay = FabPosition(3)
    }

    override fun toString(): String {
        return when (this) {
            Start -> "FabPosition.Start"
            Center -> "FabPosition.Center"
            End -> "FabPosition.End"
            else -> "FabPosition.EndOverlay"
        }
    }
}

/**
 * Placement information for a [FloatingActionButton] inside a [Scaffold].
 *
 * @property left the FAB's offset from the left edge of the bottom bar, already adjusted for RTL
 *   support
 * @property width the width of the FAB
 * @property height the height of the FAB
 */
@Immutable internal class FabPlacement(val left: Int, val width: Int, val height: Int)

// FAB spacing above the bottom bar / bottom of the Scaffold
private val FabSpacing = 16.dp

private enum class ScaffoldLayoutContent {
    TopBar,
    MainContent,
    Snackbar,
    Fab,
    BottomBar,
}
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/NavigationBar.kt
```kotlin
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

package androidx.compose.material3

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.DefaultNavigationBarOverride.NavigationBar
import androidx.compose.material3.internal.MappedInteractionSource
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.systemBarsForVisualComponents
import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.NavigationBarTokens
import androidx.compose.material3.tokens.NavigationBarVerticalItemTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.roundToInt

/**
 * [Material Design bottom navigation
 * bar](https://m3.material.io/components/navigation-bar/overview)
 *
 * Navigation bars offer a persistent and convenient way to switch between primary destinations in
 * an app.
 *
 * ![Navigation bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-bar.png)
 *
 * [NavigationBar] should contain three to five [NavigationBarItem]s, each representing a singular
 * destination.
 *
 * A simple example looks like:
 *
 * @sample androidx.compose.material3.samples.NavigationBarSample
 *
 * See [NavigationBarItem] for configuration specific to each item, and not the overall
 * [NavigationBar] component.
 *
 * @param modifier the [Modifier] to be applied to this navigation bar
 * @param containerColor the color used for the background of this navigation bar. Use
 *   [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this navigation bar. Defaults to
 *   either the matching content color for [containerColor], or to the current [LocalContentColor]
 *   if [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets of the navigation bar.
 * @param content the content of this navigation bar, typically 3-5 [NavigationBarItem]s
 */
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationBarDefaults.containerColor,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit,
) {
    with(LocalNavigationBarOverride.current) {
        NavigationBarOverrideScope(
                modifier = modifier,
                containerColor = containerColor,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                windowInsets = windowInsets,
                content = content,
            )
            .NavigationBar()
    }
}

/**
 * This override provides the default behavior of the [NavigationBar] component.
 *
 * [NavigationBarOverride] used when no override is specified.
 */
@ExperimentalMaterial3ComponentOverrideApi
object DefaultNavigationBarOverride : NavigationBarOverride {
    @Composable
    override fun NavigationBarOverrideScope.NavigationBar() {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            modifier = modifier,
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .windowInsetsPadding(windowInsets)
                        .defaultMinSize(minHeight = NavigationBarHeight)
                        .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(NavigationBarItemHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/**
 * Material Design navigation bar item.
 *
 * Navigation bars offer a persistent and convenient way to switch between primary destinations in
 * an app.
 *
 * The recommended configuration for a [NavigationBarItem] depends on how many items there are
 * inside a [NavigationBar]:
 * - Three destinations: Display icons and text labels for all destinations.
 * - Four destinations: Active destinations display an icon and text label. Inactive destinations
 *   display icons, and text labels are recommended.
 * - Five destinations: Active destinations display an icon and text label. Inactive destinations
 *   use icons, and use text labels if space permits.
 *
 * A [NavigationBarItem] always shows text labels (if it exists) when selected. Showing text labels
 * if not selected is controlled by [alwaysShowLabel].
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param label optional text label for this item
 * @param alwaysShowLabel whether to always show the label for this item. If `false`, the label will
 *   only be shown when this item is selected.
 * @param colors [NavigationBarItemColors] that will be used to resolve the colors used for this
 *   item in different states. See [NavigationBarItemDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun RowScope.NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    colors: NavigationBarItemColors = NavigationBarItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // TODO Load the motionScheme tokens from the component tokens file
    val colorAnimationSpec = MotionSchemeKeyTokens.DefaultEffects.value<Color>()
    val styledIcon =
        @Composable {
            val iconColor by
                animateColorAsState(
                    targetValue = colors.iconColor(selected = selected, enabled = enabled),
                    animationSpec = colorAnimationSpec,
                )
            // If there's a label, don't have a11y services repeat the icon description.
            val clearSemantics = label != null && (alwaysShowLabel || selected)
            Box(modifier = if (clearSemantics) Modifier.clearAndSetSemantics {} else Modifier) {
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
            }
        }

    val styledLabel: @Composable (() -> Unit)? =
        label?.let {
            @Composable {
                val style = NavigationBarTokens.LabelTextFont.value
                val textColor by
                    animateColorAsState(
                        targetValue = colors.textColor(selected = selected, enabled = enabled),
                        animationSpec = colorAnimationSpec,
                    )
                ProvideContentColorTextStyle(
                    contentColor = textColor,
                    textStyle = style,
                    content = label,
                )
            }
        }

    var itemWidth by remember { mutableIntStateOf(0) }

    Box(
        modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .defaultMinSize(minHeight = NavigationBarHeight)
            .weight(1f)
            .onSizeChanged { itemWidth = it.width },
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        val alphaAnimationProgress: State<Float> =
            animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                // TODO Load the motionScheme tokens from the component tokens file
                animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
            )
        val sizeAnimationProgress: State<Float> =
            animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                // TODO Load the motionScheme tokens from the component tokens file
                animationSpec = MotionSchemeKeyTokens.FastSpatial.value(),
            )
        // The entire item is selectable, but only the indicator pill shows the ripple. To achieve
        // this, we re-map the coordinates of the item's InteractionSource into the coordinates of
        // the indicator.
        val density = LocalDensity.current
        val calculateDeltaOffset = {
            with(density) {
                val indicatorWidth =
                    NavigationBarVerticalItemTokens.ActiveIndicatorWidth.roundToPx()
                Offset((itemWidth - indicatorWidth).toFloat() / 2, IndicatorVerticalOffset.toPx())
            }
        }
        val offsetInteractionSource =
            remember(interactionSource, calculateDeltaOffset) {
                MappedInteractionSource(interactionSource, calculateDeltaOffset)
            }

        // The indicator has a width-expansion animation which interferes with the timing of the
        // ripple, which is why they are separate composables
        val indicatorRipple =
            @Composable {
                Box(
                    Modifier.layoutId(IndicatorRippleLayoutIdTag)
                        .clip(NavigationBarTokens.ItemActiveIndicatorShape.value)
                        .indication(offsetInteractionSource, ripple())
                )
            }
        val indicator =
            @Composable {
                Box(
                    Modifier.layoutId(IndicatorLayoutIdTag)
                        .graphicsLayer { alpha = alphaAnimationProgress.value }
                        .background(
                            color = colors.indicatorColor,
                            shape = NavigationBarTokens.ItemActiveIndicatorShape.value,
                        )
                )
            }

        NavigationBarItemLayout(
            indicatorRipple = indicatorRipple,
            indicator = indicator,
            icon = styledIcon,
            label = styledLabel,
            alwaysShowLabel = alwaysShowLabel,
            alphaAnimationProgress = { alphaAnimationProgress.value },
            sizeAnimationProgress = { sizeAnimationProgress.value },
        )
    }
}

/** Defaults used in [NavigationBar]. */
object NavigationBarDefaults {
    /** Default elevation for a navigation bar. */
    val Elevation: Dp = ElevationTokens.Level0

    /** Default color for a navigation bar. */
    val containerColor: Color
        @Composable get() = NavigationBarTokens.ContainerColor.value

    /** Default window insets to be used and consumed by navigation bar */
    val windowInsets: WindowInsets
        @Composable
        get() =
            WindowInsets.systemBarsForVisualComponents.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            )
}

/** Defaults used in [NavigationBarItem]. */
object NavigationBarItemDefaults {

    /**
     * Creates a [NavigationBarItemColors] with the provided colors according to the Material
     * specification.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultNavigationBarItemColors

    /**
     * Creates a [NavigationBarItemColors] with the provided colors according to the Material
     * specification.
     *
     * @param selectedIconColor the color to use for the icon when the item is selected.
     * @param selectedTextColor the color to use for the text label when the item is selected.
     * @param indicatorColor the color to use for the indicator when the item is selected.
     * @param unselectedIconColor the color to use for the icon when the item is unselected.
     * @param unselectedTextColor the color to use for the text label when the item is unselected.
     * @param disabledIconColor the color to use for the icon when the item is disabled.
     * @param disabledTextColor the color to use for the text label when the item is disabled.
     * @return the resulting [NavigationBarItemColors] used for [NavigationBarItem]
     */
    @Composable
    fun colors(
        selectedIconColor: Color = Color.Unspecified,
        selectedTextColor: Color = Color.Unspecified,
        indicatorColor: Color = Color.Unspecified,
        unselectedIconColor: Color = Color.Unspecified,
        unselectedTextColor: Color = Color.Unspecified,
        disabledIconColor: Color = Color.Unspecified,
        disabledTextColor: Color = Color.Unspecified,
    ): NavigationBarItemColors =
        MaterialTheme.colorScheme.defaultNavigationBarItemColors.copy(
            selectedIconColor = selectedIconColor,
            selectedTextColor = selectedTextColor,
            selectedIndicatorColor = indicatorColor,
            unselectedIconColor = unselectedIconColor,
            unselectedTextColor = unselectedTextColor,
            disabledIconColor = disabledIconColor,
            disabledTextColor = disabledTextColor,
        )

    internal val ColorScheme.defaultNavigationBarItemColors: NavigationBarItemColors
        get() {
            return defaultNavigationBarItemColorsCached
                ?: NavigationBarItemColors(
                        selectedIconColor = fromToken(NavigationBarTokens.ItemActiveIconColor),
                        selectedTextColor = fromToken(NavigationBarTokens.ItemActiveLabelTextColor),
                        selectedIndicatorColor =
                            fromToken(NavigationBarTokens.ItemActiveIndicatorColor),
                        unselectedIconColor = fromToken(NavigationBarTokens.ItemInactiveIconColor),
                        unselectedTextColor =
                            fromToken(NavigationBarTokens.ItemInactiveLabelTextColor),
                        disabledIconColor =
                            fromToken(NavigationBarTokens.ItemInactiveIconColor)
                                .copy(alpha = DisabledAlpha),
                        disabledTextColor =
                            fromToken(NavigationBarTokens.ItemInactiveLabelTextColor)
                                .copy(alpha = DisabledAlpha),
                    )
                    .also { defaultNavigationBarItemColorsCached = it }
        }

    @Deprecated(
        "Use overload with disabledIconColor and disabledTextColor",
        level = DeprecationLevel.HIDDEN,
    )
    @Composable
    fun colors(
        selectedIconColor: Color = NavigationBarTokens.ItemActiveIconColor.value,
        selectedTextColor: Color = NavigationBarTokens.ItemActiveLabelTextColor.value,
        indicatorColor: Color = NavigationBarTokens.ItemActiveIndicatorColor.value,
        unselectedIconColor: Color = NavigationBarTokens.ItemInactiveIconColor.value,
        unselectedTextColor: Color = NavigationBarTokens.ItemInactiveLabelTextColor.value,
    ): NavigationBarItemColors =
        NavigationBarItemColors(
            selectedIconColor = selectedIconColor,
            selectedTextColor = selectedTextColor,
            selectedIndicatorColor = indicatorColor,
            unselectedIconColor = unselectedIconColor,
            unselectedTextColor = unselectedTextColor,
            disabledIconColor = unselectedIconColor.copy(alpha = DisabledAlpha),
            disabledTextColor = unselectedTextColor.copy(alpha = DisabledAlpha),
        )
}

/**
 * Represents the colors of the various elements of a navigation item.
 *
 * @param selectedIconColor the color to use for the icon when the item is selected.
 * @param selectedTextColor the color to use for the text label when the item is selected.
 * @param selectedIndicatorColor the color to use for the indicator when the item is selected.
 * @param unselectedIconColor the color to use for the icon when the item is unselected.
 * @param unselectedTextColor the color to use for the text label when the item is unselected.
 * @param disabledIconColor the color to use for the icon when the item is disabled.
 * @param disabledTextColor the color to use for the text label when the item is disabled.
 * @constructor create an instance with arbitrary colors.
 */
@Immutable
class NavigationBarItemColors
constructor(
    val selectedIconColor: Color,
    val selectedTextColor: Color,
    val selectedIndicatorColor: Color,
    val unselectedIconColor: Color,
    val unselectedTextColor: Color,
    val disabledIconColor: Color,
    val disabledTextColor: Color,
) {
    /**
     * Returns a copy of this NavigationBarItemColors, optionally overriding some of the values.
     * This uses the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        selectedIconColor: Color = this.selectedIconColor,
        selectedTextColor: Color = this.selectedTextColor,
        selectedIndicatorColor: Color = this.selectedIndicatorColor,
        unselectedIconColor: Color = this.unselectedIconColor,
        unselectedTextColor: Color = this.unselectedTextColor,
        disabledIconColor: Color = this.disabledIconColor,
        disabledTextColor: Color = this.disabledTextColor,
    ) =
        NavigationBarItemColors(
            selectedIconColor.takeOrElse { this.selectedIconColor },
            selectedTextColor.takeOrElse { this.selectedTextColor },
            selectedIndicatorColor.takeOrElse { this.selectedIndicatorColor },
            unselectedIconColor.takeOrElse { this.unselectedIconColor },
            unselectedTextColor.takeOrElse { this.unselectedTextColor },
            disabledIconColor.takeOrElse { this.disabledIconColor },
            disabledTextColor.takeOrElse { this.disabledTextColor },
        )

    /**
     * Represents the icon color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     * @param enabled whether the item is enabled
     */
    @Stable
    internal fun iconColor(selected: Boolean, enabled: Boolean): Color =
        when {
            !enabled -> disabledIconColor
            selected -> selectedIconColor
            else -> unselectedIconColor
        }

    /**
     * Represents the text color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     * @param enabled whether the item is enabled
     */
    @Stable
    internal fun textColor(selected: Boolean, enabled: Boolean): Color =
        when {
            !enabled -> disabledTextColor
            selected -> selectedTextColor
            else -> unselectedTextColor
        }

    /** Represents the color of the indicator used for selected items. */
    internal val indicatorColor: Color
        get() = selectedIndicatorColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavigationBarItemColors) return false

        if (selectedIconColor != other.selectedIconColor) return false
        if (unselectedIconColor != other.unselectedIconColor) return false
        if (selectedTextColor != other.selectedTextColor) return false
        if (unselectedTextColor != other.unselectedTextColor) return false
        if (selectedIndicatorColor != other.selectedIndicatorColor) return false
        if (disabledIconColor != other.disabledIconColor) return false
        if (disabledTextColor != other.disabledTextColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedIconColor.hashCode()
        result = 31 * result + unselectedIconColor.hashCode()
        result = 31 * result + selectedTextColor.hashCode()
        result = 31 * result + unselectedTextColor.hashCode()
        result = 31 * result + selectedIndicatorColor.hashCode()
        result = 31 * result + disabledIconColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()

        return result
    }
}

/**
 * Base layout for a [NavigationBarItem].
 *
 * @param indicatorRipple indicator ripple for this item when it is selected
 * @param indicator indicator for this item when it is selected
 * @param icon icon for this item
 * @param label text label for this item
 * @param alwaysShowLabel whether to always show the label for this item. If false, the label will
 *   only be shown when this item is selected.
 * @param alphaAnimationProgress progress of the animation, where 0 represents the unselected state
 *   of this item and 1 represents the selected state. This value controls the indicator's opacity.
 * @param sizeAnimationProgress progress of the animation, where 0 represents the unselected state
 *   of this item and 1 represents the selected state. This value controls other values such as
 *   indicator size, icon and label positions, etc.
 */
@Composable
private fun NavigationBarItemLayout(
    indicatorRipple: @Composable () -> Unit,
    indicator: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    alwaysShowLabel: Boolean,
    alphaAnimationProgress: () -> Float,
    sizeAnimationProgress: () -> Float,
) {
    Layout(
        modifier = Modifier.badgeBounds(),
        content = {
            indicatorRipple()
            indicator()

            Box(Modifier.layoutId(IconLayoutIdTag)) { icon() }

            if (label != null) {
                Box(
                    Modifier.layoutId(LabelLayoutIdTag).graphicsLayer {
                        alpha = if (alwaysShowLabel) 1f else alphaAnimationProgress()
                    }
                ) {
                    label()
                }
            }
        },
    ) { measurables, constraints ->
        @Suppress("NAME_SHADOWING")
        // Ensure that the progress is >= 0. It may be negative on bouncy springs, for example.
        val animationProgress = sizeAnimationProgress().coerceAtLeast(0f)
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val iconPlaceable =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.measure(looseConstraints)

        val totalIndicatorWidth = iconPlaceable.width + (IndicatorHorizontalPadding * 2).roundToPx()
        val animatedIndicatorWidth = (totalIndicatorWidth * animationProgress).roundToInt()
        val indicatorHeight = iconPlaceable.height + (IndicatorVerticalPadding * 2).roundToPx()
        val indicatorRipplePlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorRippleLayoutIdTag }
                .measure(Constraints.fixed(width = totalIndicatorWidth, height = indicatorHeight))
        val indicatorPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == IndicatorLayoutIdTag }
                ?.measure(
                    Constraints.fixed(width = animatedIndicatorWidth, height = indicatorHeight)
                )

        val labelPlaceable =
            label?.let {
                measurables.fastFirst { it.layoutId == LabelLayoutIdTag }.measure(looseConstraints)
            }

        if (label == null) {
            placeIcon(iconPlaceable, indicatorRipplePlaceable, indicatorPlaceable, constraints)
        } else {
            placeLabelAndIcon(
                labelPlaceable!!,
                iconPlaceable,
                indicatorRipplePlaceable,
                indicatorPlaceable,
                constraints,
                alwaysShowLabel,
                animationProgress,
            )
        }
    }
}

/** Places the provided [Placeable]s in the center of the provided [constraints]. */
private fun MeasureScope.placeIcon(
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable?,
    constraints: Constraints,
): MeasureResult {
    val width =
        if (constraints.maxWidth == Constraints.Infinity) {
            iconPlaceable.width + NavigationBarItemToIconMinimumPadding.roundToPx() * 2
        } else {
            constraints.maxWidth
        }
    val height = constraints.constrainHeight(NavigationBarHeight.roundToPx())

    val iconX = (width - iconPlaceable.width) / 2
    val iconY = (height - iconPlaceable.height) / 2

    val rippleX = (width - indicatorRipplePlaceable.width) / 2
    val rippleY = (height - indicatorRipplePlaceable.height) / 2

    return layout(width, height) {
        indicatorPlaceable?.let {
            val indicatorX = (width - it.width) / 2
            val indicatorY = (height - it.height) / 2
            it.placeRelative(indicatorX, indicatorY)
        }
        iconPlaceable.placeRelative(iconX, iconY)
        indicatorRipplePlaceable.placeRelative(rippleX, rippleY)
    }
}

/**
 * Places the provided [Placeable]s in the correct position, depending on [alwaysShowLabel] and
 * [animationProgress].
 *
 * When [alwaysShowLabel] is true, the positions do not move. The [iconPlaceable] and
 * [labelPlaceable] will be placed together in the center with padding between them, according to
 * the spec.
 *
 * When [animationProgress] is 1 (representing the selected state), the positions will be the same
 * as above.
 *
 * Otherwise, when [animationProgress] is 0, [iconPlaceable] will be placed in the center, like in
 * [placeIcon], and [labelPlaceable] will not be shown.
 *
 * When [animationProgress] is animating between these values, [iconPlaceable] and [labelPlaceable]
 * will be placed at a corresponding interpolated position.
 *
 * [indicatorRipplePlaceable] and [indicatorPlaceable] will always be placed in such a way that to
 * share the same center as [iconPlaceable].
 *
 * @param labelPlaceable text label placeable inside this item
 * @param iconPlaceable icon placeable inside this item
 * @param indicatorRipplePlaceable indicator ripple placeable inside this item
 * @param indicatorPlaceable indicator placeable inside this item, if it exists
 * @param constraints constraints of the item
 * @param alwaysShowLabel whether to always show the label for this item. If true, icon and label
 *   positions will not change. If false, positions transition between 'centered icon with no label'
 *   and 'top aligned icon with label'.
 * @param animationProgress progress of the animation, where 0 represents the unselected state of
 *   this item and 1 represents the selected state. Values between 0 and 1 interpolate positions of
 *   the icon and label.
 */
private fun MeasureScope.placeLabelAndIcon(
    labelPlaceable: Placeable,
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable?,
    constraints: Constraints,
    alwaysShowLabel: Boolean,
    animationProgress: Float,
): MeasureResult {
    val contentHeight =
        iconPlaceable.height +
            IndicatorVerticalPadding.toPx() +
            NavigationBarIndicatorToLabelPadding.toPx() +
            labelPlaceable.height
    val contentVerticalPadding =
        ((constraints.minHeight - contentHeight) / 2).coerceAtLeast(IndicatorVerticalPadding.toPx())
    val height = contentHeight + contentVerticalPadding * 2

    // Icon (when selected) should be `contentVerticalPadding` from top
    val selectedIconY = contentVerticalPadding
    val unselectedIconY =
        if (alwaysShowLabel) selectedIconY else (height - iconPlaceable.height) / 2

    // How far the icon needs to move between unselected and selected states.
    val iconDistance = unselectedIconY - selectedIconY

    // The interpolated fraction of iconDistance that all placeables need to move based on
    // animationProgress.
    val offset = iconDistance * (1 - animationProgress)

    // Label should be fixed padding below icon
    val labelY =
        selectedIconY +
            iconPlaceable.height +
            IndicatorVerticalPadding.toPx() +
            NavigationBarIndicatorToLabelPadding.toPx()

    val containerWidth =
        if (constraints.maxWidth == Constraints.Infinity) {
            iconPlaceable.width + NavigationBarItemToIconMinimumPadding.roundToPx() * 2
        } else {
            constraints.maxWidth
        }

    val labelX = (containerWidth - labelPlaceable.width) / 2
    val iconX = (containerWidth - iconPlaceable.width) / 2

    val rippleX = (containerWidth - indicatorRipplePlaceable.width) / 2
    val rippleY = selectedIconY - IndicatorVerticalPadding.toPx()

    return layout(containerWidth, height.roundToInt()) {
        indicatorPlaceable?.let {
            val indicatorX = (containerWidth - it.width) / 2
            val indicatorY = selectedIconY - IndicatorVerticalPadding.roundToPx()
            it.placeRelative(indicatorX, (indicatorY + offset).roundToInt())
        }
        if (alwaysShowLabel || animationProgress != 0f) {
            labelPlaceable.placeRelative(labelX, (labelY + offset).roundToInt())
        }
        iconPlaceable.placeRelative(iconX, (selectedIconY + offset).roundToInt())
        indicatorRipplePlaceable.placeRelative(rippleX, (rippleY + offset).roundToInt())
    }
}

private const val IndicatorRippleLayoutIdTag: String = "indicatorRipple"

private const val IndicatorLayoutIdTag: String = "indicator"

private const val IconLayoutIdTag: String = "icon"

private const val LabelLayoutIdTag: String = "label"

private val NavigationBarHeight: Dp = NavigationBarTokens.TallContainerHeight

/*@VisibleForTesting*/
internal val NavigationBarItemHorizontalPadding: Dp = 8.dp

/*@VisibleForTesting*/
internal val NavigationBarIndicatorToLabelPadding: Dp = 4.dp

private val IndicatorHorizontalPadding: Dp =
    (NavigationBarVerticalItemTokens.ActiveIndicatorWidth -
        NavigationBarVerticalItemTokens.IconSize) / 2

/*@VisibleForTesting*/
internal val IndicatorVerticalPadding: Dp =
    (NavigationBarVerticalItemTokens.ActiveIndicatorHeight -
        NavigationBarVerticalItemTokens.IconSize) / 2

private val IndicatorVerticalOffset: Dp = 12.dp

/*@VisibleForTesting*/
internal val NavigationBarItemToIconMinimumPadding: Dp = 44.dp

/**
 * Interface that allows libraries to override the behavior of the [NavigationBar] component.
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [LocalNavigationBarOverride] in the Compose hierarchy.
 */
@ExperimentalMaterial3ComponentOverrideApi
interface NavigationBarOverride {
    /** Behavior function that is called by the [NavigationBar] component. */
    @Composable fun NavigationBarOverrideScope.NavigationBar()
}

/**
 * Parameters available to [NavigationBar].
 *
 * @param modifier the [Modifier] to be applied to this navigation bar
 * @param containerColor the color used for the background of this navigation bar. Use
 *   [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this navigation bar. Defaults to
 *   either the matching content color for [containerColor], or to the current [LocalContentColor]
 *   if [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets of the navigation bar.
 * @param content the content of this navigation bar, typically 3-5 [NavigationBarItem]s
 */
@ExperimentalMaterial3ComponentOverrideApi
class NavigationBarOverrideScope
internal constructor(
    val modifier: Modifier = Modifier,
    val containerColor: Color,
    val contentColor: Color,
    val tonalElevation: Dp,
    val windowInsets: WindowInsets,
    val content: @Composable RowScope.() -> Unit,
)

/** CompositionLocal containing the currently-selected [NavigationBarOverride]. */
@ExperimentalMaterial3ComponentOverrideApi
val LocalNavigationBarOverride: ProvidableCompositionLocal<NavigationBarOverride> =
    compositionLocalOf {
        DefaultNavigationBarOverride
    }
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/NavigationRail.kt
```kotlin
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

package androidx.compose.material3

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.DefaultNavigationRailOverride.NavigationRail
import androidx.compose.material3.internal.MappedInteractionSource
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.systemBarsForVisualComponents
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.NavigationRailBaselineItemTokens
import androidx.compose.material3.tokens.NavigationRailCollapsedTokens
import androidx.compose.material3.tokens.NavigationRailColorTokens
import androidx.compose.material3.tokens.NavigationRailVerticalItemTokens
import androidx.compose.material3.tokens.ShapeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.roundToInt

/**
 * [Material Design bottom navigation
 * rail](https://m3.material.io/components/navigation-rail/overview)
 *
 * Navigation rails provide access to primary destinations in apps when using tablet and desktop
 * screens.
 *
 * ![Navigation rail
 * image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-rail.png)
 *
 * The navigation rail should be used to display three to seven app destinations and, optionally, a
 * [FloatingActionButton] or a logo header. Each destination is typically represented by an icon and
 * an optional text label.
 *
 * [NavigationRail] should contain multiple [NavigationRailItem]s, each representing a singular
 * destination.
 *
 * A simple example looks like:
 *
 * @sample androidx.compose.material3.samples.NavigationRailSample
 *
 * See [NavigationRailItem] for configuration specific to each item, and not the overall
 * NavigationRail component.
 *
 * @param modifier the [Modifier] to be applied to this navigation rail
 * @param containerColor the color used for the background of this navigation rail. Use
 *   [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this navigation rail. Defaults to
 *   either the matching content color for [containerColor], or to the current [LocalContentColor]
 *   if [containerColor] is not a color from the theme.
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param windowInsets a window insets of the navigation rail.
 * @param content the content of this navigation rail, typically 3-7 [NavigationRailItem]s
 */
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationRailDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    header: @Composable (ColumnScope.() -> Unit)? = null,
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    with(LocalNavigationRailOverride.current) {
        NavigationRailOverrideScope(
                modifier = modifier,
                containerColor = containerColor,
                contentColor = contentColor,
                header = header,
                windowInsets = windowInsets,
                content = content,
            )
            .NavigationRail()
    }
}

/**
 * This override provides the default behavior of the [NavigationRail] component.
 *
 * [NavigationRailOverride] used when no override is specified.
 */
@ExperimentalMaterial3ComponentOverrideApi
object DefaultNavigationRailOverride : NavigationRailOverride {
    @Composable
    override fun NavigationRailOverrideScope.NavigationRail() {
        Surface(color = containerColor, contentColor = contentColor, modifier = modifier) {
            Column(
                Modifier.fillMaxHeight()
                    .windowInsetsPadding(windowInsets)
                    .widthIn(min = NavigationRailCollapsedTokens.NarrowContainerWidth)
                    .padding(vertical = NavigationRailVerticalPadding)
                    .selectableGroup()
                    .semantics { isTraversalGroup = true },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NavigationRailVerticalPadding),
            ) {
                val header = header
                if (header != null) {
                    header()
                    Spacer(Modifier.height(NavigationRailHeaderPadding))
                }
                content()
            }
        }
    }
}

/**
 * Material Design navigation rail item.
 *
 * A [NavigationRailItem] represents a destination within a [NavigationRail].
 *
 * Navigation rails provide access to primary destinations in apps when using tablet and desktop
 * screens.
 *
 * The text label is always shown (if it exists) when selected. Showing text labels if not selected
 * is controlled by [alwaysShowLabel].
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param label optional text label for this item
 * @param alwaysShowLabel whether to always show the label for this item. If false, the label will
 *   only be shown when this item is selected.
 * @param colors [NavigationRailItemColors] that will be used to resolve the colors used for this
 *   item in different states. See [NavigationRailItemDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun NavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    colors: NavigationRailItemColors = NavigationRailItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // TODO Load the motionScheme tokens from the component tokens file
    val colorAnimationSpec = MotionSchemeKeyTokens.DefaultEffects.value<Color>()
    val styledIcon =
        @Composable {
            val iconColor by
                animateColorAsState(
                    targetValue = colors.iconColor(selected = selected, enabled = enabled),
                    animationSpec = colorAnimationSpec,
                )
            // If there's a label, don't have a11y services repeat the icon description.
            val clearSemantics = label != null && (alwaysShowLabel || selected)
            Box(modifier = if (clearSemantics) Modifier.clearAndSetSemantics {} else Modifier) {
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
            }
        }

    val styledLabel: @Composable (() -> Unit)? =
        label?.let {
            @Composable {
                val style = NavigationRailVerticalItemTokens.LabelTextFont.value
                val textColor by
                    animateColorAsState(
                        targetValue = colors.textColor(selected = selected, enabled = enabled),
                        animationSpec = colorAnimationSpec,
                    )
                ProvideContentColorTextStyle(
                    contentColor = textColor,
                    textStyle = style,
                    content = label,
                )
            }
        }

    Box(
        modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .defaultMinSize(minHeight = NavigationRailItemHeight)
            .widthIn(min = NavigationRailItemWidth),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        val alphaAnimationProgress: State<Float> =
            animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                // TODO Load the motionScheme tokens from the component tokens file
                animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
            )
        val sizeAnimationProgress: State<Float> =
            animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                // TODO Load the motionScheme tokens from the component tokens file
                animationSpec = MotionSchemeKeyTokens.FastSpatial.value(),
            )

        // The entire item is selectable, but only the indicator pill shows the ripple. To achieve
        // this, we re-map the coordinates of the item's InteractionSource into the coordinates of
        // the indicator.
        val density = LocalDensity.current
        val calculateDeltaOffset = {
            with(density) {
                val itemWidth = NavigationRailItemWidth.roundToPx()
                val indicatorWidth =
                    NavigationRailVerticalItemTokens.ActiveIndicatorWidth.roundToPx()
                Offset((itemWidth - indicatorWidth).toFloat() / 2, 0f)
            }
        }
        val offsetInteractionSource =
            remember(interactionSource, calculateDeltaOffset) {
                MappedInteractionSource(interactionSource, calculateDeltaOffset)
            }

        val indicatorShape =
            if (label != null) {
                NavigationRailBaselineItemTokens.ActiveIndicatorShape.value
            } else {
                ShapeKeyTokens.CornerFull.value
            }

        // The indicator has a width-expansion animation which interferes with the timing of the
        // ripple, which is why they are separate composables
        val indicatorRipple =
            @Composable {
                Box(
                    Modifier.layoutId(IndicatorRippleLayoutIdTag)
                        .clip(indicatorShape)
                        .indication(offsetInteractionSource, ripple())
                )
            }
        val indicator =
            @Composable {
                Box(
                    Modifier.layoutId(IndicatorLayoutIdTag)
                        .graphicsLayer { alpha = alphaAnimationProgress.value }
                        .background(color = colors.indicatorColor, shape = indicatorShape)
                )
            }

        NavigationRailItemLayout(
            indicatorRipple = indicatorRipple,
            indicator = indicator,
            icon = styledIcon,
            label = styledLabel,
            alwaysShowLabel = alwaysShowLabel,
            alphaAnimationProgress = { alphaAnimationProgress.value },
            sizeAnimationProgress = { sizeAnimationProgress.value },
        )
    }
}

/** Defaults used in [NavigationRail] */
object NavigationRailDefaults {
    /** Default container color of a navigation rail. */
    val ContainerColor: Color
        @Composable get() = NavigationRailCollapsedTokens.ContainerColor.value

    /** Default window insets for navigation rail. */
    val windowInsets: WindowInsets
        @Composable
        get() =
            WindowInsets.systemBarsForVisualComponents.only(
                WindowInsetsSides.Vertical + WindowInsetsSides.Start
            )
}

/** Defaults used in [NavigationRailItem]. */
object NavigationRailItemDefaults {
    /**
     * Creates a [NavigationRailItemColors] with the provided colors according to the Material
     * specification.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultNavigationRailItemColors

    /**
     * Creates a [NavigationRailItemColors] with the provided colors according to the Material
     * specification.
     *
     * @param selectedIconColor the color to use for the icon when the item is selected.
     * @param selectedTextColor the color to use for the text label when the item is selected.
     * @param indicatorColor the color to use for the indicator when the item is selected.
     * @param unselectedIconColor the color to use for the icon when the item is unselected.
     * @param unselectedTextColor the color to use for the text label when the item is unselected.
     * @param disabledIconColor the color to use for the icon when the item is disabled.
     * @param disabledTextColor the color to use for the text label when the item is disabled.
     * @return the resulting [NavigationRailItemColors] used for [NavigationRailItem]
     */
    @Composable
    fun colors(
        selectedIconColor: Color = NavigationRailColorTokens.ItemActiveIcon.value,
        selectedTextColor: Color = NavigationRailColorTokens.ItemActiveLabelText.value,
        indicatorColor: Color = NavigationRailColorTokens.ItemActiveIndicator.value,
        unselectedIconColor: Color = NavigationRailColorTokens.ItemInactiveIcon.value,
        unselectedTextColor: Color = NavigationRailColorTokens.ItemInactiveLabelText.value,
        disabledIconColor: Color = unselectedIconColor.copy(alpha = DisabledAlpha),
        disabledTextColor: Color = unselectedTextColor.copy(alpha = DisabledAlpha),
    ): NavigationRailItemColors =
        MaterialTheme.colorScheme.defaultNavigationRailItemColors.copy(
            selectedIconColor = selectedIconColor,
            selectedTextColor = selectedTextColor,
            selectedIndicatorColor = indicatorColor,
            unselectedIconColor = unselectedIconColor,
            unselectedTextColor = unselectedTextColor,
            disabledIconColor = disabledIconColor,
            disabledTextColor = disabledTextColor,
        )

    internal val ColorScheme.defaultNavigationRailItemColors: NavigationRailItemColors
        get() {
            return defaultNavigationRailItemColorsCached
                ?: NavigationRailItemColors(
                        selectedIconColor = fromToken(NavigationRailColorTokens.ItemActiveIcon),
                        selectedTextColor =
                            fromToken(NavigationRailColorTokens.ItemActiveLabelText),
                        selectedIndicatorColor =
                            fromToken(NavigationRailColorTokens.ItemActiveIndicator),
                        unselectedIconColor = fromToken(NavigationRailColorTokens.ItemInactiveIcon),
                        unselectedTextColor =
                            fromToken(NavigationRailColorTokens.ItemInactiveLabelText),
                        disabledIconColor =
                            fromToken(NavigationRailColorTokens.ItemInactiveIcon)
                                .copy(alpha = DisabledAlpha),
                        disabledTextColor =
                            fromToken(NavigationRailColorTokens.ItemInactiveLabelText)
                                .copy(alpha = DisabledAlpha),
                    )
                    .also { defaultNavigationRailItemColorsCached = it }
        }

    @Deprecated(
        "Use overload with disabledIconColor and disabledTextColor",
        level = DeprecationLevel.HIDDEN,
    )
    @Composable
    fun colors(
        selectedIconColor: Color = NavigationRailColorTokens.ItemActiveIcon.value,
        selectedTextColor: Color = NavigationRailColorTokens.ItemActiveLabelText.value,
        indicatorColor: Color = NavigationRailColorTokens.ItemActiveIndicator.value,
        unselectedIconColor: Color = NavigationRailColorTokens.ItemInactiveIcon.value,
        unselectedTextColor: Color = NavigationRailColorTokens.ItemInactiveLabelText.value,
    ): NavigationRailItemColors =
        NavigationRailItemColors(
            selectedIconColor = selectedIconColor,
            selectedTextColor = selectedTextColor,
            selectedIndicatorColor = indicatorColor,
            unselectedIconColor = unselectedIconColor,
            unselectedTextColor = unselectedTextColor,
            disabledIconColor = unselectedIconColor.copy(alpha = DisabledAlpha),
            disabledTextColor = unselectedTextColor.copy(alpha = DisabledAlpha),
        )
}

/**
 * Represents the colors of the various elements of a navigation item.
 *
 * @param selectedIconColor the color to use for the icon when the item is selected.
 * @param selectedTextColor the color to use for the text label when the item is selected.
 * @param selectedIndicatorColor the color to use for the indicator when the item is selected.
 * @param unselectedIconColor the color to use for the icon when the item is unselected.
 * @param unselectedTextColor the color to use for the text label when the item is unselected.
 * @param disabledIconColor the color to use for the icon when the item is disabled.
 * @param disabledTextColor the color to use for the text label when the item is disabled.
 * @constructor create an instance with arbitrary colors.
 */
@Immutable
class NavigationRailItemColors
constructor(
    val selectedIconColor: Color,
    val selectedTextColor: Color,
    val selectedIndicatorColor: Color,
    val unselectedIconColor: Color,
    val unselectedTextColor: Color,
    val disabledIconColor: Color,
    val disabledTextColor: Color,
) {
    /**
     * Returns a copy of this NavigationRailItemColors, optionally overriding some of the values.
     * This uses the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        selectedIconColor: Color = this.selectedIconColor,
        selectedTextColor: Color = this.selectedTextColor,
        selectedIndicatorColor: Color = this.selectedIndicatorColor,
        unselectedIconColor: Color = this.unselectedIconColor,
        unselectedTextColor: Color = this.unselectedTextColor,
        disabledIconColor: Color = this.disabledIconColor,
        disabledTextColor: Color = this.disabledTextColor,
    ) =
        NavigationRailItemColors(
            selectedIconColor.takeOrElse { this.selectedIconColor },
            selectedTextColor.takeOrElse { this.selectedTextColor },
            selectedIndicatorColor.takeOrElse { this.selectedIndicatorColor },
            unselectedIconColor.takeOrElse { this.unselectedIconColor },
            unselectedTextColor.takeOrElse { this.unselectedTextColor },
            disabledIconColor.takeOrElse { this.disabledIconColor },
            disabledTextColor.takeOrElse { this.disabledTextColor },
        )

    /**
     * Represents the icon color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     * @param enabled whether the item is enabled
     */
    @Stable
    internal fun iconColor(selected: Boolean, enabled: Boolean): Color =
        when {
            !enabled -> disabledIconColor
            selected -> selectedIconColor
            else -> unselectedIconColor
        }

    /**
     * Represents the text color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     * @param enabled whether the item is enabled
     */
    @Stable
    internal fun textColor(selected: Boolean, enabled: Boolean): Color =
        when {
            !enabled -> disabledTextColor
            selected -> selectedTextColor
            else -> unselectedTextColor
        }

    /** Represents the color of the indicator used for selected items. */
    internal val indicatorColor: Color
        get() = selectedIndicatorColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavigationRailItemColors) return false

        if (selectedIconColor != other.selectedIconColor) return false
        if (unselectedIconColor != other.unselectedIconColor) return false
        if (selectedTextColor != other.selectedTextColor) return false
        if (unselectedTextColor != other.unselectedTextColor) return false
        if (selectedIndicatorColor != other.selectedIndicatorColor) return false
        if (disabledIconColor != other.disabledIconColor) return false
        if (disabledTextColor != other.disabledTextColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedIconColor.hashCode()
        result = 31 * result + unselectedIconColor.hashCode()
        result = 31 * result + selectedTextColor.hashCode()
        result = 31 * result + unselectedTextColor.hashCode()
        result = 31 * result + selectedIndicatorColor.hashCode()
        result = 31 * result + disabledIconColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()

        return result
    }
}

/**
 * Base layout for a [NavigationRailItem].
 *
 * @param indicatorRipple indicator ripple for this item when it is selected
 * @param indicator indicator for this item when it is selected
 * @param icon icon for this item
 * @param label text label for this item
 * @param alwaysShowLabel whether to always show the label for this item. If false, the label will
 *   only be shown when this item is selected.
 * @param alphaAnimationProgress progress of the animation, where 0 represents the unselected state
 *   of this item and 1 represents the selected state. This value controls the indicator's color
 *   alpha.
 * @param sizeAnimationProgress progress of the animation, where 0 represents the unselected state
 *   of this item and 1 represents the selected state. This value controls other values such as
 *   indicator size, icon and label positions, etc.
 */
@Composable
private fun NavigationRailItemLayout(
    indicatorRipple: @Composable () -> Unit,
    indicator: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    alwaysShowLabel: Boolean,
    alphaAnimationProgress: () -> Float,
    sizeAnimationProgress: () -> Float,
) {
    Layout(
        modifier = Modifier.badgeBounds(),
        content = {
            indicatorRipple()
            indicator()

            Box(Modifier.layoutId(IconLayoutIdTag)) { icon() }

            if (label != null) {
                Box(
                    Modifier.layoutId(LabelLayoutIdTag).graphicsLayer {
                        alpha = if (alwaysShowLabel) 1f else alphaAnimationProgress()
                    }
                ) {
                    label()
                }
            }
        },
    ) { measurables, constraints ->
        @Suppress("NAME_SHADOWING")
        // Ensure that the progress is >= 0. It may be negative on bouncy springs, for example.
        val animationProgress = sizeAnimationProgress().coerceAtLeast(0f)
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val iconPlaceable =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.measure(looseConstraints)

        val totalIndicatorWidth = iconPlaceable.width + (IndicatorHorizontalPadding * 2).roundToPx()
        val animatedIndicatorWidth = (totalIndicatorWidth * animationProgress).roundToInt()
        val indicatorVerticalPadding =
            if (label == null) {
                IndicatorVerticalPaddingNoLabel
            } else {
                IndicatorVerticalPaddingWithLabel
            }
        val indicatorHeight = iconPlaceable.height + (indicatorVerticalPadding * 2).roundToPx()

        val indicatorRipplePlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorRippleLayoutIdTag }
                .measure(Constraints.fixed(width = totalIndicatorWidth, height = indicatorHeight))
        val indicatorPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == IndicatorLayoutIdTag }
                ?.measure(
                    Constraints.fixed(width = animatedIndicatorWidth, height = indicatorHeight)
                )

        val labelPlaceable =
            label?.let {
                measurables.fastFirst { it.layoutId == LabelLayoutIdTag }.measure(looseConstraints)
            }

        if (label == null) {
            placeIcon(iconPlaceable, indicatorRipplePlaceable, indicatorPlaceable, constraints)
        } else {
            placeLabelAndIcon(
                labelPlaceable!!,
                iconPlaceable,
                indicatorRipplePlaceable,
                indicatorPlaceable,
                constraints,
                alwaysShowLabel,
                animationProgress,
            )
        }
    }
}

/** Places the provided [Placeable]s in the center of the provided [constraints]. */
private fun MeasureScope.placeIcon(
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable?,
    constraints: Constraints,
): MeasureResult {
    val width =
        constraints.constrainWidth(
            maxOf(
                iconPlaceable.width,
                indicatorRipplePlaceable.width,
                indicatorPlaceable?.width ?: 0,
            )
        )
    val height = constraints.constrainHeight(NavigationRailItemHeight.roundToPx())

    val iconX = (width - iconPlaceable.width) / 2
    val iconY = (height - iconPlaceable.height) / 2

    val rippleX = (width - indicatorRipplePlaceable.width) / 2
    val rippleY = (height - indicatorRipplePlaceable.height) / 2

    return layout(width, height) {
        indicatorPlaceable?.let {
            val indicatorX = (width - it.width) / 2
            val indicatorY = (height - it.height) / 2
            it.placeRelative(indicatorX, indicatorY)
        }
        iconPlaceable.placeRelative(iconX, iconY)
        indicatorRipplePlaceable.placeRelative(rippleX, rippleY)
    }
}

/**
 * Places the provided [Placeable]s in the correct position, depending on [alwaysShowLabel] and
 * [animationProgress].
 *
 * When [alwaysShowLabel] is true, the positions do not move. The [iconPlaceable] and
 * [labelPlaceable] will be placed together in the center with padding between them, according to
 * the spec.
 *
 * When [animationProgress] is 1 (representing the selected state), the positions will be the same
 * as above.
 *
 * Otherwise, when [animationProgress] is 0, [iconPlaceable] will be placed in the center, like in
 * [placeIcon], and [labelPlaceable] will not be shown.
 *
 * When [animationProgress] is animating between these values, [iconPlaceable] and [labelPlaceable]
 * will be placed at a corresponding interpolated position.
 *
 * [indicatorRipplePlaceable] and [indicatorPlaceable] will always be placed in such a way that to
 * share the same center as [iconPlaceable].
 *
 * @param labelPlaceable text label placeable inside this item
 * @param iconPlaceable icon placeable inside this item
 * @param indicatorRipplePlaceable indicator ripple placeable inside this item
 * @param indicatorPlaceable indicator placeable inside this item, if it exists
 * @param constraints constraints of the item
 * @param alwaysShowLabel whether to always show the label for this item. If true, icon and label
 *   positions will not change. If false, positions transition between 'centered icon with no label'
 *   and 'top aligned icon with label'.
 * @param animationProgress progress of the animation, where 0 represents the unselected state of
 *   this item and 1 represents the selected state. Values between 0 and 1 interpolate positions of
 *   the icon and label.
 */
private fun MeasureScope.placeLabelAndIcon(
    labelPlaceable: Placeable,
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable?,
    constraints: Constraints,
    alwaysShowLabel: Boolean,
    animationProgress: Float,
): MeasureResult {
    val contentHeight =
        iconPlaceable.height +
            IndicatorVerticalPaddingWithLabel.toPx() +
            NavigationRailItemVerticalPadding.toPx() +
            labelPlaceable.height
    val contentVerticalPadding =
        ((constraints.minHeight - contentHeight) / 2).coerceAtLeast(
            IndicatorVerticalPaddingWithLabel.toPx()
        )
    val height = contentHeight + contentVerticalPadding * 2

    // Icon (when selected) should be `contentVerticalPadding` from the top
    val selectedIconY = contentVerticalPadding
    val unselectedIconY =
        if (alwaysShowLabel) selectedIconY else (height - iconPlaceable.height) / 2

    // How far the icon needs to move between unselected and selected states
    val iconDistance = unselectedIconY - selectedIconY

    // The interpolated fraction of iconDistance that all placeables need to move based on
    // animationProgress, since the icon is higher in the selected state.
    val offset = iconDistance * (1 - animationProgress)

    // Label should be fixed padding below icon
    val labelY =
        selectedIconY +
            iconPlaceable.height +
            IndicatorVerticalPaddingWithLabel.toPx() +
            NavigationRailItemVerticalPadding.toPx()

    val width =
        constraints.constrainWidth(
            maxOf(iconPlaceable.width, labelPlaceable.width, indicatorPlaceable?.width ?: 0)
        )
    val labelX = (width - labelPlaceable.width) / 2
    val iconX = (width - iconPlaceable.width) / 2
    val rippleX = (width - indicatorRipplePlaceable.width) / 2
    val rippleY = selectedIconY - IndicatorVerticalPaddingWithLabel.toPx()

    return layout(width, height.roundToInt()) {
        indicatorPlaceable?.let {
            val indicatorX = (width - it.width) / 2
            val indicatorY = selectedIconY - IndicatorVerticalPaddingWithLabel.toPx()
            it.placeRelative(indicatorX, (indicatorY + offset).roundToInt())
        }
        if (alwaysShowLabel || animationProgress != 0f) {
            labelPlaceable.placeRelative(labelX, (labelY + offset).roundToInt())
        }
        iconPlaceable.placeRelative(iconX, (selectedIconY + offset).roundToInt())
        indicatorRipplePlaceable.placeRelative(rippleX, (rippleY + offset).roundToInt())
    }
}

private const val IndicatorRippleLayoutIdTag: String = "indicatorRipple"

private const val IndicatorLayoutIdTag: String = "indicator"

private const val IconLayoutIdTag: String = "icon"

private const val LabelLayoutIdTag: String = "label"

/**
 * Vertical padding between the contents of the [NavigationRail] and its top/bottom, and internally
 * between items.
 */
internal val NavigationRailVerticalPadding: Dp = 4.dp

/**
 * Padding at the bottom of the [NavigationRail]'s header. This padding will only be added when the
 * header is not null.
 */
private val NavigationRailHeaderPadding: Dp = 8.dp

/*@VisibleForTesting*/
/** Width of an individual [NavigationRailItem]. */
internal val NavigationRailItemWidth: Dp = NavigationRailCollapsedTokens.NarrowContainerWidth

/*@VisibleForTesting*/
/** Height of an individual [NavigationRailItem]. */
internal val NavigationRailItemHeight: Dp = NavigationRailVerticalItemTokens.ActiveIndicatorWidth

/*@VisibleForTesting*/
/** Vertical padding between the contents of a [NavigationRailItem] and its top/bottom. */
internal val NavigationRailItemVerticalPadding: Dp = 4.dp

private val IndicatorHorizontalPadding: Dp =
    (NavigationRailVerticalItemTokens.ActiveIndicatorWidth -
        NavigationRailBaselineItemTokens.IconSize) / 2

private val IndicatorVerticalPaddingWithLabel: Dp =
    (NavigationRailVerticalItemTokens.ActiveIndicatorHeight -
        NavigationRailBaselineItemTokens.IconSize) / 2

private val IndicatorVerticalPaddingNoLabel: Dp =
    (NavigationRailVerticalItemTokens.ActiveIndicatorWidth -
        NavigationRailBaselineItemTokens.IconSize) / 2

/**
 * Interface that allows libraries to override the behavior of the [NavigationRail] component.
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [LocalNavigationRailOverride] in the Compose hierarchy.
 */
@ExperimentalMaterial3ComponentOverrideApi
interface NavigationRailOverride {
    /** Behavior function that is called by the [NavigationRail] component. */
    @Composable fun NavigationRailOverrideScope.NavigationRail()
}

/**
 * Parameters available to [NavigationRail].
 *
 * @param modifier the [Modifier] to be applied to this navigation rail
 * @param containerColor the color used for the background of this navigation rail. Use
 *   [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this navigation rail. Defaults to
 *   either the matching content color for [containerColor], or to the current [LocalContentColor]
 *   if [containerColor] is not a color from the theme.
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param windowInsets a window insets of the navigation rail.
 * @param content the content of this navigation rail, typically 3-7 [NavigationRailItem]s
 */
@ExperimentalMaterial3ComponentOverrideApi
class NavigationRailOverrideScope
internal constructor(
    val modifier: Modifier = Modifier,
    val containerColor: Color,
    val contentColor: Color,
    val header: @Composable (ColumnScope.() -> Unit)?,
    val windowInsets: WindowInsets,
    val content: @Composable ColumnScope.() -> Unit,
)

/** CompositionLocal containing the currently-selected [NavigationRailOverride]. */
@ExperimentalMaterial3ComponentOverrideApi
val LocalNavigationRailOverride: ProvidableCompositionLocal<NavigationRailOverride> =
    compositionLocalOf {
        DefaultNavigationRailOverride
    }
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/NavigationDrawer.kt
```kotlin
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

package androidx.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.internal.BackEventCompat
import androidx.compose.material3.internal.FloatProducer
import androidx.compose.material3.internal.PredictiveBack
import androidx.compose.material3.internal.PredictiveBackHandler
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.systemBarsForVisualComponents
import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.NavigationDrawerTokens
import androidx.compose.material3.tokens.ScrimTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Possible values of [DrawerState]. */
enum class DrawerValue {
    /** The state of the drawer when it is closed. */
    Closed,

    /** The state of the drawer when it is open. */
    Open,
}

/**
 * State of the [ModalNavigationDrawer] and [DismissibleNavigationDrawer] composable.
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Suppress("NotCloseable")
@Stable
class DrawerState(
    initialValue: DrawerValue,
    internal val confirmStateChange: (DrawerValue) -> Boolean = { true },
) {

    internal var anchoredDraggableMotionSpec: FiniteAnimationSpec<Float> =
        AnchoredDraggableDefaultAnimationSpec

    @Suppress("Deprecation")
    internal val anchoredDraggableState =
        AnchoredDraggableState(
            initialValue = initialValue,
            snapAnimationSpec = anchoredDraggableMotionSpec,
            decayAnimationSpec = AnchoredDraggableDefaults.DecayAnimationSpec,
            confirmValueChange = confirmStateChange,
            positionalThreshold = { distance: Float -> distance * DrawerPositionalThreshold },
            velocityThreshold = { with(requireDensity()) { DrawerVelocityThreshold.toPx() } },
        )

    /** Whether the drawer is open. */
    val isOpen: Boolean
        get() = currentValue == DrawerValue.Open

    /** Whether the drawer is closed. */
    val isClosed: Boolean
        get() = currentValue == DrawerValue.Closed

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the start the drawer currently
     * in. If a swipe or an animation is in progress, this corresponds the state drawer was in
     * before the swipe or animation started.
     */
    val currentValue: DrawerValue
        get() {
            return anchoredDraggableState.settledValue
        }

    /** Whether the state is currently animating. */
    val isAnimationRunning: Boolean
        get() {
            return anchoredDraggableState.isAnimationRunning
        }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is interrupted
     *
     * @return the reason the open animation ended
     */
    suspend fun open() =
        animateTo(targetValue = DrawerValue.Open, animationSpec = openDrawerMotionSpec)

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is interrupted
     *
     * @return the reason the close animation ended
     */
    suspend fun close() =
        animateTo(targetValue = DrawerValue.Closed, animationSpec = closeDrawerMotionSpec)

    /**
     * Set the state of the drawer with specific animation
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     */
    @Deprecated(
        message =
            "This method has been replaced by the open and close methods. The animation " +
                "spec is now an implementation detail of ModalDrawer."
    )
    suspend fun animateTo(targetValue: DrawerValue, anim: AnimationSpec<Float>) {
        animateTo(targetValue = targetValue, animationSpec = anim)
    }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    suspend fun snapTo(targetValue: DrawerValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * The target value of the drawer state.
     *
     * If a swipe is in progress, this is the value that the Drawer would animate to if the swipe
     * finishes. If an animation is running, this is the target value of that animation. Finally, if
     * no swipe or animation is in progress, this is the same as the [currentValue].
     */
    val targetValue: DrawerValue
        get() = anchoredDraggableState.targetValue

    /**
     * The current position (in pixels) of the drawer sheet, or Float.NaN before the offset is
     * initialized.
     *
     * @see [AnchoredDraggableState.offset] for more information.
     */
    @Deprecated(
        message =
            "Please access the offset through currentOffset, which returns the value " +
                "directly instead of wrapping it in a state object.",
        replaceWith = ReplaceWith("currentOffset"),
    )
    val offset: State<Float> =
        object : State<Float> {
            override val value: Float
                get() = anchoredDraggableState.offset
        }

    /**
     * The current position (in pixels) of the drawer sheet, or Float.NaN before the offset is
     * initialized.
     *
     * @see [AnchoredDraggableState.offset] for more information.
     */
    val currentOffset: Float
        get() = anchoredDraggableState.offset

    internal var density: Density? by mutableStateOf(null)

    internal var openDrawerMotionSpec: FiniteAnimationSpec<Float> = snap()

    internal var closeDrawerMotionSpec: FiniteAnimationSpec<Float> = snap()

    private fun requireDensity() =
        requireNotNull(density) {
            "The density on DrawerState ($this) was not set. Did you use DrawerState" +
                " with the ModalNavigationDrawer or DismissibleNavigationDrawer composables?"
        }

    internal fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    private suspend fun animateTo(
        targetValue: DrawerValue,
        animationSpec: AnimationSpec<Float>,
        velocity: Float = anchoredDraggableState.lastVelocity,
    ) {
        anchoredDraggableState.anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
            val targetOffset = anchors.positionOf(latestTarget)
            if (!targetOffset.isNaN()) {
                var prev = if (currentOffset.isNaN()) 0f else currentOffset
                animate(prev, targetOffset, velocity, animationSpec) { value, velocity ->
                    // Our onDrag coerces the value within the bounds, but an animation may
                    // overshoot, for example a spring animation or an overshooting interpolator
                    // We respect the user's intention and allow the overshoot, but still use
                    // DraggableState's drag for its mutex.
                    dragTo(value, velocity)
                    prev = value
                }
            }
        }
    }

    companion object {
        /** The default [Saver] implementation for [DrawerState]. */
        fun Saver(confirmStateChange: (DrawerValue) -> Boolean) =
            Saver<DrawerState, DrawerValue>(
                save = { it.currentValue },
                restore = { DrawerState(it, confirmStateChange) },
            )
    }
}

/**
 * Create and [remember] a [DrawerState].
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
fun rememberDrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true },
): DrawerState {
    return rememberSaveable(saver = DrawerState.Saver(confirmStateChange)) {
        DrawerState(initialValue, confirmStateChange)
    }
}

/**
 * [Material Design navigation drawer](https://m3.material.io/components/navigation-drawer/overview)
 *
 * Navigation drawers provide ergonomic access to destinations in an app.
 *
 * Modal navigation drawers block interaction with the rest of an app’s content with a scrim. They
 * are elevated above most of the app’s UI and don’t affect the screen’s layout grid.
 *
 * ![Navigation drawer
 * image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-drawer.png)
 *
 * @sample androidx.compose.material3.samples.ModalNavigationDrawerSample
 * @param drawerContent content inside this drawer
 * @param modifier the [Modifier] to be applied to this drawer
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not the drawer can be interacted by gestures
 * @param scrimColor color of the scrim that obscures content when the drawer is open
 * @param content content of the rest of the UI
 */
@Composable
fun ModalNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val navigationMenu = getString(Strings.NavigationMenu)
    val density = LocalDensity.current
    var anchorsInitialized by remember { mutableStateOf(false) }
    var minValue by remember(density) { mutableFloatStateOf(0f) }
    val maxValue = 0f
    val focusRequester = remember { FocusRequester() }

    // TODO Load the motionScheme tokens from the component tokens file
    val anchoredDraggableMotion: FiniteAnimationSpec<Float> =
        MotionSchemeKeyTokens.DefaultSpatial.value()
    val openMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.DefaultSpatial.value()
    val closeMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.FastEffects.value()

    SideEffect {
        drawerState.density = density
        drawerState.openDrawerMotionSpec = openMotion
        drawerState.closeDrawerMotionSpec = closeMotion
        drawerState.anchoredDraggableMotionSpec = anchoredDraggableMotion
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            // Keyboard focus should go to first element of the drawer when it opens
            focusRequester.requestFocus()
        }
    }

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier
            .fillMaxSize()
            .anchoredDraggable(
                state = drawerState.anchoredDraggableState,
                orientation = Orientation.Horizontal,
                enabled = gesturesEnabled,
                reverseDirection = isRtl,
            )
    ) {
        Box { content() }
        val onDismissRequest = {
            if (gesturesEnabled && drawerState.confirmStateChange(DrawerValue.Closed)) {
                scope.launch { drawerState.close() }
            }
        }
        Scrim(
            contentDescription = getString(Strings.CloseDrawer),
            onClick = if (drawerState.isOpen) onDismissRequest else null,
            alpha = { calculateFraction(minValue, maxValue, drawerState.requireOffset()) },
            color = scrimColor,
        )
        Layout(
            content = drawerContent,
            modifier =
                Modifier.offset {
                        drawerState.currentOffset.let { offset ->
                            val offsetX =
                                when {
                                    !offset.isNaN() -> offset.roundToInt()
                                    // If offset is NaN, set offset based on open/closed state
                                    drawerState.isOpen -> 0
                                    else -> -DrawerDefaults.MaximumDrawerWidth.roundToPx()
                                }
                            IntOffset(offsetX, 0)
                        }
                    }
                    .semantics {
                        paneTitle = navigationMenu
                        if (drawerState.isOpen) {
                            dismiss {
                                if (drawerState.confirmStateChange(DrawerValue.Closed)) {
                                    scope.launch { drawerState.close() }
                                }
                                true
                            }
                        }
                    }
                    .onKeyEvent {
                        // Drawer should close via escape key.
                        if (
                            drawerState.isOpen &&
                                it.type == KeyEventType.KeyUp &&
                                it.key == Key.Escape
                        ) {
                            scope.launch { drawerState.close() }
                            return@onKeyEvent true
                        }
                        return@onKeyEvent false
                    }
                    .focusRequester(focusRequester),
        ) { measurables, constraints ->
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val placeables = measurables.fastMap { it.measure(looseConstraints) }
            val width = placeables.fastMaxOfOrNull { it.width } ?: 0
            val height = placeables.fastMaxOfOrNull { it.height } ?: 0

            layout(width, height) {
                val currentClosedAnchor =
                    drawerState.anchoredDraggableState.anchors.positionOf(DrawerValue.Closed)
                val calculatedClosedAnchor = -width.toFloat()

                if (!anchorsInitialized || currentClosedAnchor != calculatedClosedAnchor) {
                    if (!anchorsInitialized) {
                        anchorsInitialized = true
                    }
                    minValue = calculatedClosedAnchor
                    drawerState.anchoredDraggableState.updateAnchors(
                        DraggableAnchors {
                            DrawerValue.Closed at minValue
                            DrawerValue.Open at maxValue
                        }
                    )
                }
                val isDrawerVisible =
                    calculateFraction(minValue, maxValue, drawerState.requireOffset()) != 0f
                if (isDrawerVisible) {
                    // Only place the drawer when it's visible so that keyboard focus doesn't
                    // navigate to an offscreen element.
                    placeables.fastForEach { it.placeRelative(0, 0) }
                }
            }
        }
    }
}

/**
 * [Material Design navigation drawer](https://m3.material.io/components/navigation-drawer/overview)
 *
 * Navigation drawers provide ergonomic access to destinations in an app. They’re often next to app
 * content and affect the screen’s layout grid.
 *
 * ![Navigation drawer
 * image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-drawer.png)
 *
 * Dismissible standard drawers can be used for layouts that prioritize content (such as a photo
 * gallery) or for apps where users are unlikely to switch destinations often. They should use a
 * visible navigation menu icon to open and close the drawer.
 *
 * @sample androidx.compose.material3.samples.DismissibleNavigationDrawerSample
 * @param drawerContent content inside this drawer
 * @param modifier the [Modifier] to be applied to this drawer
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not the drawer can be interacted by gestures
 * @param content content of the rest of the UI
 */
@Composable
fun DismissibleNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    var anchorsInitialized by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }

    // TODO Load the motionScheme tokens from the component tokens file
    val openMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.DefaultSpatial.value()
    val closeMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.FastEffects.value()

    SideEffect {
        drawerState.density = density
        drawerState.openDrawerMotionSpec = openMotion
        drawerState.closeDrawerMotionSpec = closeMotion
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            // Keyboard focus should go to first element of the drawer when it opens
            focusRequester.requestFocus()
        }
    }

    val scope = rememberCoroutineScope()
    val navigationMenu = getString(Strings.NavigationMenu)

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier.anchoredDraggable(
            state = drawerState.anchoredDraggableState,
            orientation = Orientation.Horizontal,
            enabled = gesturesEnabled,
            reverseDirection = isRtl,
        )
    ) {
        Layout(
            content = {
                Box(
                    Modifier.semantics {
                            paneTitle = navigationMenu
                            if (drawerState.isOpen) {
                                dismiss {
                                    if (drawerState.confirmStateChange(DrawerValue.Closed)) {
                                        scope.launch { drawerState.close() }
                                    }
                                    true
                                }
                            }
                        }
                        .onKeyEvent {
                            // Drawer should close via escape key.
                            if (
                                drawerState.isOpen &&
                                    it.type == KeyEventType.KeyUp &&
                                    it.key == Key.Escape
                            ) {
                                scope.launch { drawerState.close() }
                                return@onKeyEvent true
                            }
                            return@onKeyEvent false
                        }
                        .focusRequester(focusRequester)
                ) {
                    drawerContent()
                }
                Box { content() }
            }
        ) { measurables, constraints ->
            val sheetPlaceable = measurables[0].measure(constraints)
            val contentPlaceable = measurables[1].measure(constraints)
            layout(contentPlaceable.width, contentPlaceable.height) {
                val currentClosedAnchor =
                    drawerState.anchoredDraggableState.anchors.positionOf(DrawerValue.Closed)
                val calculatedClosedAnchor = -sheetPlaceable.width.toFloat()

                if (!anchorsInitialized || currentClosedAnchor != calculatedClosedAnchor) {
                    if (!anchorsInitialized) {
                        anchorsInitialized = true
                    }
                    drawerState.anchoredDraggableState.updateAnchors(
                        DraggableAnchors {
                            DrawerValue.Closed at calculatedClosedAnchor
                            DrawerValue.Open at 0f
                        }
                    )
                }

                val contentX = sheetPlaceable.width + drawerState.requireOffset().roundToInt()
                contentPlaceable.placeRelative(contentX, 0)
                // The drawer is visible when the content has been offset.
                if (contentX != 0) {
                    // Only place the drawer when it's visible so that keyboard focus doesn't
                    // navigate to an offscreen element.
                    sheetPlaceable.placeRelative(drawerState.requireOffset().roundToInt(), 0)
                }
            }
        }
    }
}

/**
 * [Material Design navigation permanent
 * drawer](https://m3.material.io/components/navigation-drawer/overview)
 *
 * Navigation drawers provide ergonomic access to destinations in an app. They’re often next to app
 * content and affect the screen’s layout grid.
 *
 * ![Navigation drawer
 * image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-drawer.png)
 *
 * The permanent navigation drawer is always visible and usually used for frequently switching
 * destinations. On mobile screens, use [ModalNavigationDrawer] instead.
 *
 * @sample androidx.compose.material3.samples.PermanentNavigationDrawerSample
 * @param drawerContent content inside this drawer
 * @param modifier the [Modifier] to be applied to this drawer
 * @param content content of the rest of the UI
 */
@Composable
fun PermanentNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(modifier.fillMaxSize()) {
        drawerContent()
        Box { content() }
    }
}

/**
 * Content inside of a modal navigation drawer.
 *
 * Note: This version of [ModalDrawerSheet] does not handle back by default. For automatic back
 * handling and predictive back animations on Android 14+, use the [ModalDrawerSheet] that accepts
 * `drawerState` as a param.
 *
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerShape defines the shape of this drawer's container
 * @param drawerContainerColor the color used for the background of this drawer. Use
 *   [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 *   the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 *   [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets for the sheet.
 * @param content content inside of a modal navigation drawer
 */
@Composable
fun ModalDrawerSheet(
    modifier: Modifier = Modifier,
    drawerShape: Shape = DrawerDefaults.shape,
    drawerContainerColor: Color = DrawerDefaults.modalContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.ModalDrawerElevation,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    DrawerSheet(
        drawerPredictiveBackState = null,
        windowInsets = windowInsets,
        modifier = modifier,
        drawerShape = drawerShape,
        drawerContainerColor = drawerContainerColor,
        drawerContentColor = drawerContentColor,
        drawerTonalElevation = drawerTonalElevation,
        content = content,
    )
}

/**
 * Content inside of a modal navigation drawer.
 *
 * Note: This version of [ModalDrawerSheet] requires a [drawerState] to be provided and will handle
 * back by default for all Android versions, as well as animate during predictive back on Android
 * 14+.
 *
 * @param drawerState state of the drawer
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerShape defines the shape of this drawer's container
 * @param drawerContainerColor the color used for the background of this drawer. Use
 *   [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 *   the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 *   [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets for the sheet.
 * @param content content inside of a modal navigation drawer
 */
@Composable
fun ModalDrawerSheet(
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    drawerShape: Shape = DrawerDefaults.shape,
    drawerContainerColor: Color = DrawerDefaults.modalContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.ModalDrawerElevation,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    DrawerPredictiveBackHandler(drawerState) { drawerPredictiveBackState ->
        DrawerSheet(
            drawerPredictiveBackState = drawerPredictiveBackState,
            windowInsets = windowInsets,
            modifier = modifier,
            drawerShape = drawerShape,
            drawerContainerColor = drawerContainerColor,
            drawerContentColor = drawerContentColor,
            drawerTonalElevation = drawerTonalElevation,
            drawerOffset = { drawerState.anchoredDraggableState.offset },
            content = content,
        )
    }
}

/**
 * Content inside of a dismissible navigation drawer.
 *
 * Note: This version of [DismissibleDrawerSheet] does not handle back by default. For automatic
 * back handling and predictive back animations on Android 14+, use the [DismissibleDrawerSheet]
 * that accepts `drawerState` as a param.
 *
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerShape defines the shape of this drawer's container
 * @param drawerContainerColor the color used for the background of this drawer. Use
 *   [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 *   the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 *   [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets for the sheet.
 * @param content content inside of a dismissible navigation drawer
 */
@Composable
fun DismissibleDrawerSheet(
    modifier: Modifier = Modifier,
    drawerShape: Shape = RectangleShape,
    drawerContainerColor: Color = DrawerDefaults.standardContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.DismissibleDrawerElevation,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    DrawerSheet(
        drawerPredictiveBackState = null,
        windowInsets = windowInsets,
        modifier = modifier,
        drawerShape = drawerShape,
        drawerContainerColor = drawerContainerColor,
        drawerContentColor = drawerContentColor,
        drawerTonalElevation = drawerTonalElevation,
        content = content,
    )
}

/**
 * Content inside of a dismissible navigation drawer.
 *
 * Note: This version of [DismissibleDrawerSheet] requires a [drawerState] to be provided and will
 * handle back by default for all Android versions, as well as animate during predictive back on
 * Android 14+.
 *
 * @param drawerState state of the drawer
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerShape defines the shape of this drawer's container
 * @param drawerContainerColor the color used for the background of this drawer. Use
 *   [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 *   the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 *   [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets for the sheet.
 * @param content content inside of a dismissible navigation drawer
 */
@Composable
fun DismissibleDrawerSheet(
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    drawerShape: Shape = RectangleShape,
    drawerContainerColor: Color = DrawerDefaults.standardContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.DismissibleDrawerElevation,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    DrawerPredictiveBackHandler(drawerState) { drawerPredictiveBackState ->
        DrawerSheet(
            drawerPredictiveBackState = drawerPredictiveBackState,
            windowInsets = windowInsets,
            modifier = modifier,
            drawerShape = drawerShape,
            drawerContainerColor = drawerContainerColor,
            drawerContentColor = drawerContentColor,
            drawerTonalElevation = drawerTonalElevation,
            drawerOffset = { drawerState.anchoredDraggableState.offset },
            content = content,
        )
    }
}

/**
 * Content inside of a permanent navigation drawer.
 *
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerShape defines the shape of this drawer's container
 * @param drawerContainerColor the color used for the background of this drawer. Use
 *   [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 *   the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 *   [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets for the sheet.
 * @param content content inside a permanent navigation drawer
 */
@Composable
fun PermanentDrawerSheet(
    modifier: Modifier = Modifier,
    drawerShape: Shape = RectangleShape,
    drawerContainerColor: Color = DrawerDefaults.standardContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.PermanentDrawerElevation,
    windowInsets: WindowInsets = DrawerDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    val navigationMenu = getString(Strings.NavigationMenu)
    DrawerSheet(
        drawerPredictiveBackState = null,
        windowInsets = windowInsets,
        modifier = modifier.semantics { paneTitle = navigationMenu },
        drawerShape = drawerShape,
        drawerContainerColor = drawerContainerColor,
        drawerContentColor = drawerContentColor,
        drawerTonalElevation = drawerTonalElevation,
        content = content,
    )
}

@Composable
internal fun DrawerSheet(
    drawerPredictiveBackState: DrawerPredictiveBackState?,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
    drawerShape: Shape = RectangleShape,
    drawerContainerColor: Color = DrawerDefaults.standardContainerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.PermanentDrawerElevation,
    drawerOffset: FloatProducer = FloatProducer { 0F },
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val maxWidth = NavigationDrawerTokens.ContainerWidth
    val maxWidthPx = with(density) { maxWidth.toPx() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val predictiveBackDrawerContainerModifier =
        if (drawerPredictiveBackState != null) {
            Modifier.predictiveBackDrawerContainer(drawerPredictiveBackState, isRtl)
        } else {
            Modifier
        }
    Surface(
        modifier =
            modifier
                .sizeIn(minWidth = MinimumDrawerWidth, maxWidth = maxWidth)
                // Scale up the Surface horizontally in case the drawer offset it greater than zero.
                // This is done to avoid showing a gap when the drawer opens and bounces when it's
                // applied with a bouncy motion. Note that the content inside the Surface is scaled
                // back down to maintain its aspect ratio (see below).
                .horizontalScaleUp(
                    drawerOffset = drawerOffset,
                    drawerWidth = maxWidthPx,
                    isRtl = isRtl,
                )
                .then(predictiveBackDrawerContainerModifier)
                .fillMaxHeight(),
        shape = drawerShape,
        color = drawerContainerColor,
        contentColor = drawerContentColor,
        tonalElevation = drawerTonalElevation,
    ) {
        val predictiveBackDrawerChildModifier =
            if (drawerPredictiveBackState != null)
                Modifier.predictiveBackDrawerChild(drawerPredictiveBackState, isRtl)
            else Modifier
        Column(
            Modifier.sizeIn(minWidth = MinimumDrawerWidth, maxWidth = maxWidth)
                // Scale the content down in case the drawer offset is greater than one. The
                // wrapping Surface is scaled up, so this is done to maintain the content's aspect
                // ratio.
                .horizontalScaleDown(
                    drawerOffset = drawerOffset,
                    drawerWidth = maxWidthPx,
                    isRtl = isRtl,
                )
                .then(predictiveBackDrawerChildModifier)
                .windowInsetsPadding(windowInsets),
            content = content,
        )
    }
}

/**
 * A [Modifier] that scales up the drawing layer on the X axis in case the [drawerOffset] is greater
 * than zero. The scaling will ensure that there is no visible gap between the drawer and the edge
 * of the screen in case the drawer bounces when it opens due to a more expressive motion setting.
 *
 * A [horizontalScaleDown] should be applied to the content of the drawer to maintain the content
 * aspect ratio as the container scales up.
 *
 * @see horizontalScaleDown
 */
private fun Modifier.horizontalScaleUp(
    drawerOffset: FloatProducer,
    drawerWidth: Float,
    isRtl: Boolean,
) = graphicsLayer {
    val offset = drawerOffset()
    scaleX = if (offset > 0f) 1f + offset / drawerWidth else 1f
    transformOrigin = TransformOrigin(if (isRtl) 0f else 1f, 0.5f)
}

/**
 * A [Modifier] that scales down the drawing layer on the X axis in case the [drawerOffset] is
 * greater than zero. This modifier should be applied to the content inside a component that was
 * scaled up with a [horizontalScaleUp] modifier. It will ensure that the content maintains its
 * aspect ratio as the container scales up.
 *
 * @see horizontalScaleUp
 */
private fun Modifier.horizontalScaleDown(
    drawerOffset: FloatProducer,
    drawerWidth: Float,
    isRtl: Boolean,
) = graphicsLayer {
    val offset = drawerOffset()
    scaleX = if (offset > 0f) 1 / (1f + offset / drawerWidth) else 1f
    transformOrigin = TransformOrigin(if (isRtl) 0f else 1f, 0f)
}

private fun Modifier.predictiveBackDrawerContainer(
    drawerPredictiveBackState: DrawerPredictiveBackState,
    isRtl: Boolean,
) = graphicsLayer {
    scaleX = calculatePredictiveBackScaleX(drawerPredictiveBackState)
    scaleY = calculatePredictiveBackScaleY(drawerPredictiveBackState)
    transformOrigin = TransformOrigin(if (isRtl) 1f else 0f, 0.5f)
}

private fun Modifier.predictiveBackDrawerChild(
    drawerPredictiveBackState: DrawerPredictiveBackState,
    isRtl: Boolean,
) = graphicsLayer {
    // Preserve the original aspect ratio and container alignment of the child
    // content, and add content margins.
    val containerScaleX = calculatePredictiveBackScaleX(drawerPredictiveBackState)
    val containerScaleY = calculatePredictiveBackScaleY(drawerPredictiveBackState)
    scaleX = if (containerScaleX != 0f) containerScaleY / containerScaleX else 1f
    transformOrigin = TransformOrigin(if (isRtl) 0f else 1f, 0f)
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleX(
    drawerPredictiveBackState: DrawerPredictiveBackState
): Float {
    val width = size.width
    return if (width.isNaN() || width == 0f) {
        1f
    } else {
        val scaleXDirection = if (drawerPredictiveBackState.swipeEdgeMatchesDrawer) 1 else -1
        1f + drawerPredictiveBackState.scaleXDistance * scaleXDirection / width
    }
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleY(
    drawerPredictiveBackState: DrawerPredictiveBackState
): Float {
    val height = size.height
    return if (height.isNaN() || height == 0f) {
        1f
    } else {
        1f - drawerPredictiveBackState.scaleYDistance / height
    }
}

/**
 * Registers a [PredictiveBackHandler] and provides animation values in [DrawerPredictiveBackState]
 * based on back progress.
 *
 * @param drawerState state of the drawer
 * @param content content of the rest of the UI
 */
@Composable
internal fun DrawerPredictiveBackHandler(
    drawerState: DrawerState,
    content: @Composable (DrawerPredictiveBackState) -> Unit,
) {
    val drawerPredictiveBackState = remember { DrawerPredictiveBackState() }
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val maxScaleXDistanceGrow: Float
    val maxScaleXDistanceShrink: Float
    val maxScaleYDistance: Float
    with(LocalDensity.current) {
        maxScaleXDistanceGrow = PredictiveBackDrawerMaxScaleXDistanceGrow.toPx()
        maxScaleXDistanceShrink = PredictiveBackDrawerMaxScaleXDistanceShrink.toPx()
        maxScaleYDistance = PredictiveBackDrawerMaxScaleYDistance.toPx()
    }

    PredictiveBackHandler(enabled = drawerState.isOpen) { progress ->
        try {
            progress.collect { backEvent ->
                drawerPredictiveBackState.update(
                    PredictiveBack.transform(backEvent.progress),
                    backEvent.swipeEdge == BackEventCompat.EDGE_LEFT,
                    isRtl,
                    maxScaleXDistanceGrow,
                    maxScaleXDistanceShrink,
                    maxScaleYDistance,
                )
            }
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            drawerPredictiveBackState.clear()
        } finally {
            if (drawerPredictiveBackState.swipeEdgeMatchesDrawer) {
                // If swipe edge matches drawer gravity and we've stretched the drawer horizontally,
                // un-stretch it smoothly so that it hides completely during the drawer close.
                scope.launch {
                    animate(
                        initialValue = drawerPredictiveBackState.scaleXDistance,
                        targetValue = 0f,
                    ) { value, _ ->
                        drawerPredictiveBackState.scaleXDistance = value
                    }
                    drawerPredictiveBackState.clear()
                }
            }
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed) {
            drawerPredictiveBackState.clear()
        }
    }

    content(drawerPredictiveBackState)
}

/** Object to hold default values for [ModalNavigationDrawer] */
object DrawerDefaults {
    /** Default Elevation for drawer container in the [ModalNavigationDrawer]. */
    val ModalDrawerElevation = ElevationTokens.Level0

    /** Default Elevation for drawer container in the [PermanentNavigationDrawer]. */
    val PermanentDrawerElevation = NavigationDrawerTokens.StandardContainerElevation

    /** Default Elevation for drawer container in the [DismissibleNavigationDrawer]. */
    val DismissibleDrawerElevation = NavigationDrawerTokens.StandardContainerElevation

    /** Default shape for a navigation drawer. */
    val shape: Shape
        @Composable get() = NavigationDrawerTokens.ContainerShape.value

    /** Default color of the scrim that obscures content when the drawer is open */
    val scrimColor: Color
        @Composable get() = ScrimTokens.ContainerColor.value.copy(ScrimTokens.ContainerOpacity)

    /** Default container color for a navigation drawer */
    @Deprecated(
        message = "Please use standardContainerColor or modalContainerColor instead.",
        replaceWith = ReplaceWith("standardContainerColor"),
        level = DeprecationLevel.WARNING,
    )
    val containerColor: Color
        @Composable get() = NavigationDrawerTokens.StandardContainerColor.value

    /**
     * Default container color for a [DismissibleNavigationDrawer] and [PermanentNavigationDrawer]
     */
    val standardContainerColor: Color
        @Composable get() = NavigationDrawerTokens.StandardContainerColor.value

    /** Default container color for a [ModalNavigationDrawer] */
    val modalContainerColor: Color
        @Composable get() = NavigationDrawerTokens.ModalContainerColor.value

    /** Default and maximum width of a navigation drawer */
    val MaximumDrawerWidth = NavigationDrawerTokens.ContainerWidth

    /** Default window insets for drawer sheets */
    val windowInsets: WindowInsets
        @Composable
        get() =
            WindowInsets.systemBarsForVisualComponents.only(
                WindowInsetsSides.Vertical + WindowInsetsSides.Start
            )
}

/**
 * Material Design navigation drawer item.
 *
 * A [NavigationDrawerItem] represents a destination within drawers, either [ModalNavigationDrawer],
 * [PermanentNavigationDrawer] or [DismissibleNavigationDrawer].
 *
 * @sample androidx.compose.material3.samples.ModalNavigationDrawerSample
 * @param label text label for this item
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param modifier the [Modifier] to be applied to this item
 * @param icon optional icon for this item, typically an [Icon]
 * @param badge optional badge to show on this item from the end side
 * @param shape optional shape for the active indicator
 * @param colors [NavigationDrawerItemColors] that will be used to resolve the colors used for this
 *   item in different states. See [NavigationDrawerItemDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun NavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = NavigationDrawerTokens.ActiveIndicatorShape.value,
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier =
            modifier
                .semantics { role = Role.Tab }
                .heightIn(min = NavigationDrawerTokens.ActiveIndicatorHeight)
                .fillMaxWidth(),
        shape = shape,
        color = colors.containerColor(selected).value,
        interactionSource = interactionSource,
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                val iconColor = colors.iconColor(selected).value
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
                Spacer(Modifier.width(12.dp))
            }
            Box(Modifier.weight(1f)) {
                val labelColor = colors.textColor(selected).value
                CompositionLocalProvider(LocalContentColor provides labelColor, content = label)
            }
            if (badge != null) {
                Spacer(Modifier.width(12.dp))
                val badgeColor = colors.badgeColor(selected).value
                CompositionLocalProvider(LocalContentColor provides badgeColor, content = badge)
            }
        }
    }
}

/** Represents the colors of the various elements of a drawer item. */
@Stable
interface NavigationDrawerItemColors {
    /**
     * Represents the icon color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     */
    @Composable fun iconColor(selected: Boolean): State<Color>

    /**
     * Represents the text color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     */
    @Composable fun textColor(selected: Boolean): State<Color>

    /**
     * Represents the badge color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     */
    @Composable fun badgeColor(selected: Boolean): State<Color>

    /**
     * Represents the container color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     */
    @Composable fun containerColor(selected: Boolean): State<Color>
}

/** Defaults used in [NavigationDrawerItem]. */
object NavigationDrawerItemDefaults {
    /**
     * Creates a [NavigationDrawerItemColors] with the provided colors according to the Material
     * specification.
     *
     * @param selectedContainerColor the color to use for the background of the item when selected
     * @param unselectedContainerColor the color to use for the background of the item when
     *   unselected
     * @param selectedIconColor the color to use for the icon when the item is selected.
     * @param unselectedIconColor the color to use for the icon when the item is unselected.
     * @param selectedTextColor the color to use for the text label when the item is selected.
     * @param unselectedTextColor the color to use for the text label when the item is unselected.
     * @param selectedBadgeColor the color to use for the badge when the item is selected.
     * @param unselectedBadgeColor the color to use for the badge when the item is unselected.
     * @return the resulting [NavigationDrawerItemColors] used for [NavigationDrawerItem]
     */
    @Composable
    fun colors(
        selectedContainerColor: Color = NavigationDrawerTokens.ActiveIndicatorColor.value,
        unselectedContainerColor: Color = Color.Transparent,
        selectedIconColor: Color = NavigationDrawerTokens.ActiveIconColor.value,
        unselectedIconColor: Color = NavigationDrawerTokens.InactiveIconColor.value,
        selectedTextColor: Color = NavigationDrawerTokens.ActiveLabelTextColor.value,
        unselectedTextColor: Color = NavigationDrawerTokens.InactiveLabelTextColor.value,
        selectedBadgeColor: Color = selectedTextColor,
        unselectedBadgeColor: Color = unselectedTextColor,
    ): NavigationDrawerItemColors =
        DefaultDrawerItemsColor(
            selectedIconColor,
            unselectedIconColor,
            selectedTextColor,
            unselectedTextColor,
            selectedContainerColor,
            unselectedContainerColor,
            selectedBadgeColor,
            unselectedBadgeColor,
        )

    /**
     * Default external padding for a [NavigationDrawerItem] according to the Material
     * specification.
     */
    val ItemPadding = PaddingValues(horizontal = 12.dp)
}

@Stable
internal class DrawerPredictiveBackState {

    var swipeEdgeMatchesDrawer by mutableStateOf(true)

    var scaleXDistance by mutableFloatStateOf(0f)

    var scaleYDistance by mutableFloatStateOf(0f)

    fun update(
        progress: Float,
        swipeEdgeLeft: Boolean,
        isRtl: Boolean,
        maxScaleXDistanceGrow: Float,
        maxScaleXDistanceShrink: Float,
        maxScaleYDistance: Float,
    ) {
        swipeEdgeMatchesDrawer = swipeEdgeLeft != isRtl
        val maxScaleXDistance =
            if (swipeEdgeMatchesDrawer) maxScaleXDistanceGrow else maxScaleXDistanceShrink
        scaleXDistance = lerp(0f, maxScaleXDistance, progress)
        scaleYDistance = lerp(0f, maxScaleYDistance, progress)
    }

    fun clear() {
        swipeEdgeMatchesDrawer = true
        scaleXDistance = 0f
        scaleYDistance = 0f
    }
}

private class DefaultDrawerItemsColor(
    val selectedIconColor: Color,
    val unselectedIconColor: Color,
    val selectedTextColor: Color,
    val unselectedTextColor: Color,
    val selectedContainerColor: Color,
    val unselectedContainerColor: Color,
    val selectedBadgeColor: Color,
    val unselectedBadgeColor: Color,
) : NavigationDrawerItemColors {
    @Composable
    override fun iconColor(selected: Boolean): State<Color> {
        return rememberUpdatedState(if (selected) selectedIconColor else unselectedIconColor)
    }

    @Composable
    override fun textColor(selected: Boolean): State<Color> {
        return rememberUpdatedState(if (selected) selectedTextColor else unselectedTextColor)
    }

    @Composable
    override fun containerColor(selected: Boolean): State<Color> {
        return rememberUpdatedState(
            if (selected) selectedContainerColor else unselectedContainerColor
        )
    }

    @Composable
    override fun badgeColor(selected: Boolean): State<Color> {
        return rememberUpdatedState(if (selected) selectedBadgeColor else unselectedBadgeColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DefaultDrawerItemsColor) return false

        if (selectedIconColor != other.selectedIconColor) return false
        if (unselectedIconColor != other.unselectedIconColor) return false
        if (selectedTextColor != other.selectedTextColor) return false
        if (unselectedTextColor != other.unselectedTextColor) return false
        if (selectedContainerColor != other.selectedContainerColor) return false
        if (unselectedContainerColor != other.unselectedContainerColor) return false
        if (selectedBadgeColor != other.selectedBadgeColor) return false
        return unselectedBadgeColor == other.unselectedBadgeColor
    }

    override fun hashCode(): Int {
        var result = selectedIconColor.hashCode()
        result = 31 * result + unselectedIconColor.hashCode()
        result = 31 * result + selectedTextColor.hashCode()
        result = 31 * result + unselectedTextColor.hashCode()
        result = 31 * result + selectedContainerColor.hashCode()
        result = 31 * result + unselectedContainerColor.hashCode()
        result = 31 * result + selectedBadgeColor.hashCode()
        result = 31 * result + unselectedBadgeColor.hashCode()
        return result
    }
}

private fun calculateFraction(a: Float, b: Float, pos: Float) =
    ((pos - a) / (b - a)).coerceIn(0f, 1f)

private val DrawerPositionalThreshold = 0.5f
private val DrawerVelocityThreshold = 400.dp
private val MinimumDrawerWidth = 240.dp

internal val PredictiveBackDrawerMaxScaleXDistanceGrow = 12.dp
internal val PredictiveBackDrawerMaxScaleXDistanceShrink = 24.dp
internal val PredictiveBackDrawerMaxScaleYDistance = 48.dp

// TODO: b/177571613 this should be a proper decay settling
// this is taken from the DrawerLayout's DragViewHelper as a min duration.
private val AnchoredDraggableDefaultAnimationSpec = TweenSpec<Float>(durationMillis = 256)
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/TextField.kt
```kotlin
/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.MaterialTheme.LocalMaterialTheme
import androidx.compose.material3.TextFieldDefaults.defaultTextFieldColors
import androidx.compose.material3.internal.AboveLabelBottomPadding
import androidx.compose.material3.internal.AboveLabelHorizontalPadding
import androidx.compose.material3.internal.ContainerId
import androidx.compose.material3.internal.FloatProducer
import androidx.compose.material3.internal.LabelId
import androidx.compose.material3.internal.LeadingId
import androidx.compose.material3.internal.MinFocusedLabelLineHeight
import androidx.compose.material3.internal.MinSupportingTextLineHeight
import androidx.compose.material3.internal.MinTextLineHeight
import androidx.compose.material3.internal.PlaceholderId
import androidx.compose.material3.internal.PrefixId
import androidx.compose.material3.internal.PrefixSuffixTextPadding
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.SuffixId
import androidx.compose.material3.internal.SupportingId
import androidx.compose.material3.internal.TextFieldId
import androidx.compose.material3.internal.TrailingId
import androidx.compose.material3.internal.defaultErrorSemantics
import androidx.compose.material3.internal.expandedAlignment
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.heightOrZero
import androidx.compose.material3.internal.layoutId
import androidx.compose.material3.internal.minimizedAlignment
import androidx.compose.material3.internal.minimizedLabelHalfHeight
import androidx.compose.material3.internal.subtractConstraintSafely
import androidx.compose.material3.internal.textFieldHorizontalIconPadding
import androidx.compose.material3.internal.textFieldLabelMinHeight
import androidx.compose.material3.internal.widthOrZero
import androidx.compose.material3.tokens.FilledTextFieldTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.MotionTokens.EasingEmphasizedAccelerateCubicBezier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * [Material Design filled text field](https://m3.material.io/components/text-fields/overview)
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Filled text fields have more visual emphasis than outlined text fields, making them stand out
 * when surrounded by other content and components.
 *
 * ![Filled text field
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-text-field.png)
 *
 * If you are looking for an outlined version, see [OutlinedTextField]. For a text field
 * specifically designed for passwords or other secure content, see [SecureTextField].
 *
 * This overload of [TextField] uses [TextFieldState] to keep track of its text content and position
 * of the cursor or selection.
 *
 * A simple single line text field looks like:
 *
 * @sample androidx.compose.material3.samples.SimpleTextFieldSample
 *
 * You can control the initial text input and selection:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithInitialValueAndSelection
 *
 * Use input and output transformations to control user input and the displayed text:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithTransformations
 *
 * You may provide a placeholder:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithPlaceholder
 *
 * You can also provide leading and trailing icons:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithIcons
 *
 * You can also provide a prefix or suffix to the text:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithPrefixAndSuffix
 *
 * To handle the error input state, use [isError] parameter:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithErrorState
 *
 * Additionally, you may provide additional message at the bottom:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithSupportingText
 *
 * You can change the content padding to create a dense text field:
 *
 * @sample androidx.compose.material3.samples.DenseTextFieldContentPadding
 *
 * Hiding a software keyboard on IME action performed:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithHideKeyboardOnImeAction
 * @param state [TextFieldState] object that holds the internal editing state of the text field.
 * @param modifier the [Modifier] to be applied to this text field.
 * @param enabled controls the enabled state of this text field. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param readOnly controls the editable state of the text field. When `true`, the text field cannot
 *   be modified. However, a user can focus it and copy text from it. Read-only text fields are
 *   usually used to display pre-filled forms that a user cannot edit.
 * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
 * @param labelPosition the position of the label. See [TextFieldLabelPosition].
 * @param label the optional label to be displayed with this text field. The default text style uses
 *   [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
 * @param placeholder the optional placeholder to be displayed when the input text is empty. The
 *   default text style uses [Typography.bodyLarge].
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 *   container.
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 *   container.
 * @param prefix the optional prefix to be displayed before the input text in the text field.
 * @param suffix the optional suffix to be displayed after the input text in the text field.
 * @param supportingText the optional supporting text to be displayed below the text field.
 * @param isError indicates if the text field's current value is in error. When `true`, the
 *   components of the text field will be displayed in an error color, and an error will be
 *   announced to accessibility services.
 * @param inputTransformation optional [InputTransformation] that will be used to transform changes
 *   to the [TextFieldState] made by the user. The transformation will be applied to changes made by
 *   hardware and software keyboard events, pasting or dropping text, accessibility services, and
 *   tests. The transformation will _not_ be applied when changing the [state] programmatically, or
 *   when the transformation is changed. If the transformation is changed on an existing text field,
 *   it will be applied to the next user edit. The transformation will not immediately affect the
 *   current [state].
 * @param outputTransformation optional [OutputTransformation] that transforms how the contents of
 *   the text field are presented.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param onKeyboardAction called when the user presses the action button in the input method editor
 *   (IME), or by pressing the enter key on a hardware keyboard. By default this parameter is null,
 *   and would execute the default behavior for a received IME Action e.g., [ImeAction.Done] would
 *   close the keyboard, [ImeAction.Next] would switch the focus to the next focusable item on the
 *   screen.
 * @param lineLimits whether the text field should be [SingleLine], scroll horizontally, and ignore
 *   newlines; or [MultiLine] and grow and scroll vertically. If [SingleLine] is passed, all newline
 *   characters ('\n') within the text will be replaced with regular whitespace (' ').
 * @param onTextLayout Callback that is executed when the text layout becomes queryable. The
 *   callback receives a function that returns a [TextLayoutResult] if the layout can be calculated,
 *   or null if it cannot. The function reads the layout result from a snapshot state object, and
 *   will invalidate its caller when the layout result changes. A [TextLayoutResult] object contains
 *   paragraph information, size of the text, baselines and other details. [Density] scope is the
 *   one that was used while creating the given text layout.
 * @param scrollState scroll state that manages either horizontal or vertical scroll of the text
 *   field. If [lineLimits] is [SingleLine], this text field is treated as single line with
 *   horizontal scroll behavior. Otherwise, the text field becomes vertically scrollable.
 * @param shape defines the shape of this text field's container.
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 *   in different states. See [TextFieldDefaults.colors].
 * @param contentPadding the padding applied to the inner text field that separates it from the
 *   surrounding elements of the text field. Note that the padding values may not be respected if
 *   they are incompatible with the text field's size constraints or layout. See
 *   [TextFieldDefaults.contentPaddingWithLabel] and [TextFieldDefaults.contentPaddingWithoutLabel].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Attached(),
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    contentPadding: PaddingValues =
        if (label == null || labelPosition is TextFieldLabelPosition.Above) {
            TextFieldDefaults.contentPaddingWithoutLabel()
        } else {
            TextFieldDefaults.contentPaddingWithLabel()
        },
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            state = state,
            modifier =
                modifier
                    .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                        minHeight = TextFieldDefaults.MinHeight,
                    ),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError)),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            lineLimits = lineLimits,
            onTextLayout = onTextLayout,
            interactionSource = interactionSource,
            inputTransformation = inputTransformation,
            outputTransformation = outputTransformation,
            scrollState = scrollState,
            decorator =
                TextFieldDefaults.decorator(
                    state = state,
                    enabled = enabled,
                    lineLimits = lineLimits,
                    outputTransformation = outputTransformation,
                    interactionSource = interactionSource,
                    labelPosition = labelPosition,
                    label = label,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    prefix = prefix,
                    suffix = suffix,
                    supportingText = supportingText,
                    isError = isError,
                    colors = colors,
                    contentPadding = contentPadding,
                    container = {
                        TextFieldDefaults.Container(
                            enabled = enabled,
                            isError = isError,
                            interactionSource = interactionSource,
                            colors = colors,
                            shape = shape,
                        )
                    },
                ),
        )
    }
}

/**
 * [Material Design filled text field](https://m3.material.io/components/text-fields/overview)
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Filled text fields have more visual emphasis than outlined text fields, making them stand out
 * when surrounded by other content and components.
 *
 * ![Filled text field
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-text-field.png)
 *
 * If you are looking for an outlined version, see [OutlinedTextField].
 *
 * If apart from input text change you also want to observe the cursor location, selection range, or
 * IME composition use the TextField overload with the [TextFieldValue] parameter instead.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 *   updated text comes as a parameter of the callback
 * @param modifier the [Modifier] to be applied to this text field
 * @param enabled controls the enabled state of this text field. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param readOnly controls the editable state of the text field. When `true`, the text field cannot
 *   be modified. However, a user can focus it and copy text from it. Read-only text fields are
 *   usually used to display pre-filled forms that a user cannot edit.
 * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
 * @param label the optional label to be displayed with this text field. The default text style uses
 *   [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 *   the input text is empty. The default text style for internal [Text] is [Typography.bodyLarge]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 *   container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 *   container
 * @param prefix the optional prefix to be displayed before the input text in the text field
 * @param suffix the optional suffix to be displayed after the input text in the text field
 * @param supportingText the optional supporting text to be displayed below the text field
 * @param isError indicates if the text field's current value is in error. If set to true, the
 *   label, bottom indicator and trailing icon by default will be displayed in error color
 * @param visualTransformation transforms the visual representation of the input [value] For
 *   example, you can use
 *   [PasswordVisualTransformation][androidx.compose.ui.text.input.PasswordVisualTransformation] to
 *   create a password text field. By default, no visual transformation is applied.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction].
 * @param singleLine when `true`, this text field becomes a single horizontally scrolling text field
 *   instead of wrapping onto multiple lines. The keyboard will be informed to not show the return
 *   key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the maxLines
 *   attribute will be automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape defines the shape of this text field's container
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 *   in different states. See [TextFieldDefaults.colors].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier =
                modifier
                    .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                        minHeight = TextFieldDefaults.MinHeight,
                    ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError)),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox =
                @Composable { innerTextField ->
                    // places leading icon, text field with label and placeholder, trailing icon
                    TextFieldDefaults.DecorationBox(
                        value = value,
                        visualTransformation = visualTransformation,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = label,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        prefix = prefix,
                        suffix = suffix,
                        supportingText = supportingText,
                        shape = shape,
                        singleLine = singleLine,
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                    )
                },
        )
    }
}

/**
 * [Material Design filled text field](https://m3.material.io/components/text-fields/overview)
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Filled text fields have more visual emphasis than outlined text fields, making them stand out
 * when surrounded by other content and components.
 *
 * ![Filled text field
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-text-field.png)
 *
 * If you are looking for an outlined version, see [OutlinedTextField].
 *
 * This overload provides access to the input text, cursor position, selection range and IME
 * composition. If you only want to observe an input text change, use the TextField overload with
 * the [String] parameter instead.
 *
 * @param value the input [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates values in
 *   [TextFieldValue]. An updated [TextFieldValue] comes as a parameter of the callback
 * @param modifier the [Modifier] to be applied to this text field
 * @param enabled controls the enabled state of this text field. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param readOnly controls the editable state of the text field. When `true`, the text field cannot
 *   be modified. However, a user can focus it and copy text from it. Read-only text fields are
 *   usually used to display pre-filled forms that a user cannot edit.
 * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
 * @param label the optional label to be displayed with this text field. The default text style uses
 *   [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 *   the input text is empty. The default text style for internal [Text] is [Typography.bodyLarge]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 *   container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 *   container
 * @param prefix the optional prefix to be displayed before the input text in the text field
 * @param suffix the optional suffix to be displayed after the input text in the text field
 * @param supportingText the optional supporting text to be displayed below the text field
 * @param isError indicates if the text field's current value is in error state. If set to true, the
 *   label, bottom indicator and trailing icon by default will be displayed in error color
 * @param visualTransformation transforms the visual representation of the input [value]. For
 *   example, you can use
 *   [PasswordVisualTransformation][androidx.compose.ui.text.input.PasswordVisualTransformation] to
 *   create a password text field. By default, no visual transformation is applied.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction].
 * @param singleLine when `true`, this text field becomes a single horizontally scrolling text field
 *   instead of wrapping onto multiple lines. The keyboard will be informed to not show the return
 *   key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the maxLines
 *   attribute will be automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape defines the shape of this text field's container
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 *   in different states. See [TextFieldDefaults.colors].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier =
                modifier
                    .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                        minHeight = TextFieldDefaults.MinHeight,
                    ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError)),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox =
                @Composable { innerTextField ->
                    // places leading icon, text field with label and placeholder, trailing icon
                    TextFieldDefaults.DecorationBox(
                        value = value.text,
                        visualTransformation = visualTransformation,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = label,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        prefix = prefix,
                        suffix = suffix,
                        supportingText = supportingText,
                        shape = shape,
                        singleLine = singleLine,
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                    )
                },
        )
    }
}

/**
 * Composable responsible for measuring and laying out leading and trailing icons, label,
 * placeholder and the input field.
 */
@Composable
internal fun TextFieldLayout(
    modifier: Modifier,
    textField: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    placeholder: @Composable ((Modifier) -> Unit)?,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    prefix: @Composable (() -> Unit)?,
    suffix: @Composable (() -> Unit)?,
    singleLine: Boolean,
    labelPosition: TextFieldLabelPosition,
    labelProgress: FloatProducer,
    container: @Composable () -> Unit,
    supporting: @Composable (() -> Unit)?,
    paddingValues: PaddingValues,
) {
    val minimizedLabelHalfHeight = minimizedLabelHalfHeight()
    val measurePolicy =
        remember(
            singleLine,
            labelPosition,
            labelProgress,
            paddingValues,
            minimizedLabelHalfHeight,
        ) {
            TextFieldMeasurePolicy(
                singleLine = singleLine,
                labelPosition = labelPosition,
                labelProgress = labelProgress,
                paddingValues = paddingValues,
                minimizedLabelHalfHeight = minimizedLabelHalfHeight,
            )
        }
    val layoutDirection = LocalLayoutDirection.current
    Layout(
        modifier = modifier,
        content = {
            // The container is given as a Composable instead of a background modifier so that
            // elements like supporting text can be placed outside of it while still contributing
            // to the text field's measurements overall.
            container()

            if (leading != null) {
                Box(
                    modifier = Modifier.layoutId(LeadingId).minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    leading()
                }
            }
            if (trailing != null) {
                Box(
                    modifier = Modifier.layoutId(TrailingId).minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    trailing()
                }
            }

            val startTextFieldPadding = paddingValues.calculateStartPadding(layoutDirection)
            val endTextFieldPadding = paddingValues.calculateEndPadding(layoutDirection)

            val horizontalIconPadding = textFieldHorizontalIconPadding()
            val startPadding =
                if (leading != null) {
                    (startTextFieldPadding - horizontalIconPadding).coerceAtLeast(0.dp)
                } else {
                    startTextFieldPadding
                }
            val endPadding =
                if (trailing != null) {
                    (endTextFieldPadding - horizontalIconPadding).coerceAtLeast(0.dp)
                } else {
                    endTextFieldPadding
                }

            if (prefix != null) {
                Box(
                    Modifier.layoutId(PrefixId)
                        .heightIn(min = MinTextLineHeight)
                        .wrapContentHeight()
                        .padding(start = startPadding, end = PrefixSuffixTextPadding)
                ) {
                    prefix()
                }
            }
            if (suffix != null) {
                Box(
                    Modifier.layoutId(SuffixId)
                        .heightIn(min = MinTextLineHeight)
                        .wrapContentHeight()
                        .padding(start = PrefixSuffixTextPadding, end = endPadding)
                ) {
                    suffix()
                }
            }

            val labelPadding =
                if (labelPosition is TextFieldLabelPosition.Above) {
                    Modifier.padding(
                        start = AboveLabelHorizontalPadding,
                        end = AboveLabelHorizontalPadding,
                        bottom = AboveLabelBottomPadding,
                    )
                } else {
                    Modifier.padding(start = startPadding, end = endPadding)
                }
            if (label != null) {
                Box(
                    Modifier.layoutId(LabelId)
                        .textFieldLabelMinHeight {
                            lerp(MinTextLineHeight, MinFocusedLabelLineHeight, labelProgress())
                        }
                        .wrapContentHeight()
                        .then(labelPadding)
                ) {
                    label()
                }
            }

            val textPadding =
                Modifier.heightIn(min = MinTextLineHeight)
                    .wrapContentHeight()
                    .padding(
                        start = if (prefix == null) startPadding else 0.dp,
                        end = if (suffix == null) endPadding else 0.dp,
                    )

            if (placeholder != null) {
                placeholder(Modifier.layoutId(PlaceholderId).then(textPadding))
            }
            Box(
                modifier = Modifier.layoutId(TextFieldId).then(textPadding),
                propagateMinConstraints = true,
            ) {
                textField()
            }

            if (supporting != null) {
                @OptIn(ExperimentalMaterial3Api::class)
                Box(
                    Modifier.layoutId(SupportingId)
                        .heightIn(min = MinSupportingTextLineHeight)
                        .wrapContentHeight()
                        .padding(TextFieldDefaults.supportingTextPadding())
                ) {
                    supporting()
                }
            }
        },
        measurePolicy = measurePolicy,
    )
}

private class TextFieldMeasurePolicy(
    private val singleLine: Boolean,
    private val labelPosition: TextFieldLabelPosition,
    private val labelProgress: FloatProducer,
    private val paddingValues: PaddingValues,
    private val minimizedLabelHalfHeight: Dp,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val labelProgress = labelProgress()
        val topPaddingValue = paddingValues.calculateTopPadding().roundToPx()
        val bottomPaddingValue = paddingValues.calculateBottomPadding().roundToPx()

        var occupiedSpaceHorizontally = 0
        var occupiedSpaceVertically = 0

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // measure leading icon
        val leadingPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LeadingId }?.measure(looseConstraints)
        occupiedSpaceHorizontally += leadingPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, leadingPlaceable.heightOrZero)

        // measure trailing icon
        val trailingPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += trailingPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, trailingPlaceable.heightOrZero)

        // measure prefix
        val prefixPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += prefixPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, prefixPlaceable.heightOrZero)

        // measure suffix
        val suffixPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += suffixPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, suffixPlaceable.heightOrZero)

        val isLabelAbove = labelPosition is TextFieldLabelPosition.Above
        val labelMeasurable = measurables.fastFirstOrNull { it.layoutId == LabelId }
        var labelPlaceable: Placeable? = null
        val labelIntrinsicHeight: Int
        if (!isLabelAbove) {
            // if label is not Above, we can measure it like normal
            val labelConstraints =
                looseConstraints.offset(
                    vertical = -bottomPaddingValue,
                    horizontal = -occupiedSpaceHorizontally,
                )
            labelPlaceable = labelMeasurable?.measure(labelConstraints)
            labelIntrinsicHeight = 0
        } else {
            // if label is Above, it must be measured after other elements, but we
            // reserve space for it using its intrinsic height as a heuristic
            labelIntrinsicHeight = labelMeasurable?.minIntrinsicHeight(constraints.minWidth) ?: 0
        }

        // supporting text must be measured after other elements, but we
        // reserve space for it using its intrinsic height as a heuristic
        val supportingMeasurable = measurables.fastFirstOrNull { it.layoutId == SupportingId }
        val supportingIntrinsicHeight =
            supportingMeasurable?.minIntrinsicHeight(constraints.minWidth) ?: 0

        // at most one of these is non-zero
        val labelHeightOrIntrinsic = labelPlaceable.heightOrZero + labelIntrinsicHeight

        // measure input field
        val effectiveTopOffset = topPaddingValue + labelHeightOrIntrinsic
        val textFieldConstraints =
            constraints
                .copy(minHeight = 0)
                .offset(
                    vertical = -effectiveTopOffset - bottomPaddingValue - supportingIntrinsicHeight,
                    horizontal = -occupiedSpaceHorizontally,
                )
        val textFieldPlaceable =
            measurables.fastFirst { it.layoutId == TextFieldId }.measure(textFieldConstraints)

        // measure placeholder
        val placeholderConstraints = textFieldConstraints.copy(minWidth = 0)
        val placeholderPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.measure(placeholderConstraints)

        occupiedSpaceVertically =
            max(
                occupiedSpaceVertically,
                max(textFieldPlaceable.heightOrZero, placeholderPlaceable.heightOrZero) +
                    effectiveTopOffset +
                    bottomPaddingValue,
            )
        val width =
            calculateWidth(
                leadingWidth = leadingPlaceable.widthOrZero,
                trailingWidth = trailingPlaceable.widthOrZero,
                prefixWidth = prefixPlaceable.widthOrZero,
                suffixWidth = suffixPlaceable.widthOrZero,
                textFieldWidth = textFieldPlaceable.width,
                labelWidth = labelPlaceable.widthOrZero,
                placeholderWidth = placeholderPlaceable.widthOrZero,
                constraints = constraints,
            )

        if (isLabelAbove) {
            // now that we know the width, measure label
            val labelConstraints =
                looseConstraints.copy(maxHeight = labelIntrinsicHeight, maxWidth = width)
            labelPlaceable = labelMeasurable?.measure(labelConstraints)
        }

        // measure supporting text
        val supportingConstraints =
            looseConstraints
                .offset(vertical = -occupiedSpaceVertically)
                .copy(minHeight = 0, maxWidth = width)
        val supportingPlaceable = supportingMeasurable?.measure(supportingConstraints)
        val supportingHeight = supportingPlaceable.heightOrZero

        val totalHeight =
            calculateHeight(
                textFieldHeight = textFieldPlaceable.height,
                labelHeight = labelPlaceable.heightOrZero,
                leadingHeight = leadingPlaceable.heightOrZero,
                trailingHeight = trailingPlaceable.heightOrZero,
                prefixHeight = prefixPlaceable.heightOrZero,
                suffixHeight = suffixPlaceable.heightOrZero,
                placeholderHeight = placeholderPlaceable.heightOrZero,
                supportingHeight = supportingPlaceable.heightOrZero,
                constraints = constraints,
                isLabelAbove = isLabelAbove,
                labelProgress = labelProgress,
            )
        val height =
            totalHeight - supportingHeight - (if (isLabelAbove) labelPlaceable.heightOrZero else 0)

        val containerPlaceable =
            measurables
                .fastFirst { it.layoutId == ContainerId }
                .measure(
                    Constraints(
                        minWidth = if (width != Constraints.Infinity) width else 0,
                        maxWidth = width,
                        minHeight = if (height != Constraints.Infinity) height else 0,
                        maxHeight = height,
                    )
                )

        return layout(width, totalHeight) {
            if (labelPlaceable != null) {
                val labelStartY =
                    when {
                        isLabelAbove -> 0
                        singleLine ->
                            Alignment.CenterVertically.align(labelPlaceable.height, height)
                        else ->
                            // The padding defined by the user only applies to the text field when
                            // the label is focused. More padding needs to be added when the text
                            // field is unfocused.
                            topPaddingValue + minimizedLabelHalfHeight.roundToPx()
                    }
                val labelEndY =
                    when {
                        isLabelAbove -> 0
                        else -> topPaddingValue
                    }
                placeWithLabel(
                    width = width,
                    totalHeight = totalHeight,
                    textfieldPlaceable = textFieldPlaceable,
                    labelPlaceable = labelPlaceable,
                    placeholderPlaceable = placeholderPlaceable,
                    leadingPlaceable = leadingPlaceable,
                    trailingPlaceable = trailingPlaceable,
                    prefixPlaceable = prefixPlaceable,
                    suffixPlaceable = suffixPlaceable,
                    containerPlaceable = containerPlaceable,
                    supportingPlaceable = supportingPlaceable,
                    labelStartY = labelStartY,
                    labelEndY = labelEndY,
                    isLabelAbove = isLabelAbove,
                    labelProgress = labelProgress,
                    textPosition =
                        topPaddingValue + (if (isLabelAbove) 0 else labelPlaceable.height),
                    layoutDirection = layoutDirection,
                )
            } else {
                placeWithoutLabel(
                    width = width,
                    totalHeight = totalHeight,
                    textPlaceable = textFieldPlaceable,
                    placeholderPlaceable = placeholderPlaceable,
                    leadingPlaceable = leadingPlaceable,
                    trailingPlaceable = trailingPlaceable,
                    prefixPlaceable = prefixPlaceable,
                    suffixPlaceable = suffixPlaceable,
                    containerPlaceable = containerPlaceable,
                    supportingPlaceable = supportingPlaceable,
                    density = density,
                )
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.maxIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.minIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.maxIntrinsicWidth(h)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.minIntrinsicWidth(h)
        }
    }

    private fun intrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int,
    ): Int {
        val textFieldWidth =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, height)
        val labelWidth =
            measurables
                .fastFirstOrNull { it.layoutId == LabelId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val trailingWidth =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val prefixWidth =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val suffixWidth =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val leadingWidth =
            measurables
                .fastFirstOrNull { it.layoutId == LeadingId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val placeholderWidth =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        return calculateWidth(
            leadingWidth = leadingWidth,
            trailingWidth = trailingWidth,
            prefixWidth = prefixWidth,
            suffixWidth = suffixWidth,
            textFieldWidth = textFieldWidth,
            labelWidth = labelWidth,
            placeholderWidth = placeholderWidth,
            constraints = Constraints(),
        )
    }

    private fun IntrinsicMeasureScope.intrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int,
    ): Int {
        var remainingWidth = width
        val leadingHeight =
            measurables
                .fastFirstOrNull { it.layoutId == LeadingId }
                ?.let {
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    intrinsicMeasurer(it, width)
                } ?: 0
        val trailingHeight =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.let {
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    intrinsicMeasurer(it, width)
                } ?: 0
        val labelHeight =
            measurables
                .fastFirstOrNull { it.layoutId == LabelId }
                ?.let { intrinsicMeasurer(it, remainingWidth) } ?: 0

        val prefixHeight =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.let {
                    val height = intrinsicMeasurer(it, remainingWidth)
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    height
                } ?: 0
        val suffixHeight =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.let {
                    val height = intrinsicMeasurer(it, remainingWidth)
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    height
                } ?: 0

        val textFieldHeight =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, remainingWidth)
        val placeholderHeight =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.let { intrinsicMeasurer(it, remainingWidth) } ?: 0

        val supportingHeight =
            measurables
                .fastFirstOrNull { it.layoutId == SupportingId }
                ?.let { intrinsicMeasurer(it, width) } ?: 0

        return calculateHeight(
            textFieldHeight = textFieldHeight,
            labelHeight = labelHeight,
            leadingHeight = leadingHeight,
            trailingHeight = trailingHeight,
            prefixHeight = prefixHeight,
            suffixHeight = suffixHeight,
            placeholderHeight = placeholderHeight,
            supportingHeight = supportingHeight,
            constraints = Constraints(),
            isLabelAbove = labelPosition is TextFieldLabelPosition.Above,
            labelProgress = labelProgress(),
        )
    }

    private fun calculateWidth(
        leadingWidth: Int,
        trailingWidth: Int,
        prefixWidth: Int,
        suffixWidth: Int,
        textFieldWidth: Int,
        labelWidth: Int,
        placeholderWidth: Int,
        constraints: Constraints,
    ): Int {
        val affixTotalWidth = prefixWidth + suffixWidth
        val middleSection =
            maxOf(
                textFieldWidth + affixTotalWidth,
                placeholderWidth + affixTotalWidth,
                // Prefix/suffix does not get applied to label
                labelWidth,
            )
        val wrappedWidth = leadingWidth + middleSection + trailingWidth
        return constraints.constrainWidth(wrappedWidth)
    }

    private fun Density.calculateHeight(
        textFieldHeight: Int,
        labelHeight: Int,
        leadingHeight: Int,
        trailingHeight: Int,
        prefixHeight: Int,
        suffixHeight: Int,
        placeholderHeight: Int,
        supportingHeight: Int,
        constraints: Constraints,
        isLabelAbove: Boolean,
        labelProgress: Float,
    ): Int {
        val verticalPadding =
            (paddingValues.calculateTopPadding() + paddingValues.calculateBottomPadding())
                .roundToPx()

        val inputFieldHeight =
            maxOf(
                textFieldHeight,
                placeholderHeight,
                prefixHeight,
                suffixHeight,
                if (isLabelAbove) 0 else lerp(labelHeight, 0, labelProgress),
            )

        val hasLabel = labelHeight > 0
        val nonOverlappedLabelHeight =
            if (hasLabel && !isLabelAbove) {
                // The label animates from overlapping the input field to floating above it,
                // so its contribution to the height calculation changes over time. A baseline
                // height is provided in the unfocused state to keep the overall height consistent
                // across the animation.
                max(
                    (minimizedLabelHalfHeight * 2).roundToPx(),
                    lerp(
                        0,
                        labelHeight,
                        EasingEmphasizedAccelerateCubicBezier.transform(labelProgress),
                    ),
                )
            } else {
                0
            }

        val middleSectionHeight = verticalPadding + nonOverlappedLabelHeight + inputFieldHeight

        return constraints.constrainHeight(
            (if (isLabelAbove) labelHeight else 0) +
                maxOf(leadingHeight, trailingHeight, middleSectionHeight) +
                supportingHeight
        )
    }

    /**
     * Places the provided text field, placeholder, and label in the TextField given the
     * PaddingValues when there is a label. When there is no label, [placeWithoutLabel] is used
     * instead.
     */
    private fun Placeable.PlacementScope.placeWithLabel(
        width: Int,
        totalHeight: Int,
        textfieldPlaceable: Placeable,
        labelPlaceable: Placeable,
        placeholderPlaceable: Placeable?,
        leadingPlaceable: Placeable?,
        trailingPlaceable: Placeable?,
        prefixPlaceable: Placeable?,
        suffixPlaceable: Placeable?,
        containerPlaceable: Placeable,
        supportingPlaceable: Placeable?,
        labelStartY: Int,
        labelEndY: Int,
        isLabelAbove: Boolean,
        labelProgress: Float,
        textPosition: Int,
        layoutDirection: LayoutDirection,
    ) {
        val yOffset = if (isLabelAbove) labelPlaceable.height else 0

        // place container
        containerPlaceable.place(0, yOffset)

        // Most elements should be positioned w.r.t the text field's "visual" height, i.e.,
        // excluding the label (if it's Above) and the supporting text on bottom
        val height =
            totalHeight -
                supportingPlaceable.heightOrZero -
                (if (isLabelAbove) labelPlaceable.height else 0)

        leadingPlaceable?.placeRelative(
            0,
            yOffset + Alignment.CenterVertically.align(leadingPlaceable.height, height),
        )

        val labelY = lerp(labelStartY, labelEndY, labelProgress)
        if (isLabelAbove) {
            val labelX =
                labelPosition.minimizedAlignment.align(
                    size = labelPlaceable.width,
                    space = width,
                    layoutDirection = layoutDirection,
                )
            // Not placeRelative because alignment already handles RTL
            labelPlaceable.place(labelX, labelY)
        } else {
            val leftIconWidth =
                if (layoutDirection == LayoutDirection.Ltr) leadingPlaceable.widthOrZero
                else trailingPlaceable.widthOrZero
            val labelStartX =
                labelPosition.expandedAlignment.align(
                    size = labelPlaceable.width,
                    space = width - leadingPlaceable.widthOrZero - trailingPlaceable.widthOrZero,
                    layoutDirection = layoutDirection,
                ) + leftIconWidth
            val labelEndX =
                labelPosition.minimizedAlignment.align(
                    size = labelPlaceable.width,
                    space = width - leadingPlaceable.widthOrZero - trailingPlaceable.widthOrZero,
                    layoutDirection = layoutDirection,
                ) + leftIconWidth
            val labelX = lerp(labelStartX, labelEndX, labelProgress)
            // Not placeRelative because alignment already handles RTL
            labelPlaceable.place(labelX, labelY)
        }

        prefixPlaceable?.placeRelative(leadingPlaceable.widthOrZero, yOffset + textPosition)

        val textHorizontalPosition = leadingPlaceable.widthOrZero + prefixPlaceable.widthOrZero
        textfieldPlaceable.placeRelative(textHorizontalPosition, yOffset + textPosition)
        placeholderPlaceable?.placeRelative(textHorizontalPosition, yOffset + textPosition)

        suffixPlaceable?.placeRelative(
            width - trailingPlaceable.widthOrZero - suffixPlaceable.width,
            yOffset + textPosition,
        )

        trailingPlaceable?.placeRelative(
            width - trailingPlaceable.width,
            yOffset + Alignment.CenterVertically.align(trailingPlaceable.height, height),
        )

        supportingPlaceable?.placeRelative(0, yOffset + height)
    }

    /**
     * Places the provided text field and placeholder in [TextField] when there is no label. When
     * there is a label, [placeWithLabel] is used
     */
    private fun Placeable.PlacementScope.placeWithoutLabel(
        width: Int,
        totalHeight: Int,
        textPlaceable: Placeable,
        placeholderPlaceable: Placeable?,
        leadingPlaceable: Placeable?,
        trailingPlaceable: Placeable?,
        prefixPlaceable: Placeable?,
        suffixPlaceable: Placeable?,
        containerPlaceable: Placeable,
        supportingPlaceable: Placeable?,
        density: Float,
    ) {
        // place container
        containerPlaceable.place(IntOffset.Zero)

        // Most elements should be positioned w.r.t the text field's "visual" height, i.e.,
        // excluding the supporting text on bottom
        val height = totalHeight - supportingPlaceable.heightOrZero
        val topPadding = (paddingValues.calculateTopPadding().value * density).roundToInt()

        leadingPlaceable?.placeRelative(
            0,
            Alignment.CenterVertically.align(leadingPlaceable.height, height),
        )

        // Single line text field without label places its text components centered vertically.
        // Multiline text field without label places its text components at the top with padding.
        fun calculateVerticalPosition(placeable: Placeable): Int {
            return if (singleLine) {
                Alignment.CenterVertically.align(placeable.height, height)
            } else {
                topPadding
            }
        }

        prefixPlaceable?.placeRelative(
            leadingPlaceable.widthOrZero,
            calculateVerticalPosition(prefixPlaceable),
        )

        val textHorizontalPosition = leadingPlaceable.widthOrZero + prefixPlaceable.widthOrZero

        textPlaceable.placeRelative(
            textHorizontalPosition,
            calculateVerticalPosition(textPlaceable),
        )

        placeholderPlaceable?.placeRelative(
            textHorizontalPosition,
            calculateVerticalPosition(placeholderPlaceable),
        )

        suffixPlaceable?.placeRelative(
            width - trailingPlaceable.widthOrZero - suffixPlaceable.width,
            calculateVerticalPosition(suffixPlaceable),
        )

        trailingPlaceable?.placeRelative(
            width - trailingPlaceable.width,
            Alignment.CenterVertically.align(trailingPlaceable.height, height),
        )

        supportingPlaceable?.placeRelative(0, height)
    }
}

internal data class IndicatorLineElement(
    val enabled: Boolean,
    val isError: Boolean,
    val interactionSource: InteractionSource,
    val colors: TextFieldColors?,
    val textFieldShape: Shape?,
    val focusedIndicatorLineThickness: Dp,
    val unfocusedIndicatorLineThickness: Dp,
) : ModifierNodeElement<IndicatorLineNode>() {
    override fun create(): IndicatorLineNode {
        return IndicatorLineNode(
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            textFieldShape = textFieldShape,
            focusedIndicatorWidth = focusedIndicatorLineThickness,
            unfocusedIndicatorWidth = unfocusedIndicatorLineThickness,
        )
    }

    override fun update(node: IndicatorLineNode) {
        node.update(
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            textFieldShape = textFieldShape,
            focusedIndicatorWidth = focusedIndicatorLineThickness,
            unfocusedIndicatorWidth = unfocusedIndicatorLineThickness,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "indicatorLine"
        properties["enabled"] = enabled
        properties["isError"] = isError
        properties["interactionSource"] = interactionSource
        properties["colors"] = colors
        properties["textFieldShape"] = textFieldShape
        properties["focusedIndicatorLineThickness"] = focusedIndicatorLineThickness
        properties["unfocusedIndicatorLineThickness"] = unfocusedIndicatorLineThickness
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class IndicatorLineNode(
    private var enabled: Boolean,
    private var isError: Boolean,
    private var interactionSource: InteractionSource,
    colors: TextFieldColors?,
    textFieldShape: Shape?,
    private var focusedIndicatorWidth: Dp,
    private var unfocusedIndicatorWidth: Dp,
) : DelegatingNode(), CompositionLocalConsumerModifierNode {
    private var focused = false
    private var trackFocusStateJob: Job? = null

    private var _colors: TextFieldColors? = colors
    private val colors: TextFieldColors
        get() =
            _colors
                ?: currentValueOf(LocalMaterialTheme)
                    .colorScheme
                    .defaultTextFieldColors(currentValueOf(LocalTextSelectionColors))

    // Must be initialized in `onAttach` so `colors` can read from the `MaterialTheme`
    private var colorAnimatable: Animatable<Color, AnimationVector4D>? = null

    private var _shape: Shape? = textFieldShape
        private set(value) {
            if (field != value) {
                field = value
                drawWithCacheModifierNode.invalidateDrawCache()
            }
        }

    private val shape: Shape
        get() =
            _shape
                ?: currentValueOf(LocalMaterialTheme)
                    .shapes
                    .fromToken(FilledTextFieldTokens.ContainerShape)

    private val widthAnimatable: Animatable<Dp, AnimationVector1D> =
        Animatable(
            initialValue =
                if (focused && this.enabled) this.focusedIndicatorWidth
                else this.unfocusedIndicatorWidth,
            typeConverter = Dp.VectorConverter,
        )

    fun update(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors?,
        textFieldShape: Shape?,
        focusedIndicatorWidth: Dp,
        unfocusedIndicatorWidth: Dp,
    ) {
        var shouldInvalidate = false

        if (this.enabled != enabled) {
            this.enabled = enabled
            shouldInvalidate = true
        }

        if (this.isError != isError) {
            this.isError = isError
            shouldInvalidate = true
        }

        if (this.interactionSource !== interactionSource) {
            this.interactionSource = interactionSource
            trackFocusStateJob?.cancel()
            trackFocusStateJob = coroutineScope.launch { trackFocusState() }
        }

        if (this._colors != colors) {
            this._colors = colors
            shouldInvalidate = true
        }

        if (this._shape != textFieldShape) {
            this._shape = textFieldShape
            shouldInvalidate = true
        }

        if (this.focusedIndicatorWidth != focusedIndicatorWidth) {
            this.focusedIndicatorWidth = focusedIndicatorWidth
            shouldInvalidate = true
        }

        if (this.unfocusedIndicatorWidth != unfocusedIndicatorWidth) {
            this.unfocusedIndicatorWidth = unfocusedIndicatorWidth
            shouldInvalidate = true
        }

        if (shouldInvalidate) {
            invalidateIndicator()
        }
    }

    override val shouldAutoInvalidate: Boolean
        get() = false

    override fun onAttach() {
        trackFocusStateJob = coroutineScope.launch { trackFocusState() }
        if (colorAnimatable == null) {
            val initialColor = colors.indicatorColor(enabled, isError, focused)
            colorAnimatable =
                Animatable(
                    initialValue = initialColor,
                    typeConverter = Color.VectorConverter(initialColor.colorSpace),
                )
        }
    }

    /** Copied from [InteractionSource.collectIsFocusedAsState] */
    private suspend fun trackFocusState() {
        focused = false
        val focusInteractions = mutableListOf<FocusInteraction.Focus>()
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is FocusInteraction.Focus -> focusInteractions.add(interaction)
                is FocusInteraction.Unfocus -> focusInteractions.remove(interaction.focus)
            }
            val isFocused = focusInteractions.isNotEmpty()
            if (isFocused != focused) {
                focused = isFocused
                invalidateIndicator()
            }
        }
    }

    private fun invalidateIndicator() {
        coroutineScope.launch {
            colorAnimatable?.animateTo(
                targetValue = colors.indicatorColor(enabled, isError, focused),
                animationSpec =
                    if (enabled) {
                        currentValueOf(LocalMaterialTheme)
                            .motionScheme
                            .fromToken<Color>(MotionSchemeKeyTokens.FastEffects)
                    } else {
                        snap()
                    },
            )
        }
        coroutineScope.launch {
            widthAnimatable.animateTo(
                targetValue =
                    if (focused && enabled) focusedIndicatorWidth else unfocusedIndicatorWidth,
                animationSpec =
                    if (enabled) {
                        currentValueOf(LocalMaterialTheme)
                            .motionScheme
                            .fromToken<Dp>(MotionSchemeKeyTokens.FastSpatial)
                    } else {
                        snap()
                    },
            )
        }
    }

    private val drawWithCacheModifierNode =
        delegate(
            CacheDrawModifierNode {
                val strokeWidth = widthAnimatable.value.toPx()
                val textFieldShapePath =
                    Path().apply {
                        addOutline(
                            this@IndicatorLineNode.shape.createOutline(
                                size,
                                layoutDirection,
                                density = this@CacheDrawModifierNode,
                            )
                        )
                    }
                val linePath =
                    Path().apply {
                        addRect(
                            Rect(
                                left = 0f,
                                top = size.height - strokeWidth,
                                right = size.width,
                                bottom = size.height,
                            )
                        )
                    }
                val clippedLine = linePath and textFieldShapePath

                onDrawWithContent {
                    drawContent()
                    drawPath(path = clippedLine, brush = SolidColor(colorAnimatable!!.value))
                }
            }
        )
}

/** Padding from text field top to label top, and from input field bottom to text field bottom */
/*@VisibleForTesting*/
internal val TextFieldWithLabelVerticalPadding = 8.dp
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/OutlinedTextField.kt
```kotlin
/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.internal.AboveLabelBottomPadding
import androidx.compose.material3.internal.AboveLabelHorizontalPadding
import androidx.compose.material3.internal.ContainerId
import androidx.compose.material3.internal.FloatProducer
import androidx.compose.material3.internal.LabelId
import androidx.compose.material3.internal.LeadingId
import androidx.compose.material3.internal.MinFocusedLabelLineHeight
import androidx.compose.material3.internal.MinSupportingTextLineHeight
import androidx.compose.material3.internal.MinTextLineHeight
import androidx.compose.material3.internal.PlaceholderId
import androidx.compose.material3.internal.PrefixId
import androidx.compose.material3.internal.PrefixSuffixTextPadding
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.SuffixId
import androidx.compose.material3.internal.SupportingId
import androidx.compose.material3.internal.TextFieldId
import androidx.compose.material3.internal.TrailingId
import androidx.compose.material3.internal.defaultErrorSemantics
import androidx.compose.material3.internal.expandedAlignment
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.heightOrZero
import androidx.compose.material3.internal.layoutId
import androidx.compose.material3.internal.minimizedAlignment
import androidx.compose.material3.internal.minimizedLabelHalfHeight
import androidx.compose.material3.internal.subtractConstraintSafely
import androidx.compose.material3.internal.textFieldHorizontalIconPadding
import androidx.compose.material3.internal.textFieldLabelMinHeight
import androidx.compose.material3.internal.widthOrZero
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * [Material Design outlined text field](https://m3.material.io/components/text-fields/overview)
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Outlined text fields have less visual emphasis than filled text fields. When they appear in
 * places like forms, where many text fields are placed together, their reduced emphasis helps
 * simplify the layout.
 *
 * ![Outlined text field
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-text-field.png)
 *
 * If you are looking for a filled version, see [TextField]. For a text field specifically designed
 * for passwords or other secure content, see [OutlinedSecureTextField].
 *
 * This overload of [OutlinedTextField] uses [TextFieldState] to keep track of its text content and
 * position of the cursor or selection.
 *
 * See example usage:
 *
 * @sample androidx.compose.material3.samples.SimpleOutlinedTextFieldSample
 * @sample androidx.compose.material3.samples.OutlinedTextFieldWithInitialValueAndSelection
 * @param state [TextFieldState] object that holds the internal editing state of the text field.
 * @param modifier the [Modifier] to be applied to this text field.
 * @param enabled controls the enabled state of this text field. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param readOnly controls the editable state of the text field. When `true`, the text field cannot
 *   be modified. However, a user can focus it and copy text from it. Read-only text fields are
 *   usually used to display pre-filled forms that a user cannot edit.
 * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
 * @param labelPosition the position of the label. See [TextFieldLabelPosition].
 * @param label the optional label to be displayed with this text field. The default text style uses
 *   [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
 * @param placeholder the optional placeholder to be displayed when the input text is empty. The
 *   default text style uses [Typography.bodyLarge].
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 *   container.
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 *   container.
 * @param prefix the optional prefix to be displayed before the input text in the text field.
 * @param suffix the optional suffix to be displayed after the input text in the text field.
 * @param supportingText the optional supporting text to be displayed below the text field.
 * @param isError indicates if the text field's current value is in error. When `true`, the
 *   components of the text field will be displayed in an error color, and an error will be
 *   announced to accessibility services.
 * @param inputTransformation optional [InputTransformation] that will be used to transform changes
 *   to the [TextFieldState] made by the user. The transformation will be applied to changes made by
 *   hardware and software keyboard events, pasting or dropping text, accessibility services, and
 *   tests. The transformation will _not_ be applied when changing the [state] programmatically, or
 *   when the transformation is changed. If the transformation is changed on an existing text field,
 *   it will be applied to the next user edit. The transformation will not immediately affect the
 *   current [state].
 * @param outputTransformation optional [OutputTransformation] that transforms how the contents of
 *   the text field are presented.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param onKeyboardAction called when the user presses the action button in the input method editor
 *   (IME), or by pressing the enter key on a hardware keyboard. By default this parameter is null,
 *   and would execute the default behavior for a received IME Action e.g., [ImeAction.Done] would
 *   close the keyboard, [ImeAction.Next] would switch the focus to the next focusable item on the
 *   screen.
 * @param lineLimits whether the text field should be [SingleLine], scroll horizontally, and ignore
 *   newlines; or [MultiLine] and grow and scroll vertically. If [SingleLine] is passed, all newline
 *   characters ('\n') within the text will be replaced with regular whitespace (' ').
 * @param onTextLayout Callback that is executed when the text layout becomes queryable. The
 *   callback receives a function that returns a [TextLayoutResult] if the layout can be calculated,
 *   or null if it cannot. The function reads the layout result from a snapshot state object, and
 *   will invalidate its caller when the layout result changes. A [TextLayoutResult] object contains
 *   paragraph information, size of the text, baselines and other details. [Density] scope is the
 *   one that was used while creating the given text layout.
 * @param scrollState scroll state that manages either horizontal or vertical scroll of the text
 *   field. If [lineLimits] is [SingleLine], this text field is treated as single line with
 *   horizontal scroll behavior. Otherwise, the text field becomes vertically scrollable.
 * @param shape defines the shape of this text field's border.
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 *   in different states. See [OutlinedTextFieldDefaults.colors].
 * @param contentPadding the padding applied to the inner text field that separates it from the
 *   surrounding elements of the text field. Note that the padding values may not be respected if
 *   they are incompatible with the text field's size constraints or layout. See
 *   [OutlinedTextFieldDefaults.contentPadding].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Attached(),
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    contentPadding: PaddingValues = OutlinedTextFieldDefaults.contentPadding(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            state = state,
            modifier =
                modifier
                    .then(
                        if (label != null && labelPosition !is TextFieldLabelPosition.Above) {
                            Modifier
                                // Merge semantics at the beginning of the modifier chain to ensure
                                // padding is considered part of the text field.
                                .semantics(mergeDescendants = true) {}
                                .padding(top = minimizedLabelHalfHeight())
                        } else {
                            Modifier
                        }
                    )
                    .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                    .defaultMinSize(
                        minWidth = OutlinedTextFieldDefaults.MinWidth,
                        minHeight = OutlinedTextFieldDefaults.MinHeight,
                    ),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError)),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            lineLimits = lineLimits,
            onTextLayout = onTextLayout,
            interactionSource = interactionSource,
            inputTransformation = inputTransformation,
            outputTransformation = outputTransformation,
            scrollState = scrollState,
            decorator =
                OutlinedTextFieldDefaults.decorator(
                    state = state,
                    enabled = enabled,
                    lineLimits = lineLimits,
                    outputTransformation = outputTransformation,
                    interactionSource = interactionSource,
                    labelPosition = labelPosition,
                    label = label,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    prefix = prefix,
                    suffix = suffix,
                    supportingText = supportingText,
                    isError = isError,
                    colors = colors,
                    contentPadding = contentPadding,
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = enabled,
                            isError = isError,
                            interactionSource = interactionSource,
                            colors = colors,
                            shape = shape,
                        )
                    },
                ),
        )
    }
}

/**
 * [Material Design outlined text field](https://m3.material.io/components/text-fields/overview)
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Outlined text fields have less visual emphasis than filled text fields. When they appear in
 * places like forms, where many text fields are placed together, their reduced emphasis helps
 * simplify the layout.
 *
 * ![Outlined text field
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-text-field.png)
 *
 * If apart from input text change you also want to observe the cursor location, selection range, or
 * IME composition use the OutlinedTextField overload with the [TextFieldValue] parameter instead.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 *   updated text comes as a parameter of the callback
 * @param modifier the [Modifier] to be applied to this text field
 * @param enabled controls the enabled state of this text field. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param readOnly controls the editable state of the text field. When `true`, the text field cannot
 *   be modified. However, a user can focus it and copy text from it. Read-only text fields are
 *   usually used to display pre-filled forms that a user cannot edit.
 * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
 * @param label the optional label to be displayed with this text field. The default text style uses
 *   [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 *   the input text is empty. The default text style for internal [Text] is [Typography.bodyLarge]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 *   container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 *   container
 * @param prefix the optional prefix to be displayed before the input text in the text field
 * @param suffix the optional suffix to be displayed after the input text in the text field
 * @param supportingText the optional supporting text to be displayed below the text field
 * @param isError indicates if the text field's current value is in error. If set to true, the
 *   label, bottom indicator and trailing icon by default will be displayed in error color
 * @param visualTransformation transforms the visual representation of the input [value] For
 *   example, you can use
 *   [PasswordVisualTransformation][androidx.compose.ui.text.input.PasswordVisualTransformation] to
 *   create a password text field. By default, no visual transformation is applied.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction]
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction]
 * @param singleLine when `true`, this text field becomes a single horizontally scrolling text field
 *   instead of wrapping onto multiple lines. The keyboard will be informed to not show the return
 *   key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the maxLines
 *   attribute will be automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape defines the shape of this text field's border
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 *   in different states. See [OutlinedTextFieldDefaults.colors].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier =
                modifier
                    .then(
                        if (label != null) {
                            Modifier
                                // Merge semantics at the beginning of the modifier chain to ensure
                                // padding is considered part of the text field.
                                .semantics(mergeDescendants = true) {}
                                .padding(top = minimizedLabelHalfHeight())
                        } else {
                            Modifier
                        }
                    )
                    .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                    .defaultMinSize(
                        minWidth = OutlinedTextFieldDefaults.MinWidth,
                        minHeight = OutlinedTextFieldDefaults.MinHeight,
                    ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError)),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox =
                @Composable { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = value,
                        visualTransformation = visualTransformation,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = label,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        prefix = prefix,
                        suffix = suffix,
                        supportingText = supportingText,
                        singleLine = singleLine,
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        container = {
                            OutlinedTextFieldDefaults.Container(
                                enabled = enabled,
                                isError = isError,
                                interactionSource = interactionSource,
                                colors = colors,
                                shape = shape,
                            )
                        },
                    )
                },
        )
    }
}

/**
 * [Material Design outlined text field](https://m3.material.io/components/text-fields/overview)
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Outlined text fields have less visual emphasis than filled text fields. When they appear in
 * places like forms, where many text fields are placed together, their reduced emphasis helps
 * simplify the layout.
 *
 * ![Outlined text field
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-text-field.png)
 *
 * This overload provides access to the input text, cursor position and selection range and IME
 * composition. If you only want to observe an input text change, use the OutlinedTextField overload
 * with the [String] parameter instead.
 *
 * @param value the input [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates values in
 *   [TextFieldValue]. An updated [TextFieldValue] comes as a parameter of the callback
 * @param modifier the [Modifier] to be applied to this text field
 * @param enabled controls the enabled state of this text field. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param readOnly controls the editable state of the text field. When `true`, the text field cannot
 *   be modified. However, a user can focus it and copy text from it. Read-only text fields are
 *   usually used to display pre-filled forms that a user cannot edit.
 * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
 * @param label the optional label to be displayed with this text field. The default text style uses
 *   [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 *   the input text is empty. The default text style for internal [Text] is [Typography.bodyLarge]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 *   container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 *   container
 * @param prefix the optional prefix to be displayed before the input text in the text field
 * @param suffix the optional suffix to be displayed after the input text in the text field
 * @param supportingText the optional supporting text to be displayed below the text field
 * @param isError indicates if the text field's current value is in error state. If set to true, the
 *   label, bottom indicator and trailing icon by default will be displayed in error color
 * @param visualTransformation transforms the visual representation of the input [value] For
 *   example, you can use
 *   [PasswordVisualTransformation][androidx.compose.ui.text.input.PasswordVisualTransformation] to
 *   create a password text field. By default, no visual transformation is applied.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction]
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction]
 * @param singleLine when `true`, this text field becomes a single horizontally scrolling text field
 *   instead of wrapping onto multiple lines. The keyboard will be informed to not show the return
 *   key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the maxLines
 *   attribute will be automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape defines the shape of this text field's border
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 *   in different states. See [OutlinedTextFieldDefaults.colors].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier =
                modifier
                    .then(
                        if (label != null) {
                            Modifier
                                // Merge semantics at the beginning of the modifier chain to ensure
                                // padding is considered part of the text field.
                                .semantics(mergeDescendants = true) {}
                                .padding(top = minimizedLabelHalfHeight())
                        } else {
                            Modifier
                        }
                    )
                    .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                    .defaultMinSize(
                        minWidth = OutlinedTextFieldDefaults.MinWidth,
                        minHeight = OutlinedTextFieldDefaults.MinHeight,
                    ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError)),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox =
                @Composable { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = value.text,
                        visualTransformation = visualTransformation,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = label,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        prefix = prefix,
                        suffix = suffix,
                        supportingText = supportingText,
                        singleLine = singleLine,
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        container = {
                            OutlinedTextFieldDefaults.Container(
                                enabled = enabled,
                                isError = isError,
                                interactionSource = interactionSource,
                                colors = colors,
                                shape = shape,
                            )
                        },
                    )
                },
        )
    }
}

/**
 * Layout of the leading and trailing icons and the text field, label and placeholder in
 * [OutlinedTextField]. It doesn't use Row to position the icons and middle part because label
 * should not be positioned in the middle part.
 */
@Composable
internal fun OutlinedTextFieldLayout(
    modifier: Modifier,
    textField: @Composable () -> Unit,
    placeholder: @Composable ((Modifier) -> Unit)?,
    label: @Composable (() -> Unit)?,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    prefix: @Composable (() -> Unit)?,
    suffix: @Composable (() -> Unit)?,
    singleLine: Boolean,
    labelPosition: TextFieldLabelPosition,
    labelProgress: FloatProducer,
    onLabelMeasured: (Size) -> Unit,
    container: @Composable () -> Unit,
    supporting: @Composable (() -> Unit)?,
    paddingValues: PaddingValues,
) {
    val horizontalIconPadding = textFieldHorizontalIconPadding()
    val measurePolicy =
        remember(
            onLabelMeasured,
            singleLine,
            labelPosition,
            labelProgress,
            paddingValues,
            horizontalIconPadding,
        ) {
            OutlinedTextFieldMeasurePolicy(
                onLabelMeasured = onLabelMeasured,
                singleLine = singleLine,
                labelPosition = labelPosition,
                labelProgress = labelProgress,
                paddingValues = paddingValues,
                horizontalIconPadding = horizontalIconPadding,
            )
        }
    val layoutDirection = LocalLayoutDirection.current
    Layout(
        modifier = modifier,
        content = {
            container()

            if (leading != null) {
                Box(
                    modifier = Modifier.layoutId(LeadingId).minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    leading()
                }
            }
            if (trailing != null) {
                Box(
                    modifier = Modifier.layoutId(TrailingId).minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    trailing()
                }
            }

            val startTextFieldPadding = paddingValues.calculateStartPadding(layoutDirection)
            val endTextFieldPadding = paddingValues.calculateEndPadding(layoutDirection)

            val startPadding =
                if (leading != null) {
                    (startTextFieldPadding - horizontalIconPadding).coerceAtLeast(0.dp)
                } else {
                    startTextFieldPadding
                }
            val endPadding =
                if (trailing != null) {
                    (endTextFieldPadding - horizontalIconPadding).coerceAtLeast(0.dp)
                } else {
                    endTextFieldPadding
                }

            if (prefix != null) {
                Box(
                    Modifier.layoutId(PrefixId)
                        .heightIn(min = MinTextLineHeight)
                        .wrapContentHeight()
                        .padding(start = startPadding, end = PrefixSuffixTextPadding)
                ) {
                    prefix()
                }
            }
            if (suffix != null) {
                Box(
                    Modifier.layoutId(SuffixId)
                        .heightIn(min = MinTextLineHeight)
                        .wrapContentHeight()
                        .padding(start = PrefixSuffixTextPadding, end = endPadding)
                ) {
                    suffix()
                }
            }

            val textPadding =
                Modifier.heightIn(min = MinTextLineHeight)
                    .wrapContentHeight()
                    .padding(
                        start = if (prefix == null) startPadding else 0.dp,
                        end = if (suffix == null) endPadding else 0.dp,
                    )

            if (placeholder != null) {
                placeholder(Modifier.layoutId(PlaceholderId).then(textPadding))
            }

            Box(
                modifier = Modifier.layoutId(TextFieldId).then(textPadding),
                propagateMinConstraints = true,
            ) {
                textField()
            }

            val labelPadding =
                if (labelPosition is TextFieldLabelPosition.Above) {
                    Modifier.padding(
                        start = AboveLabelHorizontalPadding,
                        end = AboveLabelHorizontalPadding,
                        bottom = AboveLabelBottomPadding,
                    )
                } else {
                    Modifier
                }

            if (label != null) {
                Box(
                    Modifier.textFieldLabelMinHeight {
                            lerp(MinTextLineHeight, MinFocusedLabelLineHeight, labelProgress())
                        }
                        .wrapContentHeight()
                        .layoutId(LabelId)
                        .then(labelPadding)
                ) {
                    label()
                }
            }

            if (supporting != null) {
                Box(
                    Modifier.layoutId(SupportingId)
                        .heightIn(min = MinSupportingTextLineHeight)
                        .wrapContentHeight()
                        .padding(TextFieldDefaults.supportingTextPadding())
                ) {
                    supporting()
                }
            }
        },
        measurePolicy = measurePolicy,
    )
}

private class OutlinedTextFieldMeasurePolicy(
    private val onLabelMeasured: (Size) -> Unit,
    private val singleLine: Boolean,
    private val labelPosition: TextFieldLabelPosition,
    private val labelProgress: FloatProducer,
    private val paddingValues: PaddingValues,
    private val horizontalIconPadding: Dp,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val labelProgress = labelProgress()
        var occupiedSpaceHorizontally = 0
        var occupiedSpaceVertically = 0
        val bottomPadding = paddingValues.calculateBottomPadding().roundToPx()

        val relaxedConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // measure leading icon
        val leadingPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LeadingId }?.measure(relaxedConstraints)
        occupiedSpaceHorizontally += leadingPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, leadingPlaceable.heightOrZero)

        // measure trailing icon
        val trailingPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.measure(relaxedConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += trailingPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, trailingPlaceable.heightOrZero)

        // measure prefix
        val prefixPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.measure(relaxedConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += prefixPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, prefixPlaceable.heightOrZero)

        // measure suffix
        val suffixPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.measure(relaxedConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += suffixPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, suffixPlaceable.heightOrZero)

        // measure label
        val isLabelAbove = labelPosition is TextFieldLabelPosition.Above
        val labelMeasurable = measurables.fastFirstOrNull { it.layoutId == LabelId }
        var labelPlaceable: Placeable? = null
        val labelIntrinsicHeight: Int
        if (!isLabelAbove) {
            // if label is not Above, we can measure it like normal
            val totalHorizontalPadding =
                paddingValues.calculateLeftPadding(layoutDirection).roundToPx() +
                    paddingValues.calculateRightPadding(layoutDirection).roundToPx()
            val labelHorizontalConstraintOffset =
                lerp(
                    occupiedSpaceHorizontally + totalHorizontalPadding, // label in middle
                    totalHorizontalPadding, // label in outline
                    labelProgress,
                )
            val labelConstraints =
                relaxedConstraints.offset(
                    horizontal = -labelHorizontalConstraintOffset,
                    vertical = -bottomPadding,
                )
            labelPlaceable = labelMeasurable?.measure(labelConstraints)
            val labelSize =
                labelPlaceable?.let { Size(it.width.toFloat(), it.height.toFloat()) } ?: Size.Zero
            onLabelMeasured(labelSize)
            labelIntrinsicHeight = 0
        } else {
            // if label is Above, it must be measured after other elements, but we
            // reserve space for it using its intrinsic height as a heuristic
            labelIntrinsicHeight = labelMeasurable?.minIntrinsicHeight(constraints.minWidth) ?: 0
        }

        // supporting text must be measured after other elements, but we
        // reserve space for it using its intrinsic height as a heuristic
        val supportingMeasurable = measurables.fastFirstOrNull { it.layoutId == SupportingId }
        val supportingIntrinsicHeight =
            supportingMeasurable?.minIntrinsicHeight(constraints.minWidth) ?: 0

        // measure text field
        val topPadding =
            if (isLabelAbove) {
                paddingValues.calculateTopPadding().roundToPx()
            } else {
                max(
                    labelPlaceable.heightOrZero / 2,
                    paddingValues.calculateTopPadding().roundToPx(),
                )
            }
        val textConstraints =
            constraints
                .offset(
                    horizontal = -occupiedSpaceHorizontally,
                    vertical =
                        -bottomPadding -
                            topPadding -
                            labelIntrinsicHeight -
                            supportingIntrinsicHeight,
                )
                .copy(minHeight = 0)
        val textFieldPlaceable =
            measurables.fastFirst { it.layoutId == TextFieldId }.measure(textConstraints)

        // measure placeholder
        val placeholderConstraints = textConstraints.copy(minWidth = 0)
        val placeholderPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.measure(placeholderConstraints)

        occupiedSpaceVertically =
            max(
                occupiedSpaceVertically,
                max(textFieldPlaceable.heightOrZero, placeholderPlaceable.heightOrZero) +
                    topPadding +
                    bottomPadding,
            )

        val width =
            calculateWidth(
                leadingPlaceableWidth = leadingPlaceable.widthOrZero,
                trailingPlaceableWidth = trailingPlaceable.widthOrZero,
                prefixPlaceableWidth = prefixPlaceable.widthOrZero,
                suffixPlaceableWidth = suffixPlaceable.widthOrZero,
                textFieldPlaceableWidth = textFieldPlaceable.width,
                labelPlaceableWidth = labelPlaceable.widthOrZero,
                placeholderPlaceableWidth = placeholderPlaceable.widthOrZero,
                constraints = constraints,
                labelProgress = labelProgress,
            )

        if (isLabelAbove) {
            // now that we know the width, measure label
            val labelConstraints =
                relaxedConstraints.copy(maxHeight = labelIntrinsicHeight, maxWidth = width)
            labelPlaceable = labelMeasurable?.measure(labelConstraints)
            val labelSize =
                labelPlaceable?.let { Size(it.width.toFloat(), it.height.toFloat()) } ?: Size.Zero
            onLabelMeasured(labelSize)
        }

        // measure supporting text
        val supportingConstraints =
            relaxedConstraints
                .offset(vertical = -occupiedSpaceVertically)
                .copy(minHeight = 0, maxWidth = width)
        val supportingPlaceable = supportingMeasurable?.measure(supportingConstraints)
        val supportingHeight = supportingPlaceable.heightOrZero

        val totalHeight =
            calculateHeight(
                leadingHeight = leadingPlaceable.heightOrZero,
                trailingHeight = trailingPlaceable.heightOrZero,
                prefixHeight = prefixPlaceable.heightOrZero,
                suffixHeight = suffixPlaceable.heightOrZero,
                textFieldHeight = textFieldPlaceable.height,
                labelHeight = labelPlaceable.heightOrZero,
                placeholderHeight = placeholderPlaceable.heightOrZero,
                supportingHeight = supportingPlaceable.heightOrZero,
                constraints = constraints,
                isLabelAbove = isLabelAbove,
                labelProgress = labelProgress,
            )
        val height =
            totalHeight - supportingHeight - (if (isLabelAbove) labelPlaceable.heightOrZero else 0)

        val containerPlaceable =
            measurables
                .fastFirst { it.layoutId == ContainerId }
                .measure(
                    Constraints(
                        minWidth = if (width != Constraints.Infinity) width else 0,
                        maxWidth = width,
                        minHeight = if (height != Constraints.Infinity) height else 0,
                        maxHeight = height,
                    )
                )
        return layout(width, totalHeight) {
            place(
                totalHeight = totalHeight,
                width = width,
                leadingPlaceable = leadingPlaceable,
                trailingPlaceable = trailingPlaceable,
                prefixPlaceable = prefixPlaceable,
                suffixPlaceable = suffixPlaceable,
                textFieldPlaceable = textFieldPlaceable,
                labelPlaceable = labelPlaceable,
                placeholderPlaceable = placeholderPlaceable,
                containerPlaceable = containerPlaceable,
                supportingPlaceable = supportingPlaceable,
                density = density,
                layoutDirection = layoutDirection,
                isLabelAbove = isLabelAbove,
                labelProgress = labelProgress,
                iconPadding = horizontalIconPadding.toPx(),
            )
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.maxIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.minIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.maxIntrinsicWidth(h)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.minIntrinsicWidth(h)
        }
    }

    private fun IntrinsicMeasureScope.intrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int,
    ): Int {
        val textFieldWidth =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, height)
        val labelWidth =
            measurables
                .fastFirstOrNull { it.layoutId == LabelId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val trailingWidth =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val leadingWidth =
            measurables
                .fastFirstOrNull { it.layoutId == LeadingId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val prefixWidth =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val suffixWidth =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val placeholderWidth =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        return calculateWidth(
            leadingPlaceableWidth = leadingWidth,
            trailingPlaceableWidth = trailingWidth,
            prefixPlaceableWidth = prefixWidth,
            suffixPlaceableWidth = suffixWidth,
            textFieldPlaceableWidth = textFieldWidth,
            labelPlaceableWidth = labelWidth,
            placeholderPlaceableWidth = placeholderWidth,
            constraints = Constraints(),
            labelProgress = labelProgress(),
        )
    }

    private fun IntrinsicMeasureScope.intrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int,
    ): Int {
        val labelProgress = labelProgress()
        var remainingWidth = width
        val leadingHeight =
            measurables
                .fastFirstOrNull { it.layoutId == LeadingId }
                ?.let {
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    intrinsicMeasurer(it, width)
                } ?: 0
        val trailingHeight =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.let {
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    intrinsicMeasurer(it, width)
                } ?: 0

        val labelHeight =
            measurables
                .fastFirstOrNull { it.layoutId == LabelId }
                ?.let { intrinsicMeasurer(it, lerp(remainingWidth, width, labelProgress)) } ?: 0

        val prefixHeight =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.let {
                    val height = intrinsicMeasurer(it, remainingWidth)
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    height
                } ?: 0
        val suffixHeight =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.let {
                    val height = intrinsicMeasurer(it, remainingWidth)
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    height
                } ?: 0

        val textFieldHeight =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, remainingWidth)

        val placeholderHeight =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.let { intrinsicMeasurer(it, remainingWidth) } ?: 0

        val supportingHeight =
            measurables
                .fastFirstOrNull { it.layoutId == SupportingId }
                ?.let { intrinsicMeasurer(it, width) } ?: 0

        return calculateHeight(
            leadingHeight = leadingHeight,
            trailingHeight = trailingHeight,
            prefixHeight = prefixHeight,
            suffixHeight = suffixHeight,
            textFieldHeight = textFieldHeight,
            labelHeight = labelHeight,
            placeholderHeight = placeholderHeight,
            supportingHeight = supportingHeight,
            constraints = Constraints(),
            isLabelAbove = labelPosition is TextFieldLabelPosition.Above,
            labelProgress = labelProgress,
        )
    }

    /**
     * Calculate the width of the [OutlinedTextField] given all elements that should be placed
     * inside.
     */
    private fun Density.calculateWidth(
        leadingPlaceableWidth: Int,
        trailingPlaceableWidth: Int,
        prefixPlaceableWidth: Int,
        suffixPlaceableWidth: Int,
        textFieldPlaceableWidth: Int,
        labelPlaceableWidth: Int,
        placeholderPlaceableWidth: Int,
        constraints: Constraints,
        labelProgress: Float,
    ): Int {
        val affixTotalWidth = prefixPlaceableWidth + suffixPlaceableWidth
        val middleSection =
            maxOf(
                textFieldPlaceableWidth + affixTotalWidth,
                placeholderPlaceableWidth + affixTotalWidth,
                // Prefix/suffix does not get applied to label
                lerp(labelPlaceableWidth, 0, labelProgress),
            )
        val wrappedWidth = leadingPlaceableWidth + middleSection + trailingPlaceableWidth

        // Actual LayoutDirection doesn't matter; we only need the sum
        val labelHorizontalPadding =
            (paddingValues.calculateLeftPadding(LayoutDirection.Ltr) +
                    paddingValues.calculateRightPadding(LayoutDirection.Ltr))
                .toPx()
        val focusedLabelWidth =
            ((labelPlaceableWidth + labelHorizontalPadding) * labelProgress).roundToInt()
        return constraints.constrainWidth(max(wrappedWidth, focusedLabelWidth))
    }

    /**
     * Calculate the height of the [OutlinedTextField] given all elements that should be placed
     * inside. This includes the supporting text, if it exists, even though this element is not
     * "visually" inside the text field.
     */
    private fun Density.calculateHeight(
        leadingHeight: Int,
        trailingHeight: Int,
        prefixHeight: Int,
        suffixHeight: Int,
        textFieldHeight: Int,
        labelHeight: Int,
        placeholderHeight: Int,
        supportingHeight: Int,
        constraints: Constraints,
        isLabelAbove: Boolean,
        labelProgress: Float,
    ): Int {
        val inputFieldHeight =
            maxOf(
                textFieldHeight,
                placeholderHeight,
                prefixHeight,
                suffixHeight,
                if (isLabelAbove) 0 else lerp(labelHeight, 0, labelProgress),
            )
        val topPadding = paddingValues.calculateTopPadding().toPx()
        val actualTopPadding =
            if (isLabelAbove) {
                topPadding
            } else {
                lerp(topPadding, max(topPadding, labelHeight / 2f), labelProgress)
            }
        val bottomPadding = paddingValues.calculateBottomPadding().toPx()
        val middleSectionHeight = actualTopPadding + inputFieldHeight + bottomPadding

        return constraints.constrainHeight(
            (if (isLabelAbove) labelHeight else 0) +
                maxOf(leadingHeight, trailingHeight, middleSectionHeight.roundToInt()) +
                supportingHeight
        )
    }

    /**
     * Places the provided text field, placeholder, label, optional leading and trailing icons
     * inside the [OutlinedTextField]
     */
    private fun Placeable.PlacementScope.place(
        totalHeight: Int,
        width: Int,
        leadingPlaceable: Placeable?,
        trailingPlaceable: Placeable?,
        prefixPlaceable: Placeable?,
        suffixPlaceable: Placeable?,
        textFieldPlaceable: Placeable,
        labelPlaceable: Placeable?,
        placeholderPlaceable: Placeable?,
        containerPlaceable: Placeable,
        supportingPlaceable: Placeable?,
        density: Float,
        layoutDirection: LayoutDirection,
        isLabelAbove: Boolean,
        labelProgress: Float,
        iconPadding: Float,
    ) {
        val yOffset = if (isLabelAbove) labelPlaceable.heightOrZero else 0

        // place container
        containerPlaceable.place(0, yOffset)

        // Most elements should be positioned w.r.t the text field's "visual" height, i.e.,
        // excluding the label (if it's Above) and the supporting text on bottom
        val height =
            totalHeight -
                supportingPlaceable.heightOrZero -
                (if (isLabelAbove) labelPlaceable.heightOrZero else 0)

        val topPadding = (paddingValues.calculateTopPadding().value * density).roundToInt()

        // placed center vertically and to the start edge horizontally
        leadingPlaceable?.placeRelative(
            0,
            yOffset + Alignment.CenterVertically.align(leadingPlaceable.height, height),
        )

        // label position is animated
        // in single line text field, label is centered vertically before animation starts
        labelPlaceable?.let {
            val startY =
                when {
                    isLabelAbove -> 0
                    singleLine -> Alignment.CenterVertically.align(it.height, height)
                    else -> topPadding
                }
            val endY =
                when {
                    isLabelAbove -> 0
                    else -> -(it.height / 2)
                }
            val positionY = lerp(startY, endY, labelProgress)

            if (isLabelAbove) {
                val positionX =
                    labelPosition.minimizedAlignment.align(
                        size = labelPlaceable.width,
                        space = width,
                        layoutDirection = layoutDirection,
                    )
                // Not placeRelative because alignment already handles RTL
                labelPlaceable.place(positionX, positionY)
            } else {
                val startPadding =
                    paddingValues.calculateStartPadding(layoutDirection).value * density
                val endPadding = paddingValues.calculateEndPadding(layoutDirection).value * density
                val leadingPlusPadding =
                    if (leadingPlaceable == null) {
                        startPadding
                    } else {
                        leadingPlaceable.width + (startPadding - iconPadding).coerceAtLeast(0f)
                    }
                val trailingPlusPadding =
                    if (trailingPlaceable == null) {
                        endPadding
                    } else {
                        trailingPlaceable.width + (endPadding - iconPadding).coerceAtLeast(0f)
                    }
                val leftPadding =
                    if (layoutDirection == LayoutDirection.Ltr) startPadding else endPadding
                val leftIconPlusPadding =
                    if (layoutDirection == LayoutDirection.Ltr) leadingPlusPadding
                    else trailingPlusPadding
                val startX =
                    labelPosition.expandedAlignment.align(
                        size = labelPlaceable.width,
                        space = width - (leadingPlusPadding + trailingPlusPadding).roundToInt(),
                        layoutDirection = layoutDirection,
                    ) + leftIconPlusPadding

                val endX =
                    labelPosition.minimizedAlignment.align(
                        size = labelPlaceable.width,
                        space = width - (startPadding + endPadding).roundToInt(),
                        layoutDirection = layoutDirection,
                    ) + leftPadding
                val positionX = lerp(startX, endX, labelProgress).roundToInt()
                // Not placeRelative because alignment already handles RTL
                labelPlaceable.place(positionX, positionY)
            }
        }

        fun calculateVerticalPosition(placeable: Placeable): Int {
            val defaultPosition =
                yOffset +
                    if (singleLine) {
                        // Single line text fields have text components centered vertically.
                        Alignment.CenterVertically.align(placeable.height, height)
                    } else {
                        // Multiline text fields have text components aligned to top with padding.
                        topPadding
                    }
            return if (labelPosition is TextFieldLabelPosition.Above) {
                defaultPosition
            } else {
                // Ensure components are placed below label when it's in the border
                max(defaultPosition, labelPlaceable.heightOrZero / 2)
            }
        }

        prefixPlaceable?.placeRelative(
            leadingPlaceable.widthOrZero,
            calculateVerticalPosition(prefixPlaceable),
        )

        val textHorizontalPosition = leadingPlaceable.widthOrZero + prefixPlaceable.widthOrZero

        textFieldPlaceable.placeRelative(
            textHorizontalPosition,
            calculateVerticalPosition(textFieldPlaceable),
        )

        // placed similar to the input text above
        placeholderPlaceable?.placeRelative(
            textHorizontalPosition,
            calculateVerticalPosition(placeholderPlaceable),
        )

        suffixPlaceable?.placeRelative(
            width - trailingPlaceable.widthOrZero - suffixPlaceable.width,
            calculateVerticalPosition(suffixPlaceable),
        )

        // placed center vertically and to the end edge horizontally
        trailingPlaceable?.placeRelative(
            width - trailingPlaceable.width,
            yOffset + Alignment.CenterVertically.align(trailingPlaceable.height, height),
        )

        // place supporting text
        supportingPlaceable?.placeRelative(0, yOffset + height)
    }
}

internal fun Modifier.outlineCutout(
    labelSize: () -> Size,
    alignment: Alignment.Horizontal,
    paddingValues: PaddingValues,
) =
    this.drawWithContent {
        val labelSizeValue = labelSize()
        val labelWidth = labelSizeValue.width
        if (labelWidth > 0f) {
            val innerPadding = OutlinedTextFieldInnerPadding.toPx()
            val leftPadding = paddingValues.calculateLeftPadding(layoutDirection).toPx()
            val rightPadding = paddingValues.calculateRightPadding(layoutDirection).toPx()
            val labelCenter =
                alignment.align(
                    size = labelWidth.roundToInt(),
                    space = (size.width - leftPadding - rightPadding).roundToInt(),
                    layoutDirection = layoutDirection,
                ) + leftPadding + (labelWidth / 2)
            val left = (labelCenter - (labelWidth / 2) - innerPadding).coerceAtLeast(0f)
            val right = (labelCenter + (labelWidth / 2) + innerPadding).coerceAtMost(size.width)
            val labelHeight = labelSizeValue.height
            // using label height as a cutout area to make sure that no hairline artifacts are
            // left when we clip the border
            clipRect(left, -labelHeight / 2, right, labelHeight / 2, ClipOp.Difference) {
                this@drawWithContent.drawContent()
            }
        } else {
            this@drawWithContent.drawContent()
        }
    }

private val OutlinedTextFieldInnerPadding = 4.dp
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/BottomSheetScaffold.kt
```kotlin
/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.draggableAnchors
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * [Material Design standard bottom sheet
 * scaffold](https://m3.material.io/components/bottom-sheets/overview)
 *
 * Standard bottom sheets co-exist with the screen’s main UI region and allow for simultaneously
 * viewing and interacting with both regions. They are commonly used to keep a feature or secondary
 * content visible on screen when content in main UI region is frequently scrolled or panned.
 *
 * ![Bottom sheet
 * image](https://developer.android.com/images/reference/androidx/compose/material3/bottom_sheet.png)
 *
 * This component provides API to put together several material components to construct your screen,
 * by ensuring proper layout strategy for them and collecting necessary data so these components
 * will work together correctly.
 *
 * A simple example of a standard bottom sheet looks like this:
 *
 * @sample androidx.compose.material3.samples.SimpleBottomSheetScaffoldSample
 * @param sheetContent the content of the bottom sheet
 * @param modifier the [Modifier] to be applied to the root of the scaffold
 * @param scaffoldState the state of the bottom sheet scaffold
 * @param sheetPeekHeight the height of the bottom sheet when it is collapsed
 * @param sheetMaxWidth [Dp] that defines what the maximum width the sheet will take. Pass in
 *   [Dp.Unspecified] for a sheet that spans the entire screen width.
 * @param sheetShape the shape of the bottom sheet
 * @param sheetContainerColor the background color of the bottom sheet
 * @param sheetContentColor the preferred content color provided by the bottom sheet to its
 *   children. Defaults to the matching content color for [sheetContainerColor], or if that is not a
 *   color from the theme, this will keep the same content color set above the bottom sheet.
 * @param sheetTonalElevation when [sheetContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param sheetShadowElevation the shadow elevation of the bottom sheet
 * @param sheetDragHandle optional visual marker to pull the scaffold's bottom sheet
 * @param sheetSwipeEnabled whether the sheet swiping is enabled and should react to the user's
 *   input
 * @param topBar top app bar of the screen, typically a [TopAppBar]
 * @param snackbarHost component to host [Snackbar]s that are pushed to be shown via
 *   [SnackbarHostState.showSnackbar], typically a [SnackbarHost]
 * @param containerColor the color used for the background of this scaffold. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this scaffold. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param content content of the screen. The lambda receives a [PaddingValues] that should be
 *   applied to the content root via [Modifier.padding] and [Modifier.consumeWindowInsets] to
 *   properly offset top and bottom bars. If using [Modifier.verticalScroll], apply this modifier to
 *   the child of the scroll, and not on the scroll itself.
 */
@Composable
@ExperimentalMaterial3Api
fun BottomSheetScaffold(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    sheetPeekHeight: Dp = BottomSheetDefaults.SheetPeekHeight,
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetShape: Shape = BottomSheetDefaults.ExpandedShape,
    sheetContainerColor: Color = BottomSheetDefaults.ContainerColor,
    sheetContentColor: Color = contentColorFor(sheetContainerColor),
    sheetTonalElevation: Dp = 0.dp,
    sheetShadowElevation: Dp = BottomSheetDefaults.Elevation,
    sheetDragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    sheetSwipeEnabled: Boolean = true,
    topBar: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(modifier.fillMaxSize().background(containerColor)) {
        // Using composition local provider instead of Surface as Surface implements .clip() which
        // intercepts touch events in testing.
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            BottomSheetScaffoldLayout(
                topBar = topBar,
                body = { content(PaddingValues(bottom = sheetPeekHeight)) },
                snackbarHost = { snackbarHost(scaffoldState.snackbarHostState) },
                sheetOffset = { scaffoldState.bottomSheetState.requireOffset() },
                sheetState = scaffoldState.bottomSheetState,
                bottomSheet = {
                    StandardBottomSheet(
                        state = scaffoldState.bottomSheetState,
                        peekHeight = sheetPeekHeight,
                        sheetMaxWidth = sheetMaxWidth,
                        sheetSwipeEnabled = sheetSwipeEnabled,
                        shape = sheetShape,
                        containerColor = sheetContainerColor,
                        contentColor = sheetContentColor,
                        tonalElevation = sheetTonalElevation,
                        shadowElevation = sheetShadowElevation,
                        dragHandle = sheetDragHandle,
                        content = sheetContent,
                    )
                },
            )
        }
    }
}

/**
 * State of the [BottomSheetScaffold] composable.
 *
 * @param bottomSheetState the state of the persistent bottom sheet
 * @param snackbarHostState the [SnackbarHostState] used to show snackbars inside the scaffold
 */
@ExperimentalMaterial3Api
@Stable
class BottomSheetScaffoldState(
    val bottomSheetState: SheetState,
    val snackbarHostState: SnackbarHostState,
)

/**
 * Create and [remember] a [BottomSheetScaffoldState].
 *
 * @param bottomSheetState the state of the standard bottom sheet. See
 *   [rememberStandardBottomSheetState]
 * @param snackbarHostState the [SnackbarHostState] used to show snackbars inside the scaffold
 */
@Composable
@ExperimentalMaterial3Api
fun rememberBottomSheetScaffoldState(
    bottomSheetState: SheetState = rememberStandardBottomSheetState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
): BottomSheetScaffoldState {
    return remember(bottomSheetState, snackbarHostState) {
        BottomSheetScaffoldState(
            bottomSheetState = bottomSheetState,
            snackbarHostState = snackbarHostState,
        )
    }
}

/**
 * Create and [remember] a [SheetState] for [BottomSheetScaffold].
 *
 * @param initialValue the initial value of the state. Should be either [PartiallyExpanded] or
 *   [Expanded] if [skipHiddenState] is true
 * @param confirmValueChange optional callback invoked to confirm or veto a pending state change
 * @param [skipHiddenState] whether Hidden state is skipped for [BottomSheetScaffold]
 */
@Composable
@ExperimentalMaterial3Api
fun rememberStandardBottomSheetState(
    initialValue: SheetValue = PartiallyExpanded,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    skipHiddenState: Boolean = true,
) =
    rememberSheetState(
        confirmValueChange = confirmValueChange,
        initialValue = initialValue,
        skipHiddenState = skipHiddenState,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandardBottomSheet(
    state: SheetState,
    peekHeight: Dp,
    sheetMaxWidth: Dp,
    sheetSwipeEnabled: Boolean,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    dragHandle: @Composable (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    // TODO Load the motionScheme tokens from the component tokens file
    val anchoredDraggableMotion: FiniteAnimationSpec<Float> =
        MotionSchemeKeyTokens.DefaultSpatial.value()
    val showMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.DefaultSpatial.value()
    val hideMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.FastEffects.value()

    SideEffect {
        state.showMotionSpec = showMotion
        state.hideMotionSpec = hideMotion
        state.anchoredDraggableMotionSpec = anchoredDraggableMotion
    }

    val scope = rememberCoroutineScope()
    val orientation = Orientation.Vertical
    val peekHeightPx = with(LocalDensity.current) { peekHeight.toPx() }
    val anchoredDraggableFlingBehavior =
        AnchoredDraggableDefaults.flingBehavior(
            state = state.anchoredDraggableState,
            positionalThreshold = { _ -> state.positionalThreshold.invoke() },
            animationSpec = BottomSheetAnimationSpec,
        )

    val nestedScroll =
        if (sheetSwipeEnabled) {
            Modifier.nestedScroll(
                remember(state.anchoredDraggableState) {
                    ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                        sheetState = state,
                        orientation = orientation,
                        flingBehavior = anchoredDraggableFlingBehavior,
                    )
                }
            )
        } else {
            Modifier
        }
    Surface(
        modifier =
            Modifier.widthIn(max = sheetMaxWidth)
                .fillMaxWidth()
                .requiredHeightIn(min = peekHeight)
                .then(nestedScroll)
                .draggableAnchors(state.anchoredDraggableState, orientation) {
                    sheetSize,
                    constraints ->
                    val layoutHeight = constraints.maxHeight.toFloat()
                    val sheetHeight = sheetSize.height.toFloat()

                    val newAnchors = DraggableAnchors {
                        val isHiddenAnchorAvailable =
                            sheetHeight == 0f || peekHeightPx == 0f || !state.skipHiddenState

                        // We are preserving ambiguous anchor reconciliation for first layout pass.
                        // This handles the use case where sheetPeekHeight is backed by a mutable
                        // value which is backed by 0.dp before being recalculated. We can assume
                        // the state is in its first pass by asserting anchor sizes are zero, as we
                        // enforce at least 1 anchor below. We then settle at partial as this is
                        // the anchor external users have access to via sheetPeekHeight API.
                        val isInitialLayout = state.anchoredDraggableState.anchors.size == 0
                        val isStableAtPartial =
                            state.currentValue == PartiallyExpanded && !state.isAnimationRunning

                        val isAmbiguousPartialAllowed =
                            peekHeightPx == 0f && (isInitialLayout || isStableAtPartial)

                        val isPartiallyExpandedAnchorAvailable =
                            !state.skipPartiallyExpanded &&
                                (peekHeightPx > 0f || isAmbiguousPartialAllowed) &&
                                peekHeightPx != sheetHeight

                        val isExpandedAnchorAvailable = sheetHeight > 0f

                        require(
                            isHiddenAnchorAvailable ||
                                isPartiallyExpandedAnchorAvailable ||
                                isExpandedAnchorAvailable
                        ) {
                            "BottomSheetScaffold: Require at least 1 anchor to be initialized"
                        }

                        if (isPartiallyExpandedAnchorAvailable) {
                            PartiallyExpanded at (layoutHeight - peekHeightPx)
                        }
                        if (isHiddenAnchorAvailable) {
                            Hidden at layoutHeight
                        }
                        if (isExpandedAnchorAvailable) {
                            Expanded at layoutHeight - sheetHeight
                        }
                    }
                    val newTarget =
                        when (val oldTarget = state.targetValue) {
                            Hidden -> if (newAnchors.hasPositionFor(Hidden)) Hidden else oldTarget
                            PartiallyExpanded ->
                                when {
                                    newAnchors.hasPositionFor(PartiallyExpanded) ->
                                        PartiallyExpanded

                                    newAnchors.hasPositionFor(Expanded) -> Expanded
                                    newAnchors.hasPositionFor(Hidden) -> Hidden
                                    else -> oldTarget
                                }

                            Expanded ->
                                if (newAnchors.hasPositionFor(Expanded)) Expanded else Hidden
                        }
                    return@draggableAnchors newAnchors to newTarget
                }
                .anchoredDraggable(
                    state = state.anchoredDraggableState,
                    orientation = orientation,
                    enabled = sheetSwipeEnabled,
                    flingBehavior = anchoredDraggableFlingBehavior,
                )
                // Scale up the Surface vertically in case the sheet's offset overflows below the
                // min anchor. This is done to avoid showing a gap when the sheet opens and bounces
                // when it's applied with a bouncy motion. Note that the content inside the Surface
                // is scaled back down to maintain its aspect ratio (see below).
                .verticalScaleUp(state),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        Column(
            Modifier.fillMaxWidth()
                // Scale the content down in case the sheet offset overflows below the min anchor.
                // The wrapping Surface is scaled up, so this is done to maintain the content's
                // aspect ratio.
                .verticalScaleDown(state)
        ) {
            if (dragHandle != null) {
                val partialExpandActionLabel =
                    getString(Strings.BottomSheetPartialExpandDescription)
                val dismissActionLabel = getString(Strings.BottomSheetDismissDescription)
                val expandActionLabel = getString(Strings.BottomSheetExpandDescription)
                DragHandleWithTooltip(
                    modifier =
                        Modifier.clickable {
                                when (state.currentValue) {
                                    Expanded ->
                                        scope.launch {
                                            if (!state.skipHiddenState) {
                                                state.hide()
                                            } else {
                                                state.partialExpand()
                                            }
                                        }

                                    PartiallyExpanded -> scope.launch { state.expand() }
                                    else -> scope.launch { state.show() }
                                }
                            }
                            .semantics(mergeDescendants = true) {
                                with(state) {
                                    // Provides semantics to interact with the bottomsheet if
                                    // there is more than one anchor to swipe to and swiping is
                                    // enabled.
                                    if (
                                        anchoredDraggableState.anchors.size > 1 && sheetSwipeEnabled
                                    ) {
                                        if (currentValue == PartiallyExpanded) {
                                            expand(expandActionLabel) {
                                                val canExpand = confirmValueChange(Expanded)
                                                if (canExpand) {
                                                    scope.launch { expand() }
                                                }
                                                return@expand canExpand
                                            }
                                        } else {
                                            collapse(partialExpandActionLabel) {
                                                val canPartiallyExpand =
                                                    confirmValueChange(PartiallyExpanded)
                                                scope.launch { partialExpand() }
                                                return@collapse canPartiallyExpand
                                            }
                                        }
                                        if (!state.skipHiddenState) {
                                            dismiss(dismissActionLabel) {
                                                val canHide = confirmValueChange(Hidden)
                                                scope.launch { hide() }
                                                return@dismiss canHide
                                            }
                                        }
                                    }
                                }
                            },
                    content = dragHandle,
                )
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetScaffoldLayout(
    topBar: @Composable (() -> Unit)?,
    body: @Composable () -> Unit,
    bottomSheet: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    sheetOffset: () -> Float,
    sheetState: SheetState,
) {
    Layout(
        contents = listOf<@Composable () -> Unit>(topBar ?: {}, body, bottomSheet, snackbarHost)
    ) {
        (topBarMeasurables, bodyMeasurables, bottomSheetMeasurables, snackbarHostMeasurables),
        constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val sheetPlaceables = bottomSheetMeasurables.fastMap { it.measure(looseConstraints) }

        val topBarPlaceables = topBarMeasurables.fastMap { it.measure(looseConstraints) }
        val topBarHeight = topBarPlaceables.fastMaxOfOrNull { it.height } ?: 0

        val bodyConstraints = looseConstraints.copy(maxHeight = layoutHeight - topBarHeight)
        val bodyPlaceables = bodyMeasurables.fastMap { it.measure(bodyConstraints) }

        val snackbarPlaceables = snackbarHostMeasurables.fastMap { it.measure(looseConstraints) }

        layout(layoutWidth, layoutHeight) {
            val sheetWidth = sheetPlaceables.fastMaxOfOrNull { it.width } ?: 0
            val sheetOffsetX = max(0, (layoutWidth - sheetWidth) / 2)

            val snackbarWidth = snackbarPlaceables.fastMaxOfOrNull { it.width } ?: 0
            val snackbarHeight = snackbarPlaceables.fastMaxOfOrNull { it.height } ?: 0
            val snackbarOffsetX = (layoutWidth - snackbarWidth) / 2
            val snackbarOffsetY =
                when (sheetState.currentValue) {
                    PartiallyExpanded -> sheetOffset().roundToInt() - snackbarHeight
                    Expanded,
                    Hidden -> layoutHeight - snackbarHeight
                }

            // Placement order is important for elevation
            bodyPlaceables.fastForEach { it.placeRelative(0, topBarHeight) }
            topBarPlaceables.fastForEach { it.placeRelative(0, 0) }
            sheetPlaceables.fastForEach { it.placeRelative(sheetOffsetX, 0) }
            snackbarPlaceables.fastForEach { it.placeRelative(snackbarOffsetX, snackbarOffsetY) }
        }
    }
}
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/ModalBottomSheet.kt
```kotlin
/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * [Material Design modal bottom sheet](https://m3.material.io/components/bottom-sheets/overview)
 *
 * Modal bottom sheets are used as an alternative to inline menus or simple dialogs on mobile,
 * especially when offering a long list of action items, or when items require longer descriptions
 * and icons. Like dialogs, modal bottom sheets appear in front of app content, disabling all other
 * app functionality when they appear, and remaining on screen until confirmed, dismissed, or a
 * required action has been taken.
 *
 * ![Bottom sheet
 * image](https://developer.android.com/images/reference/androidx/compose/material3/bottom_sheet.png)
 *
 * A simple example of a modal bottom sheet looks like this:
 *
 * @sample androidx.compose.material3.samples.ModalBottomSheetSample
 * @param onDismissRequest Executes when the user clicks outside of the bottom sheet, after sheet
 *   animates to [Hidden].
 * @param modifier Optional [Modifier] for the bottom sheet.
 * @param sheetState The state of the bottom sheet.
 * @param sheetMaxWidth [Dp] that defines what the maximum width the sheet will take. Pass in
 *   [Dp.Unspecified] for a sheet that spans the entire screen width.
 * @param sheetGesturesEnabled Whether the bottom sheet can be interacted with by gestures.
 * @param shape The shape of the bottom sheet.
 * @param containerColor The color used for the background of this bottom sheet
 * @param contentColor The preferred color for content inside this bottom sheet. Defaults to either
 *   the matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param scrimColor Color of the scrim that obscures content when the bottom sheet is open.
 * @param dragHandle Optional visual marker to swipe the bottom sheet.
 * @param contentWindowInsets callback which provides window insets to be passed to the bottom sheet
 *   content via [Modifier.windowInsetsPadding]. [ModalBottomSheet] will pre-emptively consume top
 *   insets based on it's current offset. This keeps content outside of the expected window insets
 *   at any position.
 * @param properties [ModalBottomSheetProperties] for further customization of this modal bottom
 *   sheet's window behavior.
 * @param content The content to be displayed inside the bottom sheet.
 */
@Composable
@ExperimentalMaterial3Api
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    // TODO Load the motionScheme tokens from the component tokens file
    val anchoredDraggableMotion: FiniteAnimationSpec<Float> =
        MotionSchemeKeyTokens.DefaultSpatial.value()
    val showMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.DefaultSpatial.value()
    val hideMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.FastEffects.value()

    SideEffect {
        sheetState.showMotionSpec = showMotion
        sheetState.hideMotionSpec = hideMotion
        sheetState.anchoredDraggableMotionSpec = anchoredDraggableMotion
    }
    val scope = rememberCoroutineScope()
    val animateToDismiss: () -> Unit = {
        if (sheetState.confirmValueChange(Hidden)) {
            scope
                .launch { sheetState.hide() }
                .invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismissRequest()
                    }
                }
        }
    }

    val settleToDismiss: () -> Unit = {
        if (sheetState.currentValue == Expanded && sheetState.hasPartiallyExpandedState) {
            // Smoothly animate away predictive back transformations since we are not fully
            // dismissing. We don't need to do this in the else below because we want to
            // preserve the predictive back transformations (scale) during the hide animation.
            scope.launch { sheetState.partialExpand() }
        } else { // Is expanded without collapsed state or is collapsed.
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
        }
    }

    ModalBottomSheetDialog(
        properties = properties,
        contentColor = contentColor,
        onDismissRequest = settleToDismiss,
    ) {
        Box(modifier = Modifier.fillMaxSize().imePadding().semantics { isTraversalGroup = true }) {
            val sheetWindowInsets = remember(sheetState) { SheetWindowInsets(sheetState) }
            val isScrimVisible: Boolean by remember {
                derivedStateOf { sheetState.targetValue != Hidden }
            }
            val scrimAlpha by
                animateFloatAsState(
                    targetValue = if (isScrimVisible) 1f else 0f,
                    animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
                    label = "ScrimAlphaAnimation",
                )
            Scrim(
                contentDescription = getString(Strings.CloseSheet),
                onClick = if (properties.shouldDismissOnClickOutside) animateToDismiss else null,
                alpha = { scrimAlpha },
                color = scrimColor,
            )
            BottomSheet(
                modifier = modifier.align(TopCenter).consumeWindowInsets(sheetWindowInsets),
                state = sheetState,
                onDismissRequest = onDismissRequest,
                maxWidth = sheetMaxWidth,
                gesturesEnabled = sheetGesturesEnabled,
                backHandlerEnabled = properties.shouldDismissOnBackPress,
                shape = shape,
                containerColor = containerColor,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                dragHandle = dragHandle,
                contentWindowInsets = contentWindowInsets,
                content = content,
            )
        }
    }
    if (sheetState.hasExpandedState) {
        LaunchedEffect(sheetState) { sheetState.show() }
    }
}

/**
 * Properties used to customize the behavior of a [ModalBottomSheet].
 *
 * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing the
 *   back button. If true, pressing the back button will call onDismissRequest.
 * @param shouldDismissOnClickOutside Whether the modal bottom sheet can be dismissed by clicking on
 *   the scrim.
 */
@Immutable
@ExperimentalMaterial3Api
expect class ModalBottomSheetProperties(
    shouldDismissOnBackPress: Boolean = true,
    shouldDismissOnClickOutside: Boolean = true,
) {
    val shouldDismissOnBackPress: Boolean
    val shouldDismissOnClickOutside: Boolean
}

/** Default values for [ModalBottomSheet] */
@Immutable
@ExperimentalMaterial3Api
expect object ModalBottomSheetDefaults {

    /** Properties used to customize the behavior of a [ModalBottomSheet]. */
    val properties: ModalBottomSheetProperties
}

/**
 * Create and [remember] a [SheetState] for [ModalBottomSheet].
 *
 * @param skipPartiallyExpanded Whether the partially expanded state, if the sheet is tall enough,
 *   should be skipped. If true, the sheet will always expand to the [Expanded] state and move to
 *   the [Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberModalBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
) =
    rememberSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
        confirmValueChange = confirmValueChange,
        initialValue = Hidden,
    )

@Stable
@OptIn(ExperimentalMaterial3Api::class)
internal class SheetWindowInsets(private val state: SheetState) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = 0

    override fun getTop(density: Density): Int {
        val offset = state.anchoredDraggableState.offset
        return if (offset.isNaN()) 0 else offset.toInt().coerceAtLeast(0)
    }

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int = 0

    override fun getBottom(density: Density): Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SheetWindowInsets) return false
        return state == other.state
    }

    override fun hashCode(): Int = state.hashCode()
}

/**
 * [Dialog]-like component providing default window behavior for [BottomSheet]. This implementation
 * explicitly provides a full-screen edge to edge layout.
 *
 * The dialog is visible as long as it is part of the composition hierarchy. In order to let the
 * user dismiss the Dialog, the implementation of onDismissRequest should contain a way to remove
 * the dialog from the composition hierarchy.
 *
 * You can add implement a custom [ModalBottomSheet] by leveraging this API alongside [BottomSheet],
 * [draggableAnchoredSheet], and [Scrim]:
 *
 * @sample androidx.compose.material3.samples.ManualModalBottomSheetSample
 * @param onDismissRequest Callback which executes when user tries to dismiss
 *   [ModalBottomSheetDialog].
 * @param contentColor The content color of this dialog. Used to inform the default behavior of the
 *   windows' system bars and content.
 * @param properties [ModalBottomSheetProperties] for further customization of this dialog.
 * @param content The content displayed in this [ModalBottomSheetDialog]. Usually [BottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal expect fun ModalBottomSheetDialog(
    onDismissRequest: () -> Unit = {},
    contentColor: Color = contentColorFor(BottomSheetDefaults.ContainerColor),
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable () -> Unit,
)
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AlertDialog.kt
```kotlin
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

package androidx.compose.material3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.DialogTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * [Material Design basic dialog](https://m3.material.io/components/dialogs/overview)
 *
 * Dialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task.
 *
 * ![Basic dialog
 * image](https://developer.android.com/images/reference/androidx/compose/material3/basic-dialog.png)
 *
 * The dialog will position its buttons, typically [TextButton]s, based on the available space. By
 * default it will try to place them horizontally next to each other and fallback to horizontal
 * placement if not enough space is available.
 *
 * Simple usage:
 *
 * @sample androidx.compose.material3.samples.AlertDialogSample
 *
 * Usage with a "Hero" icon:
 *
 * @sample androidx.compose.material3.samples.AlertDialogWithIconSample
 * @param onDismissRequest called when the user tries to dismiss the Dialog by clicking outside or
 *   pressing the back button. This is not called when the dismiss button is clicked.
 * @param confirmButton button which is meant to confirm a proposed action, thus resolving what
 *   triggered the dialog. The dialog does not set up any events for this button so they need to be
 *   set up by the caller.
 * @param modifier the [Modifier] to be applied to this dialog
 * @param dismissButton button which is meant to dismiss the dialog. The dialog does not set up any
 *   events for this button so they need to be set up by the caller.
 * @param icon optional icon that will appear above the [title] or above the [text], in case a title
 *   was not provided.
 * @param title title which should specify the purpose of the dialog. The title is not mandatory,
 *   because there may be sufficient information inside the [text].
 * @param text text which presents the details regarding the dialog's purpose.
 * @param shape defines the shape of this dialog's container
 * @param containerColor the color used for the background of this dialog. Use [Color.Transparent]
 *   to have no color.
 * @param iconContentColor the content color used for the icon.
 * @param titleContentColor the content color used for the title.
 * @param textContentColor the content color used for the text.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param properties typically platform specific properties to further configure the dialog.
 * @see BasicAlertDialog
 */
@Composable
expect fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
)

/**
 * [Basic alert dialog dialog](https://m3.material.io/components/dialogs/overview)
 *
 * Dialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task.
 *
 * ![Basic dialog
 * image](https://developer.android.com/images/reference/androidx/compose/material3/basic-dialog.png)
 *
 * This basic alert dialog expects an arbitrary content that is defined by the caller. Note that
 * your content will need to define its own styling.
 *
 * By default, the displayed dialog has the minimum height and width that the Material Design spec
 * defines. If required, these constraints can be overwritten by providing a `width` or `height`
 * [Modifier]s.
 *
 * Basic alert dialog usage with custom content:
 *
 * @sample androidx.compose.material3.samples.BasicAlertDialogSample
 * @param onDismissRequest called when the user tries to dismiss the Dialog by clicking outside or
 *   pressing the back button. This is not called when the dismiss button is clicked.
 * @param modifier the [Modifier] to be applied to this dialog's content.
 * @param properties typically platform specific properties to further configure the dialog.
 * @param content the content of the dialog
 */
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
@ExperimentalMaterial3Api
@Composable
fun BasicAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    with(LocalBasicAlertDialogOverride.current) {
        BasicAlertDialogOverrideScope(
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                properties = properties,
                content = content,
            )
            .BasicAlertDialog()
    }
}

/**
 * This override provides the default behavior of the [BasicAlertDialog] component.
 *
 * [BasicAlertDialogOverride] used when no override is specified.
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ComponentOverrideApi
object DefaultBasicAlertDialogOverride : BasicAlertDialogOverride {
    @Composable
    override fun BasicAlertDialogOverrideScope.BasicAlertDialog() {
        Dialog(onDismissRequest = onDismissRequest, properties = properties) {
            val dialogPaneDescription = getString(Strings.Dialog)
            Box(
                modifier =
                    modifier
                        .sizeIn(minWidth = DialogMinWidth, maxWidth = DialogMaxWidth)
                        .then(Modifier.semantics { paneTitle = dialogPaneDescription }),
                propagateMinConstraints = true,
            ) {
                content()
            }
        }
    }
}

/**
 * [Basic alert dialog dialog](https://m3.material.io/components/dialogs/overview)
 *
 * Dialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task.
 *
 * ![Basic dialog
 * image](https://developer.android.com/images/reference/androidx/compose/material3/basic-dialog.png)
 *
 * This basic alert dialog expects an arbitrary content that is defined by the caller. Note that
 * your content will need to define its own styling.
 *
 * By default, the displayed dialog has the minimum height and width that the Material Design spec
 * defines. If required, these constraints can be overwritten by providing a `width` or `height`
 * [Modifier]s.
 *
 * Basic alert dialog usage with custom content:
 *
 * @sample androidx.compose.material3.samples.BasicAlertDialogSample
 * @param onDismissRequest called when the user tries to dismiss the Dialog by clicking outside or
 *   pressing the back button. This is not called when the dismiss button is clicked.
 * @param modifier the [Modifier] to be applied to this dialog's content.
 * @param properties typically platform specific properties to further configure the dialog.
 * @param content the content of the dialog
 */
@Deprecated(
    "Use BasicAlertDialog instead",
    replaceWith = ReplaceWith("BasicAlertDialog(onDismissRequest, modifier, properties, content)"),
)
@ExperimentalMaterial3Api
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) = BasicAlertDialog(onDismissRequest, modifier, properties, content)

/** Contains default values used for [AlertDialog] and [BasicAlertDialog]. */
object AlertDialogDefaults {
    /** The default shape for alert dialogs */
    val shape: Shape
        @Composable get() = DialogTokens.ContainerShape.value

    /** The default container color for alert dialogs */
    val containerColor: Color
        @Composable get() = DialogTokens.ContainerColor.value

    /** The default icon color for alert dialogs */
    val iconContentColor: Color
        @Composable get() = DialogTokens.IconColor.value

    /** The default title color for alert dialogs */
    val titleContentColor: Color
        @Composable get() = DialogTokens.HeadlineColor.value

    /** The default text color for alert dialogs */
    val textContentColor: Color
        @Composable get() = DialogTokens.SupportingTextColor.value

    /** The default tonal elevation for alert dialogs */
    val TonalElevation: Dp = 0.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlertDialogImpl(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier,
    dismissButton: @Composable (() -> Unit)?,
    icon: @Composable (() -> Unit)?,
    title: @Composable (() -> Unit)?,
    text: @Composable (() -> Unit)?,
    shape: Shape,
    containerColor: Color,
    iconContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color,
    tonalElevation: Dp,
    properties: DialogProperties,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
    ) {
        AlertDialogContent(
            buttons = {
                val buttonPaddingFromMICS =
                    LocalMinimumInteractiveComponentSize.current.takeOrElse { 0.dp } -
                        ButtonDefaults.MinHeight
                AlertDialogFlowRow(
                    mainAxisSpacing = ButtonsMainAxisSpacing,
                    crossAxisSpacing =
                        (ButtonsCrossAxisSpacing - buttonPaddingFromMICS).coerceIn(
                            0.dp,
                            ButtonsCrossAxisSpacing,
                        ),
                ) {
                    confirmButton()
                    dismissButton?.invoke()
                }
            },
            icon = icon,
            title = title,
            text = text,
            shape = shape,
            containerColor = containerColor,
            tonalElevation = tonalElevation,
            // Note that a button content color is provided here from the dialog's token, but in
            // most cases, TextButtons should be used for dismiss and confirm buttons. TextButtons
            // will not consume this provided content color value, and will used their own defined
            // or default colors.
            buttonContentColor = DialogTokens.ActionLabelTextColor.value,
            iconContentColor = iconContentColor,
            titleContentColor = titleContentColor,
            textContentColor = textContentColor,
        )
    }
}

@Composable
internal fun AlertDialogContent(
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)?,
    title: (@Composable () -> Unit)?,
    text: @Composable (() -> Unit)?,
    shape: Shape,
    containerColor: Color,
    tonalElevation: Dp,
    buttonContentColor: Color,
    iconContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
    ) {
        Column(modifier = Modifier.padding(DialogPadding)) {
            icon?.let {
                CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                    Box(Modifier.padding(IconPadding).align(Alignment.CenterHorizontally)) {
                        icon()
                    }
                }
            }
            title?.let {
                ProvideContentColorTextStyle(
                    contentColor = titleContentColor,
                    textStyle = DialogTokens.HeadlineFont.value,
                ) {
                    Box(
                        // Align the title to the center when an icon is present.
                        Modifier.padding(TitlePadding)
                            .align(
                                if (icon == null) {
                                    Alignment.Start
                                } else {
                                    Alignment.CenterHorizontally
                                }
                            )
                    ) {
                        title()
                    }
                }
            }
            text?.let {
                val textStyle = DialogTokens.SupportingTextFont.value
                ProvideContentColorTextStyle(
                    contentColor = textContentColor,
                    textStyle = textStyle,
                ) {
                    Box(
                        Modifier.weight(weight = 1f, fill = false)
                            .padding(TextPadding)
                            .align(Alignment.Start)
                    ) {
                        text()
                    }
                }
            }
            Box(modifier = Modifier.align(Alignment.End)) {
                val textStyle = DialogTokens.ActionLabelTextFont.value
                ProvideContentColorTextStyle(
                    contentColor = buttonContentColor,
                    textStyle = textStyle,
                    content = buttons,
                )
            }
        }
    }
}

/**
 * [FlowRow] for dialog buttons. The confirm button is expected to be the first child of [content].
 */
@Composable
internal fun AlertDialogFlowRow(
    mainAxisSpacing: Dp,
    crossAxisSpacing: Dp,
    content: @Composable () -> Unit,
) {
    val originalLayoutDirection = LocalLayoutDirection.current
    // The confirm button comes BEFORE the dismiss button when stacked vertically,
    // but AFTER the dismiss button when stacked horizontally.
    CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDirection.flip()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(mainAxisSpacing),
            verticalArrangement = Arrangement.spacedBy(crossAxisSpacing),
        ) {
            CompositionLocalProvider(
                LocalLayoutDirection provides originalLayoutDirection,
                content = content,
            )
        }
    }
}

private fun LayoutDirection.flip(): LayoutDirection =
    when (this) {
        LayoutDirection.Ltr -> LayoutDirection.Rtl
        LayoutDirection.Rtl -> LayoutDirection.Ltr
    }

internal val DialogMinWidth = 280.dp
internal val DialogMaxWidth = 560.dp

private val ButtonsMainAxisSpacing = 8.dp
private val ButtonsCrossAxisSpacing = 8.dp

// Paddings for each of the dialog's parts.
private val DialogPadding = PaddingValues(all = 24.dp)
private val IconPadding = PaddingValues(bottom = 16.dp)
private val TitlePadding = PaddingValues(bottom = 16.dp)
private val TextPadding = PaddingValues(bottom = 24.dp)

/**
 * Interface that allows libraries to override the behavior of the [BasicAlertDialog] component.
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [LocalBasicAlertDialogOverride] in the Compose hierarchy.
 */
@ExperimentalMaterial3ComponentOverrideApi
interface BasicAlertDialogOverride {
    /** Behavior function that is called by the [BasicAlertDialog] component. */
    @Composable fun BasicAlertDialogOverrideScope.BasicAlertDialog()
}

/**
 * Parameters available to [BasicAlertDialog].
 *
 * @param onDismissRequest called when the user tries to dismiss the Dialog by clicking outside or
 *   pressing the back button. This is not called when the dismiss button is clicked.
 * @param modifier the [Modifier] to be applied to this dialog's content.
 * @param properties typically platform specific properties to further configure the dialog.
 * @param content the content of the dialog
 */
@ExperimentalMaterial3ComponentOverrideApi
class BasicAlertDialogOverrideScope
internal constructor(
    val onDismissRequest: () -> Unit,
    val modifier: Modifier = Modifier,
    val properties: DialogProperties = DialogProperties(),
    val content: @Composable () -> Unit,
)

/** CompositionLocal containing the currently-selected [BasicAlertDialogOverride]. */
@ExperimentalMaterial3ComponentOverrideApi
val LocalBasicAlertDialogOverride: ProvidableCompositionLocal<BasicAlertDialogOverride> =
    compositionLocalOf {
        DefaultBasicAlertDialogOverride
    }
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Snackbar.kt
```kotlin
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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.internal.Icons
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.SnackbarTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.max
import kotlin.math.min

/**
 * [Material Design snackbar](https://m3.material.io/components/snackbar/overview)
 *
 * Snackbars provide brief messages about app processes at the bottom of the screen.
 *
 * ![Snackbar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/snackbar.png)
 *
 * Snackbars inform users of a process that an app has performed or will perform. They appear
 * temporarily, towards the bottom of the screen. They shouldn’t interrupt the user experience, and
 * they don’t require user input to disappear.
 *
 * A Snackbar can contain a single action. "Dismiss" or "cancel" actions are optional.
 *
 * Snackbars with an action should not timeout or self-dismiss until the user performs another
 * action. Here, moving the keyboard focus indicator to navigate through interactive elements in a
 * page is not considered an action.
 *
 * This component provides only the visuals of the Snackbar. If you need to show a Snackbar with
 * defaults on the screen, use [SnackbarHostState.showSnackbar]:
 *
 * @sample androidx.compose.material3.samples.ScaffoldWithSimpleSnackbar
 *
 * If you want to customize appearance of the Snackbar, you can pass your own version as a child of
 * the [SnackbarHost] to the [Scaffold]:
 *
 * @sample androidx.compose.material3.samples.ScaffoldWithCustomSnackbar
 *
 * For a multiline sample following the Material recommended spec of a maximum of 2 lines, see:
 *
 * @sample androidx.compose.material3.samples.ScaffoldWithMultilineSnackbar
 * @param modifier the [Modifier] to be applied to this snackbar
 * @param action action / button component to add as an action to the snackbar. Consider using
 *   [ColorScheme.inversePrimary] as the color for the action, if you do not have a predefined color
 *   you wish to use instead.
 * @param dismissAction action / button component to add as an additional close affordance action
 *   when a snackbar is non self-dismissive. Consider using [ColorScheme.inverseOnSurface] as the
 *   color for the action, if you do not have a predefined color you wish to use instead.
 * @param actionOnNewLine whether or not action should be put on a separate line. Recommended for
 *   action with long action text.
 * @param shape defines the shape of this snackbar's container
 * @param containerColor the color used for the background of this snackbar. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this snackbar
 * @param actionContentColor the preferred content color for the optional [action] inside this
 *   snackbar
 * @param dismissActionContentColor the preferred content color for the optional [dismissAction]
 *   inside this snackbar
 * @param content content to show information about a process that an app has performed or will
 *   perform
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Snackbar(
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    dismissAction: @Composable (() -> Unit)? = null,
    actionOnNewLine: Boolean = false,
    shape: Shape = SnackbarDefaults.shape,
    containerColor: Color = SnackbarDefaults.color,
    contentColor: Color = SnackbarDefaults.contentColor,
    actionContentColor: Color = SnackbarDefaults.actionContentColor,
    dismissActionContentColor: Color = SnackbarDefaults.dismissActionContentColor,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = SnackbarTokens.ContainerElevation,
    ) {
        val textStyle = SnackbarTokens.SupportingTextFont.value
        val actionTextStyle = SnackbarTokens.ActionLabelTextFont.value
        CompositionLocalProvider(LocalTextStyle provides textStyle) {
            when {
                actionOnNewLine && action != null ->
                    if (ComposeMaterial3Flags.isSnackbarStylingFixEnabled) {
                        NewLineButtonSnackbar(
                            text = content,
                            action = action,
                            dismissAction = dismissAction,
                            actionTextStyle = actionTextStyle,
                            actionContentColor = actionContentColor,
                            dismissActionContentColor = dismissActionContentColor,
                        )
                    } else {
                        LegacyNewLineButtonSnackbar(
                            text = content,
                            action = action,
                            dismissAction = dismissAction,
                            actionTextStyle = actionTextStyle,
                            actionContentColor = actionContentColor,
                            dismissActionContentColor = dismissActionContentColor,
                        )
                    }

                else ->
                    if (ComposeMaterial3Flags.isSnackbarStylingFixEnabled) {
                        OneRowSnackbar(
                            text = content,
                            action = action,
                            dismissAction = dismissAction,
                            actionTextStyle = actionTextStyle,
                            actionTextColor = actionContentColor,
                            dismissActionColor = dismissActionContentColor,
                        )
                    } else {
                        LegacyOneRowSnackbar(
                            text = content,
                            action = action,
                            dismissAction = dismissAction,
                            actionTextStyle = actionTextStyle,
                            actionTextColor = actionContentColor,
                            dismissActionColor = dismissActionContentColor,
                        )
                    }
            }
        }
    }
}

/**
 * [Material Design snackbar](https://m3.material.io/components/snackbar/overview)
 *
 * Snackbars provide brief messages about app processes at the bottom of the screen.
 *
 * ![Snackbar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/snackbar.png)
 *
 * Snackbars inform users of a process that an app has performed or will perform. They appear
 * temporarily, towards the bottom of the screen. They shouldn’t interrupt the user experience, and
 * they don’t require user input to disappear.
 *
 * A Snackbar can contain a single action. "Dismiss" or "cancel" actions are optional.
 *
 * Snackbars with an action should not timeout or self-dismiss until the user performs another
 * action. Here, moving the keyboard focus indicator to navigate through interactive elements in a
 * page is not considered an action.
 *
 * This version of snackbar is designed to work with [SnackbarData] provided by the [SnackbarHost],
 * which is usually used inside of the [Scaffold].
 *
 * This components provides only the visuals of the Snackbar. If you need to show a Snackbar with
 * defaults on the screen, use [SnackbarHostState.showSnackbar]:
 *
 * @sample androidx.compose.material3.samples.ScaffoldWithSimpleSnackbar
 *
 * If you want to customize appearance of the Snackbar, you can pass your own version as a child of
 * the [SnackbarHost] to the [Scaffold]:
 *
 * @sample androidx.compose.material3.samples.ScaffoldWithCustomSnackbar
 *
 * When a [SnackbarData.visuals] sets the Snackbar's duration as [SnackbarDuration.Indefinite], it's
 * recommended to display an additional close affordance action. See
 * [SnackbarVisuals.withDismissAction]:
 *
 * @sample androidx.compose.material3.samples.ScaffoldWithIndefiniteSnackbar
 * @param snackbarData data about the current snackbar showing via [SnackbarHostState]
 * @param modifier the [Modifier] to be applied to this snackbar
 * @param actionOnNewLine whether or not action should be put on a separate line. Recommended for
 *   action with long action text.
 * @param shape defines the shape of this snackbar's container
 * @param containerColor the color used for the background of this snackbar. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this snackbar
 * @param actionColor the color of the snackbar's action
 * @param actionContentColor the preferred content color for the optional action inside this
 *   snackbar. See [SnackbarVisuals.actionLabel].
 * @param dismissActionContentColor the preferred content color for the optional dismiss action
 *   inside this snackbar. See [SnackbarVisuals.withDismissAction].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Snackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = SnackbarDefaults.shape,
    containerColor: Color = SnackbarDefaults.color,
    contentColor: Color = SnackbarDefaults.contentColor,
    actionColor: Color = SnackbarDefaults.actionColor,
    actionContentColor: Color = SnackbarDefaults.actionContentColor,
    dismissActionContentColor: Color = SnackbarDefaults.dismissActionContentColor,
) {
    val actionLabel = snackbarData.visuals.actionLabel
    val actionComposable: (@Composable () -> Unit)? =
        if (actionLabel != null) {
            @Composable {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
                    onClick = { snackbarData.performAction() },
                    content = { Text(actionLabel) },
                )
            }
        } else {
            null
        }
    val dismissActionComposable: (@Composable () -> Unit)? =
        if (snackbarData.visuals.withDismissAction) {
            @Composable {
                val contentDescription = getString(Strings.SnackbarDismiss)
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                    tooltip = { PlainTooltip { Text(contentDescription) } },
                    state = rememberTooltipState(),
                ) {
                    IconButton(
                        onClick = { snackbarData.dismiss() },
                        content = {
                            Icon(Icons.Filled.Close, contentDescription = contentDescription)
                        },
                    )
                }
            }
        } else {
            null
        }
    Snackbar(
        modifier = modifier.padding(12.dp),
        action = actionComposable,
        dismissAction = dismissActionComposable,
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        actionContentColor = actionContentColor,
        dismissActionContentColor = dismissActionContentColor,
        content = { Text(snackbarData.visuals.message) },
    )
}

@Composable
private fun NewLineButtonSnackbar(
    text: @Composable () -> Unit,
    action: @Composable () -> Unit,
    dismissAction: @Composable (() -> Unit)?,
    actionTextStyle: TextStyle,
    actionContentColor: Color,
    dismissActionContentColor: Color,
) {
    Column(
        modifier =
            Modifier.widthIn(max = ContainerMaxWidth)
                .fillMaxWidth()
                .padding(start = HorizontalSpacing)
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(vertical = SnackbarVerticalPadding)
                    .padding(end = HorizontalSpacing)
        ) {
            text()
        }

        Row(
            modifier =
                Modifier.align(Alignment.End)
                    .padding(
                        bottom = ActionButtonBottomPadding,
                        end = if (dismissAction == null) HorizontalSpacingButtonSide else 0.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides actionContentColor,
                LocalTextStyle provides actionTextStyle,
                content = action,
            )
            if (dismissAction != null) {
                CompositionLocalProvider(
                    LocalContentColor provides dismissActionContentColor,
                    content = dismissAction,
                )
            }
        }
    }
}

@Composable
private fun LegacyNewLineButtonSnackbar(
    text: @Composable () -> Unit,
    action: @Composable () -> Unit,
    dismissAction: @Composable (() -> Unit)?,
    actionTextStyle: TextStyle,
    actionContentColor: Color,
    dismissActionContentColor: Color,
) {
    Column(
        modifier =
            Modifier
                // Fill max width, up to ContainerMaxWidth.
                .widthIn(max = ContainerMaxWidth)
                .fillMaxWidth()
                .padding(start = HorizontalSpacing, bottom = SeparateButtonExtraY)
    ) {
        Box(
            Modifier.paddingFromBaseline(HeightToFirstLine, LongButtonVerticalOffset)
                .padding(end = HorizontalSpacingButtonSide)
        ) {
            text()
        }

        Box(
            Modifier.align(Alignment.End)
                .padding(end = if (dismissAction == null) HorizontalSpacingButtonSide else 0.dp)
        ) {
            Row {
                CompositionLocalProvider(
                    LocalContentColor provides actionContentColor,
                    LocalTextStyle provides actionTextStyle,
                    content = action,
                )
                if (dismissAction != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides dismissActionContentColor,
                        content = dismissAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegacyOneRowSnackbar(
    text: @Composable () -> Unit,
    action: @Composable (() -> Unit)?,
    dismissAction: @Composable (() -> Unit)?,
    actionTextStyle: TextStyle,
    actionTextColor: Color,
    dismissActionColor: Color,
) {
    val textTag = "text"
    val actionTag = "action"
    val dismissActionTag = "dismissAction"
    Layout(
        {
            Box(Modifier.layoutId(textTag).padding(vertical = LegacySnackbarVerticalPadding)) {
                text()
            }
            if (action != null) {
                Box(Modifier.layoutId(actionTag)) {
                    CompositionLocalProvider(
                        LocalContentColor provides actionTextColor,
                        LocalTextStyle provides actionTextStyle,
                        content = action,
                    )
                }
            }
            if (dismissAction != null) {
                Box(Modifier.layoutId(dismissActionTag)) {
                    CompositionLocalProvider(
                        LocalContentColor provides dismissActionColor,
                        content = dismissAction,
                    )
                }
            }
        },
        modifier =
            Modifier.padding(
                start = HorizontalSpacing,
                end = if (dismissAction == null) HorizontalSpacingButtonSide else 0.dp,
            ),
    ) { measurables, constraints ->
        val containerWidth = min(constraints.maxWidth, ContainerMaxWidth.roundToPx())
        val actionButtonPlaceable =
            measurables.fastFirstOrNull { it.layoutId == actionTag }?.measure(constraints)
        val dismissButtonPlaceable =
            measurables.fastFirstOrNull { it.layoutId == dismissActionTag }?.measure(constraints)
        val actionButtonWidth = actionButtonPlaceable?.width ?: 0
        val actionButtonHeight = actionButtonPlaceable?.height ?: 0
        val dismissButtonWidth = dismissButtonPlaceable?.width ?: 0
        val dismissButtonHeight = dismissButtonPlaceable?.height ?: 0
        val extraSpacingWidth = if (dismissButtonWidth == 0) TextEndExtraSpacing.roundToPx() else 0
        val textMaxWidth =
            (containerWidth - actionButtonWidth - dismissButtonWidth - extraSpacingWidth)
                .coerceAtLeast(constraints.minWidth)
        val textPlaceable =
            measurables
                .fastFirst { it.layoutId == textTag }
                .measure(constraints.copy(minHeight = 0, maxWidth = textMaxWidth))

        val firstTextBaseline = textPlaceable[FirstBaseline]
        val lastTextBaseline = textPlaceable[LastBaseline]
        val hasText =
            firstTextBaseline != AlignmentLine.Unspecified &&
                lastTextBaseline != AlignmentLine.Unspecified
        val isOneLine = firstTextBaseline == lastTextBaseline || !hasText
        val dismissButtonPlaceX = containerWidth - dismissButtonWidth
        val actionButtonPlaceX = dismissButtonPlaceX - actionButtonWidth

        val textPlaceY: Int
        val containerHeight: Int
        val actionButtonPlaceY: Int
        if (isOneLine) {
            val minContainerHeight = SnackbarTokens.SingleLineContainerHeight.roundToPx()
            val contentHeight = max(actionButtonHeight, dismissButtonHeight)
            containerHeight = max(minContainerHeight, contentHeight)
            textPlaceY = (containerHeight - textPlaceable.height) / 2
            actionButtonPlaceY =
                if (actionButtonPlaceable != null) {
                    actionButtonPlaceable[FirstBaseline].let {
                        if (it != AlignmentLine.Unspecified) {
                            textPlaceY + firstTextBaseline - it
                        } else {
                            0
                        }
                    }
                } else {
                    0
                }
        } else {
            val baselineOffset = HeightToFirstLine.roundToPx()
            textPlaceY = baselineOffset - firstTextBaseline
            val minContainerHeight = SnackbarTokens.TwoLinesContainerHeight.roundToPx()
            val contentHeight = textPlaceY + textPlaceable.height
            containerHeight = max(minContainerHeight, contentHeight)
            actionButtonPlaceY =
                if (actionButtonPlaceable != null) {
                    (containerHeight - actionButtonPlaceable.height) / 2
                } else {
                    0
                }
        }
        val dismissButtonPlaceY =
            if (dismissButtonPlaceable != null) {
                (containerHeight - dismissButtonPlaceable.height) / 2
            } else {
                0
            }

        layout(containerWidth, containerHeight) {
            textPlaceable.placeRelative(0, textPlaceY)
            actionButtonPlaceable?.placeRelative(actionButtonPlaceX, actionButtonPlaceY)
            dismissButtonPlaceable?.placeRelative(dismissButtonPlaceX, dismissButtonPlaceY)
        }
    }
}

@Composable
private fun OneRowSnackbar(
    text: @Composable () -> Unit,
    action: @Composable (() -> Unit)?,
    dismissAction: @Composable (() -> Unit)?,
    actionTextStyle: TextStyle,
    actionTextColor: Color,
    dismissActionColor: Color,
) {
    val textTag = "text"
    val actionTag = "action"
    val dismissActionTag = "dismissAction"
    Layout(
        {
            Box(Modifier.layoutId(textTag).padding(vertical = SnackbarVerticalPadding)) { text() }
            if (action != null) {
                Box(Modifier.layoutId(actionTag)) {
                    CompositionLocalProvider(
                        LocalContentColor provides actionTextColor,
                        LocalTextStyle provides actionTextStyle,
                        content = action,
                    )
                }
            }
            if (dismissAction != null) {
                Box(Modifier.layoutId(dismissActionTag)) {
                    CompositionLocalProvider(
                        LocalContentColor provides dismissActionColor,
                        content = dismissAction,
                    )
                }
            }
        },
        modifier =
            Modifier.padding(
                start = HorizontalSpacing,
                end = if (dismissAction == null) TextEndExtraSpacing else 0.dp,
            ),
    ) { measurables, constraints ->
        val minContainerHeight = SnackbarTokens.SingleLineContainerHeight.roundToPx()
        val containerWidth = min(constraints.maxWidth, ContainerMaxWidth.roundToPx())
        val actionButtonPlaceable =
            measurables.fastFirstOrNull { it.layoutId == actionTag }?.measure(constraints)
        val dismissButtonPlaceable =
            measurables.fastFirstOrNull { it.layoutId == dismissActionTag }?.measure(constraints)
        val actionButtonWidth = actionButtonPlaceable?.width ?: 0
        val dismissButtonWidth = dismissButtonPlaceable?.width ?: 0

        val extraSpacingWidth = if (dismissButtonWidth == 0) TextEndExtraSpacing.roundToPx() else 0
        val textMaxWidth =
            (containerWidth - actionButtonWidth - dismissButtonWidth - extraSpacingWidth)
                .coerceAtLeast(constraints.minWidth)
        val textPlaceable =
            measurables
                .fastFirst { it.layoutId == textTag }
                .measure(constraints.copy(minHeight = 0, maxWidth = textMaxWidth))

        val containerHeight =
            maxOf(
                minContainerHeight,
                textPlaceable.height,
                actionButtonPlaceable?.height ?: 0,
                dismissButtonPlaceable?.height ?: 0,
            )

        val dismissButtonPlaceX = containerWidth - dismissButtonWidth
        val actionButtonPlaceX = dismissButtonPlaceX - actionButtonWidth

        layout(containerWidth, containerHeight) {
            textPlaceable.placeRelative(0, (containerHeight - textPlaceable.height) / 2)
            actionButtonPlaceable?.placeRelative(
                actionButtonPlaceX,
                (containerHeight - actionButtonPlaceable.height) / 2,
            )
            dismissButtonPlaceable?.placeRelative(
                dismissButtonPlaceX,
                (containerHeight - dismissButtonPlaceable.height) / 2,
            )
        }
    }
}

/** Contains the default values used for [Snackbar]. */
object SnackbarDefaults {
    /** Default shape of a snackbar. */
    val shape: Shape
        @Composable get() = SnackbarTokens.ContainerShape.value

    /** Default color of a snackbar. */
    val color: Color
        @Composable get() = SnackbarTokens.ContainerColor.value

    /** Default content color of a snackbar. */
    val contentColor: Color
        @Composable get() = SnackbarTokens.SupportingTextColor.value

    /** Default action color of a snackbar. */
    val actionColor: Color
        @Composable get() = SnackbarTokens.ActionLabelTextColor.value

    /** Default action content color of a snackbar. */
    val actionContentColor: Color
        @Composable get() = SnackbarTokens.ActionLabelTextColor.value

    /** Default dismiss action content color of a snackbar. */
    val dismissActionContentColor: Color
        @Composable get() = SnackbarTokens.IconColor.value
}

private val ContainerMaxWidth = 600.dp
private val HeightToFirstLine = 30.dp
private val HorizontalSpacing = 16.dp
private val HorizontalSpacingButtonSide = 8.dp
private val SeparateButtonExtraY = 2.dp
private val LegacySnackbarVerticalPadding = 6.dp
private val TextEndExtraSpacing = 8.dp
private val LongButtonVerticalOffset = 12.dp
private val SnackbarVerticalPadding = 14.dp
private val ActionButtonBottomPadding = 4.dp
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/FloatingActionButton.kt
```kotlin
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

package androidx.compose.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme.LocalMaterialTheme
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.animateElevation
import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.material3.tokens.ExtendedFabLargeTokens
import androidx.compose.material3.tokens.ExtendedFabMediumTokens
import androidx.compose.material3.tokens.ExtendedFabPrimaryTokens
import androidx.compose.material3.tokens.ExtendedFabSmallTokens
import androidx.compose.material3.tokens.FabBaselineTokens
import androidx.compose.material3.tokens.FabLargeTokens
import androidx.compose.material3.tokens.FabMediumTokens
import androidx.compose.material3.tokens.FabPrimaryContainerTokens
import androidx.compose.material3.tokens.FabSmallTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * [Material Design floating action
 * button](https://m3.material.io/components/floating-action-button/overview)
 *
 * The FAB represents the most important action on a screen. It puts key actions within reach.
 *
 * ![FAB image](https://developer.android.com/images/reference/androidx/compose/material3/fab.png)
 *
 * FAB typically contains an icon, for a FAB with text and an icon, see
 * [ExtendedFloatingActionButton].
 *
 * @sample androidx.compose.material3.samples.FloatingActionButtonSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this FAB, typically an [Icon]
 */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) =
    FloatingActionButton(
        onClick,
        ExtendedFabPrimaryTokens.LabelTextFont.value,
        FabBaselineTokens.ContainerWidth,
        FabBaselineTokens.ContainerHeight,
        modifier,
        shape,
        containerColor,
        contentColor,
        elevation,
        interactionSource,
        content,
    )

@Composable
private fun FloatingActionButton(
    onClick: () -> Unit,
    textStyle: TextStyle,
    minWidth: Dp,
    minHeight: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = elevation.tonalElevation(),
        shadowElevation = elevation.shadowElevation(interactionSource = interactionSource).value,
        interactionSource = interactionSource,
    ) {
        ProvideContentColorTextStyle(contentColor = contentColor, textStyle = textStyle) {
            Box(
                modifier = Modifier.defaultMinSize(minWidth = minWidth, minHeight = minHeight),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}

/**
 * [Material Design small floating action
 * button](https://m3.material.io/components/floating-action-button/overview)
 *
 * The FAB represents the most important action on a screen. It puts key actions within reach.
 *
 * ![Small FAB
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small-fab.png)
 *
 * @sample androidx.compose.material3.samples.SmallFloatingActionButtonSample
 *
 * FABs can also be shown and hidden with an animation when the main content is scrolled:
 *
 * @sample androidx.compose.material3.samples.AnimatedFloatingActionButtonSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this FAB, typically an [Icon]
 */
@Composable
fun SmallFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.smallShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier =
            modifier.sizeIn(
                minWidth = FabSmallTokens.ContainerWidth,
                minHeight = FabSmallTokens.ContainerHeight,
            ),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * [Material Design medium floating action
 * button](https://m3.material.io/components/floating-action-button/overview)
 *
 * The FAB represents the most important action on a screen. It puts key actions within reach.
 *
 * @sample androidx.compose.material3.samples.MediumFloatingActionButtonSample
 *
 * FABs can also be shown and hidden with an animation when the main content is scrolled:
 *
 * @sample androidx.compose.material3.samples.AnimatedFloatingActionButtonSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this FAB, typically an [Icon]
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun MediumFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.mediumShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier =
            modifier.sizeIn(
                minWidth = FabMediumTokens.ContainerWidth,
                minHeight = FabMediumTokens.ContainerHeight,
            ),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * [Material Design large floating action
 * button](https://m3.material.io/components/floating-action-button/overview)
 *
 * The FAB represents the most important action on a screen. It puts key actions within reach.
 *
 * ![Large FAB
 * image](https://developer.android.com/images/reference/androidx/compose/material3/large-fab.png)
 *
 * @sample androidx.compose.material3.samples.LargeFloatingActionButtonSample
 *
 * FABs can also be shown and hidden with an animation when the main content is scrolled:
 *
 * @sample androidx.compose.material3.samples.AnimatedFloatingActionButtonSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this FAB, typically an [Icon]
 */
@Composable
fun LargeFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.largeShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier =
            modifier.sizeIn(
                minWidth = FabLargeTokens.ContainerWidth,
                minHeight = FabLargeTokens.ContainerHeight,
            ),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content,
    )
}

// TODO link to image
/**
 * [Material Design small extended floating action
 * button](https://m3.material.io/components/extended-fab/overview)
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * The other small extended floating action button overload supports a text label and icon.
 *
 * @sample androidx.compose.material3.samples.SmallExtendedFloatingActionButtonTextSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this FAB, typically a [Text] label
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SmallExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.smallExtendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        textStyle = SmallExtendedFabTextStyle.value,
        minWidth = SmallExtendedFabMinimumWidth,
        minHeight = SmallExtendedFabMinimumHeight,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    start = SmallExtendedFabPaddingStart,
                    end = SmallExtendedFabPaddingEnd,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// TODO link to image
/**
 * [Material Design medium extended floating action
 * button](https://m3.material.io/components/extended-fab/overview)
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * The other medium extended floating action button overload supports a text label and icon.
 *
 * @sample androidx.compose.material3.samples.MediumExtendedFloatingActionButtonTextSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this FAB, typically a [Text] label
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun MediumExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.mediumExtendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        textStyle = MediumExtendedFabTextStyle.value,
        minWidth = MediumExtendedFabMinimumWidth,
        minHeight = MediumExtendedFabMinimumHeight,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    start = MediumExtendedFabPaddingStart,
                    end = MediumExtendedFabPaddingEnd,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// TODO link to image
/**
 * [Material Design large extended floating action
 * button](https://m3.material.io/components/extended-fab/overview)
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * The other large extended floating action button overload supports a text label and icon.
 *
 * @sample androidx.compose.material3.samples.LargeExtendedFloatingActionButtonTextSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this FAB, typically a [Text] label
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun LargeExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.largeExtendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        textStyle = LargeExtendedFabTextStyle.value,
        minWidth = LargeExtendedFabMinimumWidth,
        minHeight = LargeExtendedFabMinimumHeight,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    start = LargeExtendedFabPaddingStart,
                    end = LargeExtendedFabPaddingEnd,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * [Material Design extended floating action
 * button](https://m3.material.io/components/extended-fab/overview)
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * ![Extended FAB
 * image](https://developer.android.com/images/reference/androidx/compose/material3/extended-fab.png)
 *
 * The other extended floating action button overload supports a text label and icon.
 *
 * @sample androidx.compose.material3.samples.ExtendedFloatingActionButtonTextSample
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this FAB, typically a [Text] label
 */
@Composable
fun ExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier =
                Modifier.sizeIn(minWidth = ExtendedFabMinimumWidth)
                    .padding(horizontal = ExtendedFabTextPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * [Material Design small extended floating action
 * button](https://m3.material.io/components/extended-fab/overview)
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * The other small extended floating action button overload is for FABs without an icon.
 *
 * Default content description for accessibility is extended from the extended fabs icon. For custom
 * behavior, you can provide your own via [Modifier.semantics].
 *
 * @sample androidx.compose.material3.samples.SmallExtendedFloatingActionButtonSample
 * @sample androidx.compose.material3.samples.SmallAnimatedExtendedFloatingActionButtonSample
 * @param text label displayed inside this FAB
 * @param icon icon for this FAB, typically an [Icon]
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param expanded controls the expansion state of this FAB. In an expanded state, the FAB will show
 *   both the [icon] and [text]. In a collapsed state, the FAB will show only the [icon].
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SmallExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.smallExtendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) =
    ExtendedFloatingActionButton(
        text = text,
        icon = icon,
        onClick = onClick,
        textStyle = SmallExtendedFabTextStyle.value,
        minWidth = SmallExtendedFabMinimumWidth,
        minHeight = SmallExtendedFabMinimumHeight,
        startPadding = SmallExtendedFabPaddingStart,
        endPadding = SmallExtendedFabPaddingEnd,
        iconPadding = SmallExtendedFabIconPadding,
        modifier = modifier,
        expanded = expanded,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    )

/**
 * [Material Design medium extended floating action
 * button](https://m3.material.io/components/extended-fab/overview)
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * The other medium extended floating action button overload is for FABs without an icon.
 *
 * Default content description for accessibility is extended from the extended fabs icon. For custom
 * behavior, you can provide your own via [Modifier.semantics].
 *
 * @sample androidx.compose.material3.samples.MediumExtendedFloatingActionButtonSample
 * @sample androidx.compose.material3.samples.MediumAnimatedExtendedFloatingActionButtonSample
 * @param text label displayed inside this FAB
 * @param icon icon for this FAB, typically an [Icon]
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param expanded controls the expansion state of this FAB. In an expanded state, the FAB will show
 *   both the [icon] and [text]. In a collapsed state, the FAB will show only the [icon].
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun MediumExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.mediumExtendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) =
    ExtendedFloatingActionButton(
        text = text,
        icon = icon,
        onClick = onClick,
        textStyle = MediumExtendedFabTextStyle.value,
        minWidth = MediumExtendedFabMinimumWidth,
        minHeight = MediumExtendedFabMinimumHeight,
        startPadding = MediumExtendedFabPaddingStart,
        endPadding = MediumExtendedFabPaddingEnd,
        iconPadding = MediumExtendedFabIconPadding,
        modifier = modifier,
        expanded = expanded,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    )

/**
 * [Material Design large extended floating action
 * button](https://m3.material.io/components/extended-fab/overview)
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * The other large extended floating action button overload is for FABs without an icon.
 *
 * Default content description for accessibility is extended from the extended fabs icon. For custom
 * behavior, you can provide your own via [Modifier.semantics].
 *
 * @sample androidx.compose.material3.samples.LargeExtendedFloatingActionButtonSample
 * @sample androidx.compose.material3.samples.LargeAnimatedExtendedFloatingActionButtonSample
 * @param text label displayed inside this FAB
 * @param icon icon for this FAB, typically an [Icon]
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param expanded controls the expansion state of this FAB. In an expanded state, the FAB will show
 *   both the [icon] and [text]. In a collapsed state, the FAB will show only the [icon].
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun LargeExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.largeExtendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) =
    ExtendedFloatingActionButton(
        text = text,
        icon = icon,
        onClick = onClick,
        textStyle = LargeExtendedFabTextStyle.value,
        minWidth = LargeExtendedFabMinimumWidth,
        minHeight = LargeExtendedFabMinimumHeight,
        startPadding = LargeExtendedFabPaddingStart,
        endPadding = LargeExtendedFabPaddingEnd,
        iconPadding = LargeExtendedFabIconPadding,
        modifier = modifier,
        expanded = expanded,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    )

/**
 * [Material Design extended floating action
 * button](https://m3.material.io/components/extended-fab/overview)
 *
 * Extended FABs help people take primary actions. They're wider than FABs to accommodate a text
 * label and larger target area.
 *
 * ![Extended FAB
 * image](https://developer.android.com/images/reference/androidx/compose/material3/extended-fab.png)
 *
 * The other extended floating action button overload is for FABs without an icon.
 *
 * Default content description for accessibility is extended from the extended fabs icon. For custom
 * behavior, you can provide your own via [Modifier.semantics].
 *
 * @sample androidx.compose.material3.samples.ExtendedFloatingActionButtonSample
 * @sample androidx.compose.material3.samples.AnimatedExtendedFloatingActionButtonSample
 * @param text label displayed inside this FAB
 * @param icon icon for this FAB, typically an [Icon]
 * @param onClick called when this FAB is clicked
 * @param modifier the [Modifier] to be applied to this FAB
 * @param expanded controls the expansion state of this FAB. In an expanded state, the FAB will show
 *   both the [icon] and [text]. In a collapsed state, the FAB will show only the [icon].
 * @param shape defines the shape of this FAB's container and shadow (when using [elevation])
 * @param containerColor the color used for the background of this FAB. Use [Color.Transparent] to
 *   have no color.
 * @param contentColor the preferred color for content inside this FAB. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB in
 *   different states. This controls the size of the shadow below the FAB. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this FAB. You can use this to change the FAB's appearance or
 *   preview the FAB in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun ExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        val startPadding = if (expanded) ExtendedFabStartIconPadding else 0.dp
        val endPadding = if (expanded) ExtendedFabTextPadding else 0.dp

        Row(
            modifier =
                Modifier.sizeIn(
                        minWidth =
                            if (expanded) {
                                ExtendedFabMinimumWidth
                            } else {
                                FabBaselineTokens.ContainerWidth
                            }
                    )
                    .padding(start = startPadding, end = endPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
        ) {
            icon()
            AnimatedVisibility(
                visible = expanded,
                enter = extendedFabExpandAnimation(),
                exit = extendedFabCollapseAnimation(),
            ) {
                Row(Modifier.clearAndSetSemantics {}) {
                    Spacer(Modifier.width(ExtendedFabEndIconPadding))
                    text()
                }
            }
        }
    }
}

@Composable
private fun ExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    textStyle: TextStyle,
    minWidth: Dp,
    minHeight: Dp,
    startPadding: Dp,
    endPadding: Dp,
    iconPadding: Dp,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) {
    FloatingActionButton(
        onClick = onClick,
        textStyle = textStyle,
        minWidth = Dp.Unspecified,
        minHeight = Dp.Unspecified,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    ) {
        val expandTransition = updateTransition(if (expanded) 1f else 0f, label = "expanded state")
        // TODO Load the motionScheme tokens from the component tokens file
        val sizeAnimationSpec = MotionSchemeKeyTokens.FastSpatial.value<Float>()
        val opacityAnimationSpec = MotionSchemeKeyTokens.FastEffects.value<Float>()
        val expandedWidthProgress =
            expandTransition.animateFloat(transitionSpec = { sizeAnimationSpec }) { it }
        val expandedAlphaProgress =
            expandTransition.animateFloat(transitionSpec = { opacityAnimationSpec }) { it }
        Row(
            modifier =
                Modifier.layout { measurable, constraints ->
                        val expandedWidth = measurable.maxIntrinsicWidth(constraints.maxHeight)
                        val width =
                            lerp(minWidth.roundToPx(), expandedWidth, expandedWidthProgress.value)
                        val placeable = measurable.measure(constraints)
                        layout(width, placeable.height) { placeable.place(0, 0) }
                    }
                    .sizeIn(minWidth = minWidth, minHeight = minHeight)
                    .padding(start = startPadding, end = endPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            val fullyCollapsed =
                remember(expandTransition) {
                    derivedStateOf {
                        expandTransition.currentState == 0f && !expandTransition.isRunning
                    }
                }
            if (!fullyCollapsed.value) {
                Row(
                    Modifier.clearAndSetSemantics {}
                        .graphicsLayer { alpha = expandedAlphaProgress.value }
                ) {
                    Spacer(Modifier.width(iconPadding))
                    text()
                }
            }
        }
    }
}

/** Contains the default values used by [FloatingActionButton] */
object FloatingActionButtonDefaults {
    internal val ShowHideTargetScale = 0.2f

    /** The recommended size of the icon inside a [MediumFloatingActionButton]. */
    @ExperimentalMaterial3ExpressiveApi val MediumIconSize = FabMediumTokens.IconSize

    /** The recommended size of the icon inside a [LargeFloatingActionButton]. */
    val LargeIconSize = 36.dp // TODO: FabLargeTokens.IconSize is incorrect

    /** Default shape for a floating action button. */
    val shape: Shape
        @Composable get() = FabBaselineTokens.ContainerShape.value

    /** Default shape for a small floating action button. */
    val smallShape: Shape
        @Composable get() = FabSmallTokens.ContainerShape.value

    /** Default shape for a medium floating action button. */
    @ExperimentalMaterial3ExpressiveApi
    val mediumShape: Shape
        @Composable get() = ShapeDefaults.LargeIncreased // TODO: update to use token

    /** Default shape for a large floating action button. */
    val largeShape: Shape
        @Composable get() = FabLargeTokens.ContainerShape.value

    /** Default shape for an extended floating action button. */
    val extendedFabShape: Shape
        @Composable get() = ExtendedFabPrimaryTokens.ContainerShape.value

    /** Default shape for a small extended floating action button. */
    @ExperimentalMaterial3ExpressiveApi
    val smallExtendedFabShape: Shape
        @Composable get() = ExtendedFabSmallTokens.ContainerShape.value

    /** Default shape for a medium extended floating action button. */
    @ExperimentalMaterial3ExpressiveApi
    val mediumExtendedFabShape: Shape
        @Composable get() = ShapeDefaults.LargeIncreased // TODO: update to use token

    /** Default shape for a large extended floating action button. */
    @ExperimentalMaterial3ExpressiveApi
    val largeExtendedFabShape: Shape
        @Composable get() = ExtendedFabLargeTokens.ContainerShape.value

    /** Default container color for a floating action button. */
    val containerColor: Color
        @Composable get() = FabPrimaryContainerTokens.ContainerColor.value

    /**
     * Creates a [FloatingActionButtonElevation] that represents the elevation of a
     * [FloatingActionButton] in different states. For use cases in which a less prominent
     * [FloatingActionButton] is possible consider the [loweredElevation].
     *
     * @param defaultElevation the elevation used when the [FloatingActionButton] has no other
     *   [Interaction]s.
     * @param pressedElevation the elevation used when the [FloatingActionButton] is pressed.
     * @param focusedElevation the elevation used when the [FloatingActionButton] is focused.
     * @param hoveredElevation the elevation used when the [FloatingActionButton] is hovered.
     */
    @Composable
    fun elevation(
        defaultElevation: Dp = FabPrimaryContainerTokens.ContainerElevation,
        pressedElevation: Dp = FabPrimaryContainerTokens.PressedContainerElevation,
        focusedElevation: Dp = FabPrimaryContainerTokens.FocusedContainerElevation,
        hoveredElevation: Dp = FabPrimaryContainerTokens.HoveredContainerElevation,
    ): FloatingActionButtonElevation =
        FloatingActionButtonElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
        )

    /**
     * Use this to create a [FloatingActionButton] with a lowered elevation for less emphasis. Use
     * [elevation] to get a normal [FloatingActionButton].
     *
     * @param defaultElevation the elevation used when the [FloatingActionButton] has no other
     *   [Interaction]s.
     * @param pressedElevation the elevation used when the [FloatingActionButton] is pressed.
     * @param focusedElevation the elevation used when the [FloatingActionButton] is focused.
     * @param hoveredElevation the elevation used when the [FloatingActionButton] is hovered.
     */
    @Composable
    fun loweredElevation(
        defaultElevation: Dp = ElevationTokens.Level1,
        pressedElevation: Dp = ElevationTokens.Level1,
        focusedElevation: Dp = ElevationTokens.Level1,
        hoveredElevation: Dp = ElevationTokens.Level2,
    ): FloatingActionButtonElevation =
        FloatingActionButtonElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
        )

    /**
     * Use this to create a [FloatingActionButton] that represents the default elevation of a
     * [FloatingActionButton] used for [BottomAppBar] in different states.
     *
     * @param defaultElevation the elevation used when the [FloatingActionButton] has no other
     *   [Interaction]s.
     * @param pressedElevation the elevation used when the [FloatingActionButton] is pressed.
     * @param focusedElevation the elevation used when the [FloatingActionButton] is focused.
     * @param hoveredElevation the elevation used when the [FloatingActionButton] is hovered.
     */
    fun bottomAppBarFabElevation(
        defaultElevation: Dp = 0.dp,
        pressedElevation: Dp = 0.dp,
        focusedElevation: Dp = 0.dp,
        hoveredElevation: Dp = 0.dp,
    ): FloatingActionButtonElevation =
        FloatingActionButtonElevation(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
        )
}

/**
 * Apply this modifier to a [FloatingActionButton] to show or hide it with an animation, typically
 * based on the app's main content scrolling.
 *
 * @param visible whether the FAB should be shown or hidden with an animation
 * @param alignment the direction towards which the FAB should be scaled to and from
 * @param targetScale the initial scale value when showing the FAB and the final scale value when
 *   hiding the FAB
 * @param scaleAnimationSpec the [AnimationSpec] to use for the scale part of the animation, if null
 *   the Fast Spatial spring spec from the [MotionScheme] will be used
 * @param alphaAnimationSpec the [AnimationSpec] to use for the alpha part of the animation, if null
 *   the Fast Effects spring spec from the [MotionScheme] will be used
 * @sample androidx.compose.material3.samples.AnimatedFloatingActionButtonSample
 */
@ExperimentalMaterial3ExpressiveApi
fun Modifier.animateFloatingActionButton(
    visible: Boolean,
    alignment: Alignment,
    targetScale: Float = FloatingActionButtonDefaults.ShowHideTargetScale,
    scaleAnimationSpec: AnimationSpec<Float>? = null,
    alphaAnimationSpec: AnimationSpec<Float>? = null,
): Modifier {
    return this.then(
        FabVisibleModifier(
            visible = visible,
            alignment = alignment,
            targetScale = targetScale,
            scaleAnimationSpec = scaleAnimationSpec,
            alphaAnimationSpec = alphaAnimationSpec,
        )
    )
}

internal data class FabVisibleModifier(
    private val visible: Boolean,
    private val alignment: Alignment,
    private val targetScale: Float,
    private val scaleAnimationSpec: AnimationSpec<Float>? = null,
    private val alphaAnimationSpec: AnimationSpec<Float>? = null,
) : ModifierNodeElement<FabVisibleNode>() {

    override fun create(): FabVisibleNode =
        FabVisibleNode(
            visible = visible,
            alignment = alignment,
            targetScale = targetScale,
            scaleAnimationSpec = scaleAnimationSpec,
            alphaAnimationSpec = alphaAnimationSpec,
        )

    override fun update(node: FabVisibleNode) {
        node.updateNode(
            visible = visible,
            alignment = alignment,
            targetScale = targetScale,
            scaleAnimationSpec = scaleAnimationSpec,
            alphaAnimationSpec = alphaAnimationSpec,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}

internal class FabVisibleNode(
    visible: Boolean,
    private var alignment: Alignment,
    private var targetScale: Float,
    private var scaleAnimationSpec: AnimationSpec<Float>? = null,
    private var alphaAnimationSpec: AnimationSpec<Float>? = null,
) : DelegatingNode(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    private val scaleAnimatable = Animatable(if (visible) 1f else 0f)
    private val alphaAnimatable = Animatable(if (visible) 1f else 0f)

    init {
        delegate(
            CacheDrawModifierNode {
                val layer = obtainGraphicsLayer()
                // Use a larger layer size to make sure the elevation shadow doesn't get clipped
                // and offset via layer.topLeft and DrawScope.inset to preserve the visual
                // position of the FAB.
                val layerInsetSize = 16.dp.toPx()
                val layerSize =
                    Size(size.width + layerInsetSize * 2f, size.height + layerInsetSize * 2f)
                        .toIntSize()
                val nodeSize = size.toIntSize()

                layer.apply {
                    topLeft = IntOffset(-layerInsetSize.roundToInt(), -layerInsetSize.roundToInt())

                    alpha = alphaAnimatable.value

                    // Scale towards the direction of the provided alignment
                    val alignOffset = alignment.align(IntSize(1, 1), nodeSize, layoutDirection)
                    pivotOffset = alignOffset.toOffset() + Offset(layerInsetSize, layerInsetSize)
                    scaleX = lerp(targetScale, 1f, scaleAnimatable.value)
                    scaleY = lerp(targetScale, 1f, scaleAnimatable.value)

                    record(size = layerSize) {
                        inset(layerInsetSize, layerInsetSize) { this@record.drawContent() }
                    }
                }

                onDrawWithContent { drawLayer(layer) }
            }
        )
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun updateNode(
        visible: Boolean,
        alignment: Alignment,
        targetScale: Float,
        scaleAnimationSpec: AnimationSpec<Float>?,
        alphaAnimationSpec: AnimationSpec<Float>?,
    ) {
        this.alignment = alignment
        this.targetScale = targetScale
        this.scaleAnimationSpec = scaleAnimationSpec
        this.alphaAnimationSpec = alphaAnimationSpec

        coroutineScope.launch {
            // TODO Load the motionScheme tokens from the component tokens file
            scaleAnimatable.animateTo(
                targetValue = if (visible) 1f else 0f,
                animationSpec =
                    scaleAnimationSpec
                        ?: currentValueOf(LocalMaterialTheme).motionScheme.fastSpatialSpec<Float>(),
            )
        }

        coroutineScope.launch {
            // TODO Load the motionScheme tokens from the component tokens file
            alphaAnimatable.animateTo(
                targetValue = if (visible) 1f else 0f,
                animationSpec =
                    alphaAnimationSpec
                        ?: currentValueOf(LocalMaterialTheme).motionScheme.fastEffectsSpec<Float>(),
            )
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        if (alphaAnimatable.value == 0f) {
            return layout(0, 0) {}
        }
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}

/**
 * Represents the tonal and shadow elevation for a floating action button in different states.
 *
 * See [FloatingActionButtonDefaults.elevation] for the default elevation used in a
 * [FloatingActionButton] and [ExtendedFloatingActionButton].
 */
@Stable
open class FloatingActionButtonElevation
internal constructor(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
) {
    @Composable
    internal fun shadowElevation(interactionSource: InteractionSource): State<Dp> {
        return animateElevation(interactionSource = interactionSource)
    }

    internal fun tonalElevation(): Dp {
        return defaultElevation
    }

    @Composable
    private fun animateElevation(interactionSource: InteractionSource): State<Dp> {
        val animatable =
            remember(interactionSource) {
                FloatingActionButtonElevationAnimatable(
                    defaultElevation = defaultElevation,
                    pressedElevation = pressedElevation,
                    hoveredElevation = hoveredElevation,
                    focusedElevation = focusedElevation,
                )
            }

        LaunchedEffect(this) {
            animatable.updateElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                hoveredElevation = hoveredElevation,
                focusedElevation = focusedElevation,
            )
        }

        LaunchedEffect(interactionSource) {
            val interactions = mutableListOf<Interaction>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }
                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }
                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }
                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                }
                val targetInteraction = interactions.lastOrNull()
                launch { animatable.animateElevation(to = targetInteraction) }
            }
        }

        return animatable.asState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is FloatingActionButtonElevation) return false

        if (defaultElevation != other.defaultElevation) return false
        if (pressedElevation != other.pressedElevation) return false
        if (focusedElevation != other.focusedElevation) return false
        return hoveredElevation == other.hoveredElevation
    }

    override fun hashCode(): Int {
        var result = defaultElevation.hashCode()
        result = 31 * result + pressedElevation.hashCode()
        result = 31 * result + focusedElevation.hashCode()
        result = 31 * result + hoveredElevation.hashCode()
        return result
    }
}

private class FloatingActionButtonElevationAnimatable(
    private var defaultElevation: Dp,
    private var pressedElevation: Dp,
    private var hoveredElevation: Dp,
    private var focusedElevation: Dp,
) {
    private val animatable = Animatable(defaultElevation, Dp.VectorConverter)

    private var lastTargetInteraction: Interaction? = null
    private var targetInteraction: Interaction? = null

    private fun Interaction?.calculateTarget(): Dp {
        return when (this) {
            is PressInteraction.Press -> pressedElevation
            is HoverInteraction.Enter -> hoveredElevation
            is FocusInteraction.Focus -> focusedElevation
            else -> defaultElevation
        }
    }

    suspend fun updateElevation(
        defaultElevation: Dp,
        pressedElevation: Dp,
        hoveredElevation: Dp,
        focusedElevation: Dp,
    ) {
        this.defaultElevation = defaultElevation
        this.pressedElevation = pressedElevation
        this.hoveredElevation = hoveredElevation
        this.focusedElevation = focusedElevation
        snapElevation()
    }

    private suspend fun snapElevation() {
        val target = targetInteraction.calculateTarget()
        if (animatable.targetValue != target) {
            try {
                animatable.snapTo(target)
            } finally {
                lastTargetInteraction = targetInteraction
            }
        }
    }

    suspend fun animateElevation(to: Interaction?) {
        val target = to.calculateTarget()
        // Update the interaction even if the values are the same, for when we change to another
        // interaction later
        targetInteraction = to
        try {
            if (animatable.targetValue != target) {
                animatable.animateElevation(target = target, from = lastTargetInteraction, to = to)
            }
        } finally {
            lastTargetInteraction = to
        }
    }

    fun asState(): State<Dp> = animatable.asState()
}

private val SmallExtendedFabMinimumWidth = ExtendedFabSmallTokens.ContainerHeight
private val SmallExtendedFabMinimumHeight = ExtendedFabSmallTokens.ContainerHeight
private val SmallExtendedFabPaddingStart = ExtendedFabSmallTokens.LeadingSpace
private val SmallExtendedFabPaddingEnd = ExtendedFabSmallTokens.TrailingSpace
private val SmallExtendedFabIconPadding = ExtendedFabSmallTokens.IconLabelSpace
private val SmallExtendedFabTextStyle = TypographyKeyTokens.TitleMedium

private val MediumExtendedFabMinimumWidth = ExtendedFabMediumTokens.ContainerHeight
private val MediumExtendedFabMinimumHeight = ExtendedFabMediumTokens.ContainerHeight
private val MediumExtendedFabPaddingStart = ExtendedFabMediumTokens.LeadingSpace
private val MediumExtendedFabPaddingEnd = ExtendedFabMediumTokens.TrailingSpace
// TODO: ExtendedFabMediumTokens.IconLabelSpace is incorrect
private val MediumExtendedFabIconPadding = 12.dp
private val MediumExtendedFabTextStyle = TypographyKeyTokens.TitleLarge

private val LargeExtendedFabMinimumWidth = ExtendedFabLargeTokens.ContainerHeight
private val LargeExtendedFabMinimumHeight = ExtendedFabLargeTokens.ContainerHeight
private val LargeExtendedFabPaddingStart = ExtendedFabLargeTokens.LeadingSpace
private val LargeExtendedFabPaddingEnd = ExtendedFabLargeTokens.TrailingSpace
// TODO: ExtendedFabLargeTokens.IconLabelSpace is incorrect
private val LargeExtendedFabIconPadding = 16.dp
private val LargeExtendedFabTextStyle = TypographyKeyTokens.HeadlineSmall

private val ExtendedFabStartIconPadding = 16.dp

private val ExtendedFabEndIconPadding = 12.dp

private val ExtendedFabTextPadding = 20.dp

private val ExtendedFabMinimumWidth = 80.dp

@Composable
private fun extendedFabCollapseAnimation() =
    fadeOut(
        // TODO Load the motionScheme tokens from the component tokens file
        animationSpec = MotionSchemeKeyTokens.FastEffects.value()
    ) +
        shrinkHorizontally(
            animationSpec = MotionSchemeKeyTokens.DefaultSpatial.value(),
            shrinkTowards = Alignment.Start,
        )

@Composable
private fun extendedFabExpandAnimation() =
    fadeIn(
        // TODO Load the motionScheme tokens from the component tokens file
        animationSpec = MotionSchemeKeyTokens.DefaultEffects.value()
    ) +
        expandHorizontally(
            animationSpec = MotionSchemeKeyTokens.FastSpatial.value(),
            expandFrom = Alignment.Start,
        )
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Checkbox.kt
```kotlin
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

package androidx.compose.material3

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.tokens.CheckboxTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.floor
import kotlin.math.max

/**
 * [Material Design checkbox](https://m3.material.io/components/checkbox/overview)
 *
 * Checkboxes allow users to select one or more items from a set. Checkboxes can turn an option on
 * or off.
 *
 * ![Checkbox
 * image](https://developer.android.com/images/reference/androidx/compose/material3/checkbox.png)
 *
 * Simple Checkbox sample:
 *
 * @sample androidx.compose.material3.samples.CheckboxSample
 *
 * Combined Checkbox with Text sample:
 *
 * @sample androidx.compose.material3.samples.CheckboxWithTextSample
 * @param checked whether this checkbox is checked or unchecked
 * @param onCheckedChange called when this checkbox is clicked. If `null`, then this checkbox will
 *   not be interactable, unless something else handles its input events and updates its state.
 * @param modifier the [Modifier] to be applied to this checkbox
 * @param enabled controls the enabled state of this checkbox. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [CheckboxColors] that will be used to resolve the colors used for this checkbox in
 *   different states. See [CheckboxDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this checkbox. You can use this to change the checkbox's appearance
 *   or preview the checkbox in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @see [TriStateCheckbox] if you require support for an indeterminate state.
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val strokeWidthPx = with(LocalDensity.current) { floor(CheckboxDefaults.StrokeWidth.toPx()) }
    TriStateCheckbox(
        state = ToggleableState(checked),
        onClick =
            if (onCheckedChange != null) {
                { onCheckedChange(!checked) }
            } else {
                null
            },
        checkmarkStroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Square),
        outlineStroke = Stroke(width = strokeWidthPx),
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}

/**
 * [Material Design checkbox](https://m3.material.io/components/checkbox/overview)
 *
 * Checkboxes allow users to select one or more items from a set. Checkboxes can turn an option on
 * or off.
 *
 * ![Checkbox
 * image](https://developer.android.com/images/reference/androidx/compose/material3/checkbox.png)
 *
 * This Checkbox function offers greater flexibility in visual customization. Using the [Stroke]
 * parameters, you can control the appearance of both the checkmark and the box that surrounds it.
 *
 * A sample of a `Checkbox` that uses a [Stroke] with rounded [StrokeCap] and
 * [androidx.compose.ui.graphics.StrokeJoin]:
 *
 * @sample androidx.compose.material3.samples.CheckboxRoundedStrokesSample
 * @param checked whether this checkbox is checked or unchecked
 * @param onCheckedChange called when this checkbox is clicked. If `null`, then this checkbox will
 *   not be interactable, unless something else handles its input events and updates its state.
 * @param checkmarkStroke stroke for the checkmark.
 * @param outlineStroke stroke for the checkmark's box outline. Note that this stroke is applied
 *   when drawing the outline's rounded rectangle, so attributions such as
 *   [androidx.compose.ui.graphics.StrokeJoin] will be ignored.
 * @param modifier the [Modifier] to be applied to this checkbox
 * @param enabled controls the enabled state of this checkbox. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [CheckboxColors] that will be used to resolve the colors used for this checkbox in
 *   different states. See [CheckboxDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this checkbox. You can use this to change the checkbox's appearance
 *   or preview the checkbox in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @see [TriStateCheckbox] if you require support for an indeterminate state.
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    checkmarkStroke: Stroke,
    outlineStroke: Stroke,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    TriStateCheckbox(
        state = ToggleableState(checked),
        onClick =
            if (onCheckedChange != null) {
                { onCheckedChange(!checked) }
            } else {
                null
            },
        checkmarkStroke = checkmarkStroke,
        outlineStroke = outlineStroke,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}

/**
 * [Material Design checkbox](https://m3.material.io/components/checkbox/guidelines)
 *
 * Checkboxes can have a parent-child relationship with other checkboxes. When the parent checkbox
 * is checked, all child checkboxes are checked. If a parent checkbox is unchecked, all child
 * checkboxes are unchecked. If some, but not all, child checkboxes are checked, the parent checkbox
 * becomes an indeterminate checkbox.
 *
 * ![Checkbox
 * image](https://developer.android.com/images/reference/androidx/compose/material3/indeterminate-checkbox.png)
 *
 * @sample androidx.compose.material3.samples.TriStateCheckboxSample
 * @param state whether this checkbox is checked, unchecked, or in an indeterminate state
 * @param onClick called when this checkbox is clicked. If `null`, then this checkbox will not be
 *   interactable, unless something else handles its input events and updates its [state].
 * @param modifier the [Modifier] to be applied to this checkbox
 * @param enabled controls the enabled state of this checkbox. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [CheckboxColors] that will be used to resolve the colors used for this checkbox in
 *   different states. See [CheckboxDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this checkbox. You can use this to change the checkbox's appearance
 *   or preview the checkbox in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @see [Checkbox] if you want a simple component that represents Boolean state
 */
@Composable
fun TriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val strokeWidthPx = with(LocalDensity.current) { floor(CheckboxDefaults.StrokeWidth.toPx()) }
    TriStateCheckbox(
        state = state,
        onClick = onClick,
        checkmarkStroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Square),
        outlineStroke = Stroke(width = strokeWidthPx),
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}

/**
 * [Material Design checkbox](https://m3.material.io/components/checkbox/guidelines)
 *
 * Checkboxes can have a parent-child relationship with other checkboxes. When the parent checkbox
 * is checked, all child checkboxes are checked. If a parent checkbox is unchecked, all child
 * checkboxes are unchecked. If some, but not all, child checkboxes are checked, the parent checkbox
 * becomes an indeterminate checkbox.
 *
 * ![Checkbox
 * image](https://developer.android.com/images/reference/androidx/compose/material3/indeterminate-checkbox.png)
 *
 * This Checkbox function offers greater flexibility in visual customization. Using the [Stroke]
 * parameters, you can control the appearance of both the checkmark and the box that surrounds it.
 *
 * A sample of a `TriStateCheckbox` that uses a [Stroke] with rounded [StrokeCap] and
 * [androidx.compose.ui.graphics.StrokeJoin]:
 *
 * @sample androidx.compose.material3.samples.TriStateCheckboxRoundedStrokesSample
 * @param state whether this checkbox is checked, unchecked, or in an indeterminate state
 * @param onClick called when this checkbox is clicked. If `null`, then this checkbox will not be
 *   interactable, unless something else handles its input events and updates its [state].
 * @param checkmarkStroke stroke for the checkmark.
 * @param outlineStroke stroke for the checkmark's box outline. Note that this stroke is applied
 *   when drawing the outline's rounded rectangle, so attributions such as
 *   [androidx.compose.ui.graphics.StrokeJoin] will be ignored.
 * @param modifier the [Modifier] to be applied to this checkbox
 * @param enabled controls the enabled state of this checkbox. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [CheckboxColors] that will be used to resolve the colors used for this checkbox in
 *   different states. See [CheckboxDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this checkbox. You can use this to change the checkbox's appearance
 *   or preview the checkbox in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @see [Checkbox] if you want a simple component that represents Boolean state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    checkmarkStroke: Stroke,
    outlineStroke: Stroke,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val isCheckboxStylingFixEnabled = ComposeMaterial3Flags.isCheckboxStylingFixEnabled
    val indication =
        if (isCheckboxStylingFixEnabled)
            ripple(
                bounded = false,
                radius = CheckboxTokens.StateLayerSize / 2,
                color = colors.indicatorColor(state),
            )
        else {
            ripple(bounded = false, radius = CheckboxTokens.StateLayerSize / 2)
        }
    val toggleableModifier =
        if (onClick != null) {
            Modifier.triStateToggleable(
                state = state,
                onClick = onClick,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = indication,
            )
        } else {
            Modifier
        }
    CheckboxImpl(
        enabled = enabled,
        value = state,
        modifier =
            modifier
                .then(
                    if (onClick != null) {
                        Modifier.minimumInteractiveComponentSize()
                    } else {
                        Modifier
                    }
                )
                .then(toggleableModifier)
                .then(
                    if (isCheckboxStylingFixEnabled) {
                        Modifier
                    } else {
                        Modifier.padding(CheckboxDefaultPadding)
                    }
                ),
        colors = colors,
        checkmarkStroke = checkmarkStroke,
        outlineStroke = outlineStroke,
    )
}

/** Defaults used in [Checkbox] and [TriStateCheckbox]. */
object CheckboxDefaults {
    /**
     * Creates a [CheckboxColors] that will animate between the provided colors according to the
     * Material specification.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultCheckboxColors

    /**
     * Creates a [CheckboxColors] that will animate between the provided colors according to the
     * Material specification.
     *
     * @param checkedColor the color that will be used for the border and box when checked
     * @param uncheckedColor color that will be used for the border when unchecked. By default, the
     *   inner box is transparent when unchecked.
     * @param checkmarkColor color that will be used for the checkmark when checked
     * @param disabledCheckedColor color that will be used for the box and border when disabled and
     *   checked
     * @param disabledUncheckedColor color that will be used for the border when disabled and
     *   unchecked. By default, the inner box is transparent when unchecked.
     * @param disabledIndeterminateColor color that will be used for the box and border in a
     *   [TriStateCheckbox] when disabled AND in an [ToggleableState.Indeterminate] state
     */
    @Composable
    fun colors(
        checkedColor: Color = Color.Unspecified,
        uncheckedColor: Color = Color.Unspecified,
        checkmarkColor: Color = Color.Unspecified,
        disabledCheckedColor: Color = Color.Unspecified,
        disabledUncheckedColor: Color = Color.Unspecified,
        disabledIndeterminateColor: Color = Color.Unspecified,
    ): CheckboxColors =
        MaterialTheme.colorScheme.defaultCheckboxColors.copy(
            checkedCheckmarkColor = checkmarkColor,
            uncheckedCheckmarkColor = Color.Transparent,
            disabledCheckmarkColor = checkmarkColor,
            checkedBoxColor = checkedColor,
            uncheckedBoxColor = Color.Transparent,
            disabledCheckedBoxColor = disabledCheckedColor,
            disabledUncheckedBoxColor = Color.Transparent,
            disabledIndeterminateBoxColor = disabledIndeterminateColor,
            checkedBorderColor = checkedColor,
            uncheckedBorderColor = uncheckedColor,
            disabledBorderColor = disabledCheckedColor,
            disabledUncheckedBorderColor = disabledUncheckedColor,
            disabledIndeterminateBorderColor = disabledIndeterminateColor,
        )

    /**
     * Creates a [CheckboxColors] that will animate between the provided colors according to the
     * Material specification.
     *
     * @param checkedCheckmarkColor color that will be used for the checkmark when checked
     * @param uncheckedCheckmarkColor color that will be used for the checkmark when unchecked
     * @param disabledCheckmarkColor color that will be used for the checkmark when disabled
     * @param checkedBoxColor the color that will be used for the box when checked
     * @param uncheckedBoxColor color that will be used for the box when unchecked
     * @param disabledCheckedBoxColor color that will be used for the box when disabled and checked
     * @param disabledUncheckedBoxColor color that will be used for the box when disabled and
     *   unchecked
     * @param disabledIndeterminateBoxColor color that will be used for the box and border in a
     *   [TriStateCheckbox] when disabled AND in an [ToggleableState.Indeterminate] state.
     * @param checkedBorderColor color that will be used for the border when checked
     * @param uncheckedBorderColor color that will be used for the border when unchecked
     * @param disabledBorderColor color that will be used for the border when disabled and checked
     * @param disabledUncheckedBorderColor color that will be used for the border when disabled and
     *   unchecked
     * @param disabledIndeterminateBorderColor color that will be used for the border when disabled
     *   and in an [ToggleableState.Indeterminate] state.
     */
    @Composable
    fun colors(
        checkedCheckmarkColor: Color = Color.Unspecified,
        uncheckedCheckmarkColor: Color = Color.Unspecified,
        disabledCheckmarkColor: Color = Color.Unspecified,
        checkedBoxColor: Color = Color.Unspecified,
        uncheckedBoxColor: Color = Color.Unspecified,
        disabledCheckedBoxColor: Color = Color.Unspecified,
        disabledUncheckedBoxColor: Color = Color.Unspecified,
        disabledIndeterminateBoxColor: Color = Color.Unspecified,
        checkedBorderColor: Color = Color.Unspecified,
        uncheckedBorderColor: Color = Color.Unspecified,
        disabledBorderColor: Color = Color.Unspecified,
        disabledUncheckedBorderColor: Color = Color.Unspecified,
        disabledIndeterminateBorderColor: Color = Color.Unspecified,
    ): CheckboxColors =
        MaterialTheme.colorScheme.defaultCheckboxColors.copy(
            checkedCheckmarkColor = checkedCheckmarkColor,
            uncheckedCheckmarkColor = uncheckedCheckmarkColor,
            disabledCheckmarkColor = disabledCheckmarkColor,
            checkedBoxColor = checkedBoxColor,
            uncheckedBoxColor = uncheckedBoxColor,
            disabledCheckedBoxColor = disabledCheckedBoxColor,
            disabledUncheckedBoxColor = disabledUncheckedBoxColor,
            disabledIndeterminateBoxColor = disabledIndeterminateBoxColor,
            checkedBorderColor = checkedBorderColor,
            uncheckedBorderColor = uncheckedBorderColor,
            disabledBorderColor = disabledBorderColor,
            disabledUncheckedBorderColor = disabledUncheckedBorderColor,
            disabledIndeterminateBorderColor = disabledIndeterminateBorderColor,
        )

    internal val ColorScheme.defaultCheckboxColors: CheckboxColors
        get() {
            return defaultCheckboxColorsCached
                ?: CheckboxColors(
                        checkedCheckmarkColor = fromToken(CheckboxTokens.SelectedIconColor),
                        uncheckedCheckmarkColor = Color.Transparent,
                        disabledCheckmarkColor =
                            fromToken(CheckboxTokens.SelectedDisabledIconColor),
                        checkedBoxColor = fromToken(CheckboxTokens.SelectedContainerColor),
                        uncheckedBoxColor = Color.Transparent,
                        disabledCheckedBoxColor =
                            fromToken(CheckboxTokens.SelectedDisabledContainerColor)
                                .copy(alpha = CheckboxTokens.SelectedDisabledContainerOpacity),
                        disabledUncheckedBoxColor = Color.Transparent,
                        disabledIndeterminateBoxColor =
                            fromToken(CheckboxTokens.SelectedDisabledContainerColor)
                                .copy(alpha = CheckboxTokens.SelectedDisabledContainerOpacity),
                        checkedBorderColor = fromToken(CheckboxTokens.SelectedContainerColor),
                        uncheckedBorderColor = fromToken(CheckboxTokens.UnselectedOutlineColor),
                        disabledBorderColor =
                            fromToken(CheckboxTokens.SelectedDisabledContainerColor)
                                .copy(alpha = CheckboxTokens.SelectedDisabledContainerOpacity),
                        disabledUncheckedBorderColor =
                            fromToken(CheckboxTokens.UnselectedDisabledOutlineColor)
                                .copy(alpha = CheckboxTokens.UnselectedDisabledContainerOpacity),
                        disabledIndeterminateBorderColor =
                            fromToken(CheckboxTokens.SelectedDisabledContainerColor)
                                .copy(alpha = CheckboxTokens.SelectedDisabledContainerOpacity),
                    )
                    .also { defaultCheckboxColorsCached = it }
        }

    /**
     * The default stroke width for a [Checkbox]. This width will be used for the checkmark when the
     * `Checkbox` is in a checked or indeterminate states, or for the outline when it's unchecked.
     */
    val StrokeWidth = 2.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckboxImpl(
    enabled: Boolean,
    value: ToggleableState,
    modifier: Modifier,
    colors: CheckboxColors,
    checkmarkStroke: Stroke,
    outlineStroke: Stroke,
) {
    val isCheckboxStylingFixEnabled = ComposeMaterial3Flags.isCheckboxStylingFixEnabled
    val transition = updateTransition(value)
    val defaultAnimationSpec = MotionSchemeKeyTokens.DefaultSpatial.value<Float>()
    val checkDrawFraction =
        transition.animateFloat(
            transitionSpec = {
                when {
                    // TODO Load the motionScheme tokens from the component tokens file
                    initialState == ToggleableState.Off -> defaultAnimationSpec
                    targetState == ToggleableState.Off -> snap(delayMillis = SnapAnimationDelay)
                    else -> defaultAnimationSpec
                }
            }
        ) {
            when (it) {
                ToggleableState.On -> 1f
                ToggleableState.Off -> 0f
                ToggleableState.Indeterminate -> 1f
            }
        }

    val checkCenterGravitationShiftFraction =
        transition.animateFloat(
            transitionSpec = {
                when {
                    // TODO Load the motionScheme tokens from the component tokens file
                    initialState == ToggleableState.Off -> snap()
                    targetState == ToggleableState.Off -> snap(delayMillis = SnapAnimationDelay)
                    else -> defaultAnimationSpec
                }
            }
        ) {
            when (it) {
                ToggleableState.On -> 0f
                ToggleableState.Off -> 0f
                ToggleableState.Indeterminate -> 1f
            }
        }
    val checkCache = remember { CheckDrawingCache() }
    val checkColor =
        if (isCheckboxStylingFixEnabled) {
            colors.checkmarkColor(enabled, value)
        } else {
            colors.checkmarkColor(value)
        }
    val boxColor = colors.boxColor(enabled, value)
    val borderColor = colors.borderColor(enabled, value)
    val containerSize =
        if (isCheckboxStylingFixEnabled) {
            CheckboxTokens.ContainerSize
        } else {
            CheckboxSize
        }
    Canvas(modifier.wrapContentSize(Alignment.Center).requiredSize(containerSize)) {
        drawBox(
            boxColor = boxColor.value,
            borderColor = borderColor.value,
            radius = RadiusSize.toPx(),
            stroke = outlineStroke,
        )
        drawCheck(
            checkColor = checkColor.value,
            checkFraction = checkDrawFraction.value,
            crossCenterGravitation = checkCenterGravitationShiftFraction.value,
            stroke = checkmarkStroke,
            drawingCache = checkCache,
        )
    }
}

private fun DrawScope.drawBox(boxColor: Color, borderColor: Color, radius: Float, stroke: Stroke) {
    val halfStrokeWidth = stroke.width / 2.0f
    val checkboxSize = size.width
    if (boxColor == borderColor) {
        drawRoundRect(
            boxColor,
            size = Size(checkboxSize, checkboxSize),
            cornerRadius = CornerRadius(radius),
            style = Fill,
        )
    } else {
        drawRoundRect(
            boxColor,
            topLeft = Offset(stroke.width, stroke.width),
            size = Size(checkboxSize - stroke.width * 2, checkboxSize - stroke.width * 2),
            cornerRadius = CornerRadius(max(0f, radius - stroke.width)),
            style = Fill,
        )
        drawRoundRect(
            borderColor,
            topLeft = Offset(halfStrokeWidth, halfStrokeWidth),
            size = Size(checkboxSize - stroke.width, checkboxSize - stroke.width),
            cornerRadius = CornerRadius(radius - halfStrokeWidth),
            style = stroke,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun DrawScope.drawCheck(
    checkColor: Color,
    checkFraction: Float,
    crossCenterGravitation: Float,
    stroke: Stroke,
    drawingCache: CheckDrawingCache,
) {
    val isCheckboxStylingFixEnabled = ComposeMaterial3Flags.isCheckboxStylingFixEnabled
    val width = size.width
    val checkCrossX = 0.4f
    val checkCrossY = if (isCheckboxStylingFixEnabled) 0.65f else 0.7f
    val leftX = if (isCheckboxStylingFixEnabled) 0.25f else 0.2f
    val leftY = 0.5f
    val rightX = if (isCheckboxStylingFixEnabled) 0.75f else 0.8f
    val rightY = 0.3f

    val gravitatedCrossX = lerp(checkCrossX, 0.5f, crossCenterGravitation)
    val gravitatedCrossY = lerp(checkCrossY, 0.5f, crossCenterGravitation)
    // gravitate only Y for end to achieve center line
    val gravitatedLeftY = lerp(leftY, 0.5f, crossCenterGravitation)
    val gravitatedRightY = lerp(rightY, 0.5f, crossCenterGravitation)

    with(drawingCache) {
        checkPath.rewind()
        checkPath.moveTo(width * leftX, width * gravitatedLeftY)
        checkPath.lineTo(width * gravitatedCrossX, width * gravitatedCrossY)
        checkPath.lineTo(width * rightX, width * gravitatedRightY)
        // TODO: replace with proper declarative non-android alternative when ready (b/158188351)
        pathMeasure.setPath(checkPath, false)
        pathToDraw.rewind()
        pathMeasure.getSegment(0f, pathMeasure.length * checkFraction, pathToDraw, true)
    }
    drawPath(drawingCache.pathToDraw, checkColor, style = stroke)
}

@Immutable
private class CheckDrawingCache(
    val checkPath: Path = Path(),
    val pathMeasure: PathMeasure = PathMeasure(),
    val pathToDraw: Path = Path(),
)

/**
 * Represents the colors used by the three different sections (checkmark, box, and border) of a
 * [Checkbox] or [TriStateCheckbox] in different states.
 *
 * @param checkedCheckmarkColor color that will be used for the checkmark when checked
 * @param uncheckedCheckmarkColor color that will be used for the checkmark when unchecked
 * @param checkedBoxColor the color that will be used for the box when checked
 * @param uncheckedBoxColor color that will be used for the box when unchecked
 * @param disabledCheckedBoxColor color that will be used for the box when disabled and checked
 * @param disabledUncheckedBoxColor color that will be used for the box when disabled and unchecked
 * @param disabledIndeterminateBoxColor color that will be used for the box and border in a
 *   [TriStateCheckbox] when disabled AND in an [ToggleableState.Indeterminate] state.
 * @param checkedBorderColor color that will be used for the border when checked
 * @param uncheckedBorderColor color that will be used for the border when unchecked
 * @param disabledBorderColor color that will be used for the border when disabled and checked
 * @param disabledUncheckedBorderColor color that will be used for the border when disabled and
 *   unchecked
 * @param disabledIndeterminateBorderColor color that will be used for the border when disabled and
 *   in an [ToggleableState.Indeterminate] state.
 * @param disabledCheckmarkColor color that will be used for the checkmark when disabled
 * @constructor create an instance with arbitrary colors, see [CheckboxDefaults.colors] for the
 *   default implementation that follows Material specifications.
 */
@Immutable
class CheckboxColors
constructor(
    val checkedCheckmarkColor: Color,
    val uncheckedCheckmarkColor: Color,
    val checkedBoxColor: Color,
    val uncheckedBoxColor: Color,
    val disabledCheckedBoxColor: Color,
    val disabledUncheckedBoxColor: Color,
    val disabledIndeterminateBoxColor: Color,
    val checkedBorderColor: Color,
    val uncheckedBorderColor: Color,
    val disabledBorderColor: Color,
    val disabledUncheckedBorderColor: Color,
    val disabledIndeterminateBorderColor: Color,
    val disabledCheckmarkColor: Color,
) {
    @Deprecated(
        message =
            "This constructor is deprecated. Use the primary constructor that includes 'disabledCheckmarkColor'",
        level = DeprecationLevel.WARNING,
    )
    constructor(
        checkedCheckmarkColor: Color,
        uncheckedCheckmarkColor: Color,
        checkedBoxColor: Color,
        uncheckedBoxColor: Color,
        disabledCheckedBoxColor: Color,
        disabledUncheckedBoxColor: Color,
        disabledIndeterminateBoxColor: Color,
        checkedBorderColor: Color,
        uncheckedBorderColor: Color,
        disabledBorderColor: Color,
        disabledUncheckedBorderColor: Color,
        disabledIndeterminateBorderColor: Color,
    ) : this(
        checkedCheckmarkColor = checkedCheckmarkColor,
        uncheckedCheckmarkColor = uncheckedCheckmarkColor,
        checkedBoxColor = checkedBoxColor,
        uncheckedBoxColor = uncheckedBoxColor,
        disabledCheckedBoxColor = disabledCheckedBoxColor,
        disabledUncheckedBoxColor = disabledUncheckedBoxColor,
        disabledIndeterminateBoxColor = disabledIndeterminateBoxColor,
        checkedBorderColor = checkedBorderColor,
        uncheckedBorderColor = uncheckedBorderColor,
        disabledBorderColor = disabledBorderColor,
        disabledUncheckedBorderColor = disabledUncheckedBorderColor,
        disabledIndeterminateBorderColor = disabledIndeterminateBorderColor,
        disabledCheckmarkColor = checkedCheckmarkColor,
    )

    /**
     * Returns a copy of this CheckboxColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source”
     */
    @Deprecated(
        message =
            "This function is deprecated. Use 'copy' that includes 'disabledCheckmarkColor' instead",
        level = DeprecationLevel.HIDDEN,
    )
    fun copy(
        checkedCheckmarkColor: Color = this.checkedCheckmarkColor,
        uncheckedCheckmarkColor: Color = this.uncheckedCheckmarkColor,
        checkedBoxColor: Color = this.checkedBoxColor,
        uncheckedBoxColor: Color = this.uncheckedBoxColor,
        disabledCheckedBoxColor: Color = this.disabledCheckedBoxColor,
        disabledUncheckedBoxColor: Color = this.disabledUncheckedBoxColor,
        disabledIndeterminateBoxColor: Color = this.disabledIndeterminateBoxColor,
        checkedBorderColor: Color = this.checkedBorderColor,
        uncheckedBorderColor: Color = this.uncheckedBorderColor,
        disabledBorderColor: Color = this.disabledBorderColor,
        disabledUncheckedBorderColor: Color = this.disabledUncheckedBorderColor,
        disabledIndeterminateBorderColor: Color = this.disabledIndeterminateBorderColor,
    ) =
        CheckboxColors(
            checkedCheckmarkColor = checkedCheckmarkColor.takeOrElse { this.checkedCheckmarkColor },
            uncheckedCheckmarkColor =
                uncheckedCheckmarkColor.takeOrElse { this.uncheckedCheckmarkColor },
            checkedBoxColor = checkedBoxColor.takeOrElse { this.checkedBoxColor },
            uncheckedBoxColor = uncheckedBoxColor.takeOrElse { this.uncheckedBoxColor },
            disabledCheckedBoxColor =
                disabledCheckedBoxColor.takeOrElse { this.disabledCheckedBoxColor },
            disabledUncheckedBoxColor =
                disabledUncheckedBoxColor.takeOrElse { this.disabledUncheckedBoxColor },
            disabledIndeterminateBoxColor =
                disabledIndeterminateBoxColor.takeOrElse { this.disabledIndeterminateBoxColor },
            checkedBorderColor = checkedBorderColor.takeOrElse { this.checkedBorderColor },
            uncheckedBorderColor = uncheckedBorderColor.takeOrElse { this.uncheckedBorderColor },
            disabledBorderColor = disabledBorderColor.takeOrElse { this.disabledBorderColor },
            disabledUncheckedBorderColor =
                disabledUncheckedBorderColor.takeOrElse { this.disabledUncheckedBorderColor },
            disabledIndeterminateBorderColor =
                disabledIndeterminateBorderColor.takeOrElse {
                    this.disabledIndeterminateBorderColor
                },
            disabledCheckmarkColor = checkedCheckmarkColor.takeOrElse { this.checkedCheckmarkColor },
        )

    /**
     * Returns a copy of this CheckboxColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        checkedCheckmarkColor: Color = this.checkedCheckmarkColor,
        uncheckedCheckmarkColor: Color = this.uncheckedCheckmarkColor,
        checkedBoxColor: Color = this.checkedBoxColor,
        uncheckedBoxColor: Color = this.uncheckedBoxColor,
        disabledCheckedBoxColor: Color = this.disabledCheckedBoxColor,
        disabledUncheckedBoxColor: Color = this.disabledUncheckedBoxColor,
        disabledIndeterminateBoxColor: Color = this.disabledIndeterminateBoxColor,
        checkedBorderColor: Color = this.checkedBorderColor,
        uncheckedBorderColor: Color = this.uncheckedBorderColor,
        disabledBorderColor: Color = this.disabledBorderColor,
        disabledUncheckedBorderColor: Color = this.disabledUncheckedBorderColor,
        disabledIndeterminateBorderColor: Color = this.disabledIndeterminateBorderColor,
        disabledCheckmarkColor: Color = this.disabledCheckmarkColor,
    ) =
        CheckboxColors(
            checkedCheckmarkColor = checkedCheckmarkColor.takeOrElse { this.checkedCheckmarkColor },
            uncheckedCheckmarkColor =
                uncheckedCheckmarkColor.takeOrElse { this.uncheckedCheckmarkColor },
            checkedBoxColor = checkedBoxColor.takeOrElse { this.checkedBoxColor },
            uncheckedBoxColor = uncheckedBoxColor.takeOrElse { this.uncheckedBoxColor },
            disabledCheckedBoxColor =
                disabledCheckedBoxColor.takeOrElse { this.disabledCheckedBoxColor },
            disabledUncheckedBoxColor =
                disabledUncheckedBoxColor.takeOrElse { this.disabledUncheckedBoxColor },
            disabledIndeterminateBoxColor =
                disabledIndeterminateBoxColor.takeOrElse { this.disabledIndeterminateBoxColor },
            checkedBorderColor = checkedBorderColor.takeOrElse { this.checkedBorderColor },
            uncheckedBorderColor = uncheckedBorderColor.takeOrElse { this.uncheckedBorderColor },
            disabledBorderColor = disabledBorderColor.takeOrElse { this.disabledBorderColor },
            disabledUncheckedBorderColor =
                disabledUncheckedBorderColor.takeOrElse { this.disabledUncheckedBorderColor },
            disabledIndeterminateBorderColor =
                disabledIndeterminateBorderColor.takeOrElse {
                    this.disabledIndeterminateBorderColor
                },
            disabledCheckmarkColor =
                disabledCheckmarkColor.takeOrElse { this.disabledCheckmarkColor },
        )

    /**
     * Represents the color used for the checkbox container's background indication, depending on
     * [state].
     *
     * @param state the [ToggleableState] of the checkbox
     */
    internal fun indicatorColor(state: ToggleableState): Color {
        return if (state == ToggleableState.Off) {
            uncheckedBoxColor
        } else {
            checkedBoxColor
        }
    }

    /**
     * Represents the color used for the checkmark inside the checkbox, depending on [enabled] and
     * [state].
     *
     * @param enabled whether the checkbox is enabled or not
     * @param state the [ToggleableState] of the checkbox
     */
    @Composable
    internal fun checkmarkColor(enabled: Boolean, state: ToggleableState): State<Color> {
        val target =
            if (enabled) {
                if (state == ToggleableState.Off) {
                    uncheckedCheckmarkColor
                } else {
                    checkedCheckmarkColor
                }
            } else {
                disabledCheckmarkColor
            }
        return animateColorAsState(target, colorAnimationSpecForState(state))
    }

    /**
     * Represents the color used for the checkmark inside the checkbox, depending on [state].
     *
     * @param state the [ToggleableState] of the checkbox
     */
    @Composable
    internal fun checkmarkColor(state: ToggleableState): State<Color> {
        val target =
            if (state == ToggleableState.Off) {
                uncheckedCheckmarkColor
            } else {
                checkedCheckmarkColor
            }

        return animateColorAsState(target, colorAnimationSpecForState(state))
    }

    /**
     * Represents the color used for the box (background) of the checkbox, depending on [enabled]
     * and [state].
     *
     * @param enabled whether the checkbox is enabled or not
     * @param state the [ToggleableState] of the checkbox
     */
    @Composable
    internal fun boxColor(enabled: Boolean, state: ToggleableState): State<Color> {
        val target =
            if (enabled) {
                when (state) {
                    ToggleableState.On,
                    ToggleableState.Indeterminate -> checkedBoxColor
                    ToggleableState.Off -> uncheckedBoxColor
                }
            } else {
                when (state) {
                    ToggleableState.On -> disabledCheckedBoxColor
                    ToggleableState.Indeterminate -> disabledIndeterminateBoxColor
                    ToggleableState.Off -> disabledUncheckedBoxColor
                }
            }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            animateColorAsState(target, colorAnimationSpecForState(state))
        } else {
            rememberUpdatedState(target)
        }
    }

    /**
     * Represents the color used for the border of the checkbox, depending on [enabled] and [state].
     *
     * @param enabled whether the checkbox is enabled or not
     * @param state the [ToggleableState] of the checkbox
     */
    @Composable
    internal fun borderColor(enabled: Boolean, state: ToggleableState): State<Color> {
        val target =
            if (enabled) {
                when (state) {
                    ToggleableState.On,
                    ToggleableState.Indeterminate -> checkedBorderColor
                    ToggleableState.Off -> uncheckedBorderColor
                }
            } else {
                when (state) {
                    ToggleableState.Indeterminate -> disabledIndeterminateBorderColor
                    ToggleableState.On -> disabledBorderColor
                    ToggleableState.Off -> disabledUncheckedBorderColor
                }
            }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            animateColorAsState(target, colorAnimationSpecForState(state))
        } else {
            rememberUpdatedState(target)
        }
    }

    /** Returns the color [AnimationSpec] for the given state. */
    @Composable
    private fun colorAnimationSpecForState(state: ToggleableState): AnimationSpec<Color> {
        // TODO Load the motionScheme tokens from the component tokens file
        return if (state == ToggleableState.Off) {
            // Box out
            MotionSchemeKeyTokens.FastEffects.value()
        } else {
            // Box in
            MotionSchemeKeyTokens.DefaultEffects.value()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CheckboxColors) return false

        if (checkedCheckmarkColor != other.checkedCheckmarkColor) return false
        if (uncheckedCheckmarkColor != other.uncheckedCheckmarkColor) return false
        if (disabledCheckmarkColor != other.disabledCheckmarkColor) return false
        if (checkedBoxColor != other.checkedBoxColor) return false
        if (uncheckedBoxColor != other.uncheckedBoxColor) return false
        if (disabledCheckedBoxColor != other.disabledCheckedBoxColor) return false
        if (disabledUncheckedBoxColor != other.disabledUncheckedBoxColor) return false
        if (disabledIndeterminateBoxColor != other.disabledIndeterminateBoxColor) return false
        if (checkedBorderColor != other.checkedBorderColor) return false
        if (uncheckedBorderColor != other.uncheckedBorderColor) return false
        if (disabledBorderColor != other.disabledBorderColor) return false
        if (disabledUncheckedBorderColor != other.disabledUncheckedBorderColor) return false
        if (disabledIndeterminateBorderColor != other.disabledIndeterminateBorderColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedCheckmarkColor.hashCode()
        result = 31 * result + uncheckedCheckmarkColor.hashCode()
        result = 31 * result + disabledCheckmarkColor.hashCode()
        result = 31 * result + checkedBoxColor.hashCode()
        result = 31 * result + uncheckedBoxColor.hashCode()
        result = 31 * result + disabledCheckedBoxColor.hashCode()
        result = 31 * result + disabledUncheckedBoxColor.hashCode()
        result = 31 * result + disabledIndeterminateBoxColor.hashCode()
        result = 31 * result + checkedBorderColor.hashCode()
        result = 31 * result + uncheckedBorderColor.hashCode()
        result = 31 * result + disabledBorderColor.hashCode()
        result = 31 * result + disabledUncheckedBorderColor.hashCode()
        result = 31 * result + disabledIndeterminateBorderColor.hashCode()
        return result
    }
}

private const val SnapAnimationDelay = 100

// TODO(b/188529841): Update the padding and size when the Checkbox spec is finalized.
private val CheckboxDefaultPadding = 2.dp
private val CheckboxSize = 20.dp
private val RadiusSize = 2.dp
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Switch.kt
```kotlin
/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SnapSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.SwitchTokens
import androidx.compose.material3.tokens.SwitchTokens.TrackOutlineWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * [Material Design switch](https://m3.material.io/components/switch)
 *
 * Switches toggle the state of a single item on or off.
 *
 * ![Switch
 * image](https://developer.android.com/images/reference/androidx/compose/material3/switch.png)
 *
 * @sample androidx.compose.material3.samples.SwitchSample
 *
 * Switch can be used with a custom icon via [thumbContent] parameter
 *
 * @sample androidx.compose.material3.samples.SwitchWithThumbIconSample
 * @param checked whether or not this switch is checked
 * @param onCheckedChange called when this switch is clicked. If `null`, then this switch will not
 *   be interactable, unless something else handles its input events and updates its state.
 * @param modifier the [Modifier] to be applied to this switch
 * @param thumbContent content that will be drawn inside the thumb, expected to measure
 *   [SwitchDefaults.IconSize]
 * @param enabled controls the enabled state of this switch. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [SwitchColors] that will be used to resolve the colors used for this switch in
 *   different states. See [SwitchDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this switch. You can use this to change the switch's appearance or
 *   preview the switch in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 */
@Composable
@Suppress("ComposableLambdaParameterNaming", "ComposableLambdaParameterPosition")
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    // TODO: Add Swipeable modifier b/223797571
    val toggleableModifier =
        if (onCheckedChange != null) {
            Modifier.minimumInteractiveComponentSize()
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = interactionSource,
                    indication = null,
                )
        } else {
            Modifier
        }

    SwitchImpl(
        modifier =
            modifier
                .then(toggleableModifier)
                .wrapContentSize(Alignment.Center)
                .requiredSize(SwitchWidth, SwitchHeight),
        checked = checked,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        thumbShape = SwitchTokens.HandleShape.value,
        thumbContent = thumbContent,
    )
}

@Composable
@Suppress("ComposableLambdaParameterNaming", "ComposableLambdaParameterPosition")
private fun SwitchImpl(
    modifier: Modifier,
    checked: Boolean,
    enabled: Boolean,
    colors: SwitchColors,
    thumbContent: (@Composable () -> Unit)?,
    interactionSource: InteractionSource,
    thumbShape: Shape,
) {
    val trackColor = colors.trackColor(enabled, checked)
    val resolvedThumbColor = colors.thumbColor(enabled, checked)
    val trackShape = SwitchTokens.TrackShape.value

    Box(
        modifier
            .border(TrackOutlineWidth, colors.borderColor(enabled, checked), trackShape)
            .background(trackColor, trackShape)
    ) {
        Box(
            modifier =
                Modifier.align(Alignment.CenterStart)
                    .then(
                        ThumbElement(
                            interactionSource = interactionSource,
                            checked = checked,
                            // TODO Load the motionScheme tokens from the component tokens file
                            animationSpec = MotionSchemeKeyTokens.FastSpatial.value(),
                        )
                    )
                    .indication(
                        interactionSource = interactionSource,
                        indication =
                            ripple(bounded = false, radius = SwitchTokens.StateLayerSize / 2),
                    )
                    .background(resolvedThumbColor, thumbShape),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbContent != null) {
                val iconColor = colors.iconColor(enabled, checked)
                CompositionLocalProvider(
                    LocalContentColor provides iconColor,
                    content = thumbContent,
                )
            }
        }
    }
}

private data class ThumbElement(
    val interactionSource: InteractionSource,
    val checked: Boolean,
    val animationSpec: FiniteAnimationSpec<Float>,
) : ModifierNodeElement<ThumbNode>() {
    override fun create() = ThumbNode(interactionSource, checked, animationSpec)

    override fun update(node: ThumbNode) {
        node.interactionSource = interactionSource
        if (node.checked != checked) {
            node.invalidateMeasurement()
        }
        node.checked = checked
        node.animationSpec = animationSpec
        node.update()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "switchThumb"
        properties["interactionSource"] = interactionSource
        properties["checked"] = checked
        properties["animationSpec"] = animationSpec
    }
}

private class ThumbNode(
    var interactionSource: InteractionSource,
    var checked: Boolean,
    var animationSpec: FiniteAnimationSpec<Float>,
) : Modifier.Node(), LayoutModifierNode {

    override val shouldAutoInvalidate: Boolean
        get() = false

    private var isPressed = false
    private var offsetAnim: Animatable<Float, AnimationVector1D>? = null
    private var sizeAnim: Animatable<Float, AnimationVector1D>? = null
    private var initialOffset: Float = Float.NaN
    private var initialSize: Float = Float.NaN

    override fun onAttach() {
        coroutineScope.launch {
            var pressCount = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressCount++
                    is PressInteraction.Release -> pressCount--
                    is PressInteraction.Cancel -> pressCount--
                }
                val pressed = pressCount > 0
                if (isPressed != pressed) {
                    isPressed = pressed
                    invalidateMeasurement()
                }
            }
        }
    }

    override fun onReset() {
        super.onReset()
        offsetAnim = null
        sizeAnim = null
        initialSize = Float.NaN
        initialOffset = Float.NaN
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val hasContent =
            measurable.maxIntrinsicHeight(constraints.maxWidth) != 0 &&
                measurable.maxIntrinsicWidth(constraints.maxHeight) != 0
        val size =
            when {
                isPressed -> SwitchTokens.PressedHandleWidth
                hasContent || checked -> ThumbDiameter
                else -> UncheckedThumbDiameter
            }.toPx()

        val actualSize = (sizeAnim?.value ?: size).toInt()
        val placeable = measurable.measure(Constraints.fixed(actualSize, actualSize))
        val thumbPaddingStart = (SwitchHeight - size.toDp()) / 2f
        val minBound = thumbPaddingStart.toPx()
        val thumbPathLength = (SwitchWidth - ThumbDiameter) - ThumbPadding
        val maxBound = thumbPathLength.toPx()
        val offset =
            when {
                isPressed && checked -> maxBound - TrackOutlineWidth.toPx()
                isPressed && !checked -> TrackOutlineWidth.toPx()
                checked -> maxBound
                else -> minBound
            }

        if (sizeAnim?.targetValue != size) {
            coroutineScope.launch {
                sizeAnim?.animateTo(size, if (isPressed) SnapSpec else animationSpec)
            }
        }

        if (offsetAnim?.targetValue != offset) {
            coroutineScope.launch {
                offsetAnim?.animateTo(offset, if (isPressed) SnapSpec else animationSpec)
            }
        }

        if (initialSize.isNaN() && initialOffset.isNaN()) {
            initialSize = size
            initialOffset = offset
        }

        return layout(actualSize, actualSize) {
            placeable.placeRelative(offsetAnim?.value?.toInt() ?: offset.toInt(), 0)
        }
    }

    fun update() {
        if (sizeAnim == null && !initialSize.isNaN()) {
            sizeAnim = Animatable(initialSize)
        }

        if (offsetAnim == null && !initialOffset.isNaN()) offsetAnim = Animatable(initialOffset)
    }
}

/** Contains the default values used by [Switch] */
object SwitchDefaults {
    /**
     * Creates a [SwitchColors] that represents the different colors used in a [Switch] in different
     * states.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultSwitchColors

    /**
     * Creates a [SwitchColors] that represents the different colors used in a [Switch] in different
     * states.
     *
     * @param checkedThumbColor the color used for the thumb when enabled and checked
     * @param checkedTrackColor the color used for the track when enabled and checked
     * @param checkedBorderColor the color used for the border when enabled and checked
     * @param checkedIconColor the color used for the icon when enabled and checked
     * @param uncheckedThumbColor the color used for the thumb when enabled and unchecked
     * @param uncheckedTrackColor the color used for the track when enabled and unchecked
     * @param uncheckedBorderColor the color used for the border when enabled and unchecked
     * @param uncheckedIconColor the color used for the icon when enabled and unchecked
     * @param disabledCheckedThumbColor the color used for the thumb when disabled and checked
     * @param disabledCheckedTrackColor the color used for the track when disabled and checked
     * @param disabledCheckedBorderColor the color used for the border when disabled and checked
     * @param disabledCheckedIconColor the color used for the icon when disabled and checked
     * @param disabledUncheckedThumbColor the color used for the thumb when disabled and unchecked
     * @param disabledUncheckedTrackColor the color used for the track when disabled and unchecked
     * @param disabledUncheckedBorderColor the color used for the border when disabled and unchecked
     * @param disabledUncheckedIconColor the color used for the icon when disabled and unchecked
     */
    @Composable
    fun colors(
        checkedThumbColor: Color = SwitchTokens.SelectedHandleColor.value,
        checkedTrackColor: Color = SwitchTokens.SelectedTrackColor.value,
        checkedBorderColor: Color = Color.Transparent,
        checkedIconColor: Color = SwitchTokens.SelectedIconColor.value,
        uncheckedThumbColor: Color = SwitchTokens.UnselectedHandleColor.value,
        uncheckedTrackColor: Color = SwitchTokens.UnselectedTrackColor.value,
        uncheckedBorderColor: Color = SwitchTokens.UnselectedFocusTrackOutlineColor.value,
        uncheckedIconColor: Color = SwitchTokens.UnselectedIconColor.value,
        disabledCheckedThumbColor: Color =
            SwitchTokens.DisabledSelectedHandleColor.value
                .copy(alpha = SwitchTokens.DisabledSelectedHandleOpacity)
                .compositeOver(MaterialTheme.colorScheme.surface),
        disabledCheckedTrackColor: Color =
            SwitchTokens.DisabledSelectedTrackColor.value
                .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                .compositeOver(MaterialTheme.colorScheme.surface),
        disabledCheckedBorderColor: Color = Color.Transparent,
        disabledCheckedIconColor: Color =
            SwitchTokens.DisabledSelectedIconColor.value
                .copy(alpha = SwitchTokens.DisabledSelectedIconOpacity)
                .compositeOver(MaterialTheme.colorScheme.surface),
        disabledUncheckedThumbColor: Color =
            SwitchTokens.DisabledUnselectedHandleColor.value
                .copy(alpha = SwitchTokens.DisabledUnselectedHandleOpacity)
                .compositeOver(MaterialTheme.colorScheme.surface),
        disabledUncheckedTrackColor: Color =
            SwitchTokens.DisabledUnselectedTrackColor.value
                .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                .compositeOver(MaterialTheme.colorScheme.surface),
        disabledUncheckedBorderColor: Color =
            SwitchTokens.DisabledUnselectedTrackOutlineColor.value
                .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                .compositeOver(MaterialTheme.colorScheme.surface),
        disabledUncheckedIconColor: Color =
            SwitchTokens.DisabledUnselectedIconColor.value
                .copy(alpha = SwitchTokens.DisabledUnselectedIconOpacity)
                .compositeOver(MaterialTheme.colorScheme.surface),
    ): SwitchColors =
        SwitchColors(
            checkedThumbColor = checkedThumbColor,
            checkedTrackColor = checkedTrackColor,
            checkedBorderColor = checkedBorderColor,
            checkedIconColor = checkedIconColor,
            uncheckedThumbColor = uncheckedThumbColor,
            uncheckedTrackColor = uncheckedTrackColor,
            uncheckedBorderColor = uncheckedBorderColor,
            uncheckedIconColor = uncheckedIconColor,
            disabledCheckedThumbColor = disabledCheckedThumbColor,
            disabledCheckedTrackColor = disabledCheckedTrackColor,
            disabledCheckedBorderColor = disabledCheckedBorderColor,
            disabledCheckedIconColor = disabledCheckedIconColor,
            disabledUncheckedThumbColor = disabledUncheckedThumbColor,
            disabledUncheckedTrackColor = disabledUncheckedTrackColor,
            disabledUncheckedBorderColor = disabledUncheckedBorderColor,
            disabledUncheckedIconColor = disabledUncheckedIconColor,
        )

    internal val ColorScheme.defaultSwitchColors: SwitchColors
        get() {
            return defaultSwitchColorsCached
                ?: SwitchColors(
                        checkedThumbColor = fromToken(SwitchTokens.SelectedHandleColor),
                        checkedTrackColor = fromToken(SwitchTokens.SelectedTrackColor),
                        checkedBorderColor = Color.Transparent,
                        checkedIconColor = fromToken(SwitchTokens.SelectedIconColor),
                        uncheckedThumbColor = fromToken(SwitchTokens.UnselectedHandleColor),
                        uncheckedTrackColor = fromToken(SwitchTokens.UnselectedTrackColor),
                        uncheckedBorderColor =
                            fromToken(SwitchTokens.UnselectedFocusTrackOutlineColor),
                        uncheckedIconColor = fromToken(SwitchTokens.UnselectedIconColor),
                        disabledCheckedThumbColor =
                            fromToken(SwitchTokens.DisabledSelectedHandleColor)
                                .copy(alpha = SwitchTokens.DisabledSelectedHandleOpacity)
                                .compositeOver(surface),
                        disabledCheckedTrackColor =
                            fromToken(SwitchTokens.DisabledSelectedTrackColor)
                                .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                                .compositeOver(surface),
                        disabledCheckedBorderColor = Color.Transparent,
                        disabledCheckedIconColor =
                            fromToken(SwitchTokens.DisabledSelectedIconColor)
                                .copy(alpha = SwitchTokens.DisabledSelectedIconOpacity)
                                .compositeOver(surface),
                        disabledUncheckedThumbColor =
                            fromToken(SwitchTokens.DisabledUnselectedHandleColor)
                                .copy(alpha = SwitchTokens.DisabledUnselectedHandleOpacity)
                                .compositeOver(surface),
                        disabledUncheckedTrackColor =
                            fromToken(SwitchTokens.DisabledUnselectedTrackColor)
                                .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                                .compositeOver(surface),
                        disabledUncheckedBorderColor =
                            fromToken(SwitchTokens.DisabledUnselectedTrackOutlineColor)
                                .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                                .compositeOver(surface),
                        disabledUncheckedIconColor =
                            fromToken(SwitchTokens.DisabledUnselectedIconColor)
                                .copy(alpha = SwitchTokens.DisabledUnselectedIconOpacity)
                                .compositeOver(surface),
                    )
                    .also { defaultSwitchColorsCached = it }
        }

    /** Icon size to use for `thumbContent` */
    val IconSize = 16.dp
}

/**
 * Represents the colors used by a [Switch] in different states
 *
 * @param checkedThumbColor the color used for the thumb when enabled and checked
 * @param checkedTrackColor the color used for the track when enabled and checked
 * @param checkedBorderColor the color used for the border when enabled and checked
 * @param checkedIconColor the color used for the icon when enabled and checked
 * @param uncheckedThumbColor the color used for the thumb when enabled and unchecked
 * @param uncheckedTrackColor the color used for the track when enabled and unchecked
 * @param uncheckedBorderColor the color used for the border when enabled and unchecked
 * @param uncheckedIconColor the color used for the icon when enabled and unchecked
 * @param disabledCheckedThumbColor the color used for the thumb when disabled and checked
 * @param disabledCheckedTrackColor the color used for the track when disabled and checked
 * @param disabledCheckedBorderColor the color used for the border when disabled and checked
 * @param disabledCheckedIconColor the color used for the icon when disabled and checked
 * @param disabledUncheckedThumbColor the color used for the thumb when disabled and unchecked
 * @param disabledUncheckedTrackColor the color used for the track when disabled and unchecked
 * @param disabledUncheckedBorderColor the color used for the border when disabled and unchecked
 * @param disabledUncheckedIconColor the color used for the icon when disabled and unchecked
 * @constructor create an instance with arbitrary colors. See [SwitchDefaults.colors] for the
 *   default implementation that follows Material specifications.
 */
@Immutable
class SwitchColors
constructor(
    val checkedThumbColor: Color,
    val checkedTrackColor: Color,
    val checkedBorderColor: Color,
    val checkedIconColor: Color,
    val uncheckedThumbColor: Color,
    val uncheckedTrackColor: Color,
    val uncheckedBorderColor: Color,
    val uncheckedIconColor: Color,
    val disabledCheckedThumbColor: Color,
    val disabledCheckedTrackColor: Color,
    val disabledCheckedBorderColor: Color,
    val disabledCheckedIconColor: Color,
    val disabledUncheckedThumbColor: Color,
    val disabledUncheckedTrackColor: Color,
    val disabledUncheckedBorderColor: Color,
    val disabledUncheckedIconColor: Color,
) {
    /**
     * Returns a copy of this SwitchColors, optionally overriding some of the values. This uses the
     * Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        checkedThumbColor: Color = this.checkedThumbColor,
        checkedTrackColor: Color = this.checkedTrackColor,
        checkedBorderColor: Color = this.checkedBorderColor,
        checkedIconColor: Color = this.checkedIconColor,
        uncheckedThumbColor: Color = this.uncheckedThumbColor,
        uncheckedTrackColor: Color = this.uncheckedTrackColor,
        uncheckedBorderColor: Color = this.uncheckedBorderColor,
        uncheckedIconColor: Color = this.uncheckedIconColor,
        disabledCheckedThumbColor: Color = this.disabledCheckedThumbColor,
        disabledCheckedTrackColor: Color = this.disabledCheckedTrackColor,
        disabledCheckedBorderColor: Color = this.disabledCheckedBorderColor,
        disabledCheckedIconColor: Color = this.disabledCheckedIconColor,
        disabledUncheckedThumbColor: Color = this.disabledUncheckedThumbColor,
        disabledUncheckedTrackColor: Color = this.disabledUncheckedTrackColor,
        disabledUncheckedBorderColor: Color = this.disabledUncheckedBorderColor,
        disabledUncheckedIconColor: Color = this.disabledUncheckedIconColor,
    ) =
        SwitchColors(
            checkedThumbColor.takeOrElse { this.checkedThumbColor },
            checkedTrackColor.takeOrElse { this.checkedTrackColor },
            checkedBorderColor.takeOrElse { this.checkedBorderColor },
            checkedIconColor.takeOrElse { this.checkedIconColor },
            uncheckedThumbColor.takeOrElse { this.uncheckedThumbColor },
            uncheckedTrackColor.takeOrElse { this.uncheckedTrackColor },
            uncheckedBorderColor.takeOrElse { this.uncheckedBorderColor },
            uncheckedIconColor.takeOrElse { this.uncheckedIconColor },
            disabledCheckedThumbColor.takeOrElse { this.disabledCheckedThumbColor },
            disabledCheckedTrackColor.takeOrElse { this.disabledCheckedTrackColor },
            disabledCheckedBorderColor.takeOrElse { this.disabledCheckedBorderColor },
            disabledCheckedIconColor.takeOrElse { this.disabledCheckedIconColor },
            disabledUncheckedThumbColor.takeOrElse { this.disabledUncheckedThumbColor },
            disabledUncheckedTrackColor.takeOrElse { this.disabledUncheckedTrackColor },
            disabledUncheckedBorderColor.takeOrElse { this.disabledUncheckedBorderColor },
            disabledUncheckedIconColor.takeOrElse { this.disabledUncheckedIconColor },
        )

    /**
     * Represents the color used for the switch's thumb, depending on [enabled] and [checked].
     *
     * @param enabled whether the [Switch] is enabled or not
     * @param checked whether the [Switch] is checked or not
     */
    @Stable
    internal fun thumbColor(enabled: Boolean, checked: Boolean): Color =
        if (enabled) {
            if (checked) checkedThumbColor else uncheckedThumbColor
        } else {
            if (checked) disabledCheckedThumbColor else disabledUncheckedThumbColor
        }

    /**
     * Represents the color used for the switch's track, depending on [enabled] and [checked].
     *
     * @param enabled whether the [Switch] is enabled or not
     * @param checked whether the [Switch] is checked or not
     */
    @Stable
    internal fun trackColor(enabled: Boolean, checked: Boolean): Color =
        if (enabled) {
            if (checked) checkedTrackColor else uncheckedTrackColor
        } else {
            if (checked) disabledCheckedTrackColor else disabledUncheckedTrackColor
        }

    /**
     * Represents the color used for the switch's border, depending on [enabled] and [checked].
     *
     * @param enabled whether the [Switch] is enabled or not
     * @param checked whether the [Switch] is checked or not
     */
    @Stable
    internal fun borderColor(enabled: Boolean, checked: Boolean): Color =
        if (enabled) {
            if (checked) checkedBorderColor else uncheckedBorderColor
        } else {
            if (checked) disabledCheckedBorderColor else disabledUncheckedBorderColor
        }

    /**
     * Represents the content color passed to the icon if used
     *
     * @param enabled whether the [Switch] is enabled or not
     * @param checked whether the [Switch] is checked or not
     */
    @Stable
    internal fun iconColor(enabled: Boolean, checked: Boolean): Color =
        if (enabled) {
            if (checked) checkedIconColor else uncheckedIconColor
        } else {
            if (checked) disabledCheckedIconColor else disabledUncheckedIconColor
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is SwitchColors) return false

        if (checkedThumbColor != other.checkedThumbColor) return false
        if (checkedTrackColor != other.checkedTrackColor) return false
        if (checkedBorderColor != other.checkedBorderColor) return false
        if (checkedIconColor != other.checkedIconColor) return false
        if (uncheckedThumbColor != other.uncheckedThumbColor) return false
        if (uncheckedTrackColor != other.uncheckedTrackColor) return false
        if (uncheckedBorderColor != other.uncheckedBorderColor) return false
        if (uncheckedIconColor != other.uncheckedIconColor) return false
        if (disabledCheckedThumbColor != other.disabledCheckedThumbColor) return false
        if (disabledCheckedTrackColor != other.disabledCheckedTrackColor) return false
        if (disabledCheckedBorderColor != other.disabledCheckedBorderColor) return false
        if (disabledCheckedIconColor != other.disabledCheckedIconColor) return false
        if (disabledUncheckedThumbColor != other.disabledUncheckedThumbColor) return false
        if (disabledUncheckedTrackColor != other.disabledUncheckedTrackColor) return false
        if (disabledUncheckedBorderColor != other.disabledUncheckedBorderColor) return false
        if (disabledUncheckedIconColor != other.disabledUncheckedIconColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedThumbColor.hashCode()
        result = 31 * result + checkedTrackColor.hashCode()
        result = 31 * result + checkedBorderColor.hashCode()
        result = 31 * result + checkedIconColor.hashCode()
        result = 31 * result + uncheckedThumbColor.hashCode()
        result = 31 * result + uncheckedTrackColor.hashCode()
        result = 31 * result + uncheckedBorderColor.hashCode()
        result = 31 * result + uncheckedIconColor.hashCode()
        result = 31 * result + disabledCheckedThumbColor.hashCode()
        result = 31 * result + disabledCheckedTrackColor.hashCode()
        result = 31 * result + disabledCheckedBorderColor.hashCode()
        result = 31 * result + disabledCheckedIconColor.hashCode()
        result = 31 * result + disabledUncheckedThumbColor.hashCode()
        result = 31 * result + disabledUncheckedTrackColor.hashCode()
        result = 31 * result + disabledUncheckedBorderColor.hashCode()
        result = 31 * result + disabledUncheckedIconColor.hashCode()
        return result
    }
}

/* @VisibleForTesting */
internal val ThumbDiameter = SwitchTokens.SelectedHandleWidth
internal val UncheckedThumbDiameter = SwitchTokens.UnselectedHandleWidth

private val SwitchWidth = SwitchTokens.TrackWidth
private val SwitchHeight = SwitchTokens.TrackHeight
private val ThumbPadding = (SwitchHeight - ThumbDiameter) / 2
private val SnapSpec = SnapSpec<Float>()
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Tab.kt
```kotlin
/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.PrimaryNavigationTabTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirst
import kotlin.math.max

/**
 * [Material Design tab](https://m3.material.io/components/tabs/overview)
 *
 * A default Tab, also known as a Primary Navigation Tab. Tabs organize content across different
 * screens, data sets, and other interactions.
 *
 * ![Tabs
 * image](https://developer.android.com/images/reference/androidx/compose/material3/secondary-tabs.png)
 *
 * A Tab represents a single page of content using a text label and/or icon. It represents its
 * selected state by tinting the text label and/or image with [selectedContentColor].
 *
 * This should typically be used inside of a [TabRow], see the corresponding documentation for
 * example usage.
 *
 * This Tab has slots for [text] and/or [icon] - see the other Tab overload for a generic Tab that
 * is not opinionated about its content.
 *
 * @param selected whether this tab is selected or not
 * @param onClick called when this tab is clicked
 * @param modifier the [Modifier] to be applied to this tab
 * @param enabled controls the enabled state of this tab. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param text the text label displayed in this tab
 * @param icon the icon displayed in this tab
 * @param selectedContentColor the color for the content of this tab when selected, and the color of
 *   the ripple.
 * @param unselectedContentColor the color for the content of this tab when not selected
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this tab. You can use this to change the tab's appearance or
 *   preview the tab in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @see LeadingIconTab
 */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    interactionSource: MutableInteractionSource? = null,
) {
    val styledText: @Composable (() -> Unit)? =
        text?.let {
            @Composable {
                val style =
                    PrimaryNavigationTabTokens.LabelTextFont.value.copy(
                        textAlign = TextAlign.Center
                    )
                ProvideTextStyle(style, content = text)
            }
        }
    Tab(
        modifier = modifier.badgeBounds(),
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        interactionSource = interactionSource,
    ) {
        TabBaselineLayout(icon = icon, text = styledText)
    }
}

/**
 * [Material Design tab](https://m3.material.io/components/tabs/overview)
 *
 * Tabs organize content across different screens, data sets, and other interactions.
 *
 * A LeadingIconTab represents a single page of content using a text label and an icon in front of
 * the label. It represents its selected state by tinting the text label and icon with
 * [selectedContentColor].
 *
 * This should typically be used inside of a [TabRow], see the corresponding documentation for
 * example usage.
 *
 * @param selected whether this tab is selected or not
 * @param onClick called when this tab is clicked
 * @param text the text label displayed in this tab
 * @param icon the icon displayed in this tab. Should be 24.dp.
 * @param modifier the [Modifier] to be applied to this tab
 * @param enabled controls the enabled state of this tab. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param selectedContentColor the color for the content of this tab when selected, and the color of
 *   the ripple.
 * @param unselectedContentColor the color for the content of this tab when not selected
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this tab. You can use this to change the tab's appearance or
 *   preview the tab in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @see Tab
 */
@Composable
fun LeadingIconTab(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    interactionSource: MutableInteractionSource? = null,
) {
    // The color of the Ripple should always the be selected color, as we want to show the color
    // before the item is considered selected, and hence before the new contentColor is
    // provided by TabTransition.
    val ripple = ripple(bounded = true, color = selectedContentColor)

    TabTransition(selectedContentColor, unselectedContentColor, selected) {
        Row(
            modifier =
                modifier
                    .height(SmallTabHeight)
                    .selectable(
                        selected = selected,
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.Tab,
                        interactionSource = interactionSource,
                        indication = ripple,
                    )
                    .padding(horizontal = HorizontalTextPadding)
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.requiredWidth(TextDistanceFromLeadingIcon))
            val style =
                PrimaryNavigationTabTokens.LabelTextFont.value.copy(textAlign = TextAlign.Center)
            ProvideTextStyle(style, content = text)
        }
    }
}

/**
 * [Material Design tab](https://m3.material.io/components/tabs/overview)
 *
 * Tabs organize content across different screens, data sets, and other interactions.
 *
 * ![Tabs
 * image](https://developer.android.com/images/reference/androidx/compose/material3/secondary-tabs.png)
 *
 * Generic [Tab] overload that is not opinionated about content / color. See the other overload for
 * a Tab that has specific slots for text and / or an icon, as well as providing the correct colors
 * for selected / unselected states.
 *
 * A custom tab using this API may look like:
 *
 * @sample androidx.compose.material3.samples.FancyTab
 * @param selected whether this tab is selected or not
 * @param onClick called when this tab is clicked
 * @param modifier the [Modifier] to be applied to this tab
 * @param enabled controls the enabled state of this tab. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param selectedContentColor the color for the content of this tab when selected, and the color of
 *   the ripple.
 * @param unselectedContentColor the color for the content of this tab when not selected
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this tab. You can use this to change the tab's appearance or
 *   preview the tab in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this tab
 */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // The color of the Ripple should always the selected color, as we want to show the color
    // before the item is considered selected, and hence before the new contentColor is
    // provided by TabTransition.
    val ripple = ripple(bounded = true, color = selectedContentColor)

    TabTransition(selectedContentColor, unselectedContentColor, selected) {
        Column(
            modifier =
                modifier
                    .selectable(
                        selected = selected,
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.Tab,
                        interactionSource = interactionSource,
                        indication = ripple,
                    )
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

/**
 * Transition defining how the tint color for a tab animates, when a new tab is selected. This
 * component uses [LocalContentColor] to provide an interpolated value between [activeColor] and
 * [inactiveColor] depending on the animation status.
 */
@Composable
private fun TabTransition(
    activeColor: Color,
    inactiveColor: Color,
    selected: Boolean,
    content: @Composable () -> Unit,
) {
    val transition = updateTransition(selected)
    // TODO Load the motionScheme tokens from the component tokens file
    val color by
        transition.animateColor(
            transitionSpec = {
                if (false isTransitioningTo true) {
                    // Fade-in
                    MotionSchemeKeyTokens.DefaultEffects.value()
                } else {
                    // Fade-out
                    MotionSchemeKeyTokens.FastEffects.value()
                }
            }
        ) {
            if (it) activeColor else inactiveColor
        }
    CompositionLocalProvider(LocalContentColor provides color, content = content)
}

/**
 * A [Layout] that positions [text] and an optional [icon] with the correct baseline distances. This
 * Layout will either be [SmallTabHeight] or [LargeTabHeight] depending on its content, and then
 * place the text and/or icon inside with the correct baseline alignment.
 */
@Composable
private fun TabBaselineLayout(text: @Composable (() -> Unit)?, icon: @Composable (() -> Unit)?) {
    Layout({
        if (text != null) {
            Box(Modifier.layoutId("text").padding(horizontal = HorizontalTextPadding)) { text() }
        }
        if (icon != null) {
            Box(Modifier.layoutId("icon")) { icon() }
        }
    }) { measurables, constraints ->
        val textPlaceable =
            text?.let {
                measurables
                    .fastFirst { it.layoutId == "text" }
                    .measure(
                        // Measure with loose constraints for height as we don't want the text to
                        // take up more
                        // space than it needs
                        constraints.copy(minHeight = 0)
                    )
            }

        val iconPlaceable =
            icon?.let { measurables.fastFirst { it.layoutId == "icon" }.measure(constraints) }

        val tabWidth = max(textPlaceable?.width ?: 0, iconPlaceable?.width ?: 0)

        val specHeight =
            if (textPlaceable != null && iconPlaceable != null) {
                    LargeTabHeight
                } else {
                    SmallTabHeight
                }
                .roundToPx()

        val tabHeight =
            max(
                specHeight,
                (iconPlaceable?.height ?: 0) +
                    (textPlaceable?.height ?: 0) +
                    IconDistanceFromBaseline.roundToPx(),
            )

        val firstBaseline = textPlaceable?.get(FirstBaseline)
        val lastBaseline = textPlaceable?.get(LastBaseline)

        layout(tabWidth, tabHeight) {
            when {
                textPlaceable != null && iconPlaceable != null ->
                    placeTextAndIcon(
                        density = this@Layout,
                        textPlaceable = textPlaceable,
                        iconPlaceable = iconPlaceable,
                        tabWidth = tabWidth,
                        tabHeight = tabHeight,
                        firstBaseline = firstBaseline!!,
                        lastBaseline = lastBaseline!!,
                    )
                textPlaceable != null -> placeTextOrIcon(textPlaceable, tabHeight)
                iconPlaceable != null -> placeTextOrIcon(iconPlaceable, tabHeight)
                else -> {}
            }
        }
    }
}

/** Places the provided [textOrIconPlaceable] in the vertical center of the provided [tabHeight]. */
private fun Placeable.PlacementScope.placeTextOrIcon(
    textOrIconPlaceable: Placeable,
    tabHeight: Int,
) {
    val contentY = (tabHeight - textOrIconPlaceable.height) / 2
    textOrIconPlaceable.placeRelative(0, contentY)
}

/**
 * Places the provided [textPlaceable] offset from the bottom of the tab using the correct baseline
 * offset, with the provided [iconPlaceable] placed above the text using the correct baseline
 * offset.
 */
private fun Placeable.PlacementScope.placeTextAndIcon(
    density: Density,
    textPlaceable: Placeable,
    iconPlaceable: Placeable,
    tabWidth: Int,
    tabHeight: Int,
    firstBaseline: Int,
    lastBaseline: Int,
) {
    val baselineOffset =
        if (firstBaseline == lastBaseline) {
            SingleLineTextBaselineWithIcon
        } else {
            DoubleLineTextBaselineWithIcon
        }

    // Total offset between the last text baseline and the bottom of the Tab layout
    val textOffset =
        with(density) {
            baselineOffset.roundToPx() +
                PrimaryNavigationTabTokens.ActiveIndicatorHeight.roundToPx()
        }

    // How much space there is between the top of the icon (essentially the top of this layout)
    // and the top of the text layout's bounding box (not baseline)
    val iconOffset =
        with(density) {
            iconPlaceable.height + IconDistanceFromBaseline.roundToPx() - firstBaseline
        }

    val textPlaceableX = (tabWidth - textPlaceable.width) / 2
    val textPlaceableY = tabHeight - lastBaseline - textOffset
    textPlaceable.placeRelative(textPlaceableX, textPlaceableY)

    val iconPlaceableX = (tabWidth - iconPlaceable.width) / 2
    val iconPlaceableY = textPlaceableY - iconOffset
    iconPlaceable.placeRelative(iconPlaceableX, iconPlaceableY)
}

// Tab specifications
private val SmallTabHeight = PrimaryNavigationTabTokens.ContainerHeight
private val LargeTabHeight = 72.dp

// The horizontal padding on the left and right of text
internal val HorizontalTextPadding = 16.dp

// Distance from the top of the indicator to the text baseline when there is one line of text and an
// icon
private val SingleLineTextBaselineWithIcon = 14.dp

// Distance from the top of the indicator to the last text baseline when there are two lines of text
// and an icon
private val DoubleLineTextBaselineWithIcon = 6.dp

// Distance from the first text baseline to the bottom of the icon in a combined tab
private val IconDistanceFromBaseline = 20.sp

// Distance from the end of the leading icon to the start of the text
private val TextDistanceFromLeadingIcon = 8.dp
```

## File: compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/TabRow.kt
```kotlin
/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.collection.mutableIntListOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.PrimaryNavigationTabTokens
import androidx.compose.material3.tokens.SecondaryNavigationTabTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [Material Design fixed primary tabs](https://m3.material.io/components/tabs/overview)
 *
 * Primary tabs are placed at the top of the content pane under a top app bar. They display the main
 * content destinations. Fixed tabs display all tabs in a set simultaneously. They are best for
 * switching between related content quickly, such as between transportation methods in a map. To
 * navigate between fixed tabs, tap an individual tab, or swipe left or right in the content area.
 *
 * A TabRow contains a row of [Tab]s, and displays an indicator underneath the currently selected
 * tab. A TabRow places its tabs evenly spaced along the entire row, with each tab taking up an
 * equal amount of space. See [PrimaryScrollableTabRow] for a tab row that does not enforce equal
 * size, and allows scrolling to tabs that do not fit on screen.
 *
 * A simple example with text tabs looks like:
 *
 * @sample androidx.compose.material3.samples.PrimaryTextTabs
 *
 * You can also provide your own custom tab, such as:
 *
 * @sample androidx.compose.material3.samples.FancyTabs
 *
 * Where the custom tab itself could look like:
 *
 * @sample androidx.compose.material3.samples.FancyTab
 *
 * As well as customizing the tab, you can also provide a custom [indicator], to customize the
 * indicator displayed for a tab. [indicator] will be placed to fill the entire TabRow, so it should
 * internally take care of sizing and positioning the indicator to match changes to
 * [selectedTabIndex].
 *
 * For example, given an indicator that draws a rounded rectangle near the edges of the [Tab]:
 *
 * @sample androidx.compose.material3.samples.FancyIndicator
 *
 * We can reuse [TabRowDefaults.tabIndicatorOffset] and just provide this indicator, as we aren't
 * changing how the size and position of the indicator changes between tabs:
 *
 * @sample androidx.compose.material3.samples.FancyIndicatorTabs
 *
 * You may also want to use a custom transition, to allow you to dynamically change the appearance
 * of the indicator as it animates between tabs, such as changing its color or size. [indicator] is
 * stacked on top of the entire TabRow, so you just need to provide a custom transition that
 * animates the offset of the indicator from the start of the TabRow. For example, take the
 * following example that uses a transition to animate the offset, width, and color of the same
 * FancyIndicator from before, also adding a physics based 'spring' effect to the indicator in the
 * direction of motion:
 *
 * @sample androidx.compose.material3.samples.FancyAnimatedIndicatorWithModifier
 *
 * We can now just pass this indicator directly to TabRow:
 *
 * @sample androidx.compose.material3.samples.FancyIndicatorContainerTabs
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.PrimaryIndicator], using a [TabRowDefaults.tabIndicatorOffset]
 *   modifier to animate its position.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 *   separation between the tab row and the content displayed underneath.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the row, each taking up equal
 *   space.
 */
@Composable
fun PrimaryTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    indicator: @Composable TabIndicatorScope.() -> Unit = {
        TabRowDefaults.PrimaryIndicator(
            modifier = Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true),
            width = Dp.Unspecified,
        )
    },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit,
) {
    TabRowImpl(modifier, containerColor, contentColor, indicator, divider, tabs)
}

/**
 * [Material Design fixed secondary tabs](https://m3.material.io/components/tabs/overview)
 *
 * Secondary tabs are used within a content area to further separate related content and establish
 * hierarchy. Fixed tabs display all tabs in a set simultaneously. To navigate between fixed tabs,
 * tap an individual tab, or swipe left or right in the content area.
 *
 * A TabRow contains a row of [Tab]s, and displays an indicator underneath the currently selected
 * tab. A Fixed TabRow places its tabs evenly spaced along the entire row, with each tab taking up
 * an equal amount of space. See [SecondaryScrollableTabRow] for a tab row that does not enforce
 * equal size, and allows scrolling to tabs that do not fit on screen.
 *
 * A simple example with text tabs looks like:
 *
 * @sample androidx.compose.material3.samples.SecondaryTextTabs
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.SecondaryIndicator], using a [TabRowDefaults.tabIndicatorOffset]
 *   modifier to animate its position. Note that this indicator will be forced to fill up the entire
 *   tab row, so you should use [TabRowDefaults.tabIndicatorOffset] or similar to animate the actual
 *   drawn indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 *   separation between the tab row and the content displayed underneath.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the row, each taking up equal
 *   space.
 */
@Composable
fun SecondaryTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.secondaryContainerColor,
    contentColor: Color = TabRowDefaults.secondaryContentColor,
    indicator: @Composable TabIndicatorScope.() -> Unit =
        @Composable {
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = false)
            )
        },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit,
) {
    TabRowImpl(modifier, containerColor, contentColor, indicator, divider, tabs)
}

/**
 * [Material Design scrollable primary tabs](https://m3.material.io/components/tabs/overview)
 *
 * Primary tabs are placed at the top of the content pane under a top app bar. They display the main
 * content destinations. When a set of tabs cannot fit on screen, use scrollable tabs. Scrollable
 * tabs can use longer text labels and a larger number of tabs. They are best used for browsing on
 * touch interfaces.
 *
 * A scrollable tab row contains a row of [Tab]s, and displays an indicator underneath the currently
 * selected tab. A scrollable tab row places its tabs offset from the starting edge, and allows
 * scrolling to tabs that are placed off screen. For a fixed tab row that does not allow scrolling,
 * and evenly places its tabs, see [PrimaryTabRow].
 *
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param scrollState the [ScrollState] of this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param edgePadding the padding between the starting and ending edge of the scrollable tab row,
 *   and the tabs inside the row. This padding helps inform the user that this tab row can be
 *   scrolled, unlike a [TabRow].
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.PrimaryIndicator], using a [TabRowDefaults.tabIndicatorOffset]
 *   modifier to animate its position.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 *   separation between the tab row and the content displayed underneath.
 * @param minTabWidth the minimum width for a [Tab] in this tab row regardless of content size.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the row, each taking up equal
 *   space.
 */
@Composable
fun PrimaryScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    indicator: @Composable TabIndicatorScope.() -> Unit =
        @Composable {
            TabRowDefaults.PrimaryIndicator(
                Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true),
                width = Dp.Unspecified,
            )
        },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    minTabWidth: Dp = TabRowDefaults.ScrollableTabRowMinTabWidth,
    tabs: @Composable () -> Unit,
) {
    ScrollableTabRowImpl(
        selectedTabIndex = selectedTabIndex,
        indicator = indicator,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        edgePadding = edgePadding,
        minTabWidth = minTabWidth,
        divider = divider,
        tabs = tabs,
        scrollState = scrollState,
    )
}

/**
 * [Material Design scrollable secondary tabs](https://m3.material.io/components/tabs/overview)
 *
 * Material Design scrollable tabs.
 *
 * Secondary tabs are used within a content area to further separate related content and establish
 * hierarchy. When a set of tabs cannot fit on screen, use scrollable tabs. Scrollable tabs can use
 * longer text labels and a larger number of tabs. They are best used for browsing on touch
 * interfaces.
 *
 * A scrollable tab row contains a row of [Tab]s, and displays an indicator underneath the currently
 * selected tab. A scrollable tab row places its tabs offset from the starting edge, and allows
 * scrolling to tabs that are placed off screen. For a fixed tab row that does not allow scrolling,
 * and evenly places its tabs, see [SecondaryTabRow].
 *
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param scrollState the [ScrollState] of this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param edgePadding the padding between the starting and ending edge of the scrollable tab row,
 *   and the tabs inside the row. This padding helps inform the user that this tab row can be
 *   scrolled, unlike a [TabRow].
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.SecondaryIndicator], using a [TabRowDefaults.tabIndicatorOffset]
 *   modifier to animate its position. Note that this indicator will be forced to fill up the entire
 *   tab row, so you should use [TabRowDefaults.tabIndicatorOffset] or similar to animate the actual
 *   drawn indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 *   separation between the tab row and the content displayed underneath.
 * @param minTabWidth the minimum width for a [Tab] in this tab row regardless of content size.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the row, each taking up equal
 *   space.
 */
@Composable
fun SecondaryScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    containerColor: Color = TabRowDefaults.secondaryContainerColor,
    contentColor: Color = TabRowDefaults.secondaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    indicator: @Composable TabIndicatorScope.() -> Unit =
        @Composable {
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = false)
            )
        },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    minTabWidth: Dp = TabRowDefaults.ScrollableTabRowMinTabWidth,
    tabs: @Composable () -> Unit,
) {
    ScrollableTabRowImpl(
        selectedTabIndex = selectedTabIndex,
        indicator = indicator,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        edgePadding = edgePadding,
        minTabWidth = minTabWidth,
        divider = divider,
        tabs = tabs,
        scrollState = scrollState,
    )
}

/**
 * Scope for the composable used to render a Tab indicator, this can be used for more complex
 * indicators requiring layout information about the tabs like [TabRowDefaults.PrimaryIndicator] and
 * [TabRowDefaults.SecondaryIndicator]
 */
interface TabIndicatorScope {

    /**
     * A layout modifier that provides tab positions, this can be used to animate and layout a
     * TabIndicator depending on size, position, and content size of each Tab.
     *
     * @sample androidx.compose.material3.samples.FancyAnimatedIndicatorWithModifier
     */
    fun Modifier.tabIndicatorLayout(
        measure: MeasureScope.(Measurable, Constraints, List<TabPosition>) -> MeasureResult
    ): Modifier

    /**
     * A Modifier that follows the default offset and animation
     *
     * @param selectedTabIndex the index of the current selected tab
     * @param matchContentSize this modifier can also animate the width of the indicator to match
     *   the content size of the tab.
     */
    fun Modifier.tabIndicatorOffset(
        selectedTabIndex: Int,
        matchContentSize: Boolean = false,
    ): Modifier
}

internal interface TabPositionsHolder {

    fun setTabPositions(positions: List<TabPosition>)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabRowImpl(
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    indicator: @Composable TabIndicatorScope.() -> Unit,
    divider: @Composable () -> Unit,
    tabs: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.selectableGroup(),
        color = containerColor,
        contentColor = contentColor,
    ) {
        // TODO Load the motionScheme tokens from the component tokens file
        val tabIndicatorAnimationSpec = MotionSchemeKeyTokens.DefaultSpatial.value<Dp>()
        val scope = remember {
            object : TabIndicatorScope, TabPositionsHolder {

                val tabPositions = mutableStateOf<(List<TabPosition>)>(listOf())

                override fun Modifier.tabIndicatorLayout(
                    measure:
                        MeasureScope.(Measurable, Constraints, List<TabPosition>) -> MeasureResult
                ): Modifier =
                    this.layout { measurable: Measurable, constraints: Constraints ->
                        measure(measurable, constraints, tabPositions.value)
                    }

                override fun Modifier.tabIndicatorOffset(
                    selectedTabIndex: Int,
                    matchContentSize: Boolean,
                ): Modifier =
                    this.then(
                        TabIndicatorModifier(
                            tabPositions,
                            selectedTabIndex,
                            matchContentSize,
                            tabIndicatorAnimationSpec,
                        )
                    )

                override fun setTabPositions(positions: List<TabPosition>) {
                    tabPositions.value = positions
                }
            }
        }

        Layout(
            modifier = Modifier.fillMaxWidth(),
            contents = listOf(tabs, divider, { scope.indicator() }),
        ) { (tabMeasurables, dividerMeasurables, indicatorMeasurables), constraints ->
            val tabRowWidth = constraints.maxWidth
            val tabCount = tabMeasurables.size
            var tabWidth = 0
            if (tabCount > 0) {
                tabWidth = (tabRowWidth / tabCount)
            }
            val tabRowHeight =
                tabMeasurables.fastFold(initial = 0) { max, curr ->
                    maxOf(curr.maxIntrinsicHeight(tabWidth), max)
                }

            scope.setTabPositions(
                List(tabCount) { index ->
                    var contentWidth =
                        minOf(tabMeasurables[index].maxIntrinsicWidth(tabRowHeight), tabWidth)
                            .toDp()
                    contentWidth -= HorizontalTextPadding * 2
                    // Enforce minimum touch target of 24.dp
                    val indicatorWidth = maxOf(contentWidth, 24.dp)

                    TabPosition(tabWidth.toDp() * index, tabWidth.toDp(), indicatorWidth)
                }
            )

            val tabPlaceables =
                tabMeasurables.fastMap {
                    it.measure(
                        constraints.copy(
                            minWidth = tabWidth,
                            maxWidth = tabWidth,
                            minHeight = tabRowHeight,
                            maxHeight = tabRowHeight,
                        )
                    )
                }

            val dividerPlaceables =
                dividerMeasurables.fastMap { it.measure(constraints.copy(minHeight = 0)) }

            val indicatorPlaceables =
                indicatorMeasurables.fastMap {
                    it.measure(
                        constraints.copy(
                            minWidth = tabWidth,
                            maxWidth = tabWidth,
                            minHeight = 0,
                            maxHeight = tabRowHeight,
                        )
                    )
                }

            layout(tabRowWidth, tabRowHeight) {
                tabPlaceables.fastForEachIndexed { index, placeable ->
                    placeable.placeRelative(index * tabWidth, 0)
                }

                dividerPlaceables.fastForEach { placeable ->
                    placeable.placeRelative(0, tabRowHeight - placeable.height)
                }

                indicatorPlaceables.fastForEach { it.placeRelative(0, tabRowHeight - it.height) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScrollableTabRowImpl(
    selectedTabIndex: Int,
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    edgePadding: Dp,
    minTabWidth: Dp,
    scrollState: ScrollState,
    indicator: @Composable TabIndicatorScope.() -> Unit,
    divider: @Composable () -> Unit,
    tabs: @Composable () -> Unit,
) {
    Surface(modifier = modifier, color = containerColor, contentColor = contentColor) {
        val coroutineScope = rememberCoroutineScope()
        // TODO Load the motionScheme tokens from the component tokens file
        val scrollAnimationSpec = MotionSchemeKeyTokens.DefaultSpatial.value<Float>()
        val tabIndicatorAnimationSpec: FiniteAnimationSpec<Dp> =
            MotionSchemeKeyTokens.DefaultSpatial.value()
        val scrollableTabData =
            remember(scrollState, coroutineScope) {
                ScrollableTabData(
                    scrollState = scrollState,
                    coroutineScope = coroutineScope,
                    animationSpec = scrollAnimationSpec,
                )
            }

        val scope = remember {
            object : TabIndicatorScope, TabPositionsHolder {

                val tabPositions = mutableStateOf<(List<TabPosition>)>(listOf())

                override fun Modifier.tabIndicatorLayout(
                    measure:
                        MeasureScope.(Measurable, Constraints, List<TabPosition>) -> MeasureResult
                ): Modifier =
                    this.layout { measurable: Measurable, constraints: Constraints ->
                        measure(measurable, constraints, tabPositions.value)
                    }

                override fun Modifier.tabIndicatorOffset(
                    selectedTabIndex: Int,
                    matchContentSize: Boolean,
                ): Modifier =
                    this.then(
                        TabIndicatorModifier(
                            tabPositions,
                            selectedTabIndex,
                            matchContentSize,
                            tabIndicatorAnimationSpec,
                        )
                    )

                override fun setTabPositions(positions: List<TabPosition>) {
                    tabPositions.value = positions
                }
            }
        }
        Box(contentAlignment = Alignment.BottomStart) {
            divider()
            Layout(
                contents = listOf(tabs, { scope.indicator() }),
                modifier =
                    Modifier.fillMaxWidth()
                        .wrapContentSize(align = Alignment.CenterStart)
                        .horizontalScroll(scrollState)
                        .selectableGroup()
                        .clipToBounds(),
            ) { (tabMeasurables, indicatorMeasurables), constraints ->
                val padding = edgePadding.roundToPx()
                val tabCount = tabMeasurables.size
                val layoutHeight =
                    tabMeasurables.fastFold(initial = 0) { curr, measurable ->
                        maxOf(curr, measurable.maxIntrinsicHeight(Constraints.Infinity))
                    }
                var layoutWidth = padding * 2
                val tabConstraints =
                    constraints.copy(
                        minWidth = minTabWidth.roundToPx(),
                        minHeight = layoutHeight,
                        maxHeight = layoutHeight,
                    )

                var left = edgePadding
                val tabPlaceables = tabMeasurables.fastMap { it.measure(tabConstraints) }
                // Get indicator widths based on incoming content size, not based on forced minimum
                // width applied below.
                val indicatorWidth = mutableIntListOf()
                tabMeasurables.fastForEach {
                    indicatorWidth.add(it.maxIntrinsicWidth(Constraints.Infinity))
                }

                val positions =
                    List(tabCount) { index ->
                        val tabWidth = maxOf(minTabWidth, tabPlaceables[index].width.toDp())
                        layoutWidth += tabWidth.roundToPx()
                        // Enforce minimum touch target of 24.dp
                        val contentWidth =
                            maxOf(indicatorWidth[index].toDp() - (HorizontalTextPadding * 2), 24.dp)
                        val tabPosition =
                            TabPosition(left = left, width = tabWidth, contentWidth = contentWidth)
                        left += tabWidth
                        tabPosition
                    }
                scope.setTabPositions(positions)

                val indicatorPlaceables =
                    indicatorMeasurables.fastMap {
                        it.measure(
                            constraints.copy(
                                minWidth = 0,
                                maxWidth = positions[selectedTabIndex].contentWidth.roundToPx(),
                                minHeight = 0,
                                maxHeight = layoutHeight,
                            )
                        )
                    }

                layout(layoutWidth, layoutHeight) {
                    left = edgePadding
                    tabPlaceables.fastForEachIndexed { index, placeable ->
                        placeable.placeRelative(left.roundToPx(), 0)
                        left += positions[index].width
                    }

                    indicatorPlaceables.fastForEach {
                        val relativeOffset =
                            max(0, (positions[selectedTabIndex].width.roundToPx() - it.width) / 2)
                        it.placeRelative(relativeOffset, layoutHeight - it.height)
                    }

                    scrollableTabData.onLaidOut(
                        density = this@Layout,
                        edgeOffset = padding,
                        tabPositions = positions,
                        selectedTab = selectedTabIndex,
                    )
                }
            }
        }
    }
}

internal data class TabIndicatorModifier(
    val tabPositionsState: State<List<TabPosition>>,
    val selectedTabIndex: Int,
    val followContentSize: Boolean,
    val animationSpec: FiniteAnimationSpec<Dp>,
) : ModifierNodeElement<TabIndicatorOffsetNode>() {

    override fun create(): TabIndicatorOffsetNode {
        return TabIndicatorOffsetNode(
            tabPositionsState = tabPositionsState,
            selectedTabIndex = selectedTabIndex,
            followContentSize = followContentSize,
            animationSpec = animationSpec,
        )
    }

    override fun update(node: TabIndicatorOffsetNode) {
        node.tabPositionsState = tabPositionsState
        node.selectedTabIndex = selectedTabIndex
        node.followContentSize = followContentSize
        node.animationSpec = animationSpec
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}

internal class TabIndicatorOffsetNode(
    var tabPositionsState: State<List<TabPosition>>,
    var selectedTabIndex: Int,
    var followContentSize: Boolean,
    var animationSpec: FiniteAnimationSpec<Dp>,
) : Modifier.Node(), LayoutModifierNode {

    private var offsetAnimatable: Animatable<Dp, AnimationVector1D>? = null
    private var widthAnimatable: Animatable<Dp, AnimationVector1D>? = null
    private var initialOffset: Dp? = null
    private var initialWidth: Dp? = null

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        if (tabPositionsState.value.isEmpty()) {
            return layout(0, 0) {}
        }

        val currentTabWidth =
            if (followContentSize) {
                tabPositionsState.value[selectedTabIndex].contentWidth
            } else {
                tabPositionsState.value[selectedTabIndex].width
            }

        if (initialWidth != null) {
            val widthAnim =
                widthAnimatable
                    ?: Animatable(initialWidth!!, Dp.VectorConverter).also { widthAnimatable = it }

            if (currentTabWidth != widthAnim.targetValue) {
                coroutineScope.launch { widthAnim.animateTo(currentTabWidth, animationSpec) }
            }
        } else {
            initialWidth = currentTabWidth
        }

        val indicatorOffset = tabPositionsState.value[selectedTabIndex].left

        if (initialOffset != null) {
            val offsetAnim =
                offsetAnimatable
                    ?: Animatable(initialOffset!!, Dp.VectorConverter).also {
                        offsetAnimatable = it
                    }

            if (indicatorOffset != offsetAnim.targetValue) {
                coroutineScope.launch { offsetAnim.animateTo(indicatorOffset, animationSpec) }
            }
        } else {
            initialOffset = indicatorOffset
        }

        val offset =
            if (layoutDirection == LayoutDirection.Ltr) {
                offsetAnimatable?.value ?: indicatorOffset
            } else {
                -(offsetAnimatable?.value ?: indicatorOffset)
            }

        val width = widthAnimatable?.value ?: currentTabWidth

        val placeable =
            measurable.measure(
                constraints.copy(minWidth = width.roundToPx(), maxWidth = width.roundToPx())
            )

        return layout(placeable.width, placeable.height) { placeable.place(offset.roundToPx(), 0) }
    }
}

@Suppress("ComposableLambdaInMeasurePolicy")
@Composable
private fun TabRowWithSubcomposeImpl(
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit,
    divider: @Composable () -> Unit,
    tabs: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.selectableGroup(),
        color = containerColor,
        contentColor = contentColor,
    ) {
        SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
            val tabRowWidth = constraints.maxWidth
            val tabMeasurables = subcompose(TabSlots.Tabs, tabs)
            val tabCount = tabMeasurables.size
            var tabWidth = 0
            if (tabCount > 0) {
                tabWidth = (tabRowWidth / tabCount)
            }
            val tabRowHeight =
                tabMeasurables.fastFold(initial = 0) { max, curr ->
                    maxOf(curr.maxIntrinsicHeight(tabWidth), max)
                }

            val tabPlaceables =
                tabMeasurables.fastMap {
                    it.measure(
                        constraints.copy(
                            minWidth = tabWidth,
                            maxWidth = tabWidth,
                            minHeight = tabRowHeight,
                            maxHeight = tabRowHeight,
                        )
                    )
                }

            val tabPositions =
                List(tabCount) { index ->
                    var contentWidth =
                        minOf(tabMeasurables[index].maxIntrinsicWidth(tabRowHeight), tabWidth)
                            .toDp()
                    contentWidth -= HorizontalTextPadding * 2
                    // Enforce minimum touch target of 24.dp
                    val indicatorWidth = maxOf(contentWidth, 24.dp)
                    TabPosition(tabWidth.toDp() * index, tabWidth.toDp(), indicatorWidth)
                }

            layout(tabRowWidth, tabRowHeight) {
                tabPlaceables.fastForEachIndexed { index, placeable ->
                    placeable.placeRelative(index * tabWidth, 0)
                }

                subcompose(TabSlots.Divider, divider).fastForEach {
                    val placeable = it.measure(constraints.copy(minHeight = 0))
                    placeable.placeRelative(0, tabRowHeight - placeable.height)
                }

                subcompose(TabSlots.Indicator) { indicator(tabPositions) }
                    .fastForEach {
                        it.measure(Constraints.fixed(tabRowWidth, tabRowHeight)).placeRelative(0, 0)
                    }
            }
        }
    }
}

@Suppress("ComposableLambdaInMeasurePolicy")
@Composable
private fun ScrollableTabRowWithSubcomposeImpl(
    selectedTabIndex: Int,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit,
    scrollState: ScrollState,
) {
    Surface(modifier = modifier, color = containerColor, contentColor = contentColor) {
        val coroutineScope = rememberCoroutineScope()
        // TODO Load the motionScheme tokens from the component tokens file
        val scrollAnimationSpec = MotionSchemeKeyTokens.DefaultSpatial.value<Float>()
        val scrollableTabData =
            remember(scrollState, coroutineScope) {
                ScrollableTabData(
                    scrollState = scrollState,
                    coroutineScope = coroutineScope,
                    animationSpec = scrollAnimationSpec,
                )
            }
        SubcomposeLayout(
            Modifier.fillMaxWidth()
                .wrapContentSize(align = Alignment.CenterStart)
                .horizontalScroll(scrollState)
                .selectableGroup()
                .clipToBounds()
        ) { constraints ->
            val minTabWidth = TabRowDefaults.ScrollableTabRowMinTabWidth.roundToPx()
            val padding = edgePadding.roundToPx()

            val tabMeasurables = subcompose(TabSlots.Tabs, tabs)

            val layoutHeight =
                tabMeasurables.fastFold(initial = 0) { curr, measurable ->
                    maxOf(curr, measurable.maxIntrinsicHeight(Constraints.Infinity))
                }

            val tabConstraints =
                constraints.copy(
                    minWidth = minTabWidth,
                    minHeight = layoutHeight,
                    maxHeight = layoutHeight,
                )

            val tabPlaceables = mutableListOf<Placeable>()
            val tabContentWidths = mutableListOf<Dp>()
            tabMeasurables.fastForEach {
                val placeable = it.measure(tabConstraints)
                var contentWidth =
                    minOf(it.maxIntrinsicWidth(placeable.height), placeable.width).toDp()
                contentWidth -= HorizontalTextPadding * 2
                tabPlaceables.add(placeable)
                tabContentWidths.add(contentWidth)
            }

            val layoutWidth =
                tabPlaceables.fastFold(initial = padding * 2) { curr, measurable ->
                    curr + measurable.width
                }

            // Position the children.
            layout(layoutWidth, layoutHeight) {
                // Place the tabs
                val tabPositions = mutableListOf<TabPosition>()
                var left = padding
                tabPlaceables.fastForEachIndexed { index, placeable ->
                    placeable.placeRelative(left, 0)
                    tabPositions.add(
                        TabPosition(
                            left = left.toDp(),
                            width = placeable.width.toDp(),
                            contentWidth = tabContentWidths[index],
                        )
                    )
                    left += placeable.width
                }

                // The divider is measured with its own height, and width equal to the total width
                // of the tab row, and then placed on top of the tabs.
                subcompose(TabSlots.Divider, divider).fastForEach {
                    val placeable =
                        it.measure(
                            constraints.copy(
                                minHeight = 0,
                                minWidth = layoutWidth,
                                maxWidth = layoutWidth,
                            )
                        )
                    placeable.placeRelative(0, layoutHeight - placeable.height)
                }

                // The indicator container is measured to fill the entire space occupied by the tab
                // row, and then placed on top of the divider.
                subcompose(TabSlots.Indicator) { indicator(tabPositions) }
                    .fastForEach {
                        it.measure(Constraints.fixed(layoutWidth, layoutHeight)).placeRelative(0, 0)
                    }

                scrollableTabData.onLaidOut(
                    density = this@SubcomposeLayout,
                    edgeOffset = padding,
                    tabPositions = tabPositions,
                    selectedTab = selectedTabIndex,
                )
            }
        }
    }
}

/**
 * Data class that contains information about a tab's position on screen, used for calculating where
 * to place the indicator that shows which tab is selected.
 *
 * @property left the left edge's x position from the start of the [TabRow]
 * @property right the right edge's x position from the start of the [TabRow]
 * @property width the width of this tab
 * @property contentWidth the content width of this tab. Should be a minimum of 24.dp
 */
@Immutable
class TabPosition internal constructor(val left: Dp, val width: Dp, val contentWidth: Dp) {

    val right: Dp
        get() = left + width

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TabPosition) return false

        if (left != other.left) return false
        if (width != other.width) return false
        if (contentWidth != other.contentWidth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + contentWidth.hashCode()
        return result
    }

    override fun toString(): String {
        return "TabPosition(left=$left, right=$right, width=$width, contentWidth=$contentWidth)"
    }
}

/** Contains default implementations and values used for TabRow. */
object TabRowDefaults {
    /**
     * The default minimum width for a tab in a [PrimaryScrollableTabRow] or
     * [SecondaryScrollableTabRow].
     */
    val ScrollableTabRowMinTabWidth = 90.dp

    /**
     * The default padding from the starting edge before a tab in a [PrimaryScrollableTabRow] or
     * [SecondaryScrollableTabRow].
     */
    val ScrollableTabRowEdgeStartPadding = 52.dp

    /** Default container color of a tab row. */
    @Deprecated(
        message = "Use TabRowDefaults.primaryContainerColor instead",
        replaceWith = ReplaceWith("primaryContainerColor"),
    )
    val containerColor: Color
        @Composable get() = PrimaryNavigationTabTokens.ContainerColor.value

    /** Default container color of a [PrimaryTabRow]. */
    val primaryContainerColor: Color
        @Composable get() = PrimaryNavigationTabTokens.ContainerColor.value

    /** Default container color of a [SecondaryTabRow]. */
    val secondaryContainerColor: Color
        @Composable get() = SecondaryNavigationTabTokens.ContainerColor.value

    /** Default content color of a tab row. */
    @Deprecated(
        message = "Use TabRowDefaults.primaryContentColor instead",
        replaceWith = ReplaceWith("primaryContentColor"),
    )
    val contentColor: Color
        @Composable get() = PrimaryNavigationTabTokens.ActiveLabelTextColor.value

    /** Default content color of a [PrimaryTabRow]. */
    val primaryContentColor: Color
        @Composable get() = PrimaryNavigationTabTokens.ActiveLabelTextColor.value

    /** Default content color of a [SecondaryTabRow]. */
    val secondaryContentColor: Color
        @Composable get() = SecondaryNavigationTabTokens.ActiveLabelTextColor.value

    /**
     * Default indicator, which will be positioned at the bottom of the [TabRow], on top of the
     * divider.
     *
     * @param modifier modifier for the indicator's layout
     * @param height height of the indicator
     * @param color color of the indicator
     */
    @Composable
    @Deprecated(
        message = "Use SecondaryIndicator instead.",
        replaceWith = ReplaceWith("SecondaryIndicator(modifier, height, color)"),
    )
    fun Indicator(
        modifier: Modifier = Modifier,
        height: Dp = PrimaryNavigationTabTokens.ActiveIndicatorHeight,
        color: Color =
            MaterialTheme.colorScheme.fromToken(PrimaryNavigationTabTokens.ActiveIndicatorColor),
    ) {
        Box(modifier.fillMaxWidth().height(height).background(color = color))
    }

    /**
     * Primary indicator, which will be positioned at the bottom of the [TabRow], on top of the
     * divider.
     *
     * @param modifier modifier for the indicator's layout
     * @param width width of the indicator
     * @param height height of the indicator
     * @param color color of the indicator
     * @param shape shape of the indicator
     */
    @Composable
    fun PrimaryIndicator(
        modifier: Modifier = Modifier,
        width: Dp = 24.dp,
        height: Dp = PrimaryNavigationTabTokens.ActiveIndicatorHeight,
        color: Color = PrimaryNavigationTabTokens.ActiveIndicatorColor.value,
        shape: Shape = PrimaryNavigationTabTokens.ActiveIndicatorShape,
    ) {
        Spacer(
            modifier
                .requiredHeight(height)
                .requiredWidth(width)
                .background(color = color, shape = shape)
        )
    }

    /**
     * Secondary indicator, which will be positioned at the bottom of the [TabRow], on top of the
     * divider.
     *
     * @param modifier modifier for the indicator's layout
     * @param height height of the indicator
     * @param color color of the indicator
     */
    @Composable
    fun SecondaryIndicator(
        modifier: Modifier = Modifier,
        height: Dp = PrimaryNavigationTabTokens.ActiveIndicatorHeight,
        color: Color = PrimaryNavigationTabTokens.ActiveIndicatorColor.value,
    ) {
        Box(modifier.fillMaxWidth().height(height).background(color = color))
    }

    /**
     * [Modifier] that takes up all the available width inside the [TabRow], and then animates the
     * offset of the indicator it is applied to, depending on the [currentTabPosition].
     *
     * @param currentTabPosition [TabPosition] of the currently selected tab. This is used to
     *   calculate the offset of the indicator this modifier is applied to, as well as its width.
     */
    @Deprecated(
        level = DeprecationLevel.WARNING,
        message =
            "Solely for use alongside deprecated TabRowDefaults.Indicator method. For " +
                "recommended PrimaryIndicator and SecondaryIndicator methods, please use " +
                "TabIndicatorScope.tabIndicatorOffset method.",
    )
    fun Modifier.tabIndicatorOffset(currentTabPosition: TabPosition): Modifier =
        composed(
            inspectorInfo =
                debugInspectorInfo {
                    name = "tabIndicatorOffset"
                    value = currentTabPosition
                }
        ) {
            // TODO Load the motionScheme tokens from the component tokens file
            val currentTabWidth by
                animateDpAsState(
                    targetValue = currentTabPosition.width,
                    animationSpec = MotionSchemeKeyTokens.DefaultSpatial.value(),
                )
            val indicatorOffset by
                animateDpAsState(
                    targetValue = currentTabPosition.left,
                    animationSpec = MotionSchemeKeyTokens.DefaultSpatial.value(),
                )
            fillMaxWidth()
                .wrapContentSize(Alignment.BottomStart)
                .offset { IntOffset(x = indicatorOffset.roundToPx(), y = 0) }
                .width(currentTabWidth)
        }
}

private enum class TabSlots {
    Tabs,
    Divider,
    Indicator,
}

/** Class holding onto state needed for [ScrollableTabRow] */
private class ScrollableTabData(
    private val scrollState: ScrollState,
    private val coroutineScope: CoroutineScope,
    private val animationSpec: FiniteAnimationSpec<Float>,
) {
    private var selectedTab: Int? = null

    fun onLaidOut(
        density: Density,
        edgeOffset: Int,
        tabPositions: List<TabPosition>,
        selectedTab: Int,
    ) {
        // Animate if the new tab is different from the old tab, or this is called for the first
        // time (i.e selectedTab is `null`).
        if (this.selectedTab != selectedTab) {
            this.selectedTab = selectedTab
            tabPositions.getOrNull(selectedTab)?.let {
                // Scrolls to the tab with [tabPosition], trying to place it in the center of the
                // screen or as close to the center as possible.
                val calculatedOffset = it.calculateTabOffset(density, edgeOffset, tabPositions)
                if (scrollState.value != calculatedOffset) {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(calculatedOffset, animationSpec = animationSpec)
                    }
                }
            }
        }
    }

    /**
     * @return the offset required to horizontally center the tab inside this TabRow. If the tab is
     *   at the start / end, and there is not enough space to fully centre the tab, this will just
     *   clamp to the min / max position given the max width.
     */
    private fun TabPosition.calculateTabOffset(
        density: Density,
        edgeOffset: Int,
        tabPositions: List<TabPosition>,
    ): Int =
        with(density) {
            val totalTabRowWidth = tabPositions.last().right.roundToPx() + edgeOffset
            val visibleWidth = totalTabRowWidth - scrollState.maxValue
            val tabOffset = left.roundToPx()
            val scrollerCenter = visibleWidth / 2
            val tabWidth = width.roundToPx()
            val centeredTabOffset = tabOffset - (scrollerCenter - tabWidth / 2)
            // How much space we have to scroll. If the visible width is <= to the total width, then
            // we have no space to scroll as everything is always visible.
            val availableSpace = (totalTabRowWidth - visibleWidth).coerceAtLeast(0)
            return centeredTabOffset.coerceIn(0, availableSpace)
        }
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Maintained for Binary Compatibility.")
@Composable
fun PrimaryScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    indicator: @Composable TabIndicatorScope.() -> Unit =
        @Composable {
            TabRowDefaults.PrimaryIndicator(
                Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true),
                width = Dp.Unspecified,
            )
        },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit,
) =
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        scrollState = scrollState,
        containerColor = containerColor,
        contentColor = contentColor,
        edgePadding = edgePadding,
        indicator = indicator,
        divider = divider,
        minTabWidth = TabRowDefaults.ScrollableTabRowMinTabWidth,
        tabs = tabs,
    )

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Maintained for Binary Compatibility.")
@Composable
fun SecondaryScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    containerColor: Color = TabRowDefaults.secondaryContainerColor,
    contentColor: Color = TabRowDefaults.secondaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    indicator: @Composable TabIndicatorScope.() -> Unit =
        @Composable {
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = false)
            )
        },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit,
) =
    SecondaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        scrollState = scrollState,
        containerColor = containerColor,
        contentColor = contentColor,
        edgePadding = edgePadding,
        indicator = indicator,
        divider = divider,
        minTabWidth = TabRowDefaults.ScrollableTabRowMinTabWidth,
        tabs = tabs,
    )

/**
 * [Material Design tabs](https://m3.material.io/components/tabs/overview)
 *
 * Material Design fixed tabs.
 *
 * For primary indicator tabs, use [PrimaryTabRow]. For secondary indicator tabs, use
 * [SecondaryTabRow].
 *
 * Fixed tabs display all tabs in a set simultaneously. They are best for switching between related
 * content quickly, such as between transportation methods in a map. To navigate between fixed tabs,
 * tap an individual tab, or swipe left or right in the content area.
 *
 * A TabRow contains a row of [Tab]s, and displays an indicator underneath the currently selected
 * tab. A TabRow places its tabs evenly spaced along the entire row, with each tab taking up an
 * equal amount of space. See [ScrollableTabRow] for a tab row that does not enforce equal size, and
 * allows scrolling to tabs that do not fit on screen.
 *
 * A simple example with text tabs looks like:
 *
 * @sample androidx.compose.material3.samples.TextTabs
 *
 * You can also provide your own custom tab, such as:
 *
 * @sample androidx.compose.material3.samples.FancyTabs
 *
 * Where the custom tab itself could look like:
 *
 * @sample androidx.compose.material3.samples.FancyTab
 *
 * As well as customizing the tab, you can also provide a custom [indicator], to customize the
 * indicator displayed for a tab. [indicator] will be placed to fill the entire TabRow, so it should
 * internally take care of sizing and positioning the indicator to match changes to
 * [selectedTabIndex].
 *
 * For example, given an indicator that draws a rounded rectangle near the edges of the [Tab]:
 *
 * @sample androidx.compose.material3.samples.FancyIndicator
 *
 * We can reuse [TabRowDefaults.tabIndicatorOffset] and just provide this indicator, as we aren't
 * changing how the size and position of the indicator changes between tabs:
 *
 * @sample androidx.compose.material3.samples.FancyIndicatorTabs
 *
 * You may also want to use a custom transition, to allow you to dynamically change the appearance
 * of the indicator as it animates between tabs, such as changing its color or size. [indicator] is
 * stacked on top of the entire TabRow, so you just need to provide a custom transition that
 * animates the offset of the indicator from the start of the TabRow. For example, take the
 * following example that uses a transition to animate the offset, width, and color of the same
 * FancyIndicator from before, also adding a physics based 'spring' effect to the indicator in the
 * direction of motion:
 *
 * @sample androidx.compose.material3.samples.FancyAnimatedIndicatorWithModifier
 *
 * We can now just pass this indicator directly to TabRow:
 *
 * @sample androidx.compose.material3.samples.FancyIndicatorContainerTabs
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.SecondaryIndicator], using a [TabRowDefaults.tabIndicatorOffset]
 *   modifier to animate its position. Note that this indicator will be forced to fill up the entire
 *   tab row, so you should use [TabRowDefaults.tabIndicatorOffset] or similar to animate the actual
 *   drawn indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 *   separation between the tab row and the content displayed underneath.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the row, each taking up equal
 *   space.
 */
@Composable
@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "Replaced with PrimaryTabRow and SecondaryTabRow.",
    replaceWith =
        ReplaceWith(
            "SecondaryTabRow(selectedTabIndex, modifier, containerColor, contentColor, indicator, divider, tabs)"
        ),
)
@Suppress("DEPRECATION")
fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit =
        @Composable { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                )
            }
        },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit,
) {
    TabRowWithSubcomposeImpl(modifier, containerColor, contentColor, indicator, divider, tabs)
}

/**
 * [Material Design tabs](https://m3.material.io/components/tabs/overview)
 *
 * Material Design scrollable tabs.
 *
 * For primary indicator tabs, use [PrimaryScrollableTabRow]. For secondary indicator tabs, use
 * [SecondaryScrollableTabRow].
 *
 * When a set of tabs cannot fit on screen, use scrollable tabs. Scrollable tabs can use longer text
 * labels and a larger number of tabs. They are best used for browsing on touch interfaces.
 *
 * A ScrollableTabRow contains a row of [Tab]s, and displays an indicator underneath the currently
 * selected tab. A ScrollableTabRow places its tabs offset from the starting edge, and allows
 * scrolling to tabs that are placed off screen. For a fixed tab row that does not allow scrolling,
 * and evenly places its tabs, see [TabRow].
 *
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param edgePadding the padding between the starting and ending edge of the scrollable tab row,
 *   and the tabs inside the row. This padding helps inform the user that this tab row can be
 *   scrolled, unlike a [TabRow].
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.SecondaryIndicator], using a [TabRowDefaults.tabIndicatorOffset]
 *   modifier to animate its position. Note that this indicator will be forced to fill up the entire
 *   tab row, so you should use [TabRowDefaults.tabIndicatorOffset] or similar to animate the actual
 *   drawn indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 *   separation between the tab row and the content displayed underneath.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the row, each taking up equal
 *   space.
 */
@Composable
@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "Replaced with PrimaryScrollableTabRow and SecondaryScrollableTabRow tab variants.",
    replaceWith =
        ReplaceWith(
            "SecondaryScrollableTabRow(selectedTabIndex, modifier, containerColor, contentColor, edgePadding, indicator, divider, tabs)"
        ),
)
@Suppress("DEPRECATION")
fun ScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit =
        @Composable { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
            )
        },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit,
) {
    ScrollableTabRowWithSubcomposeImpl(
        selectedTabIndex = selectedTabIndex,
        indicator = indicator,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        edgePadding = edgePadding,
        divider = divider,
        tabs = tabs,
        scrollState = rememberScrollState(),
    )
}
```

