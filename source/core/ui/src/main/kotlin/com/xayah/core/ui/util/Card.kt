package com.xayah.core.ui.util

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import com.xayah.core.model.OperationState
import com.xayah.core.model.database.Info
import com.xayah.core.model.database.ProcessingInfoEntity
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.CircularProgressIndicator
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.ProcessingCardItem
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens

@Composable
fun OperationState.StateView(enabled: Boolean = true, expanded: Boolean = false, progress: Float = -1f) {
    when (this) {
        OperationState.PROCESSING -> {
            if (progress == -1f) {
                CircularProgressIndicator(
                    modifier = Modifier.size(SizeTokens.Level24),
                    strokeCap = StrokeCap.Round,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(SizeTokens.Level24),
                    strokeCap = StrokeCap.Round,
                    progress = progress,
                )
            }
        }

        OperationState.IDLE -> {
            CircularProgressIndicator(
                modifier = Modifier.size(SizeTokens.Level24),
                strokeCap = StrokeCap.Round,
                progress = 0f,
                trackColor = (
                        if (expanded)
                            ColorSchemeKeyTokens.OnSurfaceVariant
                        else
                            ColorSchemeKeyTokens.OutlineVariant
                        ).toColor(enabled)
            )
        }

        else -> {
            Icon(
                modifier = Modifier.size(SizeTokens.Level24),
                imageVector = (
                        when (this) {
                            OperationState.DONE -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_check_circle)
                            OperationState.ERROR -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_cancel)
                            OperationState.SKIP -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_not_started)
                            OperationState.UPLOADING -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_arrow_circle_up)
                            OperationState.DOWNLOADING -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_arrow_circle_down)
                            else -> ImageVectorToken.fromVector(Icons.Rounded.Circle)
                        }
                        ).value,
                contentDescription = null,
                tint = (
                        when (this) {
                            OperationState.ERROR -> ColorSchemeKeyTokens.Error
                            OperationState.SKIP -> ColorSchemeKeyTokens.YellowPrimary
                            OperationState.UPLOADING, OperationState.DOWNLOADING -> ColorSchemeKeyTokens.GreenPrimary
                            else -> ColorSchemeKeyTokens.Primary
                        }
                        ).toColor(enabled)
            )
        }
    }
}

val Info.toProcessingCardItem: ProcessingCardItem
    get() {
        return ProcessingCardItem(
            state = state,
            progress = progress,
            title = StringResourceToken.fromString(title),
            log = StringResourceToken.fromString(log),
            content = StringResourceToken.fromString(content)
        )
    }

val ProcessingInfoEntity.toProcessingCardItem: ProcessingCardItem
    get() {
        return ProcessingCardItem(
            state = state,
            progress = progress,
            title = StringResourceToken.fromString(title),
            log = StringResourceToken.fromString(log),
            content = StringResourceToken.fromString(content)
        )
    }

fun MutableList<ProcessingCardItem>.addInfo(info: Info) {
    add(info.toProcessingCardItem)
}

fun MutableList<ProcessingCardItem>.addInfo(info: ProcessingInfoEntity) {
    add(info.toProcessingCardItem)
}
