package com.xayah.core.ui.route

import com.xayah.core.model.OpType

sealed class MainRoutes(val route: String) {
    companion object {
        const val ArgOpType = "opType"
    }

    object Home : MainRoutes(route = "main_home")

    object Log : MainRoutes(route = "main_log")

    object Tree : MainRoutes(route = "main_tree")

    object Directory : MainRoutes(route = "main_directory/{$ArgOpType}") {
        val routeBackup = "main_directory/${OpType.BACKUP.name}"
        val routeRestore = "main_directory/${OpType.RESTORE.name}"
    }

    object TaskPackages : MainRoutes(route = "main_task_packages/{$ArgOpType}") {
        val routeBackup = "main_task_packages/${OpType.BACKUP.name}"
        val routeRestore = "main_task_packages/${OpType.RESTORE.name}"
    }

    object TaskMedium : MainRoutes(route = "main_task_medium/{$ArgOpType}") {
        val routeBackup = "main_task_medium/${OpType.BACKUP.name}"
        val routeRestore = "main_task_medium/${OpType.RESTORE.name}"
    }
}
