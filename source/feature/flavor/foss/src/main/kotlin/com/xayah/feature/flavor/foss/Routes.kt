package com.xayah.feature.flavor.foss

sealed class GuideRoutes(val route: String) {
    object Intro : GuideRoutes(route = "guide_intro")
    object Env : GuideRoutes(route = "guide_env")
}
