package com.xayah.databackup.ui.activity.main.router

import android.content.Context
import com.xayah.databackup.R

sealed class MainRoutes(val route: String) {
    object Backup : MainRoutes(route = "main_backup")
    object Restore : MainRoutes(route = "main_restore")
    object Cloud : MainRoutes(route = "main_cloud")
    object Settings : MainRoutes(route = "main_settings")
    object Tree : MainRoutes(route = "main_tree")
    object Log : MainRoutes(route = "main_log")

    companion object {
        fun ofTitle(context: Context, route: String?): String {
            return when (route) {
                Restore.route -> context.getString(R.string.restore)
                Tree.route -> context.getString(R.string.directory_structure)
                Log.route -> context.getString(R.string.log)
                else -> context.getString(R.string.app_name)
            }
        }
    }
}
