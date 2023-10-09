package com.xayah.databackup.ui.activity.main.page.cloud

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.xayah.databackup.ui.activity.main.page.cloud.router.CloudNavHost

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalMaterial3Api
@Composable
fun PageCloud(cloudNavController: NavHostController) {
    CloudNavHost(navController = cloudNavController)
}
