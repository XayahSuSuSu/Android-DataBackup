package com.xayah.databackup.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R

@Composable
internal fun TaskProgressScaffold(
    overallProgress: String,
    statusLabel: String,
    isProcessing: Boolean,
    isCanceling: Boolean,
    isTerminal: Boolean,
    isBackEnabled: Boolean,
    notice: String,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Consume system back even while navigation is blocked during startup or cleanup.
    BackHandler {
        if (isBackEnabled) {
            onBack()
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = isBackEnabled) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_arrow_left), stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.surfaceTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            TaskProgressHeader(
                progress = overallProgress,
                statusLabel = statusLabel,
                isLoading = isProcessing,
            )
            AnimatedVisibility(visible = isCanceling) {
                InlineNotice(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    text = stringResource(R.string.wait_for_remaining_data_processing),
                    icon = ImageVector.vectorResource(R.drawable.ic_badge_info),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            AnimatedVisibility(visible = notice.isNotEmpty()) {
                InlineNotice(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    text = notice,
                    icon = ImageVector.vectorResource(R.drawable.ic_circle_x),
                )
            }
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                content()
            }
            FadeVisibility(visible = isTerminal) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onFinish) {
                        Text(text = stringResource(R.string.finish))
                    }
                }
            }
        }
    }
}
