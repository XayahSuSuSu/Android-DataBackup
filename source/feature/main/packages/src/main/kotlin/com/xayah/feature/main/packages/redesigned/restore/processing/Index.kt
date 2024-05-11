package com.xayah.feature.main.packages.redesigned.restore.processing

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.model.OperationState
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.ProcessingCard
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.withMainContext
import com.xayah.feature.main.packages.R
import com.xayah.feature.main.packages.redesigned.ListScaffold
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PagePackagesRestoreProcessing() {
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preItems by viewModel.preItems.collectAsStateWithLifecycle()
    val postItems by viewModel.postItems.collectAsStateWithLifecycle()
    val packageItems by viewModel.packageItems.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val navController = LocalNavController.current!!
    val dialogState = LocalSlotScope.current!!.dialogSlot

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Initialize)
    }

    val onBack: () -> Unit = remember {
        {
            if (uiState.state == OperationState.PROCESSING) {
                viewModel.launchOnIO {
                    if (dialogState.confirm(title = StringResourceToken.fromStringId(R.string.prompt), text = StringResourceToken.fromStringId(R.string.processing_exit_confirmation))) {
                        BaseUtil.kill("tar", "root")
                        viewModel.suspendEmitIntent(IndexUiIntent.DestroyService)
                        withMainContext {
                            navController.popBackStack()
                        }
                    }
                }
            } else {
                viewModel.launchOnIO {
                    viewModel.suspendEmitIntent(IndexUiIntent.DestroyService)
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

    ListScaffold(
        scrollBehavior = scrollBehavior,
        title = StringResourceToken.fromStringId(R.string.processing),
        actions = {},
        progress = null,
        innerBottomSpacer = true,
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(visible = uiState.state == OperationState.IDLE || uiState.state == OperationState.DONE, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(
                    onClick = {
                        if (uiState.state == OperationState.IDLE) viewModel.emitIntent(IndexUiIntent.Backup)
                        else navController.popBackStack()
                    },
                ) {
                    Icon(imageVector = if (uiState.state == OperationState.DONE) Icons.Filled.ChevronLeft else Icons.Filled.PlayArrow, contentDescription = null)
                }
            }
        },
        onBackClick = {
            onBack()
        }
    ) {
        var _expanded by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SizeTokens.Level16),
        ) {
            ProcessingCard(
                modifier = Modifier
                    .animateContentSize()
                    .then(if (_expanded) Modifier.height(SizeTokens.Level0) else Modifier.wrapContentHeight())
                    .paddingBottom(SizeTokens.Level16)
                    .fillMaxWidth(),
                progress = 0f,
                title = StringResourceToken.fromStringId(R.string.preprocessing),
                defExpanded = true,
                items = preItems
            )

            ProcessingCard(
                modifier = Modifier
                    .animateContentSize()
                    .then(if (_expanded) Modifier.fillMaxHeight() else Modifier)
                    .fillMaxWidth(),
                progress = -1f,
                title = StringResourceToken.fromStringId(R.string.backup),
                flagExpanded = _expanded,
                maxDisplayNum = if (_expanded) -1 else 5,
                defExpanded = true,
                items = packageItems,
                actions = {
                    IconButton(icon = ImageVectorToken.fromDrawable(if (_expanded) R.drawable.ic_rounded_collapse_content else R.drawable.ic_rounded_expand_content)) {
                        _expanded = _expanded.not()
                    }
                }
            )

            ProcessingCard(
                modifier = Modifier
                    .animateContentSize()
                    .then(if (_expanded) Modifier.height(SizeTokens.Level0) else Modifier.wrapContentHeight())
                    .paddingTop(SizeTokens.Level16)
                    .fillMaxWidth(),
                progress = 0f,
                title = StringResourceToken.fromStringId(R.string.post_processing),
                defExpanded = true,
                items = postItems
            )
        }
    }
}
