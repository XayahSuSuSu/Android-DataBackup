package com.xayah.feature.main.log

sealed class LogRoutes(val route: String) {
    companion object {
        const val ArgFileName = "fileName"
    }

    data object List : LogRoutes(route = "log_list")
    data object Detail : LogRoutes(route = "log_detail/{$ArgFileName}") {
        fun getRoute(name: String) = "log_detail/$name"
    }

    data object Logcat : LogRoutes(route = "log_logcat")

}
