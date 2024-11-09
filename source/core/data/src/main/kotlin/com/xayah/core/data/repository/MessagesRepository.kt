package com.xayah.core.data.repository

import android.content.Context
import com.xayah.core.database.dao.MessageDao
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MessagesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val cloudRepository: CloudRepository,
    private val mediaDao: MessageDao,
    private val pathUtil: PathUtil,
) {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    private fun log(onMsg: () -> String): String = run {
        val msg = onMsg()
        LogUtil.log { TAG to msg }
        msg
    }

    private val localBackupSaveDir get() = context.localBackupSaveDir()
    val backupMessagesDir get() = pathUtil.getLocalMessagesDir()
}