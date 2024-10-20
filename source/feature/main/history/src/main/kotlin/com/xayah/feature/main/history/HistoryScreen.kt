package com.xayah.feature.main.history

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.model.OpType
import com.xayah.core.model.TaskType
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.DateUtil
import com.xayah.core.util.maybePopBackStack
import com.xayah.core.util.navigateSingle

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current!!
    val uiState: HistoryUiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(uiState)
    LaunchedEffect(uiState) {
        if (uiState is HistoryUiState.Error) {
            navController.maybePopBackStack()
        }
    }
}

@SuppressLint("StringFormatInvalid")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
internal fun HistoryScreen(uiState: HistoryUiState) {
    val context = LocalContext.current
    val navController = LocalNavController.current!!
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryLargeTopBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(R.string.history),
            )
        }
    ) { innerPadding ->
        AnimatedContent(uiState, label = AnimationTokens.AnimatedContentLabel) { state ->
            when (state) {
                is HistoryUiState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            InnerTopSpacer(innerPadding = innerPadding)
                        }

                        items(items = state.items) {
                            val desc by remember(it.endTimestamp, it.opType) {
                                mutableStateOf(
                                    context.getString(
                                        when (it.opType) {
                                            OpType.BACKUP -> R.string.args_backed_up_at
                                            OpType.RESTORE -> R.string.args_restored_at
                                        }, DateUtil.formatTimestamp(it.endTimestamp, DateUtil.PATTERN_FINISH)
                                    )
                                )
                            }
                            val interactionSource = remember { MutableInteractionSource() }
                            Clickable(
                                title = stringResource(
                                    when (it.taskType) {
                                        TaskType.PACKAGE -> R.string.apps
                                        TaskType.MEDIA -> R.string.files
                                    }
                                ),
                                value = desc,
                                leadingIcon = ImageVector.vectorResource(
                                    id = when (it.opType) {
                                        OpType.BACKUP -> R.drawable.ic_rounded_acute
                                        OpType.RESTORE -> R.drawable.ic_rounded_history
                                    }
                                ),
                                interactionSource = interactionSource,
                            ) {
                                navController.navigateSingle(MainRoutes.TaskDetails.getRoute(it.id))
                            }
                        }

                        item {
                            InnerBottomSpacer(innerPadding = innerPadding)
                        }
                    }
                }

                else -> {}
            }
        }
    }
}
