package com.xayah.feature.main.task.packages.local.restore.processing

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
import com.xayah.feature.main.task.packages.common.component.ProcessingCard
import com.xayah.feature.main.task.packages.common.component.ProcessingScaffold
import com.xayah.feature.main.task.packages.local.R

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageProcessing(navController: NavHostController) {
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val taskState by viewModel.taskState.collectAsStateWithLifecycle()
    val taskTimerState by viewModel.taskTimerState.collectAsStateWithLifecycle()
    val packages by viewModel.packagesState.collectAsStateWithLifecycle()
    val packagesApkOnly by viewModel.packagesApkOnlyState.collectAsStateWithLifecycle()
    val packagesDataOnly by viewModel.packagesDataOnlyState.collectAsStateWithLifecycle()
    val packagesBoth by viewModel.packagesBothState.collectAsStateWithLifecycle()
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
        remainingCount = (packages.size - operationsSucceed.size - operationsFailed.size).coerceAtLeast(0),
        succeedCount = operationsSucceed.size,
        failedCount = operationsFailed.size,
        timer = taskTimerState,
        processingState = uiState.processingState,
        packagesApkOnly = packagesApkOnly,
        packagesDataOnly = packagesDataOnly,
        packagesBoth = packagesBoth,
        packageItemKey = { it.packageName },
        operationsProcessing = operationsProcessing,
        operationsFailed = operationsFailed,
        operationsSucceed = operationsSucceed,
        operationItemKey = { "${it.packageState}-${it.id}" },
        onProcess = {
            viewModel.emitIntent(IndexUiIntent.Process)
        },
        packageItem = { item ->
            ProcessingCard(
                modifier = Modifier.fillMaxWidth(),
                packageRestore = item,
                onCardClick = {},
                onCardLongClick = {}
            )
        },
        operationItem = { item ->
            ProcessingCard(
                modifier = Modifier.fillMaxWidth(),
                packageRestoreOp = item,
                onCardClick = {},
                onCardLongClick = {}
            )
        },
    )
}
