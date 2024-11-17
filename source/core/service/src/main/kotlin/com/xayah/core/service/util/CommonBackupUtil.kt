package com.xayah.core.service.util

import android.content.Context
import android.os.Build
import com.xayah.core.common.util.BuildConfigUtil
import com.xayah.core.datastore.readCompressionTest
import com.xayah.core.model.CompressionType
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.Tar
import com.xayah.core.util.model.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CommonBackupUtil @Inject constructor(
    @ApplicationContext val context: Context,
    private val rootService: RemoteRootService,
) {
    companion object {
        private const val TAG = "CommonBackupUtil"
    }

    private fun log(onMsg: () -> String): String = run {
        val msg = onMsg()
        LogUtil.log { TAG to msg }
        msg
    }

    fun getItselfDst(dstDir: String) = "${dstDir}/DataBackup.apk"

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
                    ?.let {
                        BuildConfigUtil.VERSION_CODE == if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            it.longVersionCode
                        } else {
                            it.versionCode.toLong()
                        }
                    } == true)
            ) {
                isSuccess = true
                out.add(log { "$targetPath exists, skip." })
            } else {
                isSuccess = rootService.copyTo(path = path, targetPath = targetPath, overwrite = true)
                if (isSuccess.not()) {
                    out.add(log { "Failed to copy $path to $targetPath." })
                } else {
                    rootService.setAllPermissions(targetPath)
                    out.add(log { "Copied from $path to $targetPath." })
                }
            }
        } else {
            isSuccess = false
            out.add(log { "Failed to get apk path of $packageName." })
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    suspend fun testArchive(src: String, ct: CompressionType) = run {
        var code: Int
        var input: List<String>
        val out = mutableListOf<String>()

        if (context.readCompressionTest().first()) {
            Tar.test(src = src, extra = ct.decompressPara)
                .also { result ->
                    code = result.code
                    input = result.input
                    if (result.isSuccess.not()) {
                        out.add(log { "$src is broken, trying to delete it." })
                        rootService.deleteRecursively(src)
                    } else {
                        out.add(log { "Everything seems fine." })
                    }
                }
        } else {
            code = 0
            input = listOf()
            out.add(log { "Skip testing." })
        }

        ShellResult(code = code, input = input, out = out)
    }
}