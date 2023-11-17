package com.xayah.feature.guide.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Pending
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromVector

sealed class EnvState {
    object Idle : EnvState()
    object Processing : EnvState()
    object Succeed : EnvState()
    object Failed : EnvState()

    val backgroundColor: ColorSchemeKeyTokens
        get() = when (this) {
            Idle -> {
                ColorSchemeKeyTokens.SurfaceVariant
            }

            Processing -> {
                ColorSchemeKeyTokens.Secondary
            }

            Succeed -> {
                ColorSchemeKeyTokens.Primary
            }

            Failed -> {
                ColorSchemeKeyTokens.Error
            }
        }

    val tint: ColorSchemeKeyTokens
        get() = when (this) {
            Idle -> {
                ColorSchemeKeyTokens.OnSurfaceVariant
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
            Idle -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_star)
            Processing -> ImageVectorToken.fromVector(Icons.Rounded.Pending)
            Succeed -> ImageVectorToken.fromVector(Icons.Rounded.Done)
            Failed -> ImageVectorToken.fromVector(Icons.Rounded.Close)
        }
}

data class EnvItem(
    val content: StringResourceToken,
    val state: EnvState,
) {
    val enabled: Boolean
        get() = state == EnvState.Idle || state == EnvState.Failed

    val succeed: Boolean
        get() = state == EnvState.Succeed
}
