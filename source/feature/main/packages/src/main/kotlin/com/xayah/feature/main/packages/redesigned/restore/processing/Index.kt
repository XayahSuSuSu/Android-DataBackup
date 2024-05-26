package com.xayah.feature.main.packages.redesigned.restore.processing

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.core.model.OperationState
import com.xayah.core.ui.component.AnimatedTextContainer
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.ProcessingCard
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.component.pagerAnimation
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.value
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.withMainContext
import com.xayah.feature.main.packages.R
import com.xayah.feature.main.packages.redesigned.ProcessingSetupScaffold
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PagePackagesRestoreProcessing(localNavController: NavHostController, viewModel: IndexViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val task by viewModel.task.collectAsStateWithLifecycle()
    val preItems by viewModel.preItems.collectAsStateWithLifecycle()
    val postItems by viewModel.postItems.collectAsStateWithLifecycle()
    val packageItems by viewModel.packageItems.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val navController = LocalNavController.current!!
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val progress: Float by remember(task?.processingIndex, packageItems.size) {
        mutableFloatStateOf(
            if (task != null)
                task!!.processingIndex.toFloat() / (packageItems.size + 1)
            else
                -1f
        )
    }

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Initialize)
    }

    val onBack: () -> Unit = remember {
        {
            if (uiState.state == OperationState.PROCESSING) {
                viewModel.launchOnIO {
                    if (dialogState.confirm(title = StringResourceToken.fromStringId(R.string.prompt), text = StringResourceToken.fromStringId(R.string.processing_exit_confirmation))) {
                        BaseUtil.kill("tar", "root")
                        viewModel.emitIntent(IndexUiIntent.DestroyService)
                        withMainContext {
                            navController.popBackStack()
                        }
                    }
                }
            } else {
                viewModel.launchOnIO {
                    viewModel.emitIntent(IndexUiIntent.DestroyService)
                    withMainContext {
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    BackHandler {
        onBack()
    }

    ProcessingSetupScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = StringResourceToken.fromStringId(R.string.processing),
        progress = progress,
        actions = {
            Button(enabled = uiState.state == OperationState.IDLE || uiState.state == OperationState.DONE, onClick = {
                if (uiState.state == OperationState.IDLE) viewModel.emitIntentOnIO(IndexUiIntent.Restore)
                else navController.popBackStack()
            }) {
                AnimatedTextContainer(targetState = if (uiState.state == OperationState.DONE) StringResourceToken.fromStringId(R.string.finish).value else StringResourceToken.fromStringId(R.string._continue).value) { text ->
                    Text(text = text)
                }
            }
        },
        onBackClick = {
            onBack()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .paddingVertical(SizeTokens.Level12)
        ) {
            val pagerState = key(packageItems.size) { rememberPagerState(pageCount = { packageItems.size + 2 }) }

            LaunchedEffect(task, preItems) {
                if (task != null) pagerState.animateScrollToPage(task!!.processingIndex)
            }

            HorizontalPager(state = pagerState, contentPadding = PaddingValues(horizontal = SizeTokens.Level48)) { page ->
                when (page) {
                    0 -> ProcessingCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .pagerAnimation(pagerState, page),
                        progress = -1f,
                        title = StringResourceToken.fromStringId(R.string.preprocessing),
                        defExpanded = true,
                        items = preItems
                    )

                    pagerState.pageCount - 1 -> ProcessingCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .pagerAnimation(pagerState, page),
                        progress = -1f,
                        title = StringResourceToken.fromStringId(R.string.post_processing),
                        defExpanded = true,
                        items = postItems
                    )

                    else -> ProcessingCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .pagerAnimation(pagerState, page),
                        progress = -1f,
                        title = packageItems[page - 1].title,
                        packageName = packageItems[page - 1].packageName,
                        defExpanded = true,
                        items = packageItems[page - 1].items
                    )
                }
            }
        }
    }
}
