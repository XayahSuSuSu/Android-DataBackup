package com.xayah.databackup.index

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.databackup.R

internal sealed class MainIndexRoutes(val route: String) {
    data object Home : MainIndexRoutes(route = "main_index_home")
    data object Cloud : MainIndexRoutes(route = "main_index_cloud")
    data object CloudAccount : MainIndexRoutes(route = "main_index_cloud_account/{$ArgAccountName}") {
        fun getRoute(name: String) = "main_index_cloud_account/$name"
    }

    data object Settings : MainIndexRoutes(route = "main_index_settings")

    companion object {
        const val ArgAccountName = "accountName"

        fun ofTitle(route: String?): StringResourceToken {
            return when (route) {
                Home.route -> StringResourceToken.fromStringId(R.string.backup)
                Cloud.route -> StringResourceToken.fromStringId(R.string.cloud)
                CloudAccount.route -> StringResourceToken.fromStringId(R.string.account)
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
