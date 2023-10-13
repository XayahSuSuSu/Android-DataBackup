package com.xayah.databackup.ui.activity.main.page.cloud.router

import android.content.Context
import com.xayah.databackup.R

const val AccountDetailArg = "entityName"

sealed class CloudRoutes(val route: String) {
    object Main : CloudRoutes(route = "cloud_main")
    object Account : CloudRoutes(route = "cloud_account")
    object AccountDetail : CloudRoutes(route = "cloud_account_detail")
    object Mount : CloudRoutes(route = "cloud_mount")

    companion object {
        fun ofTitle(context: Context, route: String?): String {
            return when (route?.split("?")?.firstOrNull()) {
                Main.route -> context.resources.getStringArray(R.array.bottom_bar_items)[2]
                Account.route, AccountDetail.route -> context.getString(R.string.account)
                Mount.route -> context.getString(R.string.mount)
                else -> context.resources.getStringArray(R.array.bottom_bar_items)[2]
            }
        }
    }
}
