package com.xayah.feature.main.task.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.value
import com.xayah.feature.main.task.R
import com.xayah.feature.main.task.TaskCard

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageTaskList() {
    val viewModel = hiltViewModel<IndexViewModel>()
    val navController = LocalNavController.current!!
    val tasksProcessingState by viewModel.tasksProcessingState.collectAsStateWithLifecycle()
    val tasksFinishedState by viewModel.tasksFinishedState.collectAsStateWithLifecycle()
    val snackbarHostState = viewModel.snackbarHostState
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                scrollBehavior = scrollBehavior,
                title = StringResourceToken.fromStringId(R.string.task_list)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .paddingHorizontal(PaddingTokens.Level4),
                    verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
                ) {
                    item {
                        Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                    }

                    if (tasksProcessingState.isNotEmpty()) {
                        item {
                            TitleMediumText(text = StringResourceToken.fromStringId(R.string.processing).value, fontWeight = FontWeight.Bold)
                        }

                        items(items = tasksProcessingState, key = { "${it.id} - ${it.isProcessing}" }) { task ->
                            AnimatedContent(
                                modifier = Modifier.animateItemPlacement(),
                                targetState = task,
                                label = AnimationTokens.AnimatedContentLabel
                            ) {
                                TaskCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                                    t = it
                                ) {
                                    viewModel.emitIntent(IndexUiIntent.ToPageTaskPackageDetail(navController, it))
                                }
                            }
                        }
                    }

                    item {
                        TitleMediumText(text = StringResourceToken.fromStringId(R.string.finished).value, fontWeight = FontWeight.Bold)
                    }

                    items(items = tasksFinishedState, key = { "${it.id} - ${it.isProcessing}" }) { task ->
                        AnimatedContent(
                            modifier = Modifier.animateItemPlacement(),
                            targetState = task,
                            label = AnimationTokens.AnimatedContentLabel
                        ) {
                            TaskCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                t = it
                            ) {
                                viewModel.emitIntent(IndexUiIntent.ToPageTaskPackageDetail(navController, it))
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.paddingBottom(PaddingTokens.Level4))
                    }
                }
            }
        }
    }
}
