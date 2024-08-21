package com.xayah.core.ui.util

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.xayah.core.model.OperationState
import com.xayah.core.model.database.Info
import com.xayah.core.model.database.ProcessingInfoEntity
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.CircularProgressIndicator
import com.xayah.core.ui.model.ProcessingCardItem
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.SizeTokens

@Composable
fun OperationState.StateView(enabled: Boolean = true, expanded: Boolean = false, isProcessing: Boolean = false) {
    when (this) {
        OperationState.PROCESSING -> {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(SizeTokens.Level24),
                    strokeCap = StrokeCap.Round,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(SizeTokens.Level24),
                    strokeCap = StrokeCap.Round,
                    progress = 0f,
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
                            ThemedColorSchemeKeyTokens.OnSurfaceVariant
                        else
                            ThemedColorSchemeKeyTokens.OutlineVariant
                        ).value.withState(enabled)
            )
        }

        else -> {
            Icon(
                modifier = Modifier.size(SizeTokens.Level24),
                imageVector = (
                        when (this) {
                            OperationState.DONE -> ImageVector.vectorResource(id = R.drawable.ic_rounded_check_circle)
                            OperationState.ERROR -> ImageVector.vectorResource(id = R.drawable.ic_rounded_cancel)
                            OperationState.SKIP -> ImageVector.vectorResource(id = R.drawable.ic_rounded_not_started)
                            OperationState.UPLOADING -> ImageVector.vectorResource(id = R.drawable.ic_rounded_arrow_circle_up)
                            OperationState.DOWNLOADING -> ImageVector.vectorResource(id = R.drawable.ic_rounded_arrow_circle_down)
                            else -> Icons.Rounded.Circle
                        }),
                contentDescription = null,
                tint = (
                        when (this) {
                            OperationState.ERROR -> ThemedColorSchemeKeyTokens.Error
                            OperationState.SKIP -> ThemedColorSchemeKeyTokens.YellowPrimary
                            OperationState.UPLOADING, OperationState.DOWNLOADING -> ThemedColorSchemeKeyTokens.GreenPrimary
                            else -> ThemedColorSchemeKeyTokens.Primary
                        }
                        ).value.withState(enabled)
            )
        }
    }
}

val Info.toProcessingCardItem: ProcessingCardItem
    get() {
        return ProcessingCardItem(
            state = state,
            progress = progress,
            title = title,
            log = log,
            content = content
        )
    }

val ProcessingInfoEntity.toProcessingCardItem: ProcessingCardItem
    get() {
        return ProcessingCardItem(
            state = state,
            progress = progress,
            title = title,
            log = log,
            content = content
        )
    }

fun MutableList<ProcessingCardItem>.addInfo(info: Info) {
    add(info.toProcessingCardItem)
}

fun MutableList<ProcessingCardItem>.addInfo(info: ProcessingInfoEntity) {
    add(info.toProcessingCardItem)
}
