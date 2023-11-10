package com.xayah.core.service.util

import android.content.Context
import com.xayah.core.datastore.readBackupUserId
import com.xayah.core.datastore.readCompatibleMode
import com.xayah.core.datastore.readCompressionType
import com.xayah.core.datastore.readFollowSymlinks
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataType
import com.xayah.core.util.ConfigsPackageRestoreName
import com.xayah.core.util.IconRelativeDir
import com.xayah.core.util.PathUtil
import com.xayah.core.util.SymbolUtil
import com.xayah.core.util.command.Tar
import com.xayah.core.util.filesDir
import com.xayah.core.util.model.ShellResult
import com.xayah.librootservice.service.RemoteRootService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import javax.inject.Inject

class PackagesBackupUtil @Inject constructor(
    @ApplicationContext val context: Context,
) {
    private val usePipe = runBlocking { context.readCompatibleMode().first() }
    val compressionType = runBlocking { context.readCompressionType().first() }
    private val userId = runBlocking { context.readBackupUserId().first() }

    @Inject
    lateinit var rootService: RemoteRootService

    private suspend fun testArchive(src: String, compressionType: CompressionType = this.compressionType) = run {
        var code: Int
        var input: List<String>
        val out = mutableListOf<String>()

        Tar.test(src = src, extra = compressionType.decompressPara)
            .also { result ->
                code = result.code
                input = result.input
                if (result.isSuccess.not()) {
                    out.add("$src is broken, trying to delete it.")
                    rootService.deleteRecursively(src)
                } else {
                    out.add("$src is tested well.")
                }
            }

        ShellResult(code = code, input = input, out = out)
    }

    fun getApkDst(dstDir: String) = "${dstDir}/${DataType.PACKAGE_APK.type}.${compressionType.suffix}"
    suspend fun getApkCur(packageName: String) = rootService.getPackageSourceDir(packageName, userId).let { list ->
        if (list.isNotEmpty()) PathUtil.getParentPath(list[0]) else ""
    }

    suspend fun backupApk(packageName: String, dstDir: String): ShellResult = run {
        val dst = getApkDst(dstDir = dstDir)
        var isSuccess: Boolean
        val out = mutableListOf<String>()
        val cur = getApkCur(packageName)
        if (cur.isNotEmpty()) {
            Tar.compressInCur(usePipe = usePipe, cur = cur, src = "./*.apk", dst = dst, extra = compressionType.compressPara)
                .also { result ->
                    isSuccess = result.isSuccess
                    out.addAll(result.out)
                }
            testArchive(src = dst).also { result ->
                isSuccess = isSuccess and result.isSuccess
                out.addAll(result.out)
            }
        } else {
            isSuccess = false
            out.add("Failed to get apk path of $packageName.")
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    fun getDataDst(dstDir: String, dataType: DataType) = "${dstDir}/${dataType.type}.${compressionType.suffix}"
    fun getDataSrcDir(dataType: DataType) = dataType.srcDir(userId)
    fun getDataSrc(srcDir: String, packageName: String) = "$srcDir/$packageName"

    /**
     * Package data: USER, USER_DE, DATA, OBB, MEDIA
     */
    suspend fun backupData(packageName: String, dataType: DataType, dstDir: String): ShellResult = run {
        val dst = getDataDst(dstDir = dstDir, dataType = dataType)
        var isSuccess: Boolean
        val out = mutableListOf<String>()
        val srcDir = getDataSrcDir(dataType)

        // Check the existence of origin path.
        val src = getDataSrc(srcDir, packageName)
        rootService.exists(src).also {
            if (it.not()) {
                if (dataType == DataType.PACKAGE_USER) {
                    isSuccess = false
                    out.add("Not exist: $src")
                    return@run ShellResult(code = -1, input = listOf(), out = out)
                } else {
                    out.add("Not exist and skip: $src")
                    return@run ShellResult(code = -2, input = listOf(), out = out)
                }
            }
        }

        // Generate exclusion items.
        val exclusionList = mutableListOf<String>()
        when (dataType) {
            DataType.PACKAGE_USER, DataType.PACKAGE_USER_DE -> {
                // Exclude cache
                val folders = listOf(".ota", "cache", "lib", "code_cache", "no_backup")
                exclusionList.addAll(folders.map { "${SymbolUtil.QUOTE}$packageName/$it${SymbolUtil.QUOTE}" })
            }

            DataType.PACKAGE_DATA, DataType.PACKAGE_OBB, DataType.PACKAGE_MEDIA -> {
                // Exclude cache
                val folders = listOf("cache")
                exclusionList.addAll(folders.map { "${SymbolUtil.QUOTE}$packageName/$it${SymbolUtil.QUOTE}" })
                // Exclude Backup_*
                exclusionList.add("${SymbolUtil.QUOTE}Backup_${SymbolUtil.QUOTE}*")
            }

            else -> {}
        }

        // Compress and test.
        Tar.compress(
            usePipe = usePipe,
            exclusionList = exclusionList,
            h = if (context.readFollowSymlinks().first()) "-h" else "",
            srcDir = srcDir,
            src = packageName,
            dst = dst,
            extra = compressionType.compressPara
        ).also { result ->
            isSuccess = result.isSuccess
            out.addAll(result.out)
        }
        testArchive(src = dst).also { result ->
            isSuccess = isSuccess and result.isSuccess
            out.addAll(result.out)
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    suspend fun backupItself(dstDir: String): ShellResult = run {
        val packageName = context.packageName
        val isSuccess: Boolean
        val out = mutableListOf<String>()

        val sourceDirList = rootService.getPackageSourceDir(packageName, userId)
        if (sourceDirList.isNotEmpty()) {
            val apkPath = PathUtil.getParentPath(sourceDirList[0])
            val path = "${apkPath}/base.apk"
            val targetPath = "${dstDir}/DataBackup.apk"
            isSuccess = rootService.copyTo(path = path, targetPath = targetPath, overwrite = true)
            if (isSuccess.not()) {
                out.add("Failed to copy $path to $targetPath.")
            } else {
                out.add("Copied from $path to $targetPath.")
            }
        } else {
            isSuccess = false
            out.add("Failed to get apk path of $packageName.")
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    val tarCompressionType = CompressionType.TAR

    fun getIconsDst(dstDir: String) = "${dstDir}/${IconRelativeDir}.${tarCompressionType.suffix}"

    suspend fun backupIcons(dstDir: String): ShellResult = run {
        val dst = getIconsDst(dstDir = dstDir)
        var isSuccess: Boolean
        val out = mutableListOf<String>()

        Tar.compress(
            usePipe = usePipe,
            exclusionList = listOf(),
            h = "",
            srcDir = context.filesDir(),
            src = IconRelativeDir,
            dst = dst,
            extra = tarCompressionType.compressPara
        ).also { result ->
            isSuccess = result.isSuccess
            out.addAll(result.out)
        }
        testArchive(src = dst, compressionType = tarCompressionType).also { result ->
            isSuccess = isSuccess and result.isSuccess
            out.addAll(result.out)
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    fun getConfigsDst(dstDir: String) = "${dstDir}/$ConfigsPackageRestoreName"

    @ExperimentalSerializationApi
    suspend inline fun <reified T> backupConfigs(data: T, dstDir: String): ShellResult = run {
        val dst = getConfigsDst(dstDir = dstDir)
        var isSuccess: Boolean
        val out = mutableListOf<String>()

        val bytes = ProtoBuf.encodeToByteArray(data)
        rootService.writeBytes(bytes = bytes, dst = dst).also {
            isSuccess = it
            if (isSuccess) {
                out.add("Failed to write configs: $dst")
            } else {
                out.add("Succeed to write configs: $dst")
            }
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }
}
