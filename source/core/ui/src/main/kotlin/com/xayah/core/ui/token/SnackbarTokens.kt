package com.xayah.core.ui.token

import androidx.compose.ui.unit.dp
import com.xayah.core.ui.material3.tokens.ElevationTokens
import com.xayah.core.ui.material3.tokens.ShapeKeyTokens
import com.xayah.core.ui.material3.tokens.TypographyKeyTokens
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens

internal object SnackbarTokens {
    val ActionLabelTextColor = ThemedColorSchemeKeyTokens.Primary
    val ActionLabelTextFont = TypographyKeyTokens.LabelLarge
    val ContainerColor = ThemedColorSchemeKeyTokens.SurfaceContainerHighestBaselineFixed
    val ContainerElevation = ElevationTokens.Level0
    val ContainerShape = ShapeKeyTokens.CornerExtraSmall
    val IconColor = ThemedColorSchemeKeyTokens.OnSurface
    val IconSize = 24.0.dp
    val SupportingTextColor = ThemedColorSchemeKeyTokens.OnSurface
    val SupportingTextFont = TypographyKeyTokens.BodyMedium
    val SingleLineContainerHeight = 48.0.dp
    val TwoLinesContainerHeight = 68.0.dp
}
