package com.xayah.databackup.data

import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.util.AppsOptionSelectedBackup
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.FilterBackupUser
import com.xayah.databackup.util.FiltersSystemAppsBackup
import com.xayah.databackup.util.FiltersUserAppsBackup
import com.xayah.databackup.util.filterApp
import com.xayah.databackup.util.readBoolean
import com.xayah.databackup.util.readInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class AppRepository {
    companion object {
        private const val TAG = "AppsRepository"
    }

    val isBackupAppsSelected: Flow<Boolean> = application.readBoolean(AppsOptionSelectedBackup)

    val appsFiltered: Flow<List<App>> = combine(
        DatabaseHelper.appDao.loadFlowApps(),
        application.readInt(FilterBackupUser),
        application.readBoolean(FiltersUserAppsBackup),
        application.readBoolean(FiltersSystemAppsBackup),
    ) { apps, userId, filterUserApps, filterSystemApps ->
        apps.filterApp(userId, filterUserApps, filterSystemApps)
    }

    val appsFilteredAndSelected: Flow<List<App>> = appsFiltered.map { apps -> apps.filter { it.isSelected } }
}
