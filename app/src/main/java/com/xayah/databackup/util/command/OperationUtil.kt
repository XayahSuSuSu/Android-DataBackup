package com.xayah.databackup.util.command

import android.content.Context
import com.xayah.databackup.R
import com.xayah.databackup.data.OperationState
import com.xayah.databackup.data.PackageBackupOperation
import com.xayah.databackup.data.PackageBackupOperationDao
import com.xayah.databackup.util.CompressionType
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.SymbolUtil.QUOTE
import com.xayah.databackup.util.command.CommonUtil.executeWithLog
import com.xayah.databackup.util.command.CommonUtil.outString
import com.xayah.databackup.util.readBackupSavePath
import com.xayah.databackup.util.readBackupUserId
import com.xayah.databackup.util.readCompatibleMode
import com.xayah.databackup.util.readCompressionType
import com.xayah.librootservice.service.RemoteRootService

fun List<String>.toLineString() = joinToString(separator = "\n")

class OperationUtil(
    private val context: Context,
    private val timestamp: Long,
    private val logUtil: LogUtil,
    private val remoteRootService: RemoteRootService,
    private val packageBackupOperationDao: PackageBackupOperationDao,
) {
    private val userId = context.readBackupUserId()
    private val compressionType = context.readCompressionType()
    private val compatibleMode = context.readCompatibleMode()
    private val packageSavePath = "${context.readBackupSavePath()}/archives/${userId}/packages"

    fun getPackageItemSavePath(packageName: String): String = "${packageSavePath}/${packageName}/$timestamp"

    suspend fun backupApk(entity: PackageBackupOperation, packageName: String) {
        // Set processing state
        entity.apkLog = context.getString(R.string.backing_up)
        entity.apkState = OperationState.Processing
        packageBackupOperationDao.upsert(entity)

        val logTag = "APK"
        val logId = logUtil.log(logTag, "Start backing up...")
        val archivePath = "${getPackageItemSavePath(packageName)}/apk.${compressionType.suffix}"
        val cmd = if (compatibleMode)
            "- ./*.apk ${if (compressionType == CompressionType.TAR) "" else "| ${compressionType.para}"} > $archivePath"
        else
            "$archivePath ./*.apk ${if (compressionType == CompressionType.TAR) "" else "-I $QUOTE${compressionType.para}$QUOTE"}"
        var isSuccess = true
        val outList = mutableListOf<String>()

        // Get the path of apk.
        val sourceDirList = remoteRootService.getPackageSourceDir(packageName, userId)
        if (sourceDirList.isNotEmpty()) {
            val apkPath = PathUtil.getParentPath(sourceDirList[0])
            logUtil.executeWithLog(logId, "cd $apkPath").also { result ->
                if (result.isSuccess.not()) isSuccess = false
                outList.add(result.outString())
            }
            logUtil.executeWithLog(logId, "tar --totals -cpf $cmd").also { result ->
                if (result.isSuccess.not()) isSuccess = false
                outList.add(result.outString())
            }
            logUtil.executeWithLog(logId, "cd /").also { result ->
                if (result.isSuccess.not()) isSuccess = false
                outList.add(result.outString())
            }
        } else {
            // Failed.
            isSuccess = false
            logUtil.log(logTag, "Failed to get apk path of $packageName.")
        }

        entity.apkLog = outList.toLineString().trim()
        entity.apkState = if (isSuccess) OperationState.DONE else OperationState.ERROR
        packageBackupOperationDao.upsert(entity)
    }

    /**
     * Package data: USER, USER_DE, DATA, OBB, MEDIA
     */
    suspend fun backupData(entity: PackageBackupOperation, packageName: String, dataType: DataType) {
        val logTag = dataType.type.uppercase()

        // Set processing state
        if (entity.userState == OperationState.ERROR) {
            val msg = "${context.getString(R.string.failed_and_terminated)}: ${DataType.PACKAGE_USER.type.uppercase()}"
            logUtil.log(logTag, msg)
            dataType.updateEntityLog(entity, msg)
            dataType.updateEntityState(entity, OperationState.ERROR)
            packageBackupOperationDao.upsert(entity)
            return
        } else {
            dataType.updateEntityLog(entity, context.getString(R.string.backing_up))
            dataType.updateEntityState(entity, OperationState.Processing)
            packageBackupOperationDao.upsert(entity)
        }

        val logId = logUtil.log(logTag, "Start backing up...")
        val archivePath = "${getPackageItemSavePath(packageName)}/${dataType.type}.${compressionType.suffix}"
        val originPath = dataType.origin(userId)
        val originPathPara = "$QUOTE$originPath$QUOTE $QUOTE$packageName$QUOTE"
        var exclude = ""
        // Check the existence of origin path.
        "$originPath/$packageName".also { path ->
            val originPathExists = remoteRootService.exists(path)
            if (originPathExists.not()) {
                if (dataType == DataType.PACKAGE_USER) {
                    val msg = "${context.getString(R.string.not_exist)}: $path"
                    dataType.updateEntityLog(entity, msg)
                    dataType.updateEntityState(entity, OperationState.ERROR)
                    logUtil.log(logTag, msg)
                } else {
                    val msg = "${context.getString(R.string.not_exist_and_skip)}: $path"
                    dataType.updateEntityLog(entity, msg)
                    dataType.updateEntityState(entity, OperationState.SKIP)
                    logUtil.log(logTag, msg)
                }
                packageBackupOperationDao.upsert(entity)
                return
            }
        }
        when (dataType) {
            DataType.PACKAGE_USER, DataType.PACKAGE_USER_DE -> {
                // Exclude cache
                val folders = listOf(".ota", "cache", "lib", "code_cache", "no_backup")
                for (item in folders) {
                    exclude += "--exclude=$QUOTE$packageName/$item$QUOTE "
                }
            }

            DataType.PACKAGE_DATA, DataType.PACKAGE_OBB, DataType.PACKAGE_MEDIA -> {
                // Exclude cache
                val folders = listOf("cache")
                for (item in folders) {
                    exclude += "--exclude=$QUOTE$packageName/$item$QUOTE "
                }
                // Exclude Backup_*
                exclude += "--exclude=${QUOTE}Backup_$QUOTE*"
            }

            else -> {
                return
            }
        }
        val cmd = if (compatibleMode)
            "- -C $originPathPara ${if (compressionType == CompressionType.TAR) "" else "| ${compressionType.para}"} > $archivePath"
        else
            "$archivePath -C $originPathPara ${if (compressionType == CompressionType.TAR) "" else "-I $QUOTE${compressionType.para}$QUOTE"}"
        var isSuccess = true
        val outList = mutableListOf<String>()

        // Compress data dir.
        logUtil.executeWithLog(logId, "tar --totals $exclude -cpf $cmd").also { result ->
            if (result.isSuccess.not()) isSuccess = false
            outList.add(result.outString())
        }

        dataType.updateEntityLog(entity, outList.toLineString().trim())
        dataType.updateEntityState(entity, if (isSuccess) OperationState.DONE else OperationState.ERROR)
        packageBackupOperationDao.upsert(entity)
    }
}
