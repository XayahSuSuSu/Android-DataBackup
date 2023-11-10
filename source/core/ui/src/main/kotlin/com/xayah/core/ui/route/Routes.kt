package com.xayah.core.ui.route

import com.xayah.core.model.OpType

sealed class MainRoutes(val route: String) {
    companion object {
        const val ArgOpType = "opType"
    }

    object Home : MainRoutes(route = "main_home")

    object Directory : MainRoutes(route = "main_directory/{$ArgOpType}") {
        val routeBackup = "main_directory/${OpType.BACKUP.name}"
        val routeRestore = "main_directory/${OpType.RESTORE.name}"
    }

    object TaskPackages : MainRoutes(route = "main_task_packages")
}
