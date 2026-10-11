package com.xayah.databackup.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** Display values only; backup and restore retain ownership of their outcome and progress semantics. */
internal data class ProcessItemCardState(
    val icon: ImageVector,
    val title: String,
    val label: String,
    val subtitle: String,
    val progress: Float = 0f,
    val isProcessing: Boolean = false,
)

@Composable
internal fun ProcessItemCard(
    category: ProcessItemCardState,
    isTerminal: Boolean,
    currentApp: ProcessItemCardState? = null,
) {
    val canShowCurrentApp = !isTerminal && currentApp != null
    var isCurrentAppVisible by rememberSaveable(isTerminal, canShowCurrentApp) { mutableStateOf(false) }
    val displayedItem = currentApp.takeIf { isCurrentAppVisible && canShowCurrentApp } ?: category
    ProcessItemHolder(
        modifier = Modifier.fillMaxWidth(),
        process = { displayedItem.progress.coerceIn(0f, 1f) },
        showProgress = !isTerminal && displayedItem.isProcessing,
    ) {
        ProcessItemCardContainer(
            onClick = if (canShowCurrentApp) {
                { isCurrentAppVisible = !isCurrentAppVisible }
            } else {
                null
            },
        ) {
            AnimatedContent(
                targetState = isCurrentAppVisible && canShowCurrentApp,
                transitionSpec = fadeSlideContentTransitionSpec(),
                label = "currentAppTransition",
            ) { isShowingApp ->
                val item = currentApp.takeIf { isShowingApp } ?: category
                ProcessItemCardContent(
                    icon = item.icon,
                    title = item.title,
                    label = item.label,
                    subtitle = item.subtitle,
                    subtitleShimmer = false,
                )
            }
        }
    }
}
