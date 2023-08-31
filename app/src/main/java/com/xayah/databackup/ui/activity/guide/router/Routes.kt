package com.xayah.databackup.ui.activity.guide.router

sealed class GuideRoutes(val route: String) {
    object Intro : GuideRoutes(route = "guide_intro")
    object Update : GuideRoutes(route = "guide_update")
    object Env : GuideRoutes(route = "guide_env")
}
