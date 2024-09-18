package com.xayah.core.work.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.xayah.core.data.repository.AppsRepo
import com.xayah.core.data.repository.INPUT_DATA_KEY_REGULAR
import com.xayah.core.data.repository.SettingsDataRepo
import com.xayah.core.datastore.di.DbDispatchers.Default
import com.xayah.core.datastore.di.Dispatcher
import com.xayah.core.util.DateUtil
import com.xayah.core.util.NotificationUtil
import com.xayah.core.work.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
internal class AppsUpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    @Dispatcher(Default) private val defaultDispatcher: CoroutineDispatcher,
    private val appsRepo: AppsRepo,
    private val settingsDataRepo: SettingsDataRepo,
) : CoroutineWorker(appContext, workerParams) {
    private val mNotificationBuilder by lazy { NotificationUtil.getProgressNotificationBuilder(appContext) }
    private var mNotificationInfo: ForegroundInfo? = null

    override suspend fun getForegroundInfo(): ForegroundInfo {
        if (mNotificationInfo == null) {
            mNotificationInfo = NotificationUtil.createForegroundInfo(
                appContext,
                mNotificationBuilder,
                appContext.getString(R.string.updating_app_list),
                ""
            )
        }

        return mNotificationInfo!!
    }

    override suspend fun doWork(): Result = withContext(defaultDispatcher) {
        val regular = inputData.getBoolean(INPUT_DATA_KEY_REGULAR, true)
        val appsUpdateTime = settingsDataRepo.settingsData.first().appsUpdateTime
        val curTime = DateUtil.getTimestamp()
        val hasPassedOneDay = DateUtil.getNumberOfDaysPassed(appsUpdateTime, curTime) >= 1
        if (regular.not() || hasPassedOneDay) {
            settingsDataRepo.setAppsUpdateTime(curTime)
            appsRepo.fullUpdate { cur, max, content ->
                mNotificationInfo = NotificationUtil.createForegroundInfo(
                    appContext,
                    mNotificationBuilder,
                    appContext.getString(R.string.updating_app_list),
                    content,
                    max,
                    cur
                )
                setForeground(
                    mNotificationInfo!!
                )
            }
        }
        Result.success()
    }

    companion object {
        fun buildRequest(regular: Boolean) = OneTimeWorkRequestBuilder<AppsUpdateWorker>()
            .setInputData(
                workDataOf(
                    INPUT_DATA_KEY_REGULAR to regular
                )
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }
}
