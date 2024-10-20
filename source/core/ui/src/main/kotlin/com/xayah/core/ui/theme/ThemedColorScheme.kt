package com.xayah.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.xayah.core.ui.material3.BaselineTonalPalette

@Immutable
data class ColorFamily(
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val l80d20: Color,
)

@Immutable
class ThemedColorScheme(
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

    val surfaceBrightBaselineFixed: Color,
    val surfaceContainerBaselineFixed: Color,
    val surfaceContainerHighBaselineFixed: Color,
    val surfaceContainerHighestBaselineFixed: Color,
    val surfaceContainerLowBaselineFixed: Color,
    val surfaceContainerLowestBaselineFixed: Color,
    val surfaceDimBaselineFixed: Color,
    val surfaceVariantDim: Color,
    val primaryL80D20: Color,
    val secondaryL80D20: Color,
    val tertiaryL80D20: Color,
    val errorL80D20: Color,

    val yellow: ColorFamily,
    val blue: ColorFamily,
    val green: ColorFamily,
    val red: ColorFamily,
    val pink: ColorFamily,
    val purple: ColorFamily,
    val orange: ColorFamily,
)

@Stable
fun ThemedColorScheme.fromToken(value: ThemedColorSchemeKeyTokens): Color {
    return when (value) {
        ThemedColorSchemeKeyTokens.Background -> background
        ThemedColorSchemeKeyTokens.Error -> error
        ThemedColorSchemeKeyTokens.ErrorContainer -> errorContainer
        ThemedColorSchemeKeyTokens.InverseOnSurface -> inverseOnSurface
        ThemedColorSchemeKeyTokens.InversePrimary -> inversePrimary
        ThemedColorSchemeKeyTokens.InverseSurface -> inverseSurface
        ThemedColorSchemeKeyTokens.OnBackground -> onBackground
        ThemedColorSchemeKeyTokens.OnError -> onError
        ThemedColorSchemeKeyTokens.OnErrorContainer -> onErrorContainer
        ThemedColorSchemeKeyTokens.OnPrimary -> onPrimary
        ThemedColorSchemeKeyTokens.OnPrimaryContainer -> onPrimaryContainer
        ThemedColorSchemeKeyTokens.OnSecondary -> onSecondary
        ThemedColorSchemeKeyTokens.OnSecondaryContainer -> onSecondaryContainer
        ThemedColorSchemeKeyTokens.OnSurface -> onSurface
        ThemedColorSchemeKeyTokens.OnSurfaceVariant -> onSurfaceVariant
        ThemedColorSchemeKeyTokens.SurfaceTint -> surfaceTint
        ThemedColorSchemeKeyTokens.OnTertiary -> onTertiary
        ThemedColorSchemeKeyTokens.OnTertiaryContainer -> onTertiaryContainer
        ThemedColorSchemeKeyTokens.Outline -> outline
        ThemedColorSchemeKeyTokens.OutlineVariant -> outlineVariant
        ThemedColorSchemeKeyTokens.Primary -> primary
        ThemedColorSchemeKeyTokens.PrimaryContainer -> primaryContainer
        ThemedColorSchemeKeyTokens.Scrim -> scrim
        ThemedColorSchemeKeyTokens.Secondary -> secondary
        ThemedColorSchemeKeyTokens.SecondaryContainer -> secondaryContainer
        ThemedColorSchemeKeyTokens.Surface -> surface
        ThemedColorSchemeKeyTokens.SurfaceVariant -> surfaceVariant
        ThemedColorSchemeKeyTokens.SurfaceBright -> surfaceBright
        ThemedColorSchemeKeyTokens.SurfaceContainer -> surfaceContainer
        ThemedColorSchemeKeyTokens.SurfaceContainerHigh -> surfaceContainerHigh
        ThemedColorSchemeKeyTokens.SurfaceContainerHighest -> surfaceContainerHighest
        ThemedColorSchemeKeyTokens.SurfaceContainerLow -> surfaceContainerLow
        ThemedColorSchemeKeyTokens.SurfaceContainerLowest -> surfaceContainerLowest
        ThemedColorSchemeKeyTokens.SurfaceDim -> surfaceDim
        ThemedColorSchemeKeyTokens.Tertiary -> tertiary
        ThemedColorSchemeKeyTokens.TertiaryContainer -> tertiaryContainer

        ThemedColorSchemeKeyTokens.SurfaceBrightBaselineFixed -> surfaceBrightBaselineFixed
        ThemedColorSchemeKeyTokens.SurfaceContainerBaselineFixed -> surfaceContainerBaselineFixed
        ThemedColorSchemeKeyTokens.SurfaceContainerHighBaselineFixed -> surfaceContainerHighBaselineFixed
        ThemedColorSchemeKeyTokens.SurfaceContainerHighestBaselineFixed -> surfaceContainerHighestBaselineFixed
        ThemedColorSchemeKeyTokens.SurfaceContainerLowBaselineFixed -> surfaceContainerLowBaselineFixed
        ThemedColorSchemeKeyTokens.SurfaceContainerLowestBaselineFixed -> surfaceContainerLowestBaselineFixed
        ThemedColorSchemeKeyTokens.SurfaceDimBaselineFixed -> surfaceDimBaselineFixed
        ThemedColorSchemeKeyTokens.SurfaceVariantDim -> surfaceVariantDim
        ThemedColorSchemeKeyTokens.PrimaryL80D20 -> primaryL80D20
        ThemedColorSchemeKeyTokens.SecondaryL80D20 -> secondaryL80D20
        ThemedColorSchemeKeyTokens.TertiaryL80D20 -> tertiaryL80D20
        ThemedColorSchemeKeyTokens.ErrorL80D20 -> errorL80D20

        ThemedColorSchemeKeyTokens.YellowPrimary -> yellow.primary
        ThemedColorSchemeKeyTokens.YellowPrimaryContainer -> yellow.primaryContainer
        ThemedColorSchemeKeyTokens.YellowOnPrimaryContainer -> yellow.onPrimaryContainer
        ThemedColorSchemeKeyTokens.YellowL80D20 -> yellow.l80d20
        ThemedColorSchemeKeyTokens.BluePrimary -> blue.primary
        ThemedColorSchemeKeyTokens.BluePrimaryContainer -> blue.primaryContainer
        ThemedColorSchemeKeyTokens.BlueOnPrimaryContainer -> blue.onPrimaryContainer
        ThemedColorSchemeKeyTokens.BlueL80D20 -> blue.l80d20
        ThemedColorSchemeKeyTokens.GreenPrimary -> green.primary
        ThemedColorSchemeKeyTokens.GreenPrimaryContainer -> green.primaryContainer
        ThemedColorSchemeKeyTokens.GreenOnPrimaryContainer -> green.onPrimaryContainer
        ThemedColorSchemeKeyTokens.GreenL80D20 -> green.l80d20
        ThemedColorSchemeKeyTokens.RedPrimary -> red.primary
        ThemedColorSchemeKeyTokens.RedPrimaryContainer -> red.primaryContainer
        ThemedColorSchemeKeyTokens.RedOnPrimaryContainer -> red.onPrimaryContainer
        ThemedColorSchemeKeyTokens.RedL80D20 -> red.l80d20
        ThemedColorSchemeKeyTokens.PinkPrimary -> pink.primary
        ThemedColorSchemeKeyTokens.PinkPrimaryContainer -> pink.primaryContainer
        ThemedColorSchemeKeyTokens.PinkOnPrimaryContainer -> pink.onPrimaryContainer
        ThemedColorSchemeKeyTokens.PinkL80D20 -> pink.l80d20
        ThemedColorSchemeKeyTokens.PurplePrimary -> purple.primary
        ThemedColorSchemeKeyTokens.PurplePrimaryContainer -> purple.primaryContainer
        ThemedColorSchemeKeyTokens.PurpleOnPrimaryContainer -> purple.onPrimaryContainer
        ThemedColorSchemeKeyTokens.PurpleL80D20 -> purple.l80d20
        ThemedColorSchemeKeyTokens.OrangePrimary -> orange.primary
        ThemedColorSchemeKeyTokens.OrangePrimaryContainer -> orange.primaryContainer
        ThemedColorSchemeKeyTokens.OrangeOnPrimaryContainer -> orange.onPrimaryContainer
        ThemedColorSchemeKeyTokens.OrangeL80D20 -> orange.l80d20

        ThemedColorSchemeKeyTokens.Transparent -> Color.Transparent
        ThemedColorSchemeKeyTokens.Unspecified -> Color.Unspecified

        else -> Color.Unspecified
    }
}

@Stable
fun Color.withState(enabled: Boolean = true) = if (enabled) this else this.copy(alpha = DisabledAlpha)

fun lightThemedColorScheme(
    primary: Color = ThemedColorLightTokens.Primary,
    onPrimary: Color = ThemedColorLightTokens.OnPrimary,
    primaryContainer: Color = ThemedColorLightTokens.PrimaryContainer,
    onPrimaryContainer: Color = ThemedColorLightTokens.OnPrimaryContainer,
    inversePrimary: Color = ThemedColorLightTokens.InversePrimary,
    secondary: Color = ThemedColorLightTokens.Secondary,
    onSecondary: Color = ThemedColorLightTokens.OnSecondary,
    secondaryContainer: Color = ThemedColorLightTokens.SecondaryContainer,
    onSecondaryContainer: Color = ThemedColorLightTokens.OnSecondaryContainer,
    tertiary: Color = ThemedColorLightTokens.Tertiary,
    onTertiary: Color = ThemedColorLightTokens.OnTertiary,
    tertiaryContainer: Color = ThemedColorLightTokens.TertiaryContainer,
    onTertiaryContainer: Color = ThemedColorLightTokens.OnTertiaryContainer,
    background: Color = ThemedColorLightTokens.Background,
    onBackground: Color = ThemedColorLightTokens.OnBackground,
    surface: Color = ThemedColorLightTokens.Surface,
    onSurface: Color = ThemedColorLightTokens.OnSurface,
    surfaceVariant: Color = ThemedColorLightTokens.SurfaceVariant,
    onSurfaceVariant: Color = ThemedColorLightTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ThemedColorLightTokens.InverseSurface,
    inverseOnSurface: Color = ThemedColorLightTokens.InverseOnSurface,
    error: Color = ThemedColorLightTokens.Error,
    onError: Color = ThemedColorLightTokens.OnError,
    errorContainer: Color = ThemedColorLightTokens.ErrorContainer,
    onErrorContainer: Color = ThemedColorLightTokens.OnErrorContainer,
    outline: Color = ThemedColorLightTokens.Outline,
    outlineVariant: Color = ThemedColorLightTokens.OutlineVariant,
    scrim: Color = ThemedColorLightTokens.Scrim,
    surfaceBright: Color = ThemedColorLightTokens.SurfaceBright,
    surfaceContainer: Color = ThemedColorLightTokens.SurfaceContainer,
    surfaceContainerHigh: Color = ThemedColorLightTokens.SurfaceContainerHigh,
    surfaceContainerHighest: Color = ThemedColorLightTokens.SurfaceContainerHighest,
    surfaceContainerLow: Color = ThemedColorLightTokens.SurfaceContainerLow,
    surfaceContainerLowest: Color = ThemedColorLightTokens.SurfaceContainerLowest,
    surfaceDim: Color = ThemedColorLightTokens.SurfaceDim,
    surfaceVariantDim: Color = ThemedColorLightTokens.SurfaceVariantDim,
    primaryL80D20: Color = ThemedColorLightTokens.PrimaryL80D20,
    secondaryL80D20: Color = ThemedColorLightTokens.SecondaryL80D20,
    tertiaryL80D20: Color = ThemedColorLightTokens.TertiaryL80D20,
    errorL80D20: Color = ThemedColorLightTokens.ErrorL80D20,
): ThemedColorScheme =
    ThemedColorScheme(
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
        surfaceBrightBaselineFixed = BaselineTonalPalette.neutral98,
        surfaceContainerBaselineFixed = BaselineTonalPalette.neutral94,
        surfaceContainerHighBaselineFixed = BaselineTonalPalette.neutral92,
        surfaceContainerHighestBaselineFixed = BaselineTonalPalette.neutral90,
        surfaceContainerLowBaselineFixed = BaselineTonalPalette.neutral96,
        surfaceContainerLowestBaselineFixed = BaselineTonalPalette.neutral100,
        surfaceDimBaselineFixed = BaselineTonalPalette.neutral87,
        surfaceVariantDim = surfaceVariantDim,
        primaryL80D20 = primaryL80D20,
        secondaryL80D20 = secondaryL80D20,
        tertiaryL80D20 = tertiaryL80D20,
        errorL80D20 = errorL80D20,
        yellow = ColorFamily(
            Yellow40,
            Yellow90,
            Yellow10,
            Yellow80,
        ),
        blue = ColorFamily(
            Blue40,
            Blue90,
            Blue10,
            Blue80,
        ),
        green = ColorFamily(
            Green40,
            Green90,
            Green10,
            Green80,
        ),
        red = ColorFamily(
            Red40,
            Red90,
            Red10,
            Red80,
        ),
        pink = ColorFamily(
            Pink40,
            Pink90,
            Pink10,
            Pink80,
        ),
        purple = ColorFamily(
            Purple40,
            Purple90,
            Purple10,
            Purple80,
        ),
        orange = ColorFamily(
            Orange40,
            Orange90,
            Orange10,
            Orange80,
        ),
    )

fun darkThemedColorScheme(
    primary: Color = ThemedColorDarkTokens.Primary,
    onPrimary: Color = ThemedColorDarkTokens.OnPrimary,
    primaryContainer: Color = ThemedColorDarkTokens.PrimaryContainer,
    onPrimaryContainer: Color = ThemedColorDarkTokens.OnPrimaryContainer,
    inversePrimary: Color = ThemedColorDarkTokens.InversePrimary,
    secondary: Color = ThemedColorDarkTokens.Secondary,
    onSecondary: Color = ThemedColorDarkTokens.OnSecondary,
    secondaryContainer: Color = ThemedColorDarkTokens.SecondaryContainer,
    onSecondaryContainer: Color = ThemedColorDarkTokens.OnSecondaryContainer,
    tertiary: Color = ThemedColorDarkTokens.Tertiary,
    onTertiary: Color = ThemedColorDarkTokens.OnTertiary,
    tertiaryContainer: Color = ThemedColorDarkTokens.TertiaryContainer,
    onTertiaryContainer: Color = ThemedColorDarkTokens.OnTertiaryContainer,
    background: Color = ThemedColorDarkTokens.Background,
    onBackground: Color = ThemedColorDarkTokens.OnBackground,
    surface: Color = ThemedColorDarkTokens.Surface,
    onSurface: Color = ThemedColorDarkTokens.OnSurface,
    surfaceVariant: Color = ThemedColorDarkTokens.SurfaceVariant,
    onSurfaceVariant: Color = ThemedColorDarkTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ThemedColorDarkTokens.InverseSurface,
    inverseOnSurface: Color = ThemedColorDarkTokens.InverseOnSurface,
    error: Color = ThemedColorDarkTokens.Error,
    onError: Color = ThemedColorDarkTokens.OnError,
    errorContainer: Color = ThemedColorDarkTokens.ErrorContainer,
    onErrorContainer: Color = ThemedColorDarkTokens.OnErrorContainer,
    outline: Color = ThemedColorDarkTokens.Outline,
    outlineVariant: Color = ThemedColorDarkTokens.OutlineVariant,
    scrim: Color = ThemedColorDarkTokens.Scrim,
    surfaceBright: Color = ThemedColorDarkTokens.SurfaceBright,
    surfaceContainer: Color = ThemedColorDarkTokens.SurfaceContainer,
    surfaceContainerHigh: Color = ThemedColorDarkTokens.SurfaceContainerHigh,
    surfaceContainerHighest: Color = ThemedColorDarkTokens.SurfaceContainerHighest,
    surfaceContainerLow: Color = ThemedColorDarkTokens.SurfaceContainerLow,
    surfaceContainerLowest: Color = ThemedColorDarkTokens.SurfaceContainerLowest,
    surfaceDim: Color = ThemedColorDarkTokens.SurfaceDim,
    surfaceVariantDim: Color = ThemedColorDarkTokens.SurfaceVariantDim,
    primaryL80D20: Color = ThemedColorDarkTokens.PrimaryL80D20,
    secondaryL80D20: Color = ThemedColorDarkTokens.SecondaryL80D20,
    tertiaryL80D20: Color = ThemedColorDarkTokens.TertiaryL80D20,
    errorL80D20: Color = ThemedColorDarkTokens.ErrorL80D20,
): ThemedColorScheme =
    ThemedColorScheme(
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
        surfaceBrightBaselineFixed = BaselineTonalPalette.neutral24,
        surfaceContainerBaselineFixed = BaselineTonalPalette.neutral12,
        surfaceContainerHighBaselineFixed = BaselineTonalPalette.neutral17,
        surfaceContainerHighestBaselineFixed = BaselineTonalPalette.neutral22,
        surfaceContainerLowBaselineFixed = BaselineTonalPalette.neutral10,
        surfaceContainerLowestBaselineFixed = BaselineTonalPalette.neutral4,
        surfaceDimBaselineFixed = BaselineTonalPalette.neutral6,
        surfaceVariantDim = surfaceVariantDim,
        primaryL80D20 = primaryL80D20,
        secondaryL80D20 = secondaryL80D20,
        tertiaryL80D20 = tertiaryL80D20,
        errorL80D20 = errorL80D20,
        yellow = ColorFamily(
            Yellow80,
            Yellow30,
            Yellow90,
            Yellow20,
        ),
        blue = ColorFamily(
            Blue80,
            Blue30,
            Blue90,
            Blue20,
        ),
        green = ColorFamily(
            Green80,
            Green30,
            Green90,
            Green20,
        ),
        red = ColorFamily(
            Red80,
            Red30,
            Red90,
            Red20,
        ),
        pink = ColorFamily(
            Pink80,
            Pink30,
            Pink90,
            Pink20,
        ),
        purple = ColorFamily(
            Purple80,
            Purple30,
            Purple90,
            Purple20,
        ),
        orange = ColorFamily(
            Orange80,
            Orange30,
            Orange90,
            Orange20,
        ),
    )

internal val LocalThemedColorScheme = staticCompositionLocalOf { lightThemedColorScheme() }

val MaterialTheme.themedColorScheme: ThemedColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalThemedColorScheme.current

val ThemedColorSchemeKeyTokens.value: Color
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.themedColorScheme.fromToken(this)

const val DisabledAlpha = 0.38f
