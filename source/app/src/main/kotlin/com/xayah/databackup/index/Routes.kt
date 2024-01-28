package com.xayah.databackup.index

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.databackup.R

internal sealed class MainIndexRoutes(val route: String) {
    data object Home : MainIndexRoutes(route = "main_index_home")
    data object Cloud : MainIndexRoutes(route = "main_index_cloud")

    data object Settings : MainIndexRoutes(route = "main_index_settings")

    companion object {

        fun ofTitle(route: String?): StringResourceToken {
            return when (route) {
                Home.route -> StringResourceToken.fromStringId(R.string.backup)
                Cloud.route -> StringResourceToken.fromStringId(R.string.cloud)
                MainRoutes.CloudAccount.route -> StringResourceToken.fromStringId(R.string.account)
                Settings.route -> StringResourceToken.fromStringId(R.string.settings)
                else -> StringResourceToken.fromStringId(R.string.app_name)
            }
        }

        val BottomBarItemList: List<BottomBarItem>
            get() = listOf(
                BottomBarItem(
                    label = StringResourceToken.fromStringId(R.string.home),
                    iconToken = ImageVectorToken.fromVector(Icons.Rounded.Home),
                    route = Home.route,
                ),
                BottomBarItem(
                    label = StringResourceToken.fromStringId(R.string.cloud),
                    iconToken = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_cloud_upload),
                    route = Cloud.route,
                ),
                BottomBarItem(
                    label = StringResourceToken.fromStringId(R.string.settings),
                    iconToken = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_settings),
                    route = Settings.route,
                ),
            )
    }
}
