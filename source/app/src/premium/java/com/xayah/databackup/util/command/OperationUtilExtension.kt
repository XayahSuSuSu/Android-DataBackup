package com.xayah.databackup.util.command

import androidx.compose.runtime.MutableState
import com.xayah.databackup.R
import com.xayah.databackup.data.OperationState
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.readCloudActiveName
import kotlinx.coroutines.flow.first

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
    type.setEntityState(entity, OperationState.Uploading)
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
