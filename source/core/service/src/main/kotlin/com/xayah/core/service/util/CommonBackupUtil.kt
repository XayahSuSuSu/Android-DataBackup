package com.xayah.core.service.util

import android.content.Context
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.BuildConfigUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.model.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CommonBackupUtil @Inject constructor(
    @ApplicationContext val context: Context,
    private val rootService: RemoteRootService,
) {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    internal fun log(onMsg: () -> String): String = run {
        val msg = onMsg()
        LogUtil.log { TAG to msg }
        msg
    }

    private fun getItselfDst(dstDir: String) = "${dstDir}/DataBackup.apk"
    suspend fun backupItself(dstDir: String): ShellResult = run {
        log { "Backing up itself..." }

        val packageName = context.packageName
        val isSuccess: Boolean
        val out = mutableListOf<String>()
        val sourceDirList = rootService.getPackageSourceDir(packageName, android.os.Process.myUid() / 100000)
        if (sourceDirList.isNotEmpty()) {
            val apkPath = PathUtil.getParentPath(sourceDirList[0])
            val path = "${apkPath}/base.apk"
            val targetPath = getItselfDst(dstDir = dstDir)

            if (rootService.exists(targetPath) && (rootService.getPackageArchiveInfo(targetPath)
                    ?.let { BuildConfigUtil.VERSION_CODE == it.longVersionCode } == true)
            ) {
                isSuccess = true
                out.add(log { "$targetPath exists, skip." })
            } else {
                isSuccess = rootService.copyTo(path = path, targetPath = targetPath, overwrite = true)
                if (isSuccess.not()) {
                    out.add(log { "Failed to copy $path to $targetPath." })
                } else {
                    out.add(log { "Copied from $path to $targetPath." })
                }
            }
        } else {
            isSuccess = false
            out.add(log { "Failed to get apk path of $packageName." })
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }
}