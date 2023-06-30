package com.xayah.databackup.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.router.currentRoute
import com.xayah.databackup.ui.activity.main.router.navigateAndPopBackStack
import com.xayah.databackup.util.ConstantUtil

@Composable
fun SlotScope.MainBottomBar() {
    val items = stringArrayResource(id = R.array.bottom_bar_items)
    val icons = listOf(
        ImageVector.vectorResource(R.drawable.ic_rounded_acute),
        ImageVector.vectorResource(R.drawable.ic_rounded_history),
        ImageVector.vectorResource(R.drawable.ic_rounded_cloud_upload),
        ImageVector.vectorResource(R.drawable.ic_rounded_settings),
    )
    val routes = ConstantUtil.MainBottomBarRoutes
    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = navController.currentRoute() == routes[index],
                onClick = {
                    navController.navigateAndPopBackStack(routes[index])
                }
            )
        }
    }
}
