package com.xayah.databackup.ui.activity.main.router

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.xayah.databackup.ui.activity.main.page.Cloud
import com.xayah.databackup.ui.activity.main.page.MainViewModel
import com.xayah.databackup.ui.activity.main.page.cloud.PageCloud

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun ComposablePageCloud(viewModel: MainViewModel) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val cloudNavController = rememberNavController()
    LaunchedEffect(null) {
        viewModel.toUiState(Cloud(scrollBehavior, cloudNavController))
    }
    PageCloud(cloudNavController = cloudNavController)
}
