package com.xayah.core.work.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.xayah.core.data.repository.AppsRepo
import com.xayah.core.datastore.di.DbDispatchers.Default
import com.xayah.core.datastore.di.Dispatcher
import com.xayah.core.util.NotificationUtil
import com.xayah.core.work.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@HiltWorker
internal class AppsInitWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    @Dispatcher(Default) private val defaultDispatcher: CoroutineDispatcher,
    private val appsRepo: AppsRepo,
) : CoroutineWorker(appContext, workerParams) {
    private val mNotificationBuilder by lazy { NotificationUtil.getProgressNotificationBuilder(appContext) }
    private var mNotificationInfo: ForegroundInfo? = null

    override suspend fun getForegroundInfo(): ForegroundInfo {
        if (mNotificationInfo == null) {
            mNotificationInfo = NotificationUtil.createForegroundInfo(
                appContext,
                mNotificationBuilder,
                appContext.getString(R.string.initializing_app_list),
                ""
            )
        }

        return mNotificationInfo!!
    }

    override suspend fun doWork(): Result = withContext(defaultDispatcher) {
        appsRepo.fullInitialize { cur, max, content ->
            mNotificationInfo = NotificationUtil.createForegroundInfo(
                appContext,
                mNotificationBuilder,
                appContext.getString(R.string.initializing_app_list),
                content,
                max,
                cur
            )
            setForeground(
                mNotificationInfo!!
            )
        }
        Result.success()
    }

    companion object {
        fun buildRequest() = OneTimeWorkRequestBuilder<AppsInitWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }
}
