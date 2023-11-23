package com.xayah.feature.main.task.medium.local.restore.processing

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromStringId
import com.xayah.feature.main.task.medium.common.component.ProcessingCard
import com.xayah.feature.main.task.medium.common.component.ProcessingScaffold
import com.xayah.feature.main.task.medium.local.R

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageProcessing(navController: NavHostController) {
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val taskState by viewModel.taskState.collectAsStateWithLifecycle()
    val taskTimerState by viewModel.taskTimerState.collectAsStateWithLifecycle()
    val medium by viewModel.mediumState.collectAsStateWithLifecycle()
    val operationsProcessing by viewModel.operationsProcessingState.collectAsStateWithLifecycle()
    val operationsFailed by viewModel.operationsFailedState.collectAsStateWithLifecycle()
    val operationsSucceed by viewModel.operationsSucceedState.collectAsStateWithLifecycle()

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Initialize)
    }

    ProcessingScaffold(
        topBarTitle = StringResourceToken.fromStringId(R.string.restore),
        snackHostState = viewModel.snackbarHostState,
        navController = navController,
        targetPath = taskState?.path ?: "",
        availableBytes = taskState?.availableBytes ?: 0.0,
        rawBytes = taskState?.rawBytes ?: 0.0,
        totalBytes = taskState?.totalBytes ?: 0.0,
        remainingCount = (medium.size - operationsSucceed.size - operationsFailed.size).coerceAtLeast(0),
        succeedCount = operationsSucceed.size,
        failedCount = operationsFailed.size,
        timer = taskTimerState,
        processingState = uiState.processingState,
        medium = medium,
        packageItemKey = { it.path },
        operationsProcessing = operationsProcessing,
        operationsFailed = operationsFailed,
        operationsSucceed = operationsSucceed,
        operationItemKey = { "${it.path}-${it.id}" },
        onProcess = {
            viewModel.emitIntent(IndexUiIntent.Process)
        },
        packageItem = { item ->
            ProcessingCard(
                modifier = Modifier.fillMaxWidth(),
                mediaRestore = item,
                onCardClick = {},
                onCardLongClick = {}
            )
        },
        operationItem = { item ->
            ProcessingCard(
                modifier = Modifier.fillMaxWidth(),
                mediaRestoreOp = item,
                onCardClick = {},
                onCardLongClick = {}
            )
        },
    )
}
