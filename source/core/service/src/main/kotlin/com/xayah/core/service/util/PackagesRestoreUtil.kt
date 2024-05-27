package com.xayah.core.service.util

import android.content.Context
import com.xayah.core.common.util.toLineString
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.readCleanRestoring
import com.xayah.core.datastore.readSelectionType
import com.xayah.core.model.DataType
import com.xayah.core.model.OperationState
import com.xayah.core.model.SelectionType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.SymbolUtil
import com.xayah.core.util.command.Appops
import com.xayah.core.util.command.Pm
import com.xayah.core.util.command.SELinux
import com.xayah.core.util.command.Tar
import com.xayah.core.util.model.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PackagesRestoreUtil @Inject constructor(
    @ApplicationContext val context: Context,
    private val rootService: RemoteRootService,
    private val taskDao: TaskDao,
    private val packageRepository: PackageRepository,
    private val cloudRepository: CloudRepository,
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

    private suspend fun PackageEntity.getDataSelected(dataType: DataType) = when (context.readSelectionType().first()) {
        SelectionType.DEFAULT -> {
            when (dataType) {
                DataType.PACKAGE_APK -> apkSelected
                DataType.PACKAGE_USER -> userSelected
                DataType.PACKAGE_USER_DE -> userDeSelected
                DataType.PACKAGE_DATA -> dataSelected
                DataType.PACKAGE_OBB -> obbSelected
                DataType.PACKAGE_MEDIA -> mediaSelected
                else -> false
            }
        }

        SelectionType.APK -> {
            dataType == DataType.PACKAGE_APK
        }

        SelectionType.DATA -> {
            dataType != DataType.PACKAGE_APK
        }

        SelectionType.BOTH -> {
            true
        }
    }

    private suspend fun TaskDetailPackageEntity.updateInfo(
        dataType: DataType,
        state: OperationState? = null,
        bytes: Long? = null,
        log: String? = null,
    ) = run {
        when (dataType) {
            DataType.PACKAGE_APK -> {
                apkInfo.also {
                    if (state != null) it.state = state
                    if (bytes != null) it.bytes = bytes
                    if (log != null) it.log = log
                }
            }

            DataType.PACKAGE_USER -> {
                userInfo.also {
                    if (state != null) it.state = state
                    if (bytes != null) it.bytes = bytes
                    if (log != null) it.log = log
                }
            }

            DataType.PACKAGE_USER_DE -> {
                userDeInfo.also {
                    if (state != null) it.state = state
                    if (bytes != null) it.bytes = bytes
                    if (log != null) it.log = log
                }
            }

            DataType.PACKAGE_DATA -> {
                dataInfo.also {
                    if (state != null) it.state = state
                    if (bytes != null) it.bytes = bytes
                    if (log != null) it.log = log
                }
            }

            DataType.PACKAGE_OBB -> {
                obbInfo.also {
                    if (state != null) it.state = state
                    if (bytes != null) it.bytes = bytes
                    if (log != null) it.log = log
                }
            }

            DataType.PACKAGE_MEDIA -> {
                mediaInfo.also {
                    if (state != null) it.state = state
                    if (bytes != null) it.bytes = bytes
                    if (log != null) it.log = log
                }
            }

            else -> {}
        }
        taskDao.upsert(this)
    }

    private fun TaskDetailPackageEntity.getLog(
        dataType: DataType,
    ) = when (dataType) {
        DataType.PACKAGE_APK -> apkInfo.log
        DataType.PACKAGE_USER -> userInfo.log
        DataType.PACKAGE_USER_DE -> userDeInfo.log
        DataType.PACKAGE_DATA -> dataInfo.log
        DataType.PACKAGE_OBB -> obbInfo.log
        DataType.PACKAGE_MEDIA -> mediaInfo.log
        else -> ""
    }

    suspend fun restoreApk(p: PackageEntity, t: TaskDetailPackageEntity, srcDir: String): ShellResult = run {
        log { "Restoring apk..." }

        val dataType = DataType.PACKAGE_APK
        val packageName = p.packageName
        val userId = p.userId
        val ct = p.indexInfo.compressionType
        val src = packageRepository.getArchiveDst(dstDir = srcDir, dataType = dataType, ct = ct)
        var isSuccess: Boolean
        val out = mutableListOf<String>()

        if (p.getDataSelected(dataType).not()) {
            isSuccess = true
            t.updateInfo(dataType = dataType, state = OperationState.SKIP)
        } else {
            // Return if the archive doesn't exist.
            if (rootService.exists(src)) {
                val sizeBytes = rootService.calculateSize(src)
                t.updateInfo(dataType = dataType, state = OperationState.PROCESSING, bytes = sizeBytes)
                // Decompress apk archive
                val tmpApkPath = pathUtil.getTmpApkPath(packageName = packageName)
                rootService.deleteRecursively(tmpApkPath)
                rootService.mkdirs(tmpApkPath)
                Tar.decompress(src = src, dst = tmpApkPath, extra = ct.decompressPara).also { result ->
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
            t.updateInfo(dataType = dataType, state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = out.toLineString())
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    /**
     * Package data: USER, USER_DE, DATA, OBB, MEDIA
     */
    suspend fun restoreData(p: PackageEntity, t: TaskDetailPackageEntity, dataType: DataType, srcDir: String): ShellResult = run {
        log { "Restoring ${dataType.type}..." }

        val packageName = p.packageName
        val userId = p.userId
        val ct = p.indexInfo.compressionType
        val src = packageRepository.getArchiveDst(dstDir = srcDir, dataType = dataType, ct = ct)
        val dstDir = packageRepository.getDataSrcDir(dataType, userId)
        val dst = packageRepository.getDataSrc(dstDir, packageName)
        val uid = rootService.getPackageUid(packageName = packageName, userId = userId)
        var isSuccess: Boolean
        val out = mutableListOf<String>()

        if (p.getDataSelected(dataType).not()) {
            isSuccess = true
            t.updateInfo(dataType = dataType, state = OperationState.SKIP)
        } else {
            // Return if the archive doesn't exist.
            if (rootService.exists(src)) {
                val sizeBytes = rootService.calculateSize(src)
                t.updateInfo(dataType = dataType, state = OperationState.PROCESSING, bytes = sizeBytes)
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
                    extra = ct.decompressPara
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
                if (dataType == DataType.PACKAGE_USER) {
                    isSuccess = false
                    out.add(log { "Not exist: $src" })
                    t.updateInfo(dataType = dataType, state = OperationState.ERROR, log = out.toLineString())
                    return@run ShellResult(code = -1, input = listOf(), out = out)
                } else {
                    out.add(log { "Not exist and skip: $src" })
                    t.updateInfo(dataType = dataType, state = OperationState.SKIP, log = out.toLineString())
                    return@run ShellResult(code = -2, input = listOf(), out = out)
                }
            }
            t.updateInfo(dataType = dataType, state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = out.toLineString())
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    suspend fun updatePackage(p: PackageEntity) = run {
        log { "Update package..." }
        val packageName = p.packageName

        val userId = p.userId
        val packageInfo = rootService.getPackageInfoAsUser(packageName, 0, userId)
        packageInfo?.apply {
            val uid = applicationInfo.uid
            log { "New uid: $uid" }
            p.extraInfo.uid = uid
        }
    }

    suspend fun restorePermissions(p: PackageEntity) = run {
        log { "Restoring permissions..." }

        val packageName = p.packageName
        val userId = p.userId
        val user = rootService.getUserHandle(userId)
        val permissions = p.extraInfo.permissions

        if (p.permissionSelected) {
            Appops.reset(userId = userId, packageName = packageName)
            log { "Permissions size: ${permissions.size}..." }
            permissions.forEach {
                log { "Permission name: ${it.name}, isGranted: ${it.isGranted}" }
                runCatching {
                    if (it.isGranted)
                        rootService.grantRuntimePermission(packageName, it.name, user!!)
                    else
                        rootService.revokeRuntimePermission(packageName, it.name, user!!)
                }
            }
        } else {
            log { "Skip." }
        }
    }

    suspend fun restoreSsaid(p: PackageEntity) = run {
        log { "Restoring ssaid..." }

        val packageName = p.packageName
        val uid = p.extraInfo.uid
        val userId = p.userId
        val ssaid = p.extraInfo.ssaid

        if (p.ssaidSelected) {
            if (ssaid.isNotEmpty()) {
                log { "Ssaid: $ssaid" }
                rootService.setPackageSsaidAsUser(packageName, uid, userId, ssaid)
            } else {
                log { "Ssaid is empty, skip." }
            }
        } else {
            log { "Skip." }
        }
    }

    suspend fun download(client: CloudClient, p: PackageEntity, t: TaskDetailPackageEntity, dataType: DataType, srcDir: String, dstDir: String, onDownloaded: suspend (path: String) -> Unit) = run {
        val ct = p.indexInfo.compressionType
        val src = packageRepository.getArchiveDst(dstDir = srcDir, dataType = dataType, ct = ct)

        if (p.getDataSelected(dataType).not()) {
            t.updateInfo(dataType = dataType, state = OperationState.SKIP)
        } else {
            t.updateInfo(dataType = dataType, state = OperationState.DOWNLOADING)

            if (client.exists(src)) {
                cloudRepository.download(client = client, src = src, dstDir = dstDir, onDownloaded = onDownloaded).apply {
                    t.updateInfo(dataType = dataType, state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = t.getLog(dataType) + "\n${outString}")
                }
            } else {
                if (dataType == DataType.PACKAGE_USER || dataType == DataType.PACKAGE_APK) {
                    t.updateInfo(dataType = dataType, state = OperationState.ERROR, log = log { "Not exist: $src" })
                } else {
                    t.updateInfo(dataType = dataType, state = OperationState.SKIP, log = log { "Not exist and skip: $src" })
                }
            }
        }
    }
}
