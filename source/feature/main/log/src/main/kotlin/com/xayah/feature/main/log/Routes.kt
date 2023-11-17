package com.xayah.feature.main.log

sealed class LogRoutes(val route: String) {
    companion object {
        const val ArgFileName = "fileName"
    }

    object List : LogRoutes(route = "lop_list")
    object Detail : LogRoutes(route = "lop_detail/{$ArgFileName}") {
        fun getRoute(name: String) = "lop_detail/$name"
    }
}
