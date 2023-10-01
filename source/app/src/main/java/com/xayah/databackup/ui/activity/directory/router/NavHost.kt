package com.xayah.databackup.ui.activity.directory.router

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xayah.databackup.R
import com.xayah.databackup.data.DirectoryType
import com.xayah.databackup.ui.activity.directory.page.PageDirectory
import com.xayah.databackup.ui.component.LocalSlotScope


@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun DirectoryNavHost(startDestination: String) {
    val navController = LocalSlotScope.current!!.navController
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(DirectoryRoutes.DirectoryBackup.route) {
            PageDirectory(title = stringResource(id = R.string.backup_dir), directoryType = DirectoryType.BACKUP)
        }
        composable(DirectoryRoutes.DirectoryRestore.route) {
            PageDirectory(title = stringResource(id = R.string.restore_dir), directoryType = DirectoryType.RESTORE)
        }
    }
}
