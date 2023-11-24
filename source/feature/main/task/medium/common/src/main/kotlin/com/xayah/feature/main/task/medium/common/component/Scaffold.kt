package com.xayah.feature.main.task.medium.common.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import com.xayah.core.model.ProcessingState
import com.xayah.core.ui.component.DialogState
import com.xayah.core.ui.component.ExtendedFab
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LabelLargeText
import com.xayah.core.ui.component.LinearProgressIndicator
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.emphasize
import com.xayah.core.ui.component.openConfirm
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.rememberDialogState
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.navigateAndPopAllStack
import com.xayah.core.ui.util.value
import com.xayah.core.util.command.BaseUtil
import com.xayah.feature.main.task.medium.common.R
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun <T> ListScaffold(
    snackbarHostState: SnackbarHostState? = null,
    topBarState: TopBarState,
    fabVisible: Boolean,
    fabEmphasizedState: Boolean,
    fabSelectedState: Boolean,
    selectedDataCount: Int,
    shimmering: Boolean,
    shimmerCount: Int,
    selectedItems: List<T>,
    notSelectedItems: List<T>,
    itemKey: ((item: T) -> Any)? = null,
    onFabClick: () -> Unit,
    onSearchTextChange: (String) -> Unit,
    mapChipGroup: @Composable (RowScope.(targetState: Boolean) -> Unit)? = null,
    actionChipGroup: @Composable (RowScope.(targetState: Boolean) -> Unit),
    shimmerItem: @Composable () -> Unit,
    item: @Composable (T) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                SecondaryTopBar(
                    scrollBehavior = scrollBehavior,
                    title = topBarState.title
                )
                if (topBarState.progress != -1f) {
                    var targetProgress by remember { mutableFloatStateOf(0f) }
                    val animatedProgress = animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(),
                        label = AnimationTokens.AnimatedProgressLabel
                    )
                    targetProgress = topBarState.progress
                    if (animatedProgress.value != 1f)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = animatedProgress.value)
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = fabVisible, enter = scaleIn(), exit = scaleOut()) {
                ExtendedFab(
                    modifier = Modifier
                        .padding(PaddingTokens.Level3)
                        .emphasize(state = fabEmphasizedState),
                    onClick = onFabClick,
                    expanded = fabSelectedState,
                    icon = if (fabSelectedState) ImageVectorToken.fromVector(Icons.Rounded.ArrowForward) else ImageVectorToken.fromVector(Icons.Rounded.Close),
                    text = StringResourceToken.fromStringArgs(
                        StringResourceToken.fromString("$selectedDataCount "),
                        StringResourceToken.fromStringId(R.string.data),
                    ),
                )

            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHost = { if (snackbarHostState != null) SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = shimmering,
                    label = AnimationTokens.AnimatedContentLabel
                ) { targetState ->
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)) {
                        item {
                            Spacer(modifier = Modifier.height(PaddingTokens.Level3))
                            SearchBar(
                                modifier = Modifier.paddingHorizontal(PaddingTokens.Level3),
                                enabled = targetState.not(),
                                onTextChange = onSearchTextChange
                            )
                        }

                        item {
                            Column {
                                if (mapChipGroup != null)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)
                                    ) {
                                        Spacer(modifier = Modifier.size(PaddingTokens.Level0))

                                        mapChipGroup(targetState)

                                        Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                                    }
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)
                                ) {
                                    Spacer(modifier = Modifier.size(PaddingTokens.Level0))

                                    actionChipGroup(targetState)

                                    Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                                }
                            }
                        }

                        if (targetState) {
                            items(count = shimmerCount) { _ ->
                                Row(
                                    modifier = Modifier
                                        .animateItemPlacement()
                                        .paddingHorizontal(PaddingTokens.Level3)
                                ) {
                                    shimmerItem()
                                }
                            }
                        } else {
                            item {
                                LabelLargeText(
                                    modifier = Modifier.paddingHorizontal(PaddingTokens.Level3),
                                    text = "${StringResourceToken.fromStringId(R.string.selected).value} - ${selectedItems.size}",
                                    color = com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens.Primary.toColor(),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(items = selectedItems, key = itemKey) { item ->
                                Row(
                                    Modifier
                                        .animateItemPlacement()
                                        .paddingHorizontal(PaddingTokens.Level3)
                                ) {
                                    item(item)
                                }
                            }

                            item {
                                LabelLargeText(
                                    modifier = Modifier.paddingHorizontal(PaddingTokens.Level3),
                                    text = "${StringResourceToken.fromStringId(R.string.not_selected).value} - ${notSelectedItems.size}",
                                    color = com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens.Secondary.toColor(),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(items = notSelectedItems, key = itemKey) { item ->
                                Row(
                                    Modifier
                                        .animateItemPlacement()
                                        .paddingHorizontal(PaddingTokens.Level3)
                                ) {
                                    item(item)
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.paddingBottom(PaddingTokens.Level3))
                        }
                    }
                }
            }
        }
    }
}

private suspend fun onFinishProcessing(
    processingState: ProcessingState,
    navController: NavHostController,
    globalNavController: NavHostController,
    dialogSlot: DialogState,
) {
    when (processingState) {
        ProcessingState.Idle -> {
            navController.popBackStack()
        }

        ProcessingState.Processing -> {
            dialogSlot.openConfirm(StringResourceToken.fromStringId(R.string.processing_exit_confirmation)).also { (confirmed, _) ->
                if (confirmed) {
                    BaseUtil.kill("tar", "root")
                    globalNavController.navigateAndPopAllStack(MainRoutes.Home.route)
                }
            }
        }

        ProcessingState.DONE -> {
            globalNavController.navigateAndPopAllStack(MainRoutes.Home.route)
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun <P, O> ProcessingScaffold(
    topBarTitle: StringResourceToken,
    snackHostState: SnackbarHostState,
    navController: NavHostController,
    targetPath: String,
    availableBytes: Double,
    rawBytes: Double,
    totalBytes: Double,
    remainingCount: Int,
    succeedCount: Int,
    failedCount: Int,
    timer: String,
    processingState: ProcessingState,
    medium: List<P>,
    packageItemKey: ((item: P) -> Any)? = null,
    operationsProcessing: List<O>,
    operationsFailed: List<O>,
    operationsSucceed: List<O>,
    operationItemKey: ((item: O) -> Any)? = null,
    onProcess: () -> Unit,
    packageItem: @Composable (P) -> Unit,
    operationItem: @Composable (O) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dialogState = rememberDialogState()
    val globalNavController = LocalNavController.current!!
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val onFinish = {
        scope.launch {
            onFinishProcessing(
                processingState = processingState,
                navController = navController,
                globalNavController = globalNavController,
                dialogSlot = dialogState
            )
        }
        Unit
    }

    BackHandler {
        onFinish()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                scrollBehavior = scrollBehavior,
                title = topBarTitle,
                onBackClick = {
                    scope.launch {
                        onFinish()
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackHostState) },
        floatingActionButton = {
            AnimatedVisibility(visible = processingState != ProcessingState.Processing, enter = scaleIn(), exit = scaleOut()) {
                ExtendedFab(
                    modifier = Modifier.padding(PaddingTokens.Level3),
                    onClick = when (processingState) {
                        ProcessingState.Idle -> onProcess
                        else -> onFinish
                    },
                    expanded = true,
                    icon = ImageVectorToken.fromVector(
                        when (processingState) {
                            ProcessingState.Idle -> Icons.Rounded.KeyboardArrowRight
                            else -> Icons.Rounded.KeyboardArrowLeft
                        }
                    ),
                    text = StringResourceToken.fromStringId(
                        when (processingState) {
                            ProcessingState.Idle -> R.string.start
                            else -> R.string.word_return
                        }
                    ),
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = processingState == ProcessingState.Idle,
                    label = AnimationTokens.AnimatedContentLabel
                ) { targetProcessingState ->
                    LazyColumn(modifier = Modifier.paddingHorizontal(PaddingTokens.Level3), verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)) {
                        item {
                            Spacer(modifier = Modifier.height(PaddingTokens.Level3))
                            ProcessingInfoCard(
                                modifier = Modifier.fillMaxWidth(),
                                targetPath = targetPath,
                                availableBytes = availableBytes,
                                rawBytes = rawBytes,
                                totalBytes = totalBytes,
                                remainingCount = remainingCount,
                                succeedCount = succeedCount,
                                failedCount = failedCount,
                                timer = timer
                            )
                        }

                        if (targetProcessingState) {
                            item {
                                LabelLargeText(
                                    text = StringResourceToken.fromStringArgs(
                                        StringResourceToken.fromStringId(R.string.data),
                                        StringResourceToken.fromString(" - ${medium.size}"),
                                    ).value,
                                    color = com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens.Secondary.toColor(),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(items = medium, key = packageItemKey) { item ->
                                Row(Modifier.animateItemPlacement()) {
                                    packageItem(item)
                                }
                            }
                        } else {
                            item {
                                LabelLargeText(
                                    text = StringResourceToken.fromStringId(R.string.processing).value,
                                    color = com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens.Primary.toColor(),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(items = operationsProcessing, key = operationItemKey) { item ->
                                Row(Modifier.animateItemPlacement()) {
                                    operationItem(item)
                                }
                            }

                            item {
                                LabelLargeText(
                                    text = StringResourceToken.fromStringId(R.string.failed).value,
                                    color = com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens.Secondary.toColor(),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(items = operationsFailed, key = operationItemKey) { item ->
                                Row(Modifier.animateItemPlacement()) {
                                    operationItem(item)
                                }
                            }

                            item {
                                LabelLargeText(
                                    text = StringResourceToken.fromStringId(R.string.succeed).value,
                                    color = com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens.Secondary.toColor(),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(items = operationsSucceed, key = operationItemKey) { item ->
                                Row(Modifier.animateItemPlacement()) {
                                    operationItem(item)
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.paddingBottom(PaddingTokens.Level3))
                        }
                    }
                }
            }
        }
    }
}
