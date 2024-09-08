package com.xayah.core.data.repository

import android.content.Context
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WorkRepo @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isFullInitRunning() = WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(FULL_INIT_WORK_NAME).map {
        var allFinished = true
        it.forEach { work ->
            if (work.state.isFinished.not()) allFinished = false
        }
        allFinished.not()
    }

    fun isFullInitAndUpdateAppsRunning() = WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(FULL_INIT_AND_UPDATE_APPS_WORK_NAME).map {
        var allFinished = true
        it.forEach { work ->
            if (work.state.isFinished.not()) allFinished = false
        }
        allFinished.not()
    }

    fun isFastInitAndUpdateAppsRunning() = WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(FAST_INIT_AND_UPDATE_APPS_WORK_NAME).map {
        var allFinished = true
        it.forEach { work ->
            if (work.state.isFinished.not()) allFinished = false
        }
        allFinished.not()
    }

    fun isLoadAppBackupsRunning() = WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(LOAD_APP_BACKUPS_WORK_NAME).map {
        var allFinished = true
        it.forEach { work ->
            if (work.state.isFinished.not()) allFinished = false
        }
        allFinished.not()
    }

    fun isFastInitAndUpdateFilesRunning() = WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(FAST_INIT_AND_UPDATE_FILES_WORK_NAME).map {
        var allFinished = true
        it.forEach { work ->
            if (work.state.isFinished.not()) allFinished = false
        }
        allFinished.not()
    }

    fun isLoadFileBackupsRunning() = WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(LOAD_FILE_BACKUPS_WORK_NAME).map {
        var allFinished = true
        it.forEach { work ->
            if (work.state.isFinished.not()) allFinished = false
        }
        allFinished.not()
    }
}

const val FULL_INIT_WORK_NAME = "DbFullInitWork"
const val FULL_INIT_AND_UPDATE_APPS_WORK_NAME = "DbFullInitAndUpdateAppsWork"
const val FAST_INIT_AND_UPDATE_APPS_WORK_NAME = "DbFastInitAndUpdateAppsWork"
const val FAST_INIT_AND_UPDATE_FILES_WORK_NAME = "DbFastInitAndUpdateFilesWork"
const val LOAD_APP_BACKUPS_WORK_NAME = "DbLoadAppBackupsWork"
const val LOAD_FILE_BACKUPS_WORK_NAME = "DbLoadFileBackupsWork"

const val INPUT_DATA_KEY_REGULAR = "InputDataKeyRegular"
const val INPUT_DATA_KEY_CLOUD_NAME = "InputDataKeyCloudName"
