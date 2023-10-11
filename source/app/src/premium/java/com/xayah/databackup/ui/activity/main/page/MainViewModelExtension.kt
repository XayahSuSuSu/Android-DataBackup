package com.xayah.databackup.ui.activity.main.page

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.navigation.NavHostController
import com.xayah.databackup.ui.component.CloudTopBar
import com.xayah.databackup.ui.component.MainBottomBar

@ExperimentalMaterial3Api
class Cloud(scrollBehavior: TopAppBarScrollBehavior?, cloudNavController: NavHostController) : MainUiState(
    scrollBehavior = scrollBehavior,
    topBar = {
        CloudTopBar(scrollBehavior = scrollBehavior, cloudNavController = cloudNavController)
    },
    bottomBar = {
        MainBottomBar()
    }
)
