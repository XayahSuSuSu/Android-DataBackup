package com.xayah.databackup.util

import android.os.Build
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.data.CmdEntity
import com.xayah.databackup.data.LogCmdType
import com.xayah.databackup.data.LogDao
import com.xayah.databackup.data.LogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

fun Array<String>.formatToString() = joinToString(separator = ", ")

@Singleton
class LogUtil @Inject constructor(private val logDao: LogDao) {
    init {
        CoroutineScope(Dispatchers.IO).launch {
            log("Version", BuildConfig.VERSION_NAME)
            log("Model", Build.MODEL)
            log("ABIs", Build.SUPPORTED_ABIS.formatToString())
            log("SDK", Build.VERSION.SDK_INT.toString())
        }
    }

    private val startTimestamp: Long = DateUtil.getTimestamp()

    fun log(tag: String, msg: String): Long {
        val logEntity = LogEntity(startTimestamp = startTimestamp, tag = tag, msg = msg)
        return logDao.insert(logEntity)
    }

    fun logCmd(logId: Long, type: LogCmdType, msg: String): Long {
        val cmdEntity = CmdEntity(logId = logId, type = type, msg = msg)
        return logDao.insert(cmdEntity)
    }
}
