package com.xayah.core.service.util

import android.content.Context
import android.content.pm.PackageManager
import com.xayah.core.common.util.toLineString
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.readCompressionLevel
import com.xayah.core.datastore.readFollowSymlinks
import com.xayah.core.datastore.readSelectionType
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataType
import com.xayah.core.model.OperationState
import com.xayah.core.model.SelectionType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.util.getCompressPara
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.IconRelativeDir
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.SymbolUtil
import com.xayah.core.util.command.Tar
import com.xayah.core.util.filesDir
import com.xayah.core.util.model.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class PackagesBackupUtil @Inject constructor(
    @ApplicationContext val context: Context,
    private val rootService: RemoteRootService,
    private val taskDao: TaskDao,
    private val packageRepository: PackageRepository,
    private val commonBackupUtil: CommonBackupUtil,
    private val cloudRepository: CloudRepository,
) {
    companion object {
        private const val TAG = "PackagesBackupUtil"
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

    private fun PackageEntity.getDataBytes(dataType: DataType) = when (dataType) {
        DataType.PACKAGE_APK -> dataStats.apkBytes
        DataType.PACKAGE_USER -> dataStats.userBytes
        DataType.PACKAGE_USER_DE -> dataStats.userDeBytes
        DataType.PACKAGE_DATA -> dataStats.dataBytes
        DataType.PACKAGE_OBB -> dataStats.obbBytes
        DataType.PACKAGE_MEDIA -> dataStats.mediaBytes
        else -> 0
    }

    private fun PackageEntity.setDataBytes(dataType: DataType, sizeBytes: Long) = when (dataType) {
        DataType.PACKAGE_APK -> dataStats.apkBytes = sizeBytes
        DataType.PACKAGE_USER -> dataStats.userBytes = sizeBytes
        DataType.PACKAGE_USER_DE -> dataStats.userDeBytes = sizeBytes
        DataType.PACKAGE_DATA -> dataStats.dataBytes = sizeBytes
        DataType.PACKAGE_OBB -> dataStats.obbBytes = sizeBytes
        DataType.PACKAGE_MEDIA -> dataStats.mediaBytes = sizeBytes
        else -> Unit
    }

    private fun PackageEntity.setDisplayBytes(dataType: DataType, sizeBytes: Long) = when (dataType) {
        DataType.PACKAGE_APK -> displayStats.apkBytes = sizeBytes
        DataType.PACKAGE_USER -> displayStats.userBytes = sizeBytes
        DataType.PACKAGE_USER_DE -> displayStats.userDeBytes = sizeBytes
        DataType.PACKAGE_DATA -> displayStats.dataBytes = sizeBytes
        DataType.PACKAGE_OBB -> displayStats.obbBytes = sizeBytes
        DataType.PACKAGE_MEDIA -> displayStats.mediaBytes = sizeBytes
        else -> Unit
    }

    private suspend fun TaskDetailPackageEntity.updateInfo(
        dataType: DataType,
        state: OperationState? = null,
        bytes: Long? = null,
        log: String? = null,
        content: String? = null,
    ) = run {
        when (dataType) {
            DataType.PACKAGE_APK -> {
                apkInfo.also {
                    if (state != null) it.state = state
                    if (bytes != null) it.bytes = bytes
                    if (log != null) it.log = log
                    if (content != null) it.content = content
                }
            }

            DataType.PACKAGE_USER -> {
                userInfo.also {
                    if (state != null) it.state = state
                    if (bytes != null) it.bytes = bytes
                    if (log != null) it.log = log
                    if (content != null) it.content = content
                }
            }

            DataType.PACKAGE_USER_DE -> {
                userDeInfo.also {
                    if (state != null) it.state = state
                    if (bytes != null) it.bytes = bytes
                    if (log != null) it.log = log
                    if (content != null) it.content = content
                }
            }

            DataType.PACKAGE_DATA -> {
                dataInfo.also {
                    if (state != null) it.state = state
                    if (bytes != null) it.bytes = bytes
                    if (log != null) it.log = log
                    if (content != null) it.content = content
                }
            }

            DataType.PACKAGE_OBB -> {
                obbInfo.also {
                    if (state != null) it.state = state
                    if (bytes != null) it.bytes = bytes
                    if (log != null) it.log = log
                    if (content != null) it.content = content
                }
            }

            DataType.PACKAGE_MEDIA -> {
                mediaInfo.also {
                    if (state != null) it.state = state
                    if (bytes != null) it.bytes = bytes
                    if (log != null) it.log = log
                    if (content != null) it.content = content
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

    private val tarCt = CompressionType.TAR
    fun getIconsDst(dstDir: String) = "${dstDir}/$IconRelativeDir.${tarCt.suffix}"
    suspend fun backupIcons(dstDir: String): ShellResult = run {
        log { "Backing up icons..." }

        val dst = getIconsDst(dstDir = dstDir)
        var isSuccess: Boolean
        val out = mutableListOf<String>()

        Tar.compress(
            exclusionList = listOf(),
            h = "",
            srcDir = context.filesDir(),
            src = IconRelativeDir,
            dst = dst,
            extra = tarCt.getCompressPara(context.readCompressionLevel().first())
        ).also { result ->
            isSuccess = result.isSuccess
            out.addAll(result.out)
        }
        commonBackupUtil.testArchive(src = dst, ct = tarCt).also { result ->
            isSuccess = isSuccess and result.isSuccess
            out.addAll(result.out)
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    private suspend fun getPackageSourceDir(packageName: String, userId: Int) = rootService.getPackageSourceDir(packageName, userId).let { list ->
        if (list.isNotEmpty()) PathUtil.getParentPath(list[0]) else ""
    }

    suspend fun backupApk(p: PackageEntity, r: PackageEntity?, t: TaskDetailPackageEntity, dstDir: String): ShellResult = run {
        log { "Backing up apk..." }

        val dataType = DataType.PACKAGE_APK
        val packageName = p.packageName
        val userId = p.userId
        val ct = p.indexInfo.compressionType
        val dst = packageRepository.getArchiveDst(dstDir = dstDir, dataType = dataType, ct = ct)
        var isSuccess: Boolean
        val out = mutableListOf<String>()
        val srcDir = getPackageSourceDir(packageName = packageName, userId = userId)

        if (p.getDataSelected(dataType).not()) {
            isSuccess = true
            t.updateInfo(dataType = dataType, state = OperationState.SKIP)
        } else {
            if (srcDir.isNotEmpty()) {
                val sizeBytes = rootService.calculateSize(srcDir)
                t.updateInfo(dataType = dataType, state = OperationState.PROCESSING, bytes = sizeBytes)
                if (rootService.exists(dst) && sizeBytes == r?.getDataBytes(dataType)) {
                    isSuccess = true
                    t.updateInfo(dataType = dataType, state = OperationState.SKIP)
                    out.add(log { "Data has not changed." })
                } else {
                    Tar.compressInCur(cur = srcDir, src = "./*.apk", dst = dst, extra = ct.getCompressPara(context.readCompressionLevel().first()))
                        .also { result ->
                            isSuccess = result.isSuccess
                            out.addAll(result.out)
                        }
                    commonBackupUtil.testArchive(src = dst, ct = ct).also { result ->
                        isSuccess = isSuccess and result.isSuccess
                        out.addAll(result.out)
                        if (result.isSuccess) {
                            p.setDataBytes(dataType, sizeBytes)
                            p.setDisplayBytes(dataType, rootService.calculateSize(dst))
                        }
                    }
                }
            } else {
                isSuccess = false
                out.add(log { "Failed to get apk path of $packageName." })
            }
            t.updateInfo(dataType = dataType, state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = out.toLineString())
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    /**
     * Package data: USER, USER_DE, DATA, OBB, MEDIA
     */
    suspend fun backupData(p: PackageEntity, t: TaskDetailPackageEntity, r: PackageEntity?, dataType: DataType, dstDir: String): ShellResult = run {
        log { "Backing up ${dataType.type}..." }

        val packageName = p.packageName
        val userId = p.userId
        val ct = p.indexInfo.compressionType
        val dst = packageRepository.getArchiveDst(dstDir = dstDir, dataType = dataType, ct = ct)
        var isSuccess: Boolean
        val out = mutableListOf<String>()
        val srcDir = packageRepository.getDataSrcDir(dataType, userId)

        if (p.getDataSelected(dataType).not()) {
            isSuccess = true
            t.updateInfo(dataType = dataType, state = OperationState.SKIP)
        } else {
            // Check the existence of origin path.
            val src = packageRepository.getDataSrc(srcDir, packageName)
            rootService.exists(src).also {
                if (it.not()) {
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

            val sizeBytes = rootService.calculateSize(src)
            t.updateInfo(dataType = dataType, state = OperationState.PROCESSING, bytes = sizeBytes)
            if (rootService.exists(dst) && sizeBytes == r?.getDataBytes(dataType)) {
                isSuccess = true
                t.updateInfo(dataType = dataType, state = OperationState.SKIP)
                out.add(log { "Data has not changed." })
            } else {
                // Compress and test.
                Tar.compress(
                    exclusionList = exclusionList,
                    h = if (context.readFollowSymlinks().first()) "-h" else "",
                    srcDir = srcDir,
                    src = packageName,
                    dst = dst,
                    extra = ct.getCompressPara(context.readCompressionLevel().first())
                ).also { result ->
                    isSuccess = result.isSuccess
                    out.addAll(result.out)
                }
                commonBackupUtil.testArchive(src = dst, ct = ct).also { result ->
                    isSuccess = isSuccess and result.isSuccess
                    out.addAll(result.out)
                    if (result.isSuccess) {
                        p.setDataBytes(dataType, sizeBytes)
                        p.setDisplayBytes(dataType, rootService.calculateSize(dst))
                    }
                }
            }

            t.updateInfo(dataType = dataType, state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = out.toLineString())
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    suspend fun backupPermissions(p: PackageEntity) = run {
        log { "Backing up permissions..." }

        val packageName = p.packageName
        val userId = p.userId

        val packageInfo = rootService.getPackageInfoAsUser(packageName, PackageManager.GET_PERMISSIONS, userId)
        packageInfo?.apply {
            p.extraInfo.permissions = rootService.getPermissions(packageInfo = this)
        }
        val permissions = p.extraInfo.permissions
        log { "Permissions size: ${permissions.size}..." }
        permissions.forEach {
            log { "Permission name: ${it.name}, isGranted: ${it.isGranted}, op: ${it.op}, mode: ${it.mode}" }
        }
    }

    suspend fun backupSsaid(p: PackageEntity) = run {
        log { "Backing up ssaid..." }

        val packageName = p.packageName
        val uid = p.extraInfo.uid
        val userId = p.userId

        val ssaid = rootService.getPackageSsaidAsUser(packageName = packageName, uid = uid, userId = userId)
        log { "Ssaid: $ssaid" }
        p.extraInfo.ssaid = ssaid
    }

    suspend fun upload(client: CloudClient, p: PackageEntity, t: TaskDetailPackageEntity, dataType: DataType, srcDir: String, dstDir: String) = run {
        val ct = p.indexInfo.compressionType
        val src = packageRepository.getArchiveDst(dstDir = srcDir, dataType = dataType, ct = ct)
        t.updateInfo(dataType = dataType, state = OperationState.UPLOADING)

        var flag = true
        var progress = 0f
        with(CoroutineScope(coroutineContext)) {
            launch {
                while (flag) {
                    t.updateInfo(dataType = dataType, content = "${(progress * 100).toInt()}%")
                    delay(500)
                }
            }
        }

        cloudRepository.upload(client = client, src = src, dstDir = dstDir, onUploading = { read, total -> progress = read.toFloat() / total }).apply {
            flag = false
            t.updateInfo(dataType = dataType, state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = t.getLog(dataType) + "\n${outString}", content = "100%")
        }
    }
}
