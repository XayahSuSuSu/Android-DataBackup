package com.xayah.databackup.ui.activity.main.router

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
    object Home : MainRoutes(route = "main_home")
    object Cloud : MainRoutes(route = "main_cloud")
    object Backup : MainRoutes(route = "main_backup")
    object Restore : MainRoutes(route = "main_restore")
}
