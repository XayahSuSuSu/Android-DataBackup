package com.xayah.core.service.util

import android.content.Context
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.rootservice.service.RemoteRootService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MessagesBackupUtil @Inject constructor(
    @ApplicationContext val context: Context,
    private val rootService: RemoteRootService,
    private val taskDao: TaskDao,
    private val mediaRepository: MediaRepository,
    private val commonBackupUtil: CommonBackupUtil,
    private val cloudRepository: CloudRepository,
) {
    companion object {
        private val TAG = this::class.java.simpleName
    }
}