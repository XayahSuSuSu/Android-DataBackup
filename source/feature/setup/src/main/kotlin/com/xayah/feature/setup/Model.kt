package com.xayah.feature.setup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens

sealed class EnvState {
    data object Idle : EnvState()
    data object Processing : EnvState()
    data object Succeed : EnvState()
    data object Failed : EnvState()

    val colorContainer: ThemedColorSchemeKeyTokens
        get() = when (this) {
            Idle -> {
                ThemedColorSchemeKeyTokens.SurfaceContainerHighBaselineFixed
            }

            Processing -> {
                ThemedColorSchemeKeyTokens.SecondaryContainer
            }

            Succeed -> {
                ThemedColorSchemeKeyTokens.PrimaryContainer
            }

            Failed -> {
                ThemedColorSchemeKeyTokens.ErrorContainer
            }
        }

    val colorL80D20: ThemedColorSchemeKeyTokens
        get() = when (this) {
            Idle -> {
                ThemedColorSchemeKeyTokens.SurfaceDimBaselineFixed
            }

            Processing -> {
                ThemedColorSchemeKeyTokens.OnSecondaryContainer
            }

            Succeed -> {
                ThemedColorSchemeKeyTokens.OnPrimaryContainer
            }

            Failed -> {
                ThemedColorSchemeKeyTokens.OnErrorContainer
            }
        }

    val onColorContainer: ThemedColorSchemeKeyTokens
        get() = when (this) {
            Idle -> {
                ThemedColorSchemeKeyTokens.OnSurface
            }

            Processing -> {
                ThemedColorSchemeKeyTokens.SecondaryContainer
            }

            Succeed -> {
                ThemedColorSchemeKeyTokens.PrimaryContainer
            }

            Failed -> {
                ThemedColorSchemeKeyTokens.ErrorContainer
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
