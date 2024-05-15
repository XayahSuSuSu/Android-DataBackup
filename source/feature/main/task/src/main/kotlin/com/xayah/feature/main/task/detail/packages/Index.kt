package com.xayah.feature.main.task.detail.packages

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import com.xayah.core.ui.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.MultiColorProgress
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.feature.main.task.R
import com.xayah.feature.main.task.TaskInfoCard
import com.xayah.feature.main.task.TaskPackageItemCard

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageTaskPackageDetail() {
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                scrollBehavior = scrollBehavior,
                title = StringResourceToken.fromStringArgs(
                    StringResourceToken.fromStringId(R.string.task),
                    StringResourceToken.fromString(uiState.taskId.toString())
                )
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
                        Spacer(modifier = Modifier.height(PaddingTokens.Level4))
                        taskState?.apply {
                            TaskInfoCard(
                                icon = ImageVectorToken.fromVector(Icons.Rounded.PhoneAndroid),
                                title = StringResourceToken.fromStringId(R.string.internal_storage).value,
                                subtitle = "${(totalBytes - availableBytes).formatSize()} (+${rawBytes.formatSize()}) / ${totalBytes.formatSize()}",
                                multiColorProgress = listOf(
                                    MultiColorProgress(
                                        progress = ((totalBytes - availableBytes) / totalBytes).toFloat().takeIf { it.isNaN().not() } ?: 0f,
                                        color = ColorSchemeKeyTokens.Primary.toColor()
                                    ),
                                    MultiColorProgress(
                                        progress = (rawBytes / totalBytes).toFloat().takeIf { it.isNaN().not() } ?: 0f,
                                        color = ColorSchemeKeyTokens.Error.toColor()
                                    ),
                                ),
                                remainingCount = totalCount - successCount - failureCount,
                                successCount = successCount,
                                failureCount = failureCount,
                                timer = if (isProcessing) internalTimerState else taskTimerState
                            )
                        }
                    }

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
                                TaskPackageItemCard(
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
                                TaskPackageItemCard(
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
                                TaskPackageItemCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                                    item = it
                                )
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
