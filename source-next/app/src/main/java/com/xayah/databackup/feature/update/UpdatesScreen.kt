package com.xayah.databackup.feature.update

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.FadeVisibility
import com.xayah.databackup.ui.component.MarkdownContent
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun UpdatesScreen(
    navController: NavHostController,
    viewModel: UpdatesViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(context = Dispatchers.IO, null) {
        viewModel.initialize()
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.update),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStackSafely() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            val errorStatus = uiState.status as? UpdatesStatus.Error
            UpdateOverviewCard(
                uiState = uiState,
                onCheckUpdates = { viewModel.refresh() },
            )

            FadeVisibility(visible = errorStatus != null) {
                ErrorCard(
                    error = errorStatus?.message.orEmpty(),
                    rateLimitResetLabel = errorStatus?.rateLimitResetLabel,
                )
            }

            FadeVisibility(visible = uiState.latestNotes.isNotBlank()) {
                ReleaseNotesCard(notes = uiState.latestNotes)
            }

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun UpdateOverviewCard(
    uiState: UpdatesUiState,
    onCheckUpdates: () -> Unit,
) {
    val context = LocalContext.current
    val statusText = remember(uiState.status) {
        when (uiState.status) {
            UpdatesStatus.Loading,
            UpdatesStatus.Refreshing -> context.getString(R.string.checking_updates)

            UpdatesStatus.UpdateAvailable -> context.getString(R.string.new_version_available)
            UpdatesStatus.UpToDate -> context.getString(R.string.version_up_to_date)
            is UpdatesStatus.Error -> context.getString(R.string.loading_failed)
        }
    }
    val actionText = remember(uiState.status) {
        when (uiState.status) {
            UpdatesStatus.Loading, UpdatesStatus.Refreshing -> context.getString(R.string.checking_updates)
            UpdatesStatus.UpdateAvailable -> context.getString(R.string.download)
            UpdatesStatus.UpToDate, is UpdatesStatus.Error -> context.getString(R.string.check_updates)
        }
    }
    val colorScheme = MaterialTheme.colorScheme
    val statusColor = remember(uiState.status, colorScheme) {
        when (uiState.status) {
            UpdatesStatus.UpdateAvailable -> colorScheme.primary
            is UpdatesStatus.Error -> colorScheme.error
            else -> colorScheme.onSurfaceVariant
        }
    }
    val updateAvailable = remember(uiState.status) {
        uiState.status == UpdatesStatus.UpdateAvailable
    }
    val checkingUpdates = remember(uiState.status) {
        uiState.status == UpdatesStatus.Loading || uiState.status == UpdatesStatus.Refreshing
    }

    val buttonContainerColor = remember(updateAvailable, colorScheme) {
        if (updateAvailable) colorScheme.tertiary else colorScheme.primary
    }
    val backgroundBrush = remember(updateAvailable, colorScheme) {
        Brush.linearGradient(
            colors = if (updateAvailable) {
                listOf(
                    colorScheme.primaryContainer,
                    colorScheme.tertiaryContainer.copy(alpha = 0.92f)
                )
            } else {
                listOf(
                    colorScheme.surfaceContainerHigh,
                    colorScheme.surfaceContainer
                )
            }
        )
    }
    val transition = rememberInfiniteTransition(label = "updatesCardTransition")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "updatesGlowAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .background(backgroundBrush)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                GlowDecoration(
                    modifier = Modifier.align(Alignment.TopEnd),
                    color = MaterialTheme.colorScheme.primary,
                    alpha = glowAlpha,
                    size = 160.dp,
                    offsetX = 50f,
                    offsetY = -50f,
                )
                GlowDecoration(
                    modifier = Modifier.align(Alignment.BottomStart),
                    color = MaterialTheme.colorScheme.tertiary,
                    alpha = glowAlpha * 0.7f,
                    size = 120.dp,
                    offsetX = -36f,
                    offsetY = 36f,
                )

                Column {
                    OverviewStatusRow(
                        modifier = Modifier.padding(bottom = 16.dp),
                        statusText = statusText,
                        statusColor = statusColor,
                    )

                    Row(modifier = Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        VersionCell(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.current),
                            value = uiState.currentVersion
                        )
                        VersionCell(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.latest),
                            value = uiState.latestVersion.takeIf { it.isNotBlank() } ?: stringResource(R.string.unknown)
                        )
                    }

                    UpdateActionButton(
                        enabled = checkingUpdates.not() && updateAvailable.not(),
                        containerColor = buttonContainerColor,
                        actionText = actionText,
                        onClick = onCheckUpdates,
                    )
                }
            }
            FadeVisibility(visible = checkingUpdates) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GlowDecoration(
    modifier: Modifier,
    color: Color,
    alpha: Float,
    size: Dp,
    offsetX: Float,
    offsetY: Float,
) {
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                this.alpha = alpha
                translationX = offsetX
                translationY = offsetY
            }
            .background(
                brush = Brush.radialGradient(colors = listOf(color, Color.Transparent)),
                shape = CircleShape
            )
    )
}

@Composable
private fun OverviewStatusRow(
    modifier: Modifier,
    statusText: String,
    statusColor: Color,
) {
    val transition = rememberInfiniteTransition(label = "statusRowTransition")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "updatesIconPulse"
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    },
                imageVector = ImageVector.vectorResource(R.drawable.ic_clock_arrow_up),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            AnimatedContent(
                targetState = statusText,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "updateStatusAnimation"
            ) { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UpdateActionButton(
    enabled: Boolean,
    containerColor: Color,
    actionText: String,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        onClick = onClick,
    ) {
        AnimatedContent(
            targetState = actionText,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "updatesActionButtonAnimation"
        ) { text ->
            Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun VersionCell(
    modifier: Modifier,
    title: String,
    value: String,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    rateLimitResetLabel: String?,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (rateLimitResetLabel != null) {
                RateLimitBanner(label = rateLimitResetLabel)
            }
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (rateLimitResetLabel != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { openReleasePage(context = context) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = stringResource(R.string.github))
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = ImageVector.vectorResource(R.drawable.ic_external_link),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RateLimitBanner(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_clock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Text(
            text = stringResource(R.string.retry_after, label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun ReleaseNotesCard(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MarkdownContent(markdown = notes)
        }
    }
}

private const val GITHUB_RELEASES_URL = "https://github.com/XayahSuSuSu/Android-DataBackup/releases"

private fun openReleasePage(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, GITHUB_RELEASES_URL.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        Toast.makeText(context, context.getString(R.string.no_browser), Toast.LENGTH_SHORT).show()
    }
}
