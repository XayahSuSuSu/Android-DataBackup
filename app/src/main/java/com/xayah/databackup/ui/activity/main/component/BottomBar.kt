package com.xayah.databackup.ui.activity.main.component

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.router.MainRoutes
import com.xayah.databackup.ui.activity.main.router.navigateAndPopBackStack
import com.xayah.databackup.ui.component.SlotScope

@Composable
fun SlotScope.MainBottomBar() {
    val items = stringArrayResource(id = R.array.bottom_bar_items)
    val icons = listOf(
        ImageVector.vectorResource(R.drawable.ic_rounded_acute),
        ImageVector.vectorResource(R.drawable.ic_rounded_history),
        ImageVector.vectorResource(R.drawable.ic_rounded_cloud_upload),
        ImageVector.vectorResource(R.drawable.ic_rounded_settings),
    )
    val routes = listOf(
        MainRoutes.Backup.route,
        MainRoutes.Restore.route,
        MainRoutes.Cloud.route,
        MainRoutes.Settings.route,
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = currentDestination?.hierarchy?.any { it.route == routes[index] } == true,
                onClick = {
                    navController.navigateAndPopBackStack(routes[index])
                }
            )
        }
    }
}
