package com.xayah.feature.main.reload

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.core.ui.component.ExtendedFab
import com.xayah.core.ui.component.FilterChip
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LabelLargeText
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.material3.CircularProgressIndicator
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageReload() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val mediumState by viewModel.mediumState.collectAsState()
    val packagesState by viewModel.packagesState.collectAsState()
    val reloadState by viewModel.reloadState.collectAsState()
    val savingState by viewModel.savingState.collectAsState()

    LaunchedEffect(uiState.typeIndex, uiState.versionIndex) {
        viewModel.emitIntent(IndexUiIntent.Update)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                scrollBehavior = scrollBehavior,
                title = StringResourceToken.fromStringId(R.string.reload),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = reloadState && savingState.not(), enter = scaleIn(), exit = scaleOut()) {
                ExtendedFab(
                    modifier = Modifier.padding(PaddingTokens.Level3),
                    onClick = {
                        viewModel.emitIntent(IndexUiIntent.Save)
                    },
                    expanded = true,
                    icon = ImageVectorToken.fromVector(Icons.Rounded.Save),
                    text = StringResourceToken.fromStringId(R.string.save),
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHost = { SnackbarHost(hostState = viewModel.snackbarHostState) },
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)) {
                    item {
                        Spacer(modifier = Modifier.height(PaddingTokens.Level0))
                    }

                    item {
                        Column {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)
                            ) {
                                Spacer(modifier = Modifier.size(PaddingTokens.Level0))

                                FilterChip(
                                    enabled = reloadState && savingState.not(),
                                    leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Key),
                                    selectedIndex = uiState.typeIndex,
                                    list = uiState.typeList,
                                    onSelected = { index, _ ->
                                        viewModel.launchOnIO {
                                            viewModel.emitStateSuspend(uiState.copy(typeIndex = index))
                                        }
                                    },
                                    onClick = {}
                                )

                                FilterChip(
                                    enabled = reloadState && savingState.not(),
                                    leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Apps),
                                    selectedIndex = uiState.versionIndex,
                                    list = uiState.versionList,
                                    onSelected = { index, _ ->
                                        viewModel.launchOnIO {
                                            viewModel.emitStateSuspend(uiState.copy(versionIndex = index))
                                        }
                                    },
                                    onClick = {}
                                )

                                Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.paddingHorizontal(PaddingTokens.Level3),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1),
                        ) {
                            if (packagesState.isFinished.not())
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeCap = StrokeCap.Round)
                            LabelLargeText(
                                text = "${StringResourceToken.fromStringId(R.string.media).value} - ${mediumState.current}/${mediumState.total}",
                                color = ColorSchemeKeyTokens.Primary.toColor(),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    items(items = mediumState.medium, key = { "${it.name}-${it.timestamp}-${it.savePath}" }) { item ->
                        Row(
                            Modifier
                                .animateItemPlacement()
                                .paddingHorizontal(PaddingTokens.Level3)
                        ) {
                            ReloadCard(entity = item)
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.paddingHorizontal(PaddingTokens.Level3),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1),
                        ) {
                            if (packagesState.isFinished.not())
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeCap = StrokeCap.Round)
                            LabelLargeText(
                                text = "${StringResourceToken.fromStringId(R.string.app_and_data).value} - ${packagesState.current}/${packagesState.total}",
                                color = ColorSchemeKeyTokens.Secondary.toColor(),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    items(items = packagesState.packages, key = { "${it.packageName}-${it.timestamp}-${it.savePath}" }) { item ->
                        Row(
                            Modifier
                                .animateItemPlacement()
                                .paddingHorizontal(PaddingTokens.Level3)
                        ) {
                            ReloadCard(entity = item)
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
