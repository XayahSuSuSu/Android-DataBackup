package com.xayah.feature.setup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens

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

    val icon: ImageVector
        @Composable
        get() = when (this) {
            Idle -> ImageVector.vectorResource(id = R.drawable.ic_rounded_package_2)
            Processing -> Icons.Rounded.MoreHoriz
            Succeed -> Icons.Rounded.Done
            Failed -> Icons.Rounded.PriorityHigh
        }
}
