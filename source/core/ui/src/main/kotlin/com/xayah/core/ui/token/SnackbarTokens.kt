package com.xayah.core.ui.token

import androidx.compose.ui.unit.dp
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.material3.tokens.ElevationTokens
import com.xayah.core.ui.material3.tokens.ShapeKeyTokens
import com.xayah.core.ui.material3.tokens.TypographyKeyTokens

internal object SnackbarTokens {
    val ActionLabelTextColor = ColorSchemeKeyTokens.Primary
    val ActionLabelTextFont = TypographyKeyTokens.LabelLarge
    val ContainerColor = ColorSchemeKeyTokens.SurfaceContainerHighestBaselineFixed
    val ContainerElevation = ElevationTokens.Level0
    val ContainerShape = ShapeKeyTokens.CornerExtraSmall
    val IconColor = ColorSchemeKeyTokens.OnSurface
    val IconSize = 24.0.dp
    val SupportingTextColor = ColorSchemeKeyTokens.OnSurface
    val SupportingTextFont = TypographyKeyTokens.BodyMedium
    val SingleLineContainerHeight = 48.0.dp
    val TwoLinesContainerHeight = 68.0.dp
}
