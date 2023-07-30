package com.xayah.databackup.ui.activity.main.router

import android.content.Context
import com.xayah.databackup.R

sealed class ScaffoldRoutes(val route: String) {
    object Guide : ScaffoldRoutes(route = "scaffold_guide")
    object Main : ScaffoldRoutes(route = "scaffold_main")
}

sealed class GuideRoutes(val route: String) {
    object Intro : ScaffoldRoutes(route = "guide_intro")
    object Update : ScaffoldRoutes(route = "guide_update")
    object Env : ScaffoldRoutes(route = "guide_env")
}

sealed class MainRoutes(val route: String) {
    object Backup : MainRoutes(route = "main_backup")
    object Tree : MainRoutes(route = "main_tree")
    object Restore : MainRoutes(route = "main_restore")
    object Cloud : MainRoutes(route = "main_cloud")
    object Settings : MainRoutes(route = "main_settings")

    companion object {
        fun ofTitle(context: Context, route: String?): String {
            return when (route) {
                Tree.route -> context.getString(R.string.directory_structure)
                else -> context.getString(R.string.app_name)
            }
        }
    }
}
