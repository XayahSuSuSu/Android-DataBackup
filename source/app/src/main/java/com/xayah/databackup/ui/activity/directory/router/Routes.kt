package com.xayah.databackup.ui.activity.directory.router

sealed class DirectoryRoutes(val route: String) {
    object DirectoryBackup : DirectoryRoutes(route = "directory_backup")
    object DirectoryRestore : DirectoryRoutes(route = "directory_restore")
}
