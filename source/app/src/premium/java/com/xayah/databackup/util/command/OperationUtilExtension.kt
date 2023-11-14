package com.xayah.databackup.util.command

import android.content.Context
import androidx.compose.runtime.MutableState
import com.xayah.core.database.dao.CloudDao
import com.xayah.core.model.DataType
import com.xayah.core.model.OperationState
import com.xayah.databackup.R
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.readCloudActiveName
import com.xayah.databackup.util.setEntityLog
import com.xayah.databackup.util.setEntityState
import com.xayah.core.rootservice.parcelables.PathParcelable
import com.xayah.core.rootservice.service.RemoteRootService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

suspend fun AdditionUtil.backupItselfExtension(targetPath: String) {
    val name = context.readCloudActiveName().first().toString()
    val base = cloudDao.queryBaseByName(name)
    if (base != null) {
        CloudUtil.copy(logUtil, targetPath, base.backupSavePath)
        rootService.deleteRecursively(targetPath)
    }
}

private suspend fun PackagesBackupUtil.setUploadingState(type: DataType) {
    type.setEntityLog(entity, getString(R.string.uploading))
    type.setEntityState(entity, OperationState.UPLOADING)
    opDao.upsert(entity)
}

suspend fun PackagesBackupUtil.mkdirsExtension() {
    val name = context.readCloudActiveName().first().toString()
    val base = cloudDao.queryBaseByName(name)
    if (base != null) {
        dirs.forEach {
            val dst = PathUtil.getRelativeBackupSavePath(it, true)
            CloudUtil.mkdir(logUtil, "${base.backupSavePath}/$dst")
        }

    }
}

suspend fun PackagesBackupUtil.backupArchiveExtension(targetPath: String, type: DataType, isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {
    if (isSuccess.value) {
        val name = context.readCloudActiveName().first().toString()
        val base = cloudDao.queryBaseByName(name)
        if (base != null) {
            setUploadingState(type)
            val dst = PathUtil.getRelativeBackupSavePath(timestampPath, true)
            CloudUtil.copy(logUtil, targetPath, "${base.backupSavePath}/$dst")
            rootService.deleteRecursively(targetPath)
        }
    }
}

suspend fun PackagesBackupUtil.getSavePath(): String {
    var path = ""
    val name = context.readCloudActiveName().first().toString()
    val base = cloudDao.queryBaseByName(name)
    if (base != null) {
        path = base.backupSavePath
    }
    return path
}

suspend fun PackagesBackupUtil.backupArchiveExtension(targetPath: String) {
    val name = context.readCloudActiveName().first().toString()
    val base = cloudDao.queryBaseByName(name)
    if (base != null) {
        val dst = PathUtil.getRelativeBackupSavePath(timestampPath, true)
        CloudUtil.copy(logUtil, targetPath, "${base.backupSavePath}/$dst")
        rootService.deleteRecursively(targetPath)
    }
}

suspend fun PackagesBackupAfterwardsUtil.backupArchiveExtension(targetPath: String) {
    val name = context.readCloudActiveName().first().toString()
    val base = cloudDao.queryBaseByName(name)
    if (base != null) {
        val dst = PathUtil.getRelativeBackupSavePath(configsPath, true)
        CloudUtil.copy(logUtil, targetPath, "${base.backupSavePath}/$dst")
        rootService.deleteRecursively(targetPath)
    }
}

suspend fun PackagesBackupAfterwardsUtil.clearUpExtension() {
    rootService.deleteRecursively(PathUtil.getBackupSavePath(cloudMode))
}

class ExtensionUtil @Inject constructor(
    @ApplicationContext val context: Context,
) {
    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var cloudDao: CloudDao

    @Inject
    lateinit var logUtil: LogUtil

    private val logTag = "ExtensionUtil"
    private val tmpPath = PathUtil.getTmpFetchPath()

    suspend fun deleteTmp() {
        rootService.deleteRecursively(tmpPath)
    }

    suspend fun fetchTmp(src: String): String {
        val logId = logUtil.log(logTag, "Fetch tmp: $src")
        val name = context.readCloudActiveName().first().toString()
        val base = cloudDao.queryBaseByName(name)
        rootService.deleteRecursively(tmpPath)

        var tmpFilePath = ""

        if (base != null) {
            Rclone.copy("${base.backupSavePath}/$src", "${tmpPath}/${PathUtil.getParentPath(src)}").also { result ->
                result.logCmd(logUtil, logId)
                tmpFilePath = "${tmpPath}/$src"
            }
        }

        return tmpFilePath
    }

    suspend fun walkFileTree(src: String): List<PathParcelable> {
        val logId = logUtil.log(logTag, "walkFileTree: $src")
        val name = context.readCloudActiveName().first().toString()
        val base = cloudDao.queryBaseByName(name)
        val pathParcelableList = mutableListOf<PathParcelable>()

        if (base != null) {
            Rclone.lsf("${base.backupSavePath}/$src").also { result ->
                result.logCmd(logUtil, logId)
                result.out.forEach {
                    pathParcelableList.add(PathParcelable(it))
                }
            }
        }

        return pathParcelableList
    }

    suspend fun getSavePath(): String {
        var path = ""
        val name = context.readCloudActiveName().first().toString()
        val base = cloudDao.queryBaseByName(name)
        if (base != null) {
            path = base.backupSavePath
        }
        return path
    }
}
