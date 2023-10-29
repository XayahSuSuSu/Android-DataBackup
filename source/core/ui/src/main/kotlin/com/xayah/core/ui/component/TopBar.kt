package com.xayah.core.ui.component

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.value

@ExperimentalMaterial3Api
@Composable
fun SecondaryTopBar(scrollBehavior: TopAppBarScrollBehavior?, title: StringResourceToken) {
    val navController = LocalNavController.current!!
    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = title.value) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            ArrowBackButton {
                navController.popBackStack()
            }
        },
    )
}