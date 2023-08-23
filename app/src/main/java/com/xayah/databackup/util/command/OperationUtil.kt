package com.xayah.databackup.util.command

import android.content.Context
import com.xayah.databackup.util.CompressionType
import com.xayah.databackup.util.DateUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.SymbolUtil.QUOTE
import com.xayah.databackup.util.readBackupSavePath
import com.xayah.databackup.util.readBackupUserId
import com.xayah.databackup.util.readCompatibleMode
import com.xayah.databackup.util.readCompressionType
import com.xayah.librootservice.service.RemoteRootService


class OperationUtil(private val context: Context, private val remoteRootService: RemoteRootService) {
    private val timestamp = DateUtil.getTimestamp()
    private val userId = context.readBackupUserId()
    private val compressionType = context.readCompressionType()
    private val compatibleMode = context.readCompatibleMode()
    private val packageSavePath = "${context.readBackupSavePath()}/archives/${userId}/packages"
    private val userPath = PathUtil.getPackageUserPath(userId)

    fun getPackageItemSavePath(packageName: String): String = "${packageSavePath}/${packageName}/$timestamp"

    suspend fun backupApk(packageName: String) {
        val archivePath = "${getPackageItemSavePath(packageName)}/apk.${compressionType.suffix}"
        val cmd = if (compatibleMode)
            "- ./*.apk ${if (compressionType == CompressionType.TAR) "" else "| ${compressionType.para}"} > $archivePath"
        else
            "$archivePath ./*.apk ${if (compressionType == CompressionType.TAR) "" else "-I $QUOTE${compressionType.para}$QUOTE"}"

        // Get the path of apk.
        val sourceDirList = remoteRootService.getPackageSourceDir(packageName, userId)
        if (sourceDirList.isNotEmpty()) {
            val apkPath = PathUtil.getParentPath(sourceDirList[0])
            CommonUtil.execute("cd $apkPath")
            CommonUtil.execute("tar --totals -cpf $cmd")
            CommonUtil.execute("cd /")
        } else {
            // Failed.
        }
    }
}
