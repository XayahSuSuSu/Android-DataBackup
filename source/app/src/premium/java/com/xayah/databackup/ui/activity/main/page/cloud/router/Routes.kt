package com.xayah.databackup.ui.activity.main.page.cloud.router

import android.content.Context
import com.xayah.databackup.R

sealed class CloudRoutes(val route: String) {
    object Main : CloudRoutes(route = "cloud_main")
    object Account : CloudRoutes(route = "cloud_account")
    object CreateAccount : CloudRoutes(route = "cloud_create_account")
    object Mount : CloudRoutes(route = "cloud_mount")

    companion object {
        fun ofTitle(context: Context, route: String?): String {
            return when (route) {
                Main.route -> context.resources.getStringArray(R.array.bottom_bar_items)[2]
                Account.route -> context.getString(R.string.account)
                CreateAccount.route -> context.getString(R.string.create_account)
                Mount.route -> context.getString(R.string.mount)
                else -> context.resources.getStringArray(R.array.bottom_bar_items)[2]
            }
        }
    }
}
