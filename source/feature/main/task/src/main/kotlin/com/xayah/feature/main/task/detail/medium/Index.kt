package com.xayah.feature.main.task.detail.medium

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.value
import com.xayah.feature.main.task.DetailScaffold
import com.xayah.feature.main.task.R
import com.xayah.feature.main.task.TaskMediaItemCard

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageTaskMediaDetail() {
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val taskState by viewModel.taskState.collectAsStateWithLifecycle()
    val internalTimerState by viewModel.internalTimerState.collectAsStateWithLifecycle()
    val taskTimerState by viewModel.taskTimerState.collectAsStateWithLifecycle()
    val taskProcessingDetailsState by viewModel.taskProcessingDetailsState.collectAsStateWithLifecycle()
    val taskSuccessDetailsState by viewModel.taskSuccessDetailsState.collectAsStateWithLifecycle()
    val taskFailureDetailsState by viewModel.taskFailureDetailsState.collectAsStateWithLifecycle()
    val snackbarHostState = viewModel.snackbarHostState
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    DetailScaffold(
        scrollBehavior, snackbarHostState, uiState.taskId, taskState, internalTimerState, taskTimerState
    ) {
        if (taskProcessingDetailsState.isNotEmpty()) {
            item {
                TitleMediumText(text = StringResourceToken.fromStringId(R.string.processing).value, fontWeight = FontWeight.Bold)
            }

            items(items = taskProcessingDetailsState, key = { "${it.id}p" }) { item ->
                AnimatedContent(
                    modifier = Modifier.animateItemPlacement(),
                    targetState = item,
                    label = AnimationTokens.AnimatedContentLabel
                ) {
                    TaskMediaItemCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        item = it
                    )
                }
            }
        }

        if (taskFailureDetailsState.isNotEmpty()) {
            item {
                TitleMediumText(text = StringResourceToken.fromStringId(R.string.failed).value, fontWeight = FontWeight.Bold)
            }

            items(items = taskFailureDetailsState, key = { "${it.id}f" }) { item ->
                AnimatedContent(
                    modifier = Modifier.animateItemPlacement(),
                    targetState = item,
                    label = AnimationTokens.AnimatedContentLabel
                ) {
                    TaskMediaItemCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        item = it
                    )
                }
            }
        }

        if (taskSuccessDetailsState.isNotEmpty()) {
            item {
                TitleMediumText(text = StringResourceToken.fromStringId(R.string.succeed).value, fontWeight = FontWeight.Bold)
            }

            items(items = taskSuccessDetailsState, key = { "${it.id}s" }) { item ->
                AnimatedContent(
                    modifier = Modifier.animateItemPlacement(),
                    targetState = item,
                    label = AnimationTokens.AnimatedContentLabel
                ) {
                    TaskMediaItemCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        item = it
                    )
                }
            }
        }
    }
}
