package com.xayah.feature.main.home

import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromStringId
import com.xayah.feature.main.home.common.model.BottomBarItem
import com.xayah.feature.main.home.premium.R
import com.xayah.core.ui.R as UiR

sealed class HomeRoutes(val route: String) {
    object Backup : HomeRoutes(route = "home_backup")
    object Restore : HomeRoutes(route = "home_restore")
    object Cloud : HomeRoutes(route = "home_cloud")
    object CloudAccount : HomeRoutes(route = "home_cloud_account/{$ArgAccountName}") {
        fun getRoute(name: String) = "home_cloud_account/$name"
    }

    object Settings : HomeRoutes(route = "home_settings")

    companion object {
        const val ArgAccountName = "accountName"

        fun ofTitle(route: String?): StringResourceToken {
            return when (route) {
                Backup.route -> StringResourceToken.fromStringId(R.string.backup)
                Restore.route -> StringResourceToken.fromStringId(R.string.restore)
                Cloud.route -> StringResourceToken.fromStringId(R.string.cloud)
                CloudAccount.route -> StringResourceToken.fromStringId(R.string.account)
                Settings.route -> StringResourceToken.fromStringId(R.string.settings)
                else -> StringResourceToken.fromStringId(R.string.app_name)
            }
        }

        val RouteList: List<BottomBarItem>
            get() = listOf(
                BottomBarItem(
                    label = StringResourceToken.fromStringId(R.string.backup),
                    iconToken = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_acute),
                    route = Backup.route,
                ),
                BottomBarItem(
                    label = StringResourceToken.fromStringId(R.string.restore),
                    iconToken = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_history),
                    route = Restore.route,
                ),
                BottomBarItem(
                    label = StringResourceToken.fromStringId(R.string.cloud),
                    iconToken = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_cloud_upload),
                    route = Cloud.route,
                ),
                BottomBarItem(
                    label = StringResourceToken.fromStringId(R.string.settings),
                    iconToken = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_settings),
                    route = Settings.route,
                ),
            )
    }
}
