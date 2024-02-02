package com.xayah.core.ui.util

import com.xayah.core.model.OperationState
import com.xayah.core.model.R
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken

val OperationState.icon: ImageVectorToken
    get() = when (this) {
        OperationState.IDLE -> {
            ImageVectorToken.fromDrawable(R.drawable.ic_rounded_adjust_circle)
        }

        OperationState.SKIP -> {
            ImageVectorToken.fromDrawable(R.drawable.ic_rounded_not_started_circle)
        }

        OperationState.PROCESSING -> {
            ImageVectorToken.fromDrawable(R.drawable.ic_rounded_pending_circle)
        }

        OperationState.UPLOADING -> {
            ImageVectorToken.fromDrawable(R.drawable.ic_rounded_arrow_circle_up)
        }

        OperationState.DOWNLOADING -> {
            ImageVectorToken.fromDrawable(R.drawable.ic_rounded_arrow_circle_down)
        }

        OperationState.DONE -> {
            ImageVectorToken.fromDrawable(R.drawable.ic_rounded_check_circle)
        }

        OperationState.ERROR -> {
            ImageVectorToken.fromDrawable(R.drawable.ic_rounded_cancel_circle)
        }
    }

val OperationState.color: ColorSchemeKeyTokens
    get() = when (this) {
        OperationState.PROCESSING -> {
            ColorSchemeKeyTokens.SecondaryContainer
        }

        OperationState.UPLOADING, OperationState.DOWNLOADING -> {
            ColorSchemeKeyTokens.SecondaryContainer
        }

        OperationState.DONE -> {
            ColorSchemeKeyTokens.PrimaryContainer
        }

        OperationState.ERROR -> {
            ColorSchemeKeyTokens.ErrorContainer
        }

        else -> ColorSchemeKeyTokens.Primary
    }

val OperationState.containerColor: ColorSchemeKeyTokens
    get() = when (this) {
        OperationState.PROCESSING -> {
            ColorSchemeKeyTokens.Secondary
        }

        OperationState.UPLOADING, OperationState.DOWNLOADING -> {
            ColorSchemeKeyTokens.Secondary
        }

        OperationState.DONE -> {
            ColorSchemeKeyTokens.Primary
        }

        OperationState.ERROR -> {
            ColorSchemeKeyTokens.Error
        }

        else -> ColorSchemeKeyTokens.Transparent
    }

