package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.crash.CrashViewModel
import com.xayah.databackup.ui.activity.guide.page.GuideViewModel
import com.xayah.databackup.ui.activity.main.page.MainViewModel
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.CommonTokens

@ExperimentalMaterial3Api
@Composable
fun GuideScaffold(
    navController: NavHostController,
    viewModel: GuideViewModel,
    content: @Composable () -> Unit
) {
    val uiState = viewModel.uiState.value
    Scaffold(
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(onClick = { uiState.onFabClick(navController) }) {
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
        bottomBar = uiState.bottomBar ?: {},
        floatingActionButton = uiState.floatingActionButton,
        floatingActionButtonPosition = uiState.floatingActionButtonPosition
    ) { innerPadding ->
        Column {
            TopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                content()
            }

            // If there's no bottom bar, the navigation bar needs to be transparent.
            if (uiState.bottomBar != null) BottomSpacer(innerPadding = innerPadding)
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun CrashScaffold(viewModel: CrashViewModel) {
    val uiState by viewModel.uiState
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .paddingHorizontal(CommonTokens.PaddingMedium)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
        ) {
            TopSpacer(innerPadding = innerPadding)

            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = ColorScheme.onSurfaceVariant(),
                modifier = Modifier
                    .size(CommonTokens.IconMediumSize)
                    .paddingBottom(CommonTokens.PaddingSmall)
            )
            TopBarTitle(text = stringResource(id = R.string.app_crashed))
            LabelSmallText(text = uiState.text)

            BottomSpacer(innerPadding = innerPadding)
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun DirectoryScaffold(title: String, onFabClick: () -> Unit, content: @Composable () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DirectoryTopBar(scrollBehavior = scrollBehavior, title = title)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onFabClick,
                expanded = true,
                icon = { Icon(imageVector = Icons.Rounded.Add, contentDescription = null) },
                text = { Text(text = stringResource(id = R.string.add)) },
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column {
            TopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}
