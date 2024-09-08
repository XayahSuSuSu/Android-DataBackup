package com.xayah.feature.main.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.SetOnResume
import com.xayah.core.ui.util.LocalNavController

@Composable
fun ListRoute(
    viewModel: ListViewModel = hiltViewModel(), // Initialize/Reset list data
) {
    val navController = LocalNavController.current!!
    val uiState: ListUiState by viewModel.uiState.collectAsStateWithLifecycle()
    SetOnResume(onResume = viewModel::onResume)
    ListScreen(uiState) {
        viewModel.toNextPage(navController)
    }
}

@Composable
internal fun ListScreen(uiState: ListUiState, onFabClick: () -> Unit) {
    val scrollState = rememberLazyListState()

    ListBottomSheet()

    Scaffold(
        topBar = { ListTopBar() },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(visible = uiState is ListUiState.Success && uiState.selected != 0L, enter = scaleIn(), exit = scaleOut()) {
                ExtendedFloatingActionButton(
                    onClick = onFabClick,
                    icon = { Icon(Icons.Rounded.ChevronRight, null) },
                    text = { Text(text = stringResource(id = R.string._continue)) },
                )
            }
        }
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f), content = {
                ListItems(scrollState = scrollState)
            })
        }
    }
}
