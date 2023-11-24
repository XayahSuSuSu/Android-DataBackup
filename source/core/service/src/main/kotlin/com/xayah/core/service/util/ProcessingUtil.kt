package com.xayah.core.service.util

import android.content.Context
import com.xayah.core.datastore.readBackupUserId
import com.xayah.core.datastore.readCleanRestoring
import com.xayah.core.datastore.readCompatibleMode
import com.xayah.core.datastore.readCompressionType
import com.xayah.core.datastore.readFollowSymlinks
import com.xayah.core.datastore.readRestoreUserId
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataType
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.IconRelativeDir
import com.xayah.core.util.PathUtil
import com.xayah.core.util.SymbolUtil
import com.xayah.core.util.command.Pm
import com.xayah.core.util.command.SELinux
import com.xayah.core.util.command.Tar
import com.xayah.core.util.filesDir
import com.xayah.core.util.model.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import com.xayah.core.util.LogUtil.log as KLog

class PackagesBackupUtil @Inject constructor(
    @ApplicationContext val context: Context,
) {
    companion object {
        private const val TAG = "PackagesBackupUtil"
    }

    private val usePipe = runBlocking { context.readCompatibleMode().first() }
    val compressionType = runBlocking { context.readCompressionType().first() }
    private val userId = runBlocking { context.readBackupUserId().first() }
    private fun log(msg: () -> String): String = run {
        KLog { TAG to msg() }
        msg()
    }

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
                    out.add(log { "$src is broken, trying to delete it." })
                    rootService.deleteRecursively(src)
                } else {
                    out.add(log { "$src is tested well." })
                }
            }

        ShellResult(code = code, input = input, out = out)
    }

    private fun getApkDst(dstDir: String) = "${dstDir}/${DataType.PACKAGE_APK.type}.${compressionType.suffix}"
    suspend fun getApkCur(packageName: String) = rootService.getPackageSourceDir(packageName, userId).let { list ->
        if (list.isNotEmpty()) PathUtil.getParentPath(list[0]) else ""
    }

    suspend fun backupApk(packageName: String, dstDir: String): ShellResult = run {
        log { "Backing up apk..." }

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
            out.add(log { "Failed to get apk path of $packageName." })
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    private fun getDataDst(dstDir: String, dataType: DataType) = "${dstDir}/${dataType.type}.${compressionType.suffix}"
    fun getDataSrcDir(dataType: DataType) = dataType.srcDir(userId)
    fun getDataSrc(srcDir: String, packageName: String) = "$srcDir/$packageName"

    /**
     * Package data: USER, USER_DE, DATA, OBB, MEDIA
     */
    suspend fun backupData(packageName: String, dataType: DataType, dstDir: String): ShellResult = run {
        log { "Backing up ${dataType.type}..." }

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
                    out.add(log { "Not exist: $src" })
                    return@run ShellResult(code = -1, input = listOf(), out = out)
                } else {
                    out.add(log { "Not exist and skip: $src" })
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
        log { "ExclusionList: $exclusionList." }

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
        log { "Backing up itself..." }

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
                out.add(log { "Failed to copy $path to $targetPath." })
            } else {
                out.add(log { "Copied from $path to $targetPath." })
            }
        } else {
            isSuccess = false
            out.add(log { "Failed to get apk path of $packageName." })
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    private val tarCompressionType = CompressionType.TAR

    private fun getIconsDst(dstDir: String) = "${dstDir}/${IconRelativeDir}.${tarCompressionType.suffix}"

    suspend fun backupIcons(dstDir: String): ShellResult = run {
        log { "Backing up icons..." }

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
}

class PackagesRestoreUtil @Inject constructor(
    @ApplicationContext val context: Context,
) {
    companion object {
        private const val TAG = "PackagesRestoreUtil"
    }

    private val userId = runBlocking { context.readRestoreUserId().first() }
    private fun log(msg: () -> String): String = run {
        KLog { TAG to msg() }
        msg()
    }

    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var pathUtil: PathUtil

    fun getApkSrc(srcDir: String, compressionType: CompressionType) = "${srcDir}/${DataType.PACKAGE_APK.type}.${compressionType.suffix}"

    suspend fun restoreApk(packageName: String, srcDir: String, compressionType: CompressionType): ShellResult = run {
        log { "Restoring apk..." }

        val src = getApkSrc(srcDir = srcDir, compressionType = compressionType)
        var isSuccess: Boolean
        val out = mutableListOf<String>()

        // Return if the archive doesn't exist.
        if (rootService.exists(src)) {
            // Decompress apk archive
            val tmpApkPath = pathUtil.getTmpApkPath(packageName = packageName)
            rootService.deleteRecursively(tmpApkPath)
            rootService.mkdirs(tmpApkPath)
            Tar.decompress(src = src, dst = tmpApkPath, extra = compressionType.decompressPara)
                .also { result ->
                    isSuccess = result.isSuccess
                    out.addAll(result.out)
                }

            // Install apks
            rootService.listFilePaths(tmpApkPath).also { apksPath ->
                when (apksPath.size) {
                    0 -> {
                        isSuccess = false
                        out.add(log { "$tmpApkPath is empty." })
                    }

                    1 -> {
                        Pm.install(userId = userId, src = apksPath.first()).also { result ->
                            isSuccess = isSuccess and result.isSuccess
                            out.addAll(result.out)
                        }
                    }

                    else -> {
                        var pmSession = ""
                        Pm.Install.create(userId = userId).also { result ->
                            if (result.isSuccess) pmSession = result.outString
                        }
                        if (pmSession.isNotEmpty()) {
                            out.add(log { "Install session: $pmSession." })

                        } else {
                            isSuccess = false
                            out.add(log { "Failed to get install session." })
                        }

                        apksPath.forEach { apkPath ->
                            Pm.Install.write(session = pmSession, srcName = PathUtil.getFileName(apkPath), src = apkPath).also { result ->
                                isSuccess = isSuccess and result.isSuccess
                                out.addAll(result.out)
                            }
                        }

                        Pm.Install.commit(pmSession).also { result ->
                            isSuccess = isSuccess and result.isSuccess
                            out.addAll(result.out)
                        }
                    }
                }
            }
            rootService.deleteRecursively(tmpApkPath)

            // Check the installation again.
            rootService.queryInstalled(packageName = packageName, userId = userId).also {
                if (it.not()) {
                    isSuccess = false
                    log { "Not installed: $packageName." }
                }
            }
        } else {
            isSuccess = false
            out.add(log { "Not exist: $src" })
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    private fun getDataDstDir(dataType: DataType) = dataType.srcDir(userId)
    private fun getDataDst(dataType: DataType, packageName: String) = "${getDataDstDir(dataType)}/$packageName"
    fun getDataSrc(srcDir: String, dataType: DataType, compressionType: CompressionType) = "${srcDir}/${dataType.type}.${compressionType.suffix}"

    /**
     * Package data: USER, USER_DE, DATA, OBB, MEDIA
     */
    suspend fun restoreData(packageName: String, dataType: DataType, srcDir: String, compressionType: CompressionType): ShellResult = run {
        log { "Restoring ${dataType.type}..." }

        val src = getDataSrc(srcDir = srcDir, dataType = dataType, compressionType = compressionType)
        val dst = getDataDst(dataType = dataType, packageName = packageName)
        val dstDir = getDataDstDir(dataType = dataType)
        val uid = rootService.getPackageUid(packageName = packageName, userId = userId)
        var isSuccess: Boolean
        val out = mutableListOf<String>()

        // Return if the archive doesn't exist.
        if (rootService.exists(src)) {
            // Generate exclusion items.
            val exclusionList = mutableListOf<String>()
            when (dataType) {
                DataType.PACKAGE_USER, DataType.PACKAGE_USER_DE, DataType.PACKAGE_DATA, DataType.PACKAGE_OBB, DataType.PACKAGE_MEDIA -> {
                    // Exclude cache
                    val folders = listOf(".ota", "cache", "lib", "code_cache", "no_backup")
                    exclusionList.addAll(folders.map { "${SymbolUtil.QUOTE}$packageName/$it${SymbolUtil.QUOTE}" })
                    if (dataType == DataType.PACKAGE_DATA || dataType == DataType.PACKAGE_OBB || dataType == DataType.PACKAGE_MEDIA) {
                        // Exclude Backup_*
                        exclusionList.add("${SymbolUtil.QUOTE}Backup_${SymbolUtil.QUOTE}*")
                    }

                }

                else -> {}
            }
            log { "ExclusionList: $exclusionList." }

            // Get the SELinux context of the path.
            val pathContext: String
            SELinux.getContext(path = dst).also { result ->
                pathContext = if (result.isSuccess) result.outString else ""
            }

            log { "Original SELinux context: $pathContext." }

            // Decompress the archive.
            Tar.decompress(
                exclusionList = exclusionList,
                clear = if (context.readCleanRestoring().first()) "--recursive-unlink" else "",
                m = true,
                src = src,
                dst = dstDir,
                extra = compressionType.decompressPara
            ).also { result ->
                isSuccess = result.isSuccess
                out.addAll(result.out)
            }

            // Restore SELinux context.
            if (uid != -1) {
                SELinux.chown(uid = uid, path = dst).also { result ->
                    isSuccess = result.isSuccess
                    out.addAll(result.out)
                }
                if (pathContext.isNotEmpty()) {
                    SELinux.chcon(context = pathContext, path = dst).also { result ->
                        isSuccess = result.isSuccess
                        out.addAll(result.out)
                    }
                } else {
                    val parentContext: String
                    SELinux.getContext(dstDir).also { result ->
                        parentContext = if (result.isSuccess) result.outString.replace("system_data_file", "app_data_file") else ""
                    }
                    if (parentContext.isNotEmpty()) {
                        SELinux.chcon(context = parentContext, path = dst).also { result ->
                            isSuccess = result.isSuccess
                            out.addAll(result.out)
                        }
                    } else {
                        isSuccess = false
                        out.add(log { "Failed to restore context: $dst" })
                    }
                }
            } else {
                isSuccess = false
                out.add(log { "Failed to get uid of $packageName." })
            }
        } else {
            out.add(log { "Not exist and skip: $src" })
            return@run ShellResult(code = -2, input = listOf(), out = out)
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

}

class MediumBackupUtil @Inject constructor(
    @ApplicationContext val context: Context,
) {
    companion object {
        private const val TAG = "MediumBackupUtil"
    }

    private val usePipe = runBlocking { context.readCompatibleMode().first() }
    private val compressionType = CompressionType.TAR
    private val userId = runBlocking { context.readBackupUserId().first() }
    private fun log(msg: () -> String): String = run {
        KLog { TAG to msg() }
        msg()
    }

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
                    out.add(log { "$src is broken, trying to delete it." })
                    rootService.deleteRecursively(src)
                } else {
                    out.add(log { "$src is tested well." })
                }
            }

        ShellResult(code = code, input = input, out = out)
    }

    private fun getDataDst(dstDir: String) = "${dstDir}/${DataType.MEDIA_MEDIA.type}.${compressionType.suffix}"

    /**
     * Media data: MEDIA
     */
    suspend fun backupData(src: String, dstDir: String): ShellResult = run {
        log { "Backing up media..." }

        val dst = getDataDst(dstDir = dstDir)
        var isSuccess: Boolean
        val out = mutableListOf<String>()
        val srcDir = PathUtil.getParentPath(src)
        val srcName = PathUtil.getFileName(src)

        // Check the existence of origin path.
        rootService.exists(src).also {
            if (it.not()) {
                isSuccess = false
                out.add(log { "Not exist: $src" })
                return@run ShellResult(code = -1, input = listOf(), out = out)
            }
        }

        // Compress and test.
        Tar.compress(
            usePipe = usePipe,
            exclusionList = listOf(),
            h = if (context.readFollowSymlinks().first()) "-h" else "",
            srcDir = srcDir,
            src = srcName,
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
        log { "Backing up itself..." }

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
                out.add(log { "Failed to copy $path to $targetPath." })
            } else {
                out.add(log { "Copied from $path to $targetPath." })
            }
        } else {
            isSuccess = false
            out.add(log { "Failed to get apk path of $packageName." })
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }
}

class MediumRestoreUtil @Inject constructor(
    @ApplicationContext val context: Context,
) {
    companion object {
        private const val TAG = "MediumRestoreUtil"
    }

    private val compressionType = CompressionType.TAR
    private fun log(msg: () -> String): String = run {
        KLog { TAG to msg() }
        msg()
    }

    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var pathUtil: PathUtil

    fun getDataSrc(srcDir: String) = "${srcDir}/${DataType.MEDIA_MEDIA.type}.${compressionType.suffix}"

    /**
     * Media data
     */
    suspend fun restoreData(path: String, srcDir: String): ShellResult = run {
        log { "Restoring media..." }

        val src = getDataSrc(srcDir = srcDir)
        val dstDir = PathUtil.getParentPath(path)
        var isSuccess: Boolean
        val out = mutableListOf<String>()

        // Return if the archive doesn't exist.
        if (rootService.exists(src)) {
            // Decompress the archive.
            Tar.decompress(
                exclusionList = listOf(),
                clear = if (context.readCleanRestoring().first()) "--recursive-unlink" else "",
                m = false,
                src = src,
                dst = dstDir,
                extra = compressionType.decompressPara
            ).also { result ->
                isSuccess = result.isSuccess
                out.addAll(result.out)
            }
        } else {
            out.add(log { "Not exist: $src" })
            return@run ShellResult(code = -1, input = listOf(), out = out)
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }
}
