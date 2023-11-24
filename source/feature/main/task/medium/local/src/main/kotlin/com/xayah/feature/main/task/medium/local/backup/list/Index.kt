package com.xayah.feature.main.task.medium.local.backup.list

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.core.ui.component.ActionChip
import com.xayah.core.ui.component.AnimatedRoundChip
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.feature.main.task.medium.common.component.ListScaffold
import com.xayah.feature.main.task.medium.common.component.MediumCard
import com.xayah.feature.main.task.medium.common.component.MediumCardShimmer
import com.xayah.feature.main.task.medium.local.R
import com.xayah.feature.main.task.medium.local.backup.TaskMediumBackupRoutes

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageList(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarState by viewModel.topBarState.collectAsStateWithLifecycle()
    val mediumState by viewModel.mediumState.collectAsStateWithLifecycle()
    val mediumSelected by viewModel.mediumSelectedState.collectAsStateWithLifecycle()
    val mediumNotSelected by viewModel.mediumNotSelectedState.collectAsStateWithLifecycle()
    val shimmering by viewModel.shimmeringState.collectAsStateWithLifecycle()

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Update)
    }

    ListScaffold(
        snackbarHostState = viewModel.snackbarHostState,
        topBarState = topBarState,
        fabVisible = true,
        fabEmphasizedState = uiState.emphasizedState,
        fabSelectedState = mediumSelected.isNotEmpty(),
        selectedDataCount = mediumSelected.size,
        shimmering = shimmering,
        shimmerCount = uiState.shimmerCount,
        selectedItems = mediumSelected,
        notSelectedItems = mediumNotSelected,
        itemKey = { "${it.path} - ${it.selected}" },
        onFabClick = {
            if (mediumSelected.isEmpty()) viewModel.emitIntent(IndexUiIntent.Emphasize)
            else navController.navigate(TaskMediumBackupRoutes.Processing.route)
        },
        onSearchTextChange = { text ->
            viewModel.emitIntent(IndexUiIntent.FilterByKey(key = text))
        },
        actionChipGroup = { targetState ->
            ActionChip(
                enabled = targetState.not(),
                label = StringResourceToken.fromStringId(R.string.add),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Add),
                onClick = {
                    viewModel.emitIntent(IndexUiIntent.AddMedia(context = context))
                },
            )
            ActionChip(
                enabled = targetState.not(),
                label = StringResourceToken.fromStringArgs(
                    StringResourceToken.fromStringId(R.string.batching_select),
                    StringResourceToken.fromString("(${uiState.batchSelection.size})"),
                ),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Checklist),
                onClick = {
                    viewModel.emitIntent(IndexUiIntent.BatchingSelectAll)
                },
            )
            var batchingDataSelection by remember { mutableStateOf(true) }
            ActionChip(
                enabled = targetState.not(),
                label = StringResourceToken.fromStringId(R.string.data),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.TripOrigin),
                onClick = {
                    viewModel.launchOnIO {
                        val pathList = uiState.batchSelection.ifEmpty { mediumState.map { it.path } }

                        if (batchingDataSelection)
                            viewModel.emitIntent(IndexUiIntent.BatchSelectOp(selected = true, pathList = pathList))
                        else
                            viewModel.emitIntent(IndexUiIntent.BatchSelectOp(selected = false, pathList = pathList))

                        batchingDataSelection = batchingDataSelection.not()
                    }
                },
            )
        },
        shimmerItem = {
            MediumCardShimmer()
        }
    ) { item ->
        MediumCard(
            cardSelected = item.path in uiState.batchSelection,
            mediaBackup = item,
            onDataSelected = {
                viewModel.emitIntent(
                    IndexUiIntent.UpdateMedia(
                        entity = item.copy(selected = item.selected.not())
                    )
                )
            },
            onCardClick = {
                if (uiState.batchSelection.isNotEmpty()) {
                    viewModel.emitIntent(IndexUiIntent.BatchingSelect(path = item.path))
                } else {
                    viewModel.emitIntent(
                        IndexUiIntent.UpdateMedia(
                            entity = item.copy(selected = item.selected.not())
                        )
                    )
                }
            },
            onCardLongClick = {
                viewModel.emitIntent(IndexUiIntent.BatchingSelect(path = item.path))
            },
            onUpdate = {
                viewModel.emitIntent(IndexUiIntent.RefreshMedia(entity = item))
            },
        ) {
            AnimatedRoundChip(text = item.sizeDisplay)
        }
    }
}
