package com.xayah.databackup.ui.activity.main.router

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.xayah.databackup.ui.activity.main.page.MainUiState
import com.xayah.databackup.ui.activity.main.page.MainViewModel
import com.xayah.databackup.ui.activity.main.page.cloud.PageCloud

@ExperimentalMaterial3Api
@Composable
fun ComposablePageCloud(viewModel: MainViewModel) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    LaunchedEffect(null) {
        viewModel.toUiState(MainUiState.Main(scrollBehavior))
    }
    PageCloud()
}
