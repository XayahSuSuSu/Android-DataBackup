package com.xayah.feature.setup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PriorityHigh
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromVector

sealed class EnvState {
    data object Idle : EnvState()
    data object Processing : EnvState()
    data object Succeed : EnvState()
    data object Failed : EnvState()

    val colorContainer: ColorSchemeKeyTokens
        get() = when (this) {
            Idle -> {
                ColorSchemeKeyTokens.SurfaceContainerHighBaselineFixed
            }

            Processing -> {
                ColorSchemeKeyTokens.SecondaryContainer
            }

            Succeed -> {
                ColorSchemeKeyTokens.PrimaryContainer
            }

            Failed -> {
                ColorSchemeKeyTokens.ErrorContainer
            }
        }

    val colorL80D20: ColorSchemeKeyTokens
        get() = when (this) {
            Idle -> {
                ColorSchemeKeyTokens.SurfaceDimBaselineFixed
            }

            Processing -> {
                ColorSchemeKeyTokens.OnSecondaryContainer
            }

            Succeed -> {
                ColorSchemeKeyTokens.OnPrimaryContainer
            }

            Failed -> {
                ColorSchemeKeyTokens.OnErrorContainer
            }
        }

    val onColorContainer: ColorSchemeKeyTokens
        get() = when (this) {
            Idle -> {
                ColorSchemeKeyTokens.OnSurface
            }

            Processing -> {
                ColorSchemeKeyTokens.SecondaryContainer
            }

            Succeed -> {
                ColorSchemeKeyTokens.PrimaryContainer
            }

            Failed -> {
                ColorSchemeKeyTokens.ErrorContainer
            }
        }

    val icon: ImageVectorToken
        get() = when (this) {
            Idle -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_package_2)
            Processing -> ImageVectorToken.fromVector(Icons.Rounded.MoreHoriz)
            Succeed -> ImageVectorToken.fromVector(Icons.Rounded.Done)
            Failed -> ImageVectorToken.fromVector(Icons.Rounded.PriorityHigh)
        }
}
