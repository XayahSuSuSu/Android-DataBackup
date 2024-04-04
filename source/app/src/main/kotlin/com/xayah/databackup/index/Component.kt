package com.xayah.databackup.index

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavHostController
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.util.currentRoute
import com.xayah.core.ui.util.navigateAndPopBackStack
import com.xayah.core.ui.util.value

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun MainIndexScaffold(navController: NavHostController, snackbarHostState: SnackbarHostState, content: @Composable BoxScope.() -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val currentRoute = navController.currentRoute()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            PrimaryBottomBar(currentRoute = currentRoute, navController = navController, routeList = MainIndexRoutes.BottomBarItemList)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column {
            Box(modifier = Modifier.weight(1f), content = content)

            InnerBottomSpacer(innerPadding = innerPadding)
        }
    }
}

@Composable
fun PrimaryBottomBar(currentRoute: String?, navController: NavHostController, routeList: List<BottomBarItem>) {
    NavigationBar {
        routeList.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.iconToken.value, contentDescription = null) },
                label = { Text(text = item.label.value) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route)
                        navController.navigateAndPopBackStack(item.route)
                }
            )
        }
    }
}
