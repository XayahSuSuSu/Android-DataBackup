package com.xayah.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.xayah.core.model.OperationState
import com.xayah.core.model.R
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens

val OperationState.icon: ImageVector
    @Composable
    get() = when (this) {
        OperationState.IDLE -> {
            ImageVector.vectorResource(id = R.drawable.ic_rounded_adjust_circle)
        }

        OperationState.SKIP -> {
            ImageVector.vectorResource(id = R.drawable.ic_rounded_not_started_circle)
        }

        OperationState.PROCESSING -> {
            ImageVector.vectorResource(id = R.drawable.ic_rounded_pending_circle)
        }

        OperationState.UPLOADING -> {
            ImageVector.vectorResource(id = R.drawable.ic_rounded_arrow_circle_up)
        }

        OperationState.DOWNLOADING -> {
            ImageVector.vectorResource(id = R.drawable.ic_rounded_arrow_circle_down)
        }

        OperationState.DONE -> {
            ImageVector.vectorResource(id = R.drawable.ic_rounded_check_circle)
        }

        OperationState.ERROR -> {
            ImageVector.vectorResource(id = R.drawable.ic_rounded_cancel_circle)
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

