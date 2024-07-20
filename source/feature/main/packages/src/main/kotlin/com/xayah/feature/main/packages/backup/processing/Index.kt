package com.xayah.feature.main.packages.backup.processing

import android.annotation.SuppressLint
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.saveScreenOffCountDown
import com.xayah.core.model.OperationState
import com.xayah.core.ui.component.AnimatedTextContainer
import com.xayah.core.ui.component.AppsReportCard
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.ProcessingCard
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.component.pagerAnimation
import com.xayah.core.ui.material3.SnackbarDuration
import com.xayah.core.ui.material3.SnackbarType
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.withMainContext
import com.xayah.feature.main.packages.ProcessingSetupScaffold
import com.xayah.feature.main.packages.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay

@SuppressLint("StringFormatInvalid")
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PagePackagesBackupProcessing(viewModel: IndexViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val task by viewModel.task.collectAsStateWithLifecycle()
    val preItems by viewModel.preItems.collectAsStateWithLifecycle()
    val postItems by viewModel.postItems.collectAsStateWithLifecycle()
    val packageItems by viewModel.packageItems.collectAsStateWithLifecycle()
    val packageSize by viewModel.packageSize.collectAsStateWithLifecycle()
    val packageSucceed by viewModel.packageSucceed.collectAsStateWithLifecycle()
    val packageFailed by viewModel.packageFailed.collectAsStateWithLifecycle()
    val timer by viewModel.timer.collectAsStateWithLifecycle()
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
    val screenOffCountDown by viewModel.screenOffCountDown.collectAsStateWithLifecycle()

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(IndexUiIntent.Initialize)
    }

    LaunchedEffect(screenOffCountDown, uiState.state) {
        viewModel.launchOnIO {
            if (screenOffCountDown != 0) {
                if (uiState.state != OperationState.PROCESSING) {
                    context.saveScreenOffCountDown(0)
                } else {
                    viewModel.launchOnIO {
                        viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                        viewModel.emitEffect(
                            IndexUiEffect.ShowSnackbar(
                                type = SnackbarType.Success,
                                message = context.getString(R.string.args_screen_off_in_seconds, screenOffCountDown),
                                duration = SnackbarDuration.Indefinite
                            )
                        )
                    }
                    var count = screenOffCountDown
                    while (count != 0) {
                        delay(1000)
                        count--
                    }
                    viewModel.emitEffectOnIO(IndexUiEffect.DismissSnackbar)
                    context.saveScreenOffCountDown(count)
                    viewModel.emitIntent(IndexUiIntent.TurnOffScreen)
                }
            }
        }
    }

    val onBack: () -> Unit = remember {
        {
            if (uiState.state == OperationState.PROCESSING) {
                viewModel.launchOnIO {
                    if (dialogState.confirm(title = context.getString(R.string.prompt), text = context.getString(R.string.processing_exit_confirmation))) {
                        BaseUtil.kill(context, "tar", "root")
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
        title = stringResource(id = 
            when (uiState.state) {
                OperationState.PROCESSING -> R.string.processing
                OperationState.DONE -> R.string.backup_completed
                else -> R.string.backup
            }
        ),
        progress = progress,
        actions = {
            Button(enabled = uiState.state == OperationState.IDLE || uiState.state == OperationState.DONE, onClick = {
                if (uiState.state == OperationState.IDLE) viewModel.emitIntentOnIO(IndexUiIntent.Backup)
                else navController.popBackStack()
            }) {
                AnimatedTextContainer(targetState = if (uiState.state == OperationState.DONE) stringResource(id = R.string.finish) else stringResource(id = R.string._continue)) { text ->
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
            val scope = rememberCoroutineScope()
            val pagerState = key(packageItems.size) { rememberPagerState(pageCount = { packageItems.size + 3 }) }

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
                        title = stringResource(id = R.string.preprocessing),
                        defExpanded = true,
                        items = preItems
                    )

                    pagerState.pageCount - 2 -> ProcessingCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .pagerAnimation(pagerState, page),
                        progress = -1f,
                        title = stringResource(id = R.string.post_processing),
                        defExpanded = true,
                        items = postItems
                    )

                    pagerState.pageCount - 1 -> AppsReportCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .pagerAnimation(pagerState, page),
                        scope = scope,
                        pagerState = pagerState,
                        title = stringResource(id = R.string.report),
                        timer = timer,
                        packageSize = packageSize,
                        succeed = packageSucceed,
                        failed = packageFailed,
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
