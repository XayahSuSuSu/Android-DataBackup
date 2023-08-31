package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavHostController
import com.xayah.databackup.ui.activity.main.page.guide.GuideViewModel
import com.xayah.databackup.ui.activity.main.page.main.MainViewModel
import com.xayah.databackup.ui.token.CommonTokens

@ExperimentalMaterial3Api
@Composable
fun GuideScaffold(
    scaffoldNavController: NavHostController,
    navController: NavHostController,
    viewModel: GuideViewModel,
    content: @Composable () -> Unit
) {
    val uiState = viewModel.uiState.value
    Scaffold(
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(onClick = { uiState.onFabClick(scaffoldNavController, navController) }) {
                Icon(uiState.fabIcon, null)
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium)) {
            TopSpacer(innerPadding = innerPadding)
            TopSpacer(innerPadding = innerPadding)

            GuideTopBar(title = uiState.title, icon = uiState.icon)
            content()
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun MainScaffold(viewModel: MainViewModel, content: @Composable () -> Unit) {
    val uiState = viewModel.uiState.value
    Scaffold(
        modifier = if (uiState.scrollBehavior != null) Modifier.nestedScroll(uiState.scrollBehavior.nestedScrollConnection) else Modifier,
        topBar = uiState.topBar,
        bottomBar = uiState.bottomBar,
        floatingActionButton = uiState.floatingActionButton,
        floatingActionButtonPosition = uiState.floatingActionButtonPosition
    ) { innerPadding ->
        Column {
            TopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                content()
            }

            BottomSpacer(innerPadding = innerPadding)
        }
    }
}
