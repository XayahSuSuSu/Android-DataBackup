package com.xayah.databackup.ui.activity.main.page.cloud.router

import android.content.Context
import com.xayah.databackup.R

const val AccountDetailArg = "entityName"
const val ReloadArg = "cloudMode"

sealed class CloudRoutes(val route: String) {
    object Main : CloudRoutes(route = "cloud_main")
    object Account : CloudRoutes(route = "cloud_account")
    object AccountDetail : CloudRoutes(route = "cloud_account_detail")
    object Mount : CloudRoutes(route = "cloud_mount")
    object Reload : CloudRoutes(route = "cloud_reload")

    companion object {
        fun ofTitle(context: Context, route: String?): String {
            return when (route?.split("?")?.firstOrNull()) {
                Main.route -> context.getString(R.string.cloud)
                Account.route, AccountDetail.route -> context.getString(R.string.account)
                Mount.route -> context.getString(R.string.mount)
                Reload.route -> context.getString(R.string.reload)
                else -> context.getString(R.string.cloud)
            }
        }
    }
}
