package com.xayah.feature.main.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.component.SetOnResume
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.maybePopBackStack

@Composable
fun DetailsRoute(
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current!!
    val uiState: DetailsUiState by viewModel.uiState.collectAsStateWithLifecycle()
    SetOnResume(onResume = viewModel::onResume)
    AppDetailsScreen(uiState, viewModel)
    LaunchedEffect(uiState) {
        if (uiState is DetailsUiState.Error) {
            navController.maybePopBackStack()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppDetailsScreen(uiState: DetailsUiState, viewModel: DetailsViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryLargeTopBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(R.string.details),
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(content = {
                    when (uiState) {
                        is DetailsUiState.Success.App -> {
                            AppDetails(
                                uiState = uiState,
                                onSetDataStates = viewModel::setDataStates,
                                onAddLabel = viewModel::addLabel,
                                onDeleteLabel = viewModel::deleteLabel,
                                onSelectLabel = viewModel::selectAppLabel,
                                onBlock = viewModel::block,
                                onFreeze = viewModel::freezeApp,
                                onLaunch = viewModel::launchApp,
                                onProtect = viewModel::protect,
                                onDelete = viewModel::delete,
                            )
                        }

                        is DetailsUiState.Success.File -> {
                            FileDetails(
                                uiState = uiState,
                                onAddLabel = viewModel::addLabel,
                                onDeleteLabel = viewModel::deleteLabel,
                                onSelectLabel = viewModel::selectFileLabel,
                                onBlock = viewModel::block,
                                onProtect = viewModel::protect,
                                onDelete = viewModel::delete,
                            )
                        }
                        else -> {}
                    }
                }
            )

            InnerBottomSpacer(innerPadding = innerPadding)
        }
    }
}
