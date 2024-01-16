package com.xayah.databackup.index

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.xayah.core.ui.util.navigateAndPopBackStack
import com.xayah.core.ui.util.value

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
